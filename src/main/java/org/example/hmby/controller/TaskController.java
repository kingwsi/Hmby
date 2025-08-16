package org.example.hmby.controller;

import lombok.AllArgsConstructor;
import org.example.hmby.utils.TaskManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @author ws
 * @since 2025/8/16
 */
@AllArgsConstructor
@RestController
@RequestMapping("/api/task")
public class TaskController {
    
    private final TaskManager taskManager;
    
    @GetMapping("/subscribe/{taskId}")
    public SseEmitter subscribe(@PathVariable String taskId) {
        return taskManager.subscribe(taskId);
    }
}
