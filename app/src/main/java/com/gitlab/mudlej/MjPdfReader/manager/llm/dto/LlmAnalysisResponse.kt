package com.gitlab.mudlej.MjPdfReader.manager.llm.dto

// Wire format the model is instructed to emit. `page` is 1-indexed position within the
// analyzed window/chunk (page 1 = first image sent in that request), not an absolute PDF page.
data class LlmAnalysisResponse(
    val items: List<LlmItemDto> = emptyList(),
    val mentions: List<LlmMentionDto> = emptyList(),
)

data class LlmItemDto(
    val label: String,
    val type: String,
    val page: Int,
    val bbox: List<Float>,
)

data class LlmMentionDto(
    val text: String,
    val page: Int,
    val refersToLabel: String,
)
