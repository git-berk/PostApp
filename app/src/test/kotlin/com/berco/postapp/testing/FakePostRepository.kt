package com.berco.postapp.testing

import com.berco.postapp.domain.model.Post
import com.berco.postapp.domain.model.withUpdatedBody
import com.berco.postapp.domain.model.withUpdatedTitle
import com.berco.postapp.domain.model.withoutPost
import com.berco.postapp.domain.repository.PostRepository
import com.berco.postapp.domain.repository.SyncState
import com.berco.postapp.domain.result.AppError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate

// Mutations run the real domain transforms so ViewModel tests exercise the actual
// behavior; only the network side is faked.
class FakePostRepository : PostRepository {

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    override val posts: Flow<List<Post>> = _posts.asStateFlow()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.NotLoaded)
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    var refreshCount = 0
        private set

    private var onRefresh: () -> Unit = { setLoaded(emptyList()) }

    fun refreshReturns(posts: List<Post>) {
        onRefresh = { setLoaded(posts) }
    }

    // Leaves syncState at Loading, modelling a fetch that has not come back yet.
    fun refreshNeverCompletes() {
        onRefresh = { }
    }

    fun refreshFails(error: AppError = AppError.Network) {
        onRefresh = { _syncState.value = SyncState.Failed(error) }
    }

    // Puts the repository straight into a loaded state, for tests that are not
    // about the fetch itself.
    fun setLoaded(posts: List<Post>) {
        _posts.value = posts
        _syncState.value = SyncState.Loaded
    }

    override suspend fun refreshIfNeeded() {
        if (_syncState.value is SyncState.NotLoaded) refresh()
    }

    override suspend fun refresh() {
        refreshCount++
        _syncState.value = SyncState.Loading
        onRefresh()
    }

    override fun updateTitle(id: Int, title: String): Boolean =
        mutate { it.withUpdatedTitle(id, title) }

    override fun updateBody(id: Int, body: String): Boolean =
        mutate { it.withUpdatedBody(id, body) }

    override fun delete(id: Int): Boolean = mutate { it.withoutPost(id) }

    private fun mutate(transform: (List<Post>) -> List<Post>): Boolean {
        val previous = _posts.getAndUpdate(transform)
        return previous !== _posts.value
    }
}

fun post(
    id: Int,
    title: String = "Title $id",
    body: String = "Body $id",
    userId: Int = 1,
) = Post(id = id, userId = userId, title = title, body = body)

fun posts(count: Int): List<Post> = (1..count).map { post(it) }
