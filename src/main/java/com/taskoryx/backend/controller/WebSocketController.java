package com.taskoryx.backend.controller;

import com.taskoryx.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    @MessageMapping("/project.join")
    @SendTo("/topic/project/{projectId}")
    public Map<String, Object> joinProject(Map<String, String> payload, Principal principal) {
        log.debug("User {} joined project room {}", principal.getName(), payload.get("projectId"));
        return Map.of(
            "type", "USER_JOINED",
            "userId", principal.getName(),
            "timestamp", LocalDateTime.now().toString()
        );
    }

    @MessageMapping("/ping")
    @SendToUser("/queue/pong")
    public Map<String, Object> ping(Principal principal) {
        return Map.of("type", "PONG", "timestamp", LocalDateTime.now().toString());
    }
}
