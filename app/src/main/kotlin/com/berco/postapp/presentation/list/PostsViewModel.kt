package com.berco.postapp.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.berco.postapp.domain.model.Post
import com.berco.postapp.domain.repository.PostRepository
import com.berco.postapp.domain.repository.SyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

@HiltViewModel
class PostsViewModel @Inject constructor(
    private val repository: PostRepository,
) : ViewModel() {

    val uiState: StateFlow<PostListUiState> =
        combine(repository.posts, repository.syncState, ::toPostListUiState)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = PostListUiState.Loading,
            )

    init {
        viewModelScope.launch { repository.refreshIfNeeded() }
    }
    fun refresh() =
        viewModelScope.launch { repository.refresh() }

    fun updateTitle(id: Int, title: String): Boolean = repository.updateTitle(id, title)

    fun updateBody(id: Int, body: String): Boolean = repository.updateBody(id, body)

    fun delete(id: Int) {
        repository.delete(id)
    }

}
internal fun toPostListUiState(posts: List<Post>, sync: SyncState): PostListUiState = when {
    posts.isNotEmpty() -> PostListUiState.Content(
        posts = posts,
        refreshError = (sync as? SyncState.Failed)?.error,
    )

    sync is SyncState.Failed -> PostListUiState.Error(sync.error)
    sync is SyncState.Loaded -> PostListUiState.Empty
    else -> PostListUiState.Loading
}
