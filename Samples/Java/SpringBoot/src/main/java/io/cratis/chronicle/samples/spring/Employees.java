// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.spring;

import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.eventSequences.ConstraintViolation;
import io.cratis.chronicle.spring.Chronicle;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Employees over HTTP.
 *
 * <p>{@code Chronicle} is injected like any other bean. It has no coroutines in its signature, so every
 * call is an ordinary Java method call, and it is already pointed at the right namespace. The whole
 * handler runs inside a unit of work, so the two events in {@code hire} land together or not at all —
 * and if the email is already taken, the constraint stops both.
 */
@RestController
@RequestMapping("/api/employees")
public class Employees {
    private final Chronicle chronicle;

    public Employees(Chronicle chronicle) {
        this.chronicle = chronicle;
    }

    /** What the caller sends to hire someone. */
    public record Hire(String firstName, String lastName, String title, String email) {}

    /** What the caller sends to promote someone. */
    public record Promote(String newTitle) {}

    @PostMapping("/{id}/hire")
    public ResponseEntity<Object> hire(@PathVariable String id, @RequestBody Hire hire) {
        chronicle.append(id, new EmployeeHired(hire.firstName(), hire.lastName(), hire.title()));
        AppendResult result = chronicle.append(id, new EmployeeEmailSet(hire.email()));

        if (result.isSuccess()) {
            return ResponseEntity.accepted().build();
        }

        List<String> violations = result.getConstraintViolations().stream()
            .map(ConstraintViolation::getMessage)
            .toList();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("violations", violations));
    }

    @PostMapping("/{id}/promote")
    public ResponseEntity<Object> promote(@PathVariable String id, @RequestBody Promote promote) {
        chronicle.append(id, new EmployeePromoted(promote.newTitle()));
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeState> get(@PathVariable String id) {
        EmployeeState state = chronicle.readModel(EmployeeState.class, id);
        return state != null ? ResponseEntity.ok(state) : ResponseEntity.notFound().build();
    }

    @GetMapping
    public List<EmployeeState> list() {
        return chronicle.readModels(EmployeeState.class);
    }
}
