package com.berco.postapp.data.repository

import com.berco.postapp.data.mapper.toDomain
import com.berco.postapp.data.remote.PostApi
import com.berco.postapp.domain.model.Post
import com.berco.postapp.domain.model.withUpdatedBody
import com.berco.postapp.domain.model.withUpdatedTitle
import com.berco.postapp.domain.model.withoutPost
import com.berco.postapp.domain.repository.PostRepository
import com.berco.postapp.domain.repository.SyncState
import com.berco.postapp.domain.result.AppError
import com.berco.postapp.domain.util.DispatcherProvider
import com.squareup.moshi.JsonDataException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class PostRepositoryImpl @Inject constructor(
    private val api: PostApi,
    private val dispatchers: DispatcherProvider,
) : PostRepository {

    private val cache = MutableStateFlow<List<Post>>(emptyList())
    override val posts: Flow<List<Post>> = cache.asStateFlow() // Replaceable with a Room Flow<List<Post>> later if needed.

    private val _syncState = MutableStateFlow<SyncState>(SyncState.NotLoaded)
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val refreshLock = Mutex()

    override suspend fun refreshIfNeeded() {
        if (_syncState.value is SyncState.NotLoaded) refresh()
    }

    override suspend fun refresh() = withContext(dispatchers.io) {
        if (!refreshLock.tryLock()) return@withContext
        try {
            runRefresh()
        } finally {
            refreshLock.unlock()
        }
    }

    private suspend fun runRefresh() {
        val previous = _syncState.value

        try {
            _syncState.value = SyncState.Loading
            cache.value = api.getPosts().toDomain()
            _syncState.value = SyncState.Loaded
        } catch (e: CancellationException) {
            _syncState.value = previous
            throw e
        } catch (e: IOException) {
            _syncState.value = SyncState.Failed(AppError.Network)
        } catch (e: HttpException) {
            _syncState.value = SyncState.Failed(AppError.Server(e.code()))
        } catch (e: JsonDataException) {
            _syncState.value = SyncState.Failed(AppError.Serialization)
        } catch (e: Exception) {
            _syncState.value = SyncState.Failed(AppError.Unknown(e))
        }
    }

    override fun updateTitle(id: Int, title: String): Boolean =
        mutate { it.withUpdatedTitle(id, title) }

    override fun updateBody(id: Int, body: String): Boolean =
        mutate { it.withUpdatedBody(id, body) }

    override fun delete(id: Int): Boolean = mutate { it.withoutPost(id) }

    private fun mutate(transform: (List<Post>) -> List<Post>): Boolean {
        val previous = cache.getAndUpdate(transform)
        return previous !== cache.value
    }
}
