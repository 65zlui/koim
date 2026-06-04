package org.example

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KoimApplication

fun main(args: Array<String>) {
    runApplication<KoimApplication>(*args)
}
