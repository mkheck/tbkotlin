package com.thehecklers.kotlinboot

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.CrudRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class KotlinBootApplication {
    @Bean
    fun loadData(repo: MetarRepository) = CommandLineRunner {
        (1..10).forEach { repo.save(METAR("IFR", "Weather summary number $it")) }
    }
}

fun main(args: Array<String>) {
    runApplication<KotlinBootApplication>(*args)
}

@RestController
class MetarController(private val repo: MetarRepository) {
    @GetMapping
    fun getAllMetars() = repo.findAll()

    @GetMapping("/{id}")
    fun getMetarById(@PathVariable id: String) = repo.findById(id)
}

interface MetarRepository : CrudRepository<METAR, String>

@Document
data class METAR(
    val flight_rules: String,
    val raw: String,
    @Id
    var id: String? = null
)
