package com.gitlab.mudlej.MjPdfReader.data.llm

// Normalized [0,1] coordinates relative to the rendered page image sent to the LLM.
data class ReferenceBoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)
