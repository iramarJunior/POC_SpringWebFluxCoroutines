package com.iramarfalcao.pocspringwebfluxcoroutines

import com.iramarfalcao.pocspringwebfluxcoroutines.models.Banner
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.server.*

@SpringBootApplication
class CoroutinesApplication {

    @FlowPreview
    @Bean
    fun routes(handlers: Handlers) = coRouter {
//        GET("/", handlers::index)
        GET("/suspend", handlers::suspending)
        GET("/sequential-flow", handlers::sequentialFlow)
        GET("/concurrent-flow", handlers::concurrentFlow)
        GET("/error", handlers::error)
    }
}

fun main(args: Array<String>) {
    runApplication<CoroutinesApplication>(*args)
}

@Suppress("DuplicatedCode")
@Component
class Handlers(builder: WebClient.Builder) {

    private val banner = Banner("title", "Lorem ipsum dolor sit amet, consectetur adipiscing elit.")
    private val client = builder.baseUrl("http://localhost:8080").build()

//    suspend fun index(request: ServerRequest) =
//        ServerResponse
//            .ok()
//            .renderAndAwait("index", mapOf("banner" to banner))

    suspend fun suspending(request: ServerRequest) =
        ServerResponse
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValueAndAwait(banner)

    suspend fun sequentialFlow(request: ServerRequest) = flow {
        for (i in 1..4) {
            emit(client
                .get()
                .uri("/suspend")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .awaitBody<Banner>())
        }
    }.let {
        ServerResponse
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyAndAwait(it)
    }

    @FlowPreview
    suspend fun concurrentFlow(request: ServerRequest): ServerResponse = flow {
        for (i in 1..4) emit("/suspend")
    }.flatMapMerge {
        flow {
            emit(client
                .get()
                .uri(it)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .awaitBody<Banner>())
        }
    }.let {
        ServerResponse
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyAndAwait(it)
    }

    suspend fun error(request: ServerRequest): ServerResponse {
        throw IllegalStateException()
    }
}
