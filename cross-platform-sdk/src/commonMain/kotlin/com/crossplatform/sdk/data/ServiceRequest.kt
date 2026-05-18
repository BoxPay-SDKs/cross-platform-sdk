package com.crossplatform.sdk.data

import com.crossplatform.sdk.BuildKonfig
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.presentation.generateRandomAlphanumericString
import com.crossplatform.sdk.presentation.getEndpoint
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object ServiceRequest {

    private var _client: HttpClient? = null
    private var _plainClient: HttpClient? = null  // ✅ client without base URL


    // ✅ Reuse existing or build new
    fun getClient(): HttpClient {
        return _client ?: buildClient().also { _client = it }
    }

    // ✅ New plain client for different base URLs
    fun getPlainClient(): HttpClient {
        return _plainClient ?: buildPlainClient().also { _plainClient = it }
    }

    fun buildPlainClient(): HttpClient {
        _plainClient?.close()
        return HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(Logging) {
                level = LogLevel.BODY
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 30_000
                socketTimeoutMillis  = 60_000
            }
            HttpResponseValidator {
                handleResponseExceptionWithRequest { exception, _ ->
                    throw exception
                }
            }
        }.also { _plainClient = it }
    }

    // ✅ Force rebuild (call when token/env changes)
    fun buildClient(): HttpClient {
        _client?.close()
        val checkoutDetails = CheckoutDetailsHandler.checkoutDetails

        return HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(Logging) {
                level = LogLevel.BODY
            }
            install(DefaultRequest) {
                url("${getEndpoint(checkoutDetails.env)}${checkoutDetails.token}/")
                header("X-Request-Id", generateRandomAlphanumericString(10))
                header("X-Client-Connector-Name", "KMP SDK")
                header("X-Client-Connector-Version", BuildKonfig.SDK_VERSION)
                checkoutDetails.shopperToken?.let {
                    header("Authorization", "Session $it")
                }
            }
            HttpResponseValidator {
                handleResponseExceptionWithRequest { exception, _ ->
                    throw exception
                }
            }
        }.also { _client = it }
    }

    // ✅ Call when SDK is done
    fun close() {
        _client?.close()
        _client = null
    }
}