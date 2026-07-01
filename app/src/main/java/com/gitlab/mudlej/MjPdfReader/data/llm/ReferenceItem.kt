package com.gitlab.mudlej.MjPdfReader.data.llm

data class ReferenceItem(
    val id: String,
    val label: String,
    val type: ReferenceItemType,
    val pageNumber: Int,
    val bbox: ReferenceBoundingBox,
)
