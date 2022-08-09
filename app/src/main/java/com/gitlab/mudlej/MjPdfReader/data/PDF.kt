package com.gitlab.mudlej.MjPdfReader.data

import android.net.Uri

class PDF(
    var name: String = "",
    var password: String? = null,
    var pageNumber: Int = -1,
    var length: Int = -1,
    var uri: Uri? = null,
    var zoom: Float = 1F,
    var isPortrait: Boolean = true,
    var isFullScreenToggled: Boolean = false,
    var contentHash: String? = null,
) {

    companion object {
        // constants
        const val FILE_TYPE = "application/pdf"
        const val HASH_SIZE = 1024 * 1024

        // keys
        const val nameKey = "name"
        const val passwordKey = "password"
        const val pageNumberKey = "pageNumber"
        const val lengthKey = "length"
        const val uriKey = "uri"
        const val zoomKey = "zoom"
        const val isPortraitKey = "isPortrait"
        const val isFullScreenToggledKey = "isFullScreenToggled"
    }

    fun getTitle(): String {
        // get .pdf start index (the dot)
        val extensionIndex: Int =
            if (name.lastIndexOf('.') == -1) name.length else name.lastIndexOf('.')

        return String.format(
            "[%s/%s] %s", pageNumber + 1, length, name.substring(0, extensionIndex))
    }
    fun togglePortrait() { isPortrait = !isPortrait }

    fun setPageCount(count: Int) {
        if (count == length || count < 1) return
        length = count
    }
}