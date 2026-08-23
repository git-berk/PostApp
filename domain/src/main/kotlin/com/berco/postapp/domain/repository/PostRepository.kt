package com.berco.postapp.domain.repository

import com.berco.postapp.domain.model.Post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

// The data layer is the source of truth: both screens observe `posts`.
interface PostRepository {
    val posts: Flow<List<Post>>

    val syncState: StateFlow<SyncState>

    suspend fun refresh()

    suspend fun refreshIfNeeded()

    fun updateTitle(id: Int, title: String): Boolean

    fun updateBody(id: Int, body: String): Boolean

    fun delete(id: Int): Boolean
}
