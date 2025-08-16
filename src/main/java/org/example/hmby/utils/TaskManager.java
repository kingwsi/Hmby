package org.example.hmby.utils;

import org.example.hmby.enumerate.SseEventType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author ws
 * @since 2025/8/16
 */
@Component
public class TaskManager {
    private final Map<String, Map<String, Object>> progressMap = new ConcurrentHashMap<>();
    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();
    
    // 更新进度并通知客户端
    @Async
    public void updateProgress(SseEventType eventType, String taskId, Map<String, Object> data) {
        progressMap.put(taskId, data);
        SseEmitter emitter = emitterMap.getOrDefault(taskId, null);

        try {
            emitter.send(SseEmitter.event().name(eventType.name()).data(data));
        } catch (Exception e) {
            emitter.complete();
        }
    }

    @Async
    public void completionProgress(String taskId) {
        SseEmitter emitter = emitterMap.getOrDefault(taskId, null);
        emitter.complete();
        emitterMap.remove(taskId);
    }

    @Async
    public void removeTask(String taskId) {
        progressMap.remove(taskId);
    }

    // 添加订阅
    public SseEmitter subscribe(String taskId) {
        SseEmitter emitter = new SseEmitter(0L); // 不超时
        emitterMap.put(taskId, emitter);

        // 如果已有进度，立即推送一次
        if (progressMap.containsKey(taskId)) {
            try {
                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data(progressMap.get(taskId)));
            } catch (Exception ignored) {}
        }

        return emitter;
    }
    
    public static class Prefix{
        public final static String TRANSLATE = "TRANSLATE_";
    }

}
