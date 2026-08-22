package com.berco.postapp.presentation.detail

import com.berco.postapp.domain.model.Post
sealed interface PostDetailUiState {
    data object Loading : PostDetailUiState
    data object Deleted : PostDetailUiState
    data class Content(val post: Post, val edit: EditUiState) : PostDetailUiState
}
