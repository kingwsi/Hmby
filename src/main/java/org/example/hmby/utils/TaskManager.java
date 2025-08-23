package org.example.hmby.utils;

import org.example.hmby.enumerate.SseEventType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TaskManager {

    // 任务进度
    private final Map<String, Map<String, Object>> progressMap = new ConcurrentHashMap<>();

    // 每个任务对应一个订阅者（SseEmitter）
    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    // 更新进度并通知客户端
    @Async
    public void updateProgress(SseEventType eventType, Type type, String taskId, Map<String, Object> data) {

        progressMap.put("%s_%s".formatted(type.name(), taskId), data);
        SseEmitter emitter = emitterMap.get(taskId);
        if (emitter == null) return;

        try {
            emitter.send(SseEmitter.event().name(eventType.name()).data(data));
        } catch (Exception e) {
            cleanup(taskId, emitter, e);
        }
    }

    // 任务完成
    @Async
    public void completionProgress(Type type, String taskId) {
        SseEmitter emitter = emitterMap.get("%s_%s".formatted(type.name(), taskId));
        if (emitter != null) {
            try {
                emitter.complete();
            } finally {
                emitterMap.remove(taskId);
                progressMap.remove(taskId);
            }
        }
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
            } catch (IOException ignored) {
            }
        }

        // 设置回调（客户端断开时清理）
        emitter.onCompletion(() -> cleanup(taskId, emitter, null));
        emitter.onTimeout(() -> cleanup(taskId, emitter, null));
        emitter.onError((e) -> cleanup(taskId, emitter, new RuntimeException(e.getMessage())));

        return emitter;
    }

    // 定时心跳：每 15 秒发一次
    @Scheduled(fixedRate = 15000)
    public void sendHeartbeat() {
        emitterMap.forEach((taskId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
            } catch (Exception e) {
                cleanup(taskId, emitter, e);
            }
        });
    }

    // 清理逻辑
    private void cleanup(String taskId, SseEmitter emitter, Exception e) {
        emitterMap.remove(taskId, emitter);
        progressMap.remove(taskId);
        if (e != null) {
            emitter.completeWithError(e);
        } else {
            emitter.complete();
        }
    }

    public enum Type {
        TRANSLATE,

    }
}

