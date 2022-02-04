package ru.typik.reactor

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest
class MemoryLeakTest {

    companion object {
        val logger: Logger = LoggerFactory.getLogger("test")

        @Container
        val postgres = PostgreSQLContainer<Nothing>("postgres:13-alpine")
                .apply {
                    withDatabaseName("tpp")
                    withUsername("postgres")
                    withPassword("postgres")
                    withInitScript("init.sql")
                    portBindings.add("5432:5432")
                }
    }

    @Autowired
    private lateinit var csvService: CsvService

    @Test
    fun test() {
        while (true) {
            val cdl = CountDownLatch(1)

            csvService.createByteArray()
                    .doOnNext { logger.info("ByteArray with size ${it.size} was created") }
                    .subscribe { cdl.countDown() }

            assertTrue(cdl.await(30, TimeUnit.SECONDS))
        }
    }
}