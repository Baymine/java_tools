package com.learn.grammar.web;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TransportClient {
    private static final String BASE_URL = "http://localhost:8089";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();


    public static void main(String[] args) throws Exception {
        System.out.println("Starting Transport Client Demo...\n");

        // HTTP
//        demonstrateHttp();
//        System.out.println("\n--------------------------------------\n");
//        demonstrateSse();
        System.out.println("\n--------------------------------------\n");
        demonstrateWebSocket();
        System.out.println("\n--------------------------------------\n");
    }
    private static void demonstrateHttp() throws Exception {
        System.out.println(">>> HTTP DEMO <<<<");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/http-data"))
                .GET()
                .header("Accept", "application/json")
                .build();
        CompletableFuture<HttpResponse<String>> futureResponse =
                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        futureResponse.thenApply(response -> {
            System.out.println("[HTTP Client] Status Code: " + response.statusCode());
            System.out.println("[HTTP Client] Response Body: ");

            System.out.println(response.body());
            return response;
        }).exceptionally(ex -> {
            System.err.println("[HTTP Client] Error: " + ex.getMessage());
            return null;
        }).join(); // Wait for completion for this demo part
    }

    private static void demonstrateSse() throws Exception {
        System.out.println(">>> SSE Demo <<<");
        System.out.println("[SSE Client] Connecting to " + BASE_URL + "/sse-events");
        System.out.println("[SSE Client] Will print events for ~15 seconds. Server pushes every 3 seconds.");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/sse-events"))
                .header("Accept", "text/event-stream")
                .GET()
                .build();

        CountDownLatch sseLatch = new CountDownLatch(1);

        CompletableFuture.runAsync(() -> {
            try {
                HttpResponse<java.io.InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

                if(response.statusCode() == 200) {
                    System.out.println("[SSE Client] Connected. Waiting for events...");
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))){
                        String line;
                        StringBuilder eventData = new StringBuilder();
                        String eventType = "message";
                        long startTime = System.currentTimeMillis();
                        long timeout = 15000; // 15 seconds timeout

                        while ((line = reader.readLine()) != null &&
                               !Thread.currentThread().isInterrupted() &&
                               System.currentTimeMillis() - startTime < timeout) {

                            if (line.startsWith("event:")) {
                                eventType = line.substring("event:".length()).trim();
                            } else if (line.startsWith("data")) {
                                eventData.append(line.substring("data:".length()).trim());
                            } else if (line.isEmpty() && eventData.length() > 0) {
                                System.out.printf("[SSE client] Event Type: %s, Data: %s%n", eventType, eventData.toString());
                                eventData.setLength(0);
                                eventType = "message";
                            } else if (line.startsWith(":")) {
                                System.out.printf("[SSE Client] Comment: %s%n", line.substring(1).trim());
                            }
                        }
                    }
                } else {
                    System.err.println("[SSE Client] Failed to connect. Status code: " + response.statusCode());
                }
            } catch (IOException e) {
                System.err.println("[SSE Client] IO Error: " + e.getMessage());
            } catch (InterruptedException e) {
                System.err.println("[SSE Client] Interrupted.");
                Thread.currentThread().interrupt();
            } finally {
                System.out.println("[SSE Client] Stream closed or disconnected.");
                sseLatch.countDown();
            }
        });

        // Wait for SSE demo to complete before moving on
        boolean completed = sseLatch.await(16, TimeUnit.SECONDS);
        if (!completed) {
            System.out.println("[SSE Client] Demo timeout reached.");
        }
    }

    private static void demonstrateWebSocket() throws InterruptedException {
        System.out.println(">>>WebSocket Demo<<<");
        CountDownLatch latch = new CountDownLatch(1);  // keeping the main thread
        CountDownLatch connectLatch = new CountDownLatch(1); // to track connection status

        class CustomListener implements WebSocket.Listener {
            private StringBuilder partialMessage = new StringBuilder();

            public void handleError(Throwable error) {
                System.err.println("[WebSocket Client] Error: " + error.getMessage());
                if (error.getCause() != null) {
                    System.err.println("[WebSocket Client] Cause: " + error.getCause().getMessage());
                }
                latch.countDown();
                connectLatch.countDown(); // In case it wasn't counted down yet
            }
            @Override
            public void onOpen(WebSocket webSocket) {
                System.out.println("[WebSocket Client] Connected to server!");
                webSocket.sendText("Hello from Java Client", true);
                webSocket.request(1);  // Request next message
                connectLatch.countDown(); // Signal that connection is established
            }

            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                System.out.println("[WebSocket Client] Received: " + data);
                partialMessage.append(data);

                if (last) {
                    System.out.println("[WebSocket Client] Full message: " + partialMessage.toString());
                    partialMessage.setLength(0); // Reset
                }
                webSocket.request(1);
                return null;
            }

            @Override
            public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                System.out.println("[WebSocket Client] Closed with status " + statusCode + ", reason: " + reason);
                latch.countDown();
                connectLatch.countDown(); // In case it wasn't counted down yet
                return null;
            }
        }

        CustomListener listener = new CustomListener();

        try {
            // Use a CompletableFuture with timeout instead of join() to avoid blocking indefinitely
            CompletableFuture<WebSocket> futureWs = httpClient.newWebSocketBuilder()
                    .buildAsync(URI.create("ws://localhost:8089/websocket-chat"), listener);

            // Set a timeout for the connection attempt
            WebSocket ws = null;
            try {
                ws = futureWs.get(5, TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                System.err.println("[WebSocket Client] Connection attempt timed out after 5 seconds");
                futureWs.cancel(true);
                throw new Exception("Connection timeout");
            } catch (Exception e) {
                listener.handleError(e);
                throw e;
            }

            // Wait for the onOpen callback to be called or for an error
            if (!connectLatch.await(5, TimeUnit.SECONDS)) {
                System.err.println("[WebSocket Client] Timed out waiting for connection to be established");
                if (ws != null && !ws.isInputClosed()) {
                    ws.abort();
                }
                throw new Exception("Connection establishment timeout");
            }

            System.out.println("[WebSocket Client] Type 'exit' to close WebSocket and continue, or send messages.");
            System.out.println("[WebSocket Client] (Auto-exit after 30 seconds of inactivity)");

            // Set up a timer to auto-exit after 30 seconds of inactivity
            final WebSocket finalWs = ws;
            java.util.Timer timer = new java.util.Timer(true);
            timer.schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    System.out.println("\n[WebSocket Client] Auto-exiting due to inactivity");
                    if (finalWs != null && !finalWs.isInputClosed()) {
                        finalWs.sendClose(WebSocket.NORMAL_CLOSURE, "Auto-exit due to inactivity");
                    }
                    latch.countDown();
                }
            }, 30000); // 30 seconds

            try (Scanner scanner = new Scanner(System.in)) {
                String input;
                while (!ws.isInputClosed()) {
                    System.out.print("ws> ");

                    // Use a separate thread for reading input to avoid blocking the main thread
                    CompletableFuture<String> inputFuture = CompletableFuture.supplyAsync(() -> {
                        try {
                            return scanner.nextLine();
                        } catch (Exception e) {
                            return "exit"; // Default to exit on error
                        }
                    });

                    try {
                        input = inputFuture.get(10, TimeUnit.SECONDS);
                        timer.cancel(); // Reset inactivity timer

                        // Create a new timer
                        timer = new java.util.Timer(true);
                        timer.schedule(new java.util.TimerTask() {
                            @Override
                            public void run() {
                                System.out.println("\n[WebSocket Client] Auto-exiting due to inactivity");
                                if (finalWs != null && !finalWs.isInputClosed()) {
                                    finalWs.sendClose(WebSocket.NORMAL_CLOSURE, "Auto-exit due to inactivity");
                                }
                                latch.countDown();
                            }
                        }, 30000); // 30 seconds

                        if ("exit".equalsIgnoreCase(input)) {
                            break;
                        }

                        ws.sendText(input, true);
                    } catch (java.util.concurrent.TimeoutException e) {
                        // Input timeout, continue the loop
                        continue;
                    } catch (Exception e) {
                        System.err.println("[WebSocket Client] Error reading input: " + e.getMessage());
                        break;
                    }
                }
            } finally {
                timer.cancel(); // Make sure to cancel the timer
            }

            if (ws != null && !ws.isInputClosed()) {
                try {
                    ws.sendClose(WebSocket.NORMAL_CLOSURE, "Client initiated close").get(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    System.err.println("[WebSocket Client] Error closing WebSocket: " + e.getMessage());
                    ws.abort();
                }
            }

            // Wait for the onClose callback to be called
            if (!latch.await(5, TimeUnit.SECONDS)) {
                System.err.println("[WebSocket Client] Timeout waiting for WebSocket to close.");
            }
        } catch (Exception e) {
            System.err.println("[WebSocket Client] Failed to connect: " + e.getMessage());

            // Get the root cause
            Throwable rootCause = e;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }

            System.err.println("[WebSocket Client] Root cause: " + rootCause.getClass().getName() + ": " + rootCause.getMessage());
            System.out.println("[WebSocket Client] Make sure the server is running at ws://localhost:8089/websocket-chat");
        }
    }
}
