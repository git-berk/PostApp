package com.berco.postapp.data.repository

import com.berco.postapp.data.mapper.toDomain
import com.berco.postapp.data.remote.PostApi
import com.berco.postapp.domain.model.Post
import com.berco.postapp.domain.repository.PostRepository
import com.berco.postapp.domain.result.AppError
import com.berco.postapp.domain.result.AppResult
import com.berco.postapp.domain.util.DispatcherProvider
import com.squareup.moshi.JsonDataException
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class PostRepositoryImpl @Inject constructor(
    private val api: PostApi,
    private val dispatchers: DispatcherProvider,
) : PostRepository {

    // Retrofit, OkHttp and Moshi exceptions are translated here so that nothing
    // above this module ever has to know which HTTP client is in use.
    override suspend fun getPosts(): AppResult<List<Post>> = withContext(dispatchers.io) {
        try {
            AppResult.Success(api.getPosts().toDomain())
        } catch (e: IOException) {
            AppResult.Failure(AppError.Network)
        } catch (e: HttpException) {
            AppResult.Failure(AppError.Server(e.code()))
        } catch (e: JsonDataException) {
            AppResult.Failure(AppError.Serialization)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e))
        }
    }
}
