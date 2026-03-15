package com.rituals.backend.controller;

import com.rituals.backend.entity.AppUser;
import com.rituals.backend.entity.Todo;
import com.rituals.backend.repository.AppUserRepository;
import com.rituals.backend.service.TodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class TodoController {

    private final TodoService todoService;
    private final AppUserRepository userRepository;

    private AppUser getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow();
    }

    @GetMapping
    public ResponseEntity<List<Todo>> getTodos() {
        return ResponseEntity.ok(todoService.getTodosByUser(getCurrentUser().getId()));
    }

    @PostMapping
    public ResponseEntity<Todo> addTodo(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(todoService.addTodo(getCurrentUser(), payload.get("title")));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Todo> toggleTodo(@PathVariable("id") Long id) {
        return ResponseEntity.ok(todoService.toggleTodo(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTodo(@PathVariable("id") Long id) {
        todoService.deleteTodo(id);
        return ResponseEntity.ok().build();
    }
}
