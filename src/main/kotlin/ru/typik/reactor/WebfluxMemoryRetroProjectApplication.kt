package ru.typik.reactor

import com.fasterxml.jackson.dataformat.csv.CsvMapper
import io.netty.util.ReferenceCountUtil
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
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router


@Table("csv")
data class CsvRow(
    @Id
    val id: Long,
    val column1: String,
    val column2: String,
    val column3: String,
)

interface CsvRepository: ReactiveCrudRepository<CsvRow, Long>

@SpringBootApplication
class WebfluxMemoryRetroProjectApplication( val csvRepository: CsvRepository){

    @Bean
    fun routing(): RouterFunction<ServerResponse> = router {
        accept(MediaType.ALL).nest {
            GET("/test") {
                csvRepository.findAll()
                    .reduce(ByteArrayOutputStream()) { output, el ->
                        output.write(el.toString().toByteArray())
                        output.write("\n".toByteArray())
                        output
                    }
                    .map { it.toByteArray() }
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
