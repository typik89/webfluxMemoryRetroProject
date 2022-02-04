package ru.typik.reactor

import java.io.ByteArrayOutputStream
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ByteArrayResource
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Mono


@Table("csv")
data class CsvRow(
        @Id
        val id: Long,
        val column1: String,
        val column2: String,
        val column3: String,
)

interface CsvRepository : ReactiveCrudRepository<CsvRow, Long>

@Service
class CsvService(val csvRepository: CsvRepository) {
    fun createByteArray(): Mono<ByteArray> =
            csvRepository.findAll()
                    .reduce(ByteArrayOutputStream()) { output, el ->
                        output.write(el.toString().toByteArray())
                        output.write("\n".toByteArray())
                        output
                    }
                    .map { it.toByteArray() }
}

@SpringBootApplication
class WebfluxMemoryRetroProjectApplication(val csvService: CsvService) {

    @Bean
    fun routing(): RouterFunction<ServerResponse> = router {
        accept(MediaType.ALL).nest {
            GET("/test") {
                csvService.createByteArray()
                        .flatMap { result ->
                            ServerResponse.ok()
                                    .headers { httpHeaders ->
                                        httpHeaders.contentType = MediaType("application", "force-download")
                                        httpHeaders.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=test.txt")
                                    }
                                    .bodyValue(ByteArrayResource(result))
                        }
            }
        }
    }
}

fun main(args: Array<String>) {
    runApplication<WebfluxMemoryRetroProjectApplication>(*args)
}