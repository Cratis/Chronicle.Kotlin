// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.spring

import io.cratis.chronicle.IEventStore
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** What the caller sends to hire someone. */
data class Hire(val firstName: String, val lastName: String, val title: String, val email: String)

/** What the caller sends to promote someone. */
data class Promote(val newTitle: String)

/**
 * Employees over HTTP.
 *
 * `IEventStore` is injected like any other bean and is already pointed at the right namespace. The
 * whole handler runs inside a unit of work, so the two events in [hire] land together or not at all —
 * and if the email is already taken, the constraint stops both.
 *
 * Spring MVC handlers are blocking, so the coroutine API is bridged with `runBlocking`. On WebFlux, or
 * anywhere else that is already suspending, drop the `runBlocking` and mark the handler `suspend`.
 */
@RestController
@RequestMapping("/api/employees")
class Employees(private val eventStore: IEventStore) {
    @PostMapping("/{id}/hire")
    fun hire(@PathVariable id: String, @RequestBody hire: Hire): ResponseEntity<Any> = runBlocking {
        eventStore.eventLog.append(id, EmployeeHired(hire.firstName, hire.lastName, hire.title))
        val result = eventStore.eventLog.append(id, EmployeeEmailSet(hire.email))

        if (result.isSuccess) {
            ResponseEntity.accepted().build()
        } else {
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(mapOf("violations" to result.constraintViolations.map { it.message }))
        }
    }

    @PostMapping("/{id}/promote")
    fun promote(@PathVariable id: String, @RequestBody promote: Promote): ResponseEntity<Any> = runBlocking {
        eventStore.eventLog.append(id, EmployeePromoted(promote.newTitle))
        ResponseEntity.accepted().build()
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: String): ResponseEntity<EmployeeState> = runBlocking {
        eventStore.readModels.getInstanceByKey(EmployeeState::class, id)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

    @GetMapping
    fun list(): List<EmployeeState> = runBlocking {
        eventStore.readModels.getInstances(EmployeeState::class)
    }
}
