package com.learn.grammar.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import spark.Spark;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;

class MessagePayload {
    private final String message;
    private final String timestamp;
    private final String transport;

    public MessagePayload(String message, String timestamp, String transport) {
        this.message = message;
        this.timestamp = timestamp;
        this.transport = transport;
    }

    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getTransport() {
        return transport;
    }
}

@WebSocket
public class TransportServer {
    private static final Queue<Session> webSocketSessions = new LinkedBlockingQueue<>();

    private static final Map<String, PrintWriter> sseClient = new ConcurrentHashMap<>();
    public static final ObjectMapper objectMapper = new ObjectMapper();
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;

    public static void main(String[] args) {
        int port = 8089;
        Spark.port(port);

        System.out.println("Server listening on port: " + port);

        // WebSocket must be registered before any routes
        Spark.webSocket("/websocket-chat", TransportServer.class);

        // HTTP
        Spark.get("/http-data", ((request, response) -> {
            System.out.println("[HTTP] Received request from " + request.ip());
            response.type("application/json");
            MessagePayload payload = new MessagePayload(
                    "Hello from HTTP!",
                    LocalTime.now().format(TIME_FORMATTER),
                    "HTTP"
            );
            return objectMapper.writeValueAsString(payload);
        }));

        // SSE
        Spark.get("/sse-events", (req, res)->{
            String clientId = req.session(true).id();
            System.out.println("[SSE] Client connected: " + clientId + " from " + req.ip());

            res.raw().setContentType("text/event-stream");
            res.raw().setCharacterEncoding("UTF-8");
            res.raw().setHeader("Cache-Control", "no-cache");
            res.raw().setHeader("Connection", "keep-alive");

            try (PrintWriter writer = new PrintWriter(res.raw().getOutputStream())){
                sseClient.put(clientId, writer);
                writer.flush();

                while (sseClient.containsKey(clientId)) {
                    try {
                        Thread.sleep(1000);
                        writer.write(": keep-alive\n\n");
                        writer.flush();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        System.err.println("[SSE] Error writing to client " + clientId + ": " + e.getMessage());
                        break;
                    }
                }
            } catch (IOException e) {
                System.err.println("[SSE] Error setting up stream for " + clientId + ": " + e.getMessage());
            } finally {
                sseClient.remove(clientId);
                System.out.println("[SSE] Client disconnected: " + clientId);
            }
            return null;
        });

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            String time = LocalTime.now().format(TIME_FORMATTER);
            String sseMessage = String.format("event: time-update\ndata: {\"time\": \"%s\", \"source\":\"SSE Push\"}\n\n", time);
            String wsMessage = String.format("{\"time\":\"%s\", \"source\":\"WebSocket Push\"}", time);

            sseClient.values().forEach(writer -> {
                try {
                    writer.write(sseMessage);
                    writer.flush();
                } catch (Exception e) {
                    System.err.println("[SSE Scheduler] Error writing to a client: " + e.getMessage());
                }
            });
            broadcastWebSocketMessage("Server Push: " + wsMessage);
        }, 5, 3, TimeUnit.SECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down server...");
            scheduler.shutdownNow();
            Spark.stop();
            System.out.println("Server stopped.");
        }));
    }

    // websocket handler methods
    @OnWebSocketConnect
    public void onConnect(Session session) throws IOException {
        System.out.println("[WebSocket] Client connected: " + session.getRemoteAddress());
        webSocketSessions.add(session);
        session.getRemote().sendString("Welcome to WebSocket Chat!");
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        System.out.println("[WebSocket] Client disconnected: " + session.getRemoteAddress() + " | Reason: " + reason);
        webSocketSessions.remove(session);
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message){
        System.out.println("[WebSocket] Received from " + session.getRemoteAddress() + ": " + message);
        String reply = "Server echo: " + message;
        broadcastWebSocketMessage("Client " + session.getRemoteAddress() + ": " + message);
        // session.getRemote().sendString(reply);
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        System.err.println("[WebSocket] Error from session " + session.getRemoteAddress() + ": " + error.getMessage());
        error.printStackTrace();
    }

    private static void broadcastWebSocketMessage(String message) {
        webSocketSessions.stream()
                .filter(Session::isOpen)
                .forEach(s -> {
                    try {
                        s.getRemote().sendString(message);
                    } catch (IOException e) {
                        System.err.println("[WebSocket Broadcast] Error sending to " + s.getRemoteAddress() + ": " + e.getMessage());
                    }
                });
    }
}