package com.gitlab.mudlej.MjPdfReader.manager.extractor

import android.graphics.Bitmap
import android.graphics.RectF
import com.gitlab.mudlej.MjPdfReader.data.Bookmark
import com.gitlab.mudlej.MjPdfReader.data.Link
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import com.shockwave.pdfium.util.SizeF

class PdfExtractorImpl(
    private val pdfiumCore: PdfiumCore,
    private val pdfDocument: PdfDocument
) : PdfExtractor {

    override fun getPageText(pageNumber: Int): String {
        val index = getIndex(pageNumber) ?: return ""
        pdfiumCore.openPage(pdfDocument, index)

        return try {
            pdfiumCore.getPageText(pdfDocument, index)
        }
        catch (throwable: Throwable) {
            throwable.printStackTrace()
            "";
        }
    }

    override fun getPageCount() = pdfiumCore.getPageCount(pdfDocument)

    override fun getPageLinks(pageNumber: Int): List<PdfDocument.Link> {
        try {
            pdfiumCore.openPage(pdfDocument, pageNumber)
        }
        catch (throwable: Throwable) {
            throwable.printStackTrace()
            return listOf()
        }
        return pdfiumCore.getPageLinks(pdfDocument, pageNumber).filter { it.uri != null }
    }

    override fun getAllBookmarks(): List<Bookmark> {
        val tableOfContents = pdfiumCore.getTableOfContents(pdfDocument)
        return tableOfContents.map { bookmark -> Bookmark(bookmark, level = 0) }
    }

    override fun getAllLinks(): List<Link> {
        val links = mutableListOf<Link>()
        for (i in 0 until getPageCount()) {
            val pageLinks = getPageLinks(i)
            for (link in pageLinks) {
                if (link.uri.isNullOrEmpty() || link.uri.isBlank()) {
                    continue
                }
                links.add(Link(
                    text = "",      // couldn't be extracted yet
                    url = link.uri,
                    pageNumber = i + 1
                ))
            }
        }
        return links
    }

    override fun renderPageBitmap(pageNumber: Int, targetWidth: Int): Bitmap? {
        val index = getIndex(pageNumber) ?: return null
        return try {
            pdfiumCore.openPage(pdfDocument, index)
            val widthPt = pdfiumCore.getPageWidthPoint(pdfDocument, index)
            val heightPt = pdfiumCore.getPageHeightPoint(pdfDocument, index)
            if (widthPt <= 0 || heightPt <= 0) return null
            val targetHeight = (targetWidth.toFloat() * heightPt / widthPt).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            pdfiumCore.renderPageBitmap(pdfDocument, bitmap, index, 0, 0, targetWidth, targetHeight, true)
            bitmap
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
            null
        }
    }

    override fun getTextBounds(pageNumber: Int, start: Int, end: Int): List<RectF> {
        val index = getIndex(pageNumber) ?: return emptyList()
        return try {
            pdfiumCore.openPage(pdfDocument, index)
            pdfiumCore.getPageTextBounds(pdfDocument, index, start, end).map { rect -> RectF(rect) }
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
            emptyList()
        }
    }

    override fun getPageSizePoints(pageNumber: Int): SizeF {
        val index = getIndex(pageNumber) ?: return SizeF(0f, 0f)
        return try {
            pdfiumCore.openPage(pdfDocument, index)
            SizeF(
                pdfiumCore.getPageWidthPoint(pdfDocument, index).toFloat(),
                pdfiumCore.getPageHeightPoint(pdfDocument, index).toFloat()
            )
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
            SizeF(0f, 0f)
        }
    }

    private fun getIndex(pageNumber: Int): Int? {
        return if (pageNumber < 1) null else pageNumber - 1
    }
}