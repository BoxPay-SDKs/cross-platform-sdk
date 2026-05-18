package com.crossplatform.sdk.data

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CancellationException

sealed class ApiResponse<out T> {
    data object Loading : ApiResponse<Nothing>()

    data class Success<T>(
        val data: T,
        val responseCode: Int
    ) : ApiResponse<T>()

    data class Error(
        val message: String,
        val errorBody: String? = null,
        val exception: Throwable? = null
    ) : ApiResponse<Nothing>()
}

/**
 * Executes a network call and wraps the result in [ApiResponse].
 * Marked as inline/reified so Ktor can deserialize the body into type T.
 */
suspend inline fun <reified T> executeWithResponse(
    apiCall: () -> HttpResponse
): ApiResponse<T> {
    return try {
        val response = apiCall()

        if (response.status.isSuccess()) {
            ApiResponse.Success(
                data = response.body<T>(), // reified T allows this to work
                responseCode = response.status.value
            )
        } else {
            ApiResponse.Error(
                message = "Status: ${response.status.description}",
                errorBody = response.bodyAsText()
            )
        }
    } catch (e: CancellationException) {
        // Must rethrow so the coroutine scope can actually cancel
        throw e
    } catch (e: IOException) {
        ApiResponse.Error(
            message = "Network error: Please check your internet connection.",
            exception = e
        )
    } catch (e: Exception) {
        ApiResponse.Error(
            message = e.message ?: "An unexpected error occurred",
            exception = e
        )
    }
}