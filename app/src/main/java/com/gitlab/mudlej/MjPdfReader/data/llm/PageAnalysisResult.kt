package com.gitlab.mudlej.MjPdfReader.data.llm

data class PageAnalysisResult(
    val items: List<ReferenceItem>,
    val mentions: List<ReferenceMention>,
)
