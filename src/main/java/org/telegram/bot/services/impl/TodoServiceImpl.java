package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Todo;
import org.telegram.bot.repositories.TodoRepository;
import org.telegram.bot.services.TodoService;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TodoServiceImpl implements TodoService {

    private final TodoRepository todoRepository;

    @Override
    public Todo get(Long todoId) {
        log.debug("Request to get Todo by todoId: {} ", todoId);
        return todoRepository.findById(todoId).orElse(null);
    }

    @Override
    public Todo save(Todo todo) {
        log.debug("Request to Save Todo {} ", todo);
        return todoRepository.save(todo);
    }

    @Override
    public List<Todo> getList() {
        log.debug("Request to get list of all Todo");
        return todoRepository.findAll();
    }

    @Override
    public Boolean remove(Long todoId) {
        log.debug("Request to remove Todo by id {}", todoId);

        Optional<Todo> optionalTodo = todoRepository.findById(todoId);
        if (optionalTodo.isPresent()) {
            todoRepository.deleteById(todoId);
            return true;
        }

        return false;
    }
}
