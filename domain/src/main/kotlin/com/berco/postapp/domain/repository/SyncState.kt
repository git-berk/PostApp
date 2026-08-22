package com.berco.postapp.domain.repository

import com.berco.postapp.domain.result.AppError

sealed interface SyncState {
    data object NotLoaded : SyncState
    data object Loading : SyncState
    data object Loaded : SyncState
    data class Failed(val error: AppError) : SyncState
}
