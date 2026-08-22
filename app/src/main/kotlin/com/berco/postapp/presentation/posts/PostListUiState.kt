package com.berco.postapp.presentation.posts

import com.berco.postapp.domain.model.Post
import com.berco.postapp.domain.result.AppError

sealed interface PostListUiState {
    data object Loading : PostListUiState
    data object Empty : PostListUiState
    data class Content(val posts: List<Post>) : PostListUiState
    data class Error(val error: AppError) : PostListUiState
}
