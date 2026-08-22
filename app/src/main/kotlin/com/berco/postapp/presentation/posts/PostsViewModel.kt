package com.berco.postapp.presentation.posts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.berco.postapp.domain.model.Post
import com.berco.postapp.domain.model.findById
import com.berco.postapp.domain.model.withUpdatedBody
import com.berco.postapp.domain.model.withUpdatedTitle
import com.berco.postapp.domain.model.withoutPost
import com.berco.postapp.domain.result.fold
import com.berco.postapp.domain.usecase.GetPostsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

// Scoped to the nav graph so the list and the detail screen read and write one
// list. Edits made in detail are therefore already visible when the list resumes,
// with no result plumbing between the two destinations.
@HiltViewModel
class PostsViewModel @Inject constructor(
    private val getPosts: GetPostsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PostListUiState>(PostListUiState.Loading)
    val uiState: StateFlow<PostListUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }
    fun load() {
        loadJob?.cancel()
        _uiState.value = PostListUiState.Loading
        loadJob = viewModelScope.launch {
            _uiState.value = getPosts().fold(
                onSuccess = { posts ->
                    if (posts.isEmpty()) PostListUiState.Empty else PostListUiState.Content(posts)
                },
                onFailure = { PostListUiState.Error(it) },
            )
        }
    }

    fun post(id: Int) = uiState
        .map { it.postsOrNull()?.findById(id) }
        .distinctUntilChanged()

    fun currentPost(id: Int): Post? = uiState.value.postsOrNull()?.findById(id)

    fun updateTitle(id: Int, title: String): Boolean = mutate { it.withUpdatedTitle(id, title) }

    fun updateBody(id: Int, body: String): Boolean = mutate { it.withUpdatedBody(id, body) }

    fun delete(id: Int): Boolean = mutate { it.withoutPost(id) }

    private fun mutate(transform: (List<Post>) -> List<Post>): Boolean {
        val previous = _uiState.getAndUpdate { state ->
            val current = state as? PostListUiState.Content ?: return@getAndUpdate state
            val updated = transform(current.posts)
            when {
                updated === current.posts -> state
                updated.isEmpty() -> PostListUiState.Empty
                else -> PostListUiState.Content(updated)
            }
        }
        return previous !== _uiState.value
    }
}

private fun PostListUiState.postsOrNull(): List<Post>? = (this as? PostListUiState.Content)?.posts
