package com.taskoryx.backend.service;

import com.taskoryx.backend.dto.response.notification.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    // Send notification to specific user
    public void sendNotificationToUser(UUID userId, NotificationResponse notification) {
        try {
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                notification
            );
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification to user {}", userId, e);
        }
    }

    // Broadcast to all users in a project
    public void sendToProject(UUID projectId, String eventType, Object payload) {
        try {
            messagingTemplate.convertAndSend(
                "/topic/project/" + projectId,
                new WebSocketEvent(eventType, payload)
            );
        } catch (Exception e) {
            log.error("Failed to broadcast to project {}", projectId, e);
        }
    }

    // Broadcast task update to project members
    public void broadcastTaskUpdate(UUID projectId, String eventType, Object taskData) {
        sendToProject(projectId, eventType, taskData);
    }

    // Inner class for WebSocket events
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class WebSocketEvent {
        private String type;
        private Object payload;
    }
}
