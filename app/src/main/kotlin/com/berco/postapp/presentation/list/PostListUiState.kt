package com.berco.postapp.presentation.list

import com.berco.postapp.domain.model.Post
import com.berco.postapp.domain.result.AppError

sealed interface PostListUiState {
    data object Loading : PostListUiState
    data object Empty : PostListUiState
    data class Error(val error: AppError) : PostListUiState

    // A failed refresh that still has posts to show keeps them on screen and
    // reports the failure transiently; the full-screen Error is reserved for
    // having nothing to display.
    data class Content(
        val posts: List<Post>,
        val refreshError: AppError? = null,
    ) : PostListUiState
}
