package com.parking.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.parking.entity.ParkingSpot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pushes spot status updates to all connected clients.
 *
 * Clients subscribe to /ws/occupancy and receive JSON frames whenever a
 * spot's status changes. Per-parking filtering is left to the client; the
 * payload always includes parkingId so clients can ignore unrelated updates.
 */
@Slf4j
@Component
public class OccupancyWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.debug("WebSocket connected: {} (total={})", session.getId(), sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.debug("WebSocket closed: {} ({})", session.getId(), status);
    }

    public void broadcastSpotUpdate(ParkingSpot spot) {
        Map<String, Object> payload = Map.of(
                "type", "SPOT_STATUS",
                "parkingId", spot.getParking().getId(),
                "spotId", spot.getId(),
                "spotNumber", spot.getSpotNumber(),
                "status", spot.getStatus(),
                "timestamp", Instant.now()
        );
        sendToAll(payload);
    }

    private void sendToAll(Object payload) {
        TextMessage msg;
        try {
            msg = new TextMessage(mapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize WebSocket payload", e);
            return;
        }
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                sessions.remove(session);
                continue;
            }
            try {
                session.sendMessage(msg);
            } catch (IOException e) {
                log.warn("Failed to send to {}: {}", session.getId(), e.getMessage());
            }
        }
    }
}
