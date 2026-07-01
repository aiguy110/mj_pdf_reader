package com.gitlab.mudlej.MjPdfReader.manager.llm

import android.graphics.Bitmap
import android.util.Base64
import com.gitlab.mudlej.MjPdfReader.data.llm.PageAnalysisResult
import com.gitlab.mudlej.MjPdfReader.data.llm.ReferenceBoundingBox
import com.gitlab.mudlej.MjPdfReader.data.llm.ReferenceItem
import com.gitlab.mudlej.MjPdfReader.data.llm.ReferenceItemType
import com.gitlab.mudlej.MjPdfReader.data.llm.ReferenceMention
import com.gitlab.mudlej.MjPdfReader.manager.extractor.PdfExtractor
import com.gitlab.mudlej.MjPdfReader.manager.llm.dto.LlmAnalysisResponse
import com.gitlab.mudlej.MjPdfReader.manager.llm.dto.LlmMentionDto
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class ReferenceAnalyzer(private val pdfExtractor: PdfExtractor) {

    companion object {
        const val DEFAULT_WINDOW_RADIUS = 1
        const val DOCUMENT_CHUNK_SIZE = 3
        const val DOCUMENT_CONCURRENCY = 3
        const val RENDER_TARGET_WIDTH = 1024

        private const val SYSTEM_PROMPT = """
You are analyzing pages from a PDF document (likely an academic paper). You are given, for each
page in this request, the page's extracted text followed by a rendered image of that page.

Identify:
1. Equations and figures on these pages, each with a visible label/number (e.g. "Equation 3",
   "Figure 2a"). Ignore unlabeled decorative images.
2. In-text mentions elsewhere on these pages that reference one of those labels (e.g. "as shown in
   Eq. 3", "see Fig. 2a", "Figure 2 illustrates").

Respond with ONLY a JSON object (no markdown fences, no commentary) matching exactly this shape:
{
  "items": [ { "label": string, "type": "equation" | "figure", "page": number, "bbox": [left, top, right, bottom] } ],
  "mentions": [ { "text": string, "page": number, "refersToLabel": string } ]
}

Rules:
- "page" is the 1-indexed position of the page within THIS request (page 1 = the first page's
  text/image sent above), not any absolute page number from the document itself.
- "bbox" values are normalized to the range [0,1], relative to that page's rendered image
  (left/top = 0,0 at the top-left corner, right/bottom = 1,1 at the bottom-right corner).
- "text" must be an exact substring of that page's provided text, the specific phrase that
  mentions the reference (e.g. "Eq. 3", not the whole sentence).
- "refersToLabel" must exactly match one of the "label" values in "items".
- If there are no items or no mentions, return empty arrays for them.
"""
    }

    private val client = OpenRouterClient()
    private val gson = Gson()

    suspend fun analyze(apiKey: String, centerPage: Int): PageAnalysisResult {
        val pageCount = pdfExtractor.getPageCount()
        val windowStart = (centerPage - DEFAULT_WINDOW_RADIUS).coerceAtLeast(0)
        val windowEnd = (centerPage + DEFAULT_WINDOW_RADIUS).coerceAtMost(pageCount - 1)
        val (items, rawMentions) = fetchChunkRaw(apiKey, windowStart, windowEnd)
        return PageAnalysisResult(items, resolveMentions(rawMentions, items))
    }

    fun windowKeyFor(centerPage: Int, pageCount: Int): String {
        val windowStart = (centerPage - DEFAULT_WINDOW_RADIUS).coerceAtLeast(0)
        val windowEnd = (centerPage + DEFAULT_WINDOW_RADIUS).coerceAtMost(pageCount - 1)
        return "$windowStart-$windowEnd"
    }

    suspend fun analyzeDocument(apiKey: String, onProgress: (done: Int, total: Int) -> Unit): PageAnalysisResult {
        val pageCount = pdfExtractor.getPageCount()
        val chunkRanges = (0 until pageCount step DOCUMENT_CHUNK_SIZE).map { start ->
            start to (start + DOCUMENT_CHUNK_SIZE - 1).coerceAtMost(pageCount - 1)
        }

        val allItems = mutableListOf<ReferenceItem>()
        val allRawMentions = mutableListOf<Pair<Int, LlmMentionDto>>()
        var completed = 0
        val progressLock = Any()
        val semaphore = Semaphore(DOCUMENT_CONCURRENCY)

        coroutineScope {
            chunkRanges.map { (start, end) ->
                async {
                    semaphore.withPermit {
                        val (items, rawMentions) = fetchChunkRaw(apiKey, start, end)
                        synchronized(progressLock) {
                            allItems.addAll(items)
                            allRawMentions.addAll(rawMentions)
                            completed++
                            onProgress(completed, chunkRanges.size)
                        }
                    }
                }
            }.awaitAll()
        }

        return PageAnalysisResult(allItems, resolveMentions(allRawMentions, allItems))
    }

    // Returns parsed items (absolute page numbers) and raw mention DTOs paired with their absolute mention page,
    // without resolving refersToLabel yet -- resolution happens after all chunks of a run are collected, so
    // document-mode mentions can resolve against items extracted from any other chunk.
    private suspend fun fetchChunkRaw(
        apiKey: String,
        windowStart: Int,
        windowEnd: Int,
    ): Pair<List<ReferenceItem>, List<Pair<Int, LlmMentionDto>>> = withContext(Dispatchers.IO) {
        val pageBlocks = (windowStart..windowEnd).map { absolutePage ->
            val pageNumber = absolutePage + 1 // PdfExtractor's page-taking methods are 1-indexed
            val bitmap = pdfExtractor.renderPageBitmap(pageNumber, RENDER_TARGET_WIDTH)
            val base64 = bitmap?.let { encodeJpegBase64(it) }.orEmpty()
            PageBlock(pageText = pdfExtractor.getPageText(pageNumber), imageBase64Jpeg = base64)
        }

        val rawContent = client.requestAnalysis(apiKey, SYSTEM_PROMPT, pageBlocks)
        val parsed = parseResponse(rawContent)

        val items = parsed.items.mapIndexedNotNull { index, dto ->
            val type = when (dto.type.trim().lowercase()) {
                "equation" -> ReferenceItemType.EQUATION
                "figure" -> ReferenceItemType.FIGURE
                else -> null
            } ?: return@mapIndexedNotNull null
            if (dto.bbox.size != 4) return@mapIndexedNotNull null
            val absolutePageNumber = windowStart + (dto.page - 1)
            ReferenceItem(
                id = "p$absolutePageNumber-${type.name.lowercase()}-$index",
                label = dto.label,
                type = type,
                pageNumber = absolutePageNumber,
                bbox = ReferenceBoundingBox(dto.bbox[0], dto.bbox[1], dto.bbox[2], dto.bbox[3]),
            )
        }

        val rawMentions = parsed.mentions.map { dto -> (windowStart + (dto.page - 1)) to dto }
        items to rawMentions
    }

    private fun resolveMentions(
        rawMentions: List<Pair<Int, LlmMentionDto>>,
        items: List<ReferenceItem>,
    ): List<ReferenceMention> {
        return rawMentions.mapNotNull { (mentionPageNumber, dto) ->
            val pageText = pdfExtractor.getPageText(mentionPageNumber + 1)
            val charStart = pageText.indexOf(dto.text)
            if (charStart < 0) return@mapNotNull null
            val resolvedItemId = items.firstOrNull {
                it.label.trim().equals(dto.refersToLabel.trim(), ignoreCase = true)
            }?.id
            ReferenceMention(
                mentionPageNumber = mentionPageNumber,
                textSnippet = dto.text,
                charStart = charStart,
                charEnd = charStart + dto.text.length,
                resolvedItemId = resolvedItemId,
            )
        }
    }

    private fun parseResponse(rawContent: String): LlmAnalysisResponse {
        val stripped = rawContent.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        return gson.fromJson(stripped, LlmAnalysisResponse::class.java)
    }

    private fun encodeJpegBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }
}
