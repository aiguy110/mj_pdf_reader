package com.gitlab.mudlej.MjPdfReader.data.llm

data class ReferenceMention(
    val mentionPageNumber: Int,
    val textSnippet: String,
    val charStart: Int,
    val charEnd: Int,
    val resolvedItemId: String?,
)
