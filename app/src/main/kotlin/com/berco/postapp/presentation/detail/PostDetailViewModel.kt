package com.berco.postapp.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val postId: Int = PostDetailFragmentArgs.fromSavedStateHandle(savedStateHandle).postId

    private val _editState = MutableStateFlow(EditUiState())
    val editState: StateFlow<EditUiState> = _editState.asStateFlow()

    fun startEditing(target: EditorTarget, currentValue: String) {
        _editState.value = EditUiState(target = target, draft = currentValue)
    }

    fun onDraftChanged(value: String) {
        _editState.update { it.copy(draft = value) }
    }

    fun cancelEditing() {
        _editState.value = EditUiState()
    }

    fun commitEdit(): String? = _editState
        .getAndUpdate { EditUiState() }
        .draft
        .trim()
        .takeIf { it.isNotEmpty() }
}
