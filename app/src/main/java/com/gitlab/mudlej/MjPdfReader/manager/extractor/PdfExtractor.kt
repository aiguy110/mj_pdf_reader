package com.gitlab.mudlej.MjPdfReader.manager.extractor

import com.gitlab.mudlej.MjPdfReader.data.Bookmark
import com.shockwave.pdfium.PdfDocument

interface PdfExtractor {

    fun getPageText(pageNumber: Int): String

    fun getPageCount(): Int

    fun getPageLinks(pageNumber: Int): List<PdfDocument.Link>

    fun getBookmarks(pageNumber: Int): List<Bookmark>
}