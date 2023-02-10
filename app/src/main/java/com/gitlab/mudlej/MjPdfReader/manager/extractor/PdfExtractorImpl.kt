package com.gitlab.mudlej.MjPdfReader.manager.extractor

import android.app.Activity
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.gitlab.mudlej.MjPdfReader.data.Bookmark
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore

class PdfExtractorImpl(
    private val pdfiumCore: PdfiumCore,
    private val pdfDocument: PdfDocument
) : PdfExtractor {

    override fun getPageText(pageNumber: Int): String {
        val index = getIndex(pageNumber) ?: return ""
        pdfiumCore.openPage(pdfDocument, index)
        return pdfiumCore.getPageText(pdfDocument, index)
    }

    override fun getPageCount() = pdfiumCore.getPageCount(pdfDocument)

    override fun getPageLinks(pageNumber: Int): List<PdfDocument.Link> {
        val index = getIndex(pageNumber) ?: return listOf()
        pdfiumCore.openPage(pdfDocument, index)
        return pdfiumCore.getPageLinks(pdfDocument, pageNumber)
    }

    override fun getBookmarks(pageNumber: Int): List<Bookmark> {
        val tableOfContents = pdfiumCore.getTableOfContents(pdfDocument)
        return tableOfContents.map { bookmark -> Bookmark(bookmark, level = 0) }
    }

    private fun getIndex(pageNumber: Int): Int? {
        return if (pageNumber < 1) null else pageNumber - 1
    }
}