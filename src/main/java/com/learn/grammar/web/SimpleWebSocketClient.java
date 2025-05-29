package com.learn.grammar.web;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SimpleWebSocketClient {
    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        CountDownLatch latch = new CountDownLatch(1);

        WebSocket.Listener listener = new WebSocket.Listener() {
            @Override
            public void onOpen(WebSocket webSocket) {
                System.out.println("WebSocket opened");
                webSocket.request(1);
            }

            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                System.out.println("Received: " + data);
                webSocket.request(1);
                return null;
            }

            @Override
            public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                System.out.println("WebSocket closed: " + statusCode + ", " + reason);
                latch.countDown();
                return null;
            }

            @Override
            public void onError(WebSocket webSocket, Throwable error) {
                System.out.println("Error: " + error.getMessage());
                if (error.getCause() != null) {
                    System.out.println("Cause: " + error.getCause().getMessage());
                }
                error.printStackTrace();
                latch.countDown();
            }
        };

        try {
            CompletableFuture<WebSocket> ws = client.newWebSocketBuilder()
                    .buildAsync(URI.create("ws://localhost:8089/websocket-chat"), listener);

            // Wait for the WebSocket to be established or for an error
            ws.get(5, TimeUnit.SECONDS);
            
            // Keep the main thread alive
            latch.await(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.out.println("Failed to connect: " + e.getMessage());
            Throwable rootCause = e;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            System.out.println("Root cause: " + rootCause.getClass().getName() + ": " + rootCause.getMessage());
        }
    }
}
