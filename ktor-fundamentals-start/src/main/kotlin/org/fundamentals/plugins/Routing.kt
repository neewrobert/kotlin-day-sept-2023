package org.fundamentals.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.fundamentals.model.Priority
import org.fundamentals.model.Task
import org.fundamentals.model.TaskRepository
import org.fundamentals.view.tasksAsTable

fun Application.configureRouting() {
    routing {
        staticResources("/tasks-ui", "tasks-ui")

        route("/tasks") {
            get {
                val tasks = TaskRepository.allTasks()
                call.respondText(
                    contentType = ContentType.parse("text/html"),
                    text = tasks.tasksAsTable()
                )
            }
            get("/byName/{taskName}") {
                val name = call.parameters["taskName"]
                if (name == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }
                val task = TaskRepository.taskByName(name)
                if (task == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }
                call.respondText(
                    contentType = ContentType.parse("text/html"),
                    text = listOf(task).tasksAsTable()
                )
            }

            get("/byPriority/{priority}") {

                val priority = call.parameters["priority"]
                if (priority == null || priority !in Priority.entries.map { it.name }) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }
                val tasks = TaskRepository.tasksByPriority(Priority.valueOf(priority))
                call.respondText(
                    contentType = ContentType.parse("text/html"),
                    text = tasks.tasksAsTable()
                )
            }

            post {
                val formContent = call.receiveParameters()
                val params = Triple(
                    formContent["name"] ?: "",
                    formContent["description"] ?: "",
                    formContent["priority"] ?: ""
                )
                if (params.toList().any { it.isEmpty() }) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }
                try {
                    val priority = Priority.valueOf(params.third)
                    TaskRepository.addTask(
                        Task(
                            params.first,
                            params.second,
                            priority
                        )
                    )
                    call.respondRedirect("/tasks")
                } catch (ex: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest)
                } catch (ex: IllegalStateException) {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
        }


    }
}
