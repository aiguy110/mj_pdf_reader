package com.gitlab.mudlej.MjPdfReader.manager.extractor

import android.graphics.Bitmap
import android.graphics.RectF
import com.gitlab.mudlej.MjPdfReader.data.Bookmark
import com.gitlab.mudlej.MjPdfReader.data.Link
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.util.SizeF

interface PdfExtractor {

    fun getPageText(pageNumber: Int): String

    fun getPageCount(): Int

    fun getPageLinks(pageNumber: Int): List<PdfDocument.Link>

    fun getAllBookmarks(): List<Bookmark>

    fun getAllLinks(): List<Link>

    // pageNumber is 1-indexed, matching getPageText's existing convention.
    fun renderPageBitmap(pageNumber: Int, targetWidth: Int): Bitmap?

    // pageNumber is 1-indexed. Returns page-space (PDF point unit) rects for the [start, end) char range.
    fun getTextBounds(pageNumber: Int, start: Int, end: Int): List<RectF>

    // pageNumber is 1-indexed. Returns page size in PDF point units (same space PdfDocument.Link.bounds uses).
    fun getPageSizePoints(pageNumber: Int): SizeF
}