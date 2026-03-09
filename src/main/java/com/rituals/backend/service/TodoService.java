package com.rituals.backend.service;

import com.rituals.backend.entity.AppUser;
import com.rituals.backend.entity.Todo;
import com.rituals.backend.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TodoService {

    private final TodoRepository todoRepository;

    public List<Todo> getTodosByUser(Long userId) {
        return todoRepository.findByUserIdOrderByDateDesc(userId);
    }

    @Transactional
    public Todo addTodo(AppUser user, String title) {
        Todo todo = Todo.builder()
                .user(user)
                .title(title)
                .isCompleted(false)
                .date(LocalDate.now())
                .build();
        return todoRepository.save(todo);
    }

    @Transactional
    public Todo toggleTodo(Long todoId) {
        Todo todo = todoRepository.findById(todoId).orElseThrow();
        todo.setIsCompleted(!todo.getIsCompleted());
        return todoRepository.save(todo);
    }

    @Transactional
    public void deleteTodo(Long todoId) {
        todoRepository.deleteById(todoId);
    }
}
