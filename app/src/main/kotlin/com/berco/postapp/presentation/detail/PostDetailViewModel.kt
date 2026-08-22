package com.berco.postapp.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.berco.postapp.domain.model.findById
import com.berco.postapp.domain.repository.PostRepository
import com.berco.postapp.domain.repository.SyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val repository: PostRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val postId: Int = PostDetailFragmentArgs.fromSavedStateHandle(savedStateHandle).postId

    private val editState = MutableStateFlow(EditUiState())

    val uiState: StateFlow<PostDetailUiState> = combine(
        repository.posts,
        repository.syncState,
        editState,
    ) { posts, sync, edit ->
        val post = posts.findById(postId)
        when {
            post != null -> PostDetailUiState.Content(post, edit)
            sync is SyncState.Loaded -> PostDetailUiState.Deleted
            else -> PostDetailUiState.Loading
        }
    }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = PostDetailUiState.Loading,
        )

    init {
        // This screen can be the one restored after process death, with the list
        // fragment never created, so it cannot rely on the list to load the data.
        viewModelScope.launch { repository.refreshIfNeeded() }
    }

    fun startEditing(target: EditorTarget, currentValue: String) {
        editState.value = EditUiState(target = target, draft = currentValue)
    }

    fun onDraftChanged(value: String) {
        editState.update { it.copy(draft = value) }
    }

    fun cancelEditing() {
        editState.value = EditUiState()
    }

    fun commitEdit(): Boolean {
        val committed = editState.getAndUpdate { EditUiState() }
        val draft = committed.draft.trim()
        if (draft.isEmpty()) return false

        return when (committed.target) {
            EditorTarget.TITLE -> repository.updateTitle(postId, draft)
            EditorTarget.BODY -> repository.updateBody(postId, draft)
            EditorTarget.NONE -> false
        }
    }
}
