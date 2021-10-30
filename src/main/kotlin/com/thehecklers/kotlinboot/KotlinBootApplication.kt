package com.thehecklers.kotlinboot

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.util.List

@SpringBootApplication
class KotlinBootApplication {
    @Bean
    fun clr(repo: AirportRepository) = CommandLineRunner {
        repo.saveAll(
            List.of(
                Airport("KSTL", "St. Louis Lambert International Airport"),
                Airport("KORD", "Chicago O'Hare International Airport"),
                Airport("KFAT", "Fresno Yosemite Airport"),
                Airport("KGAG", "Gage Airport"),
                Airport("KLOL", "Derby Field"),
                Airport("KSUX", "Sioux Gateway/Brig General Bud Day Field"),
                Airport("KLOL", "Derby Field"),
                Airport("KBUM", "Butler Memorial Airport")
            )
        )
    }

    @Bean
    fun client() = WebClient.create("http://localhost:9876/metar")
}

fun main(args: Array<String>) {
    runApplication<KotlinBootApplication>(*args)
}

@RestController
class MetarController(private val service: WxService) {
    @GetMapping
    fun getAllAirports() = service.getAllAirports()

    @GetMapping("/{id}")
    fun getAirportById(@PathVariable id: String) = service.getAirportById(id)

    @GetMapping("/metar/{id}")
    fun getMetarForAirport(@PathVariable id: String): METAR? = service.getMetarForAirportById(id)
}

@Service
class WxService(private val repo: AirportRepository, private val client: WebClient) {
    fun getAllAirports() =  repo.findAll()

    fun getAirportById(id: String) = repo.findById(id)

    fun getMetarForAirportById(id: String) = client.get()
            .uri("?loc=$id")
            .retrieve()
            .bodyToMono<METAR>()
            .block()
}

interface AirportRepository : CrudRepository<Airport, String>

@Document
data class Airport(@Id val id: String, val name: String)

data class METAR(val flight_rules: String, val raw: String)
