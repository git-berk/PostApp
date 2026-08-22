package com.berco.postapp.presentation.detail

enum class EditorTarget { NONE, TITLE, BODY }

data class EditUiState(
    val target: EditorTarget = EditorTarget.NONE,
    val draft: String = "",
) {
    val isEditingTitle: Boolean get() = target == EditorTarget.TITLE
    val isEditingBody: Boolean get() = target == EditorTarget.BODY
}
