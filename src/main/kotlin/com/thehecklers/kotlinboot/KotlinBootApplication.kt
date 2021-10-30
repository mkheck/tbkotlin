package com.thehecklers.kotlinboot

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.TEXT_EVENT_STREAM
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Flux
import java.time.Duration

@SpringBootApplication
class KotlinBootApplication {
    @Bean
    fun clr(repo: AirportRepository) = CommandLineRunner {
        repo.deleteAll()
            .thenMany(
                Flux.just(
                    Airport("KSTL", "St. Louis Lambert International Airport"),
                    Airport("KORD", "Chicago O'Hare International Airport"),
                    Airport("KFAT", "Fresno Yosemite Airport"),
                    Airport("KGAG", "Gage Airport"),
                    Airport("KLOL", "Derby Field"),
                    Airport("KSUX", "Sioux Gateway/Brig General Bud Day Field"),
                    Airport("KBUM", "Butler Memorial Airport")
                )
            )
            .flatMap { repo.save(it) }
            .subscribe()
    }

    @Bean
    fun client() = WebClient.create("http://localhost:9876/metar")

    @Bean
    fun routerFunction(svc: WxService) = router {
        accept(APPLICATION_JSON).nest {
            GET("/", svc::getAllAirports)
            GET("/{id}", svc::getAirportById)
        }
        accept(TEXT_EVENT_STREAM).nest {
            GET("/metar/{id}", svc::getMetarsForAirportById)
        }
    }
}

fun main(args: Array<String>) {
    runApplication<KotlinBootApplication>(*args)
}

@Service
class WxService(private val repo: AirportRepository, private val client: WebClient) {
    fun getAllAirports(req: ServerRequest) = ok().body<Airport>(repo.findAll())

    fun getAirportById(req: ServerRequest) = ok().body<Airport>(repo.findById(req.pathVariable("id")))

    fun getMetarsForAirportById(req: ServerRequest) = ok()
        .contentType(TEXT_EVENT_STREAM)
        .body<METAR>(Flux.interval(Duration.ofSeconds(1))
            .flatMap {
                client.get()
                    .uri("?loc=${req.pathVariable("id")}")
                    .retrieve()
                    .bodyToMono<METAR>()
                    .defaultIfEmpty(METAR("???", "METAR unavailable for this airport code"))
            })
}

interface AirportRepository : ReactiveCrudRepository<Airport, String>

@Document
data class Airport(@Id val id: String, val name: String)

data class METAR(val flight_rules: String, val raw: String)
