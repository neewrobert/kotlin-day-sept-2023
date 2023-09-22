package org.applications.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*
import org.applications.model.Priority
import org.applications.model.Task
import org.applications.model.TaskRepository
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver

fun Application.configureTemplating() {
    install(Thymeleaf) {
        setTemplateResolver(ClassLoaderTemplateResolver().apply {
            prefix = "templates/thymeleaf/"
            suffix = ".html"
            characterEncoding = "utf-8"
        })
    }
    routing {
        route("/tasks") {
            get {
                val tasks = TaskRepository.allTasks()
                call.respond(ThymeleafContent("all-tasks", mapOf("tasks" to tasks)))
            }
            get("/byName") {
                val name = call.request.queryParameters["taskName"]
                if (name == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }
                val task = TaskRepository.taskByName(name)
                if (task == null) {
                    val message = "There is no task called '$name'"
                    call.respond(ThymeleafContent("error", mapOf("message" to message)))
                    return@get
                }
                call.respond(ThymeleafContent("single-task", mapOf("task" to task)))
            }

            get("/byPriority") {
                val priorityAsText = call.parameters["priority"]
                if (priorityAsText == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }
                try {
                    val priority = Priority.valueOf(priorityAsText)
                    val tasks = TaskRepository.tasksByPriority(priority)
                    if (tasks.isEmpty()) {
                        val message = "There are no tasks with priority '$priority'"
                        call.respond(ThymeleafContent("error", mapOf("message" to message)))
                        return@get
                    }
                    call.respond(ThymeleafContent("all-tasks", mapOf("tasks" to tasks)))
                } catch (ex: IllegalArgumentException) {
                    val message = "Priority must be one of: ${Priority.values().contentToString()}"
                    call.respond(ThymeleafContent("error", mapOf("message" to message)))
                    return@get
                }
            }

            post {
                val formContent = call.receiveParameters()
                val params = Triple(
                    formContent["name"] ?: "",
                    formContent["description"] ?: "",
                    formContent["priority"] ?: ""
                )
                if (params.toList().any { it.isEmpty() }) {
                    call.respond(ThymeleafContent("error", mapOf("message" to "All fields must be filled")))
                    return@post
                }

                try{
                    val priority = Priority.valueOf(params.third)
                    TaskRepository.addTask(
                        Task(
                            params.first,
                            params.second,
                            priority)
                    )
                    call.respondRedirect("/tasks")
                } catch (ex: IllegalArgumentException) {
                    val message = "Priority must be one of: ${Priority.values().contentToString()}"
                    call.respond(ThymeleafContent("error", mapOf("message" to message)))
                    return@post
                }

            }

        }
    }
}
