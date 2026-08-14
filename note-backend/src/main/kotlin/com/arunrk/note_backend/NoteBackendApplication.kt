package com.arunrk.note_backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class NoteBackendApplication

fun main(args: Array<String>) {
	runApplication<NoteBackendApplication>(*args)
}
