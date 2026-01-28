/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.server.storage.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.common.result.Result;
import org.apache.seata.config.ConfigurationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple HTTP server for storage migration management API
 * This is a lightweight alternative to embedded Tomcat
 *
 * To enable, add to application.yml:
 * seata:
 *   management:
 *     enabled: true
 *     port: 7091
 *
 * Or use system property: -Dseata.management.enabled=true -Dseata.management.port=7091
 *
 * API Endpoints:
 * - POST /api/v1/storage/migration/start  - Start migration
 * - GET  /api/v1/storage/migration/status - Get migration status
 * - GET  /api/v1/storage/migration/modes  - Get available modes
 * - GET  /api/v1/storage/migration/check?sourceMode=FILE&targetMode=DB - Check migration support
 *
 */
public class ManagementApiServer implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementApiServer.class);

    private static final String MANAGEMENT_ENABLED_CONFIG = "seata.management.enabled";
    private static final String MANAGEMENT_PORT_CONFIG = "seata.management.port";
    private static final int DEFAULT_MANAGEMENT_PORT = 7091;

    // HTTP status codes
    private static final int HTTP_OK = 200;
    private static final int HTTP_CREATED = 201;
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_METHOD_NOT_ALLOWED = 405;
    private static final int HTTP_INTERNAL_ERROR = 500;
    private static final int HTTP_NO_CONTENT = 204;

    private final int port;
    private final boolean enabled;
    private final ObjectMapper objectMapper;
    private volatile com.sun.net.httpserver.HttpServer httpServer;
    private final StorageModeMigrationController migrationController;

    public ManagementApiServer() {
        this.objectMapper = new ObjectMapper();
        this.migrationController = new StorageModeMigrationController();
        this.enabled = isManagementEnabled();
        this.port = getManagementPort();

        if (enabled) {
            start();
        }
    }

    private boolean isManagementEnabled() {
        try {
            String enabledStr = ConfigurationFactory.getInstance().getConfig(MANAGEMENT_ENABLED_CONFIG);
            return Boolean.parseBoolean(enabledStr);
        } catch (Exception e) {
            return false;
        }
    }

    private int getManagementPort() {
        try {
            String portStr = ConfigurationFactory.getInstance().getConfig(MANAGEMENT_PORT_CONFIG);
            return Integer.parseInt(portStr);
        } catch (Exception e) {
            return DEFAULT_MANAGEMENT_PORT;
        }
    }

    private void start() {
        try {
            // Use Java's built-in HttpServer (no external dependencies)
            httpServer = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress(port), 0);

            // Create context for migration API
            httpServer.createContext("/api/v1/storage/migration", exchange -> {
                try {
                    handleRequest(exchange);
                } catch (Exception e) {
                    LOGGER.error("Error handling request", e);
                    sendError(exchange, HTTP_INTERNAL_ERROR, e.getMessage());
                }
            });

            // Create health check endpoint
            httpServer.createContext("/health", exchange -> {
                byte[] response = "{\"status\":\"UP\"}".getBytes();
                exchange.sendResponseHeaders(HTTP_OK, response.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }
            });

            httpServer.setExecutor(null);
            httpServer.start();

            LOGGER.info("Storage migration management API server started on port {}", port);

        } catch (IOException e) {
            LOGGER.error("Failed to start management API server on port {}", port, e);
        }
    }

    private void handleRequest(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        // CORS headers
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(HTTP_NO_CONTENT, -1);
            return;
        }

        // Route requests
        if ("POST".equalsIgnoreCase(method) && path.endsWith("/start")) {
            handleStartMigration(exchange);
        } else if ("GET".equalsIgnoreCase(method)) {
            if (path.endsWith("/status")) {
                handleGetStatus(exchange);
            } else if (path.endsWith("/modes")) {
                handleGetModes(exchange);
            } else if (path.contains("/check")) {
                handleCheckMigration(exchange);
            } else {
                sendError(exchange, HTTP_NOT_FOUND, "Not found");
            }
        } else {
            sendError(exchange, HTTP_METHOD_NOT_ALLOWED, "Method not allowed");
        }
    }

    private void handleStartMigration(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            String body = readInputStream(is);
            StorageModeMigrationController.MigrationRequest request =
                    objectMapper.readValue(body, StorageModeMigrationController.MigrationRequest.class);

            Result<StorageModeMigrationController.MigrationStatusResponse> result =
                    migrationController.startMigration(request);

            sendJsonResponse(exchange, HTTP_OK, result);
        } catch (Exception e) {
            LOGGER.error("Error starting migration", e);
            sendError(exchange, HTTP_BAD_REQUEST, e.getMessage());
        }
    }

    private void handleGetStatus(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        Result<StorageModeMigrationController.MigrationStatusResponse> result =
                migrationController.getMigrationStatus();
        sendJsonResponse(exchange, HTTP_OK, result);
    }

    private void handleGetModes(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        Result<org.apache.seata.common.store.StoreMode[]> result =
                migrationController.getAvailableModes();
        sendJsonResponse(exchange, HTTP_OK, result);
    }

    private void handleCheckMigration(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String sourceMode = getQueryParam(query, "sourceMode");
        String targetMode = getQueryParam(query, "targetMode");

        if (StringUtils.isBlank(sourceMode) || StringUtils.isBlank(targetMode)) {
            sendError(exchange, HTTP_BAD_REQUEST, "sourceMode and targetMode are required");
            return;
        }

        try {
            Result<Boolean> result = migrationController.checkMigrationSupported(
                    org.apache.seata.common.store.StoreMode.get(sourceMode),
                    org.apache.seata.common.store.StoreMode.get(targetMode));
            sendJsonResponse(exchange, HTTP_OK, result);
        } catch (Exception e) {
            sendError(exchange, HTTP_BAD_REQUEST, "Invalid mode: " + e.getMessage());
        }
    }

    private String getQueryParam(String query, String param) {
        if (query == null) {
            return null;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && param.equals(kv[0])) {
                return kv[1];
            }
        }
        return null;
    }

    private String readInputStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) != -1) {
            sb.append(new String(buffer, 0, len));
        }
        return sb.toString();
    }

    private void sendJsonResponse(com.sun.net.httpserver.HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = objectMapper.writeValueAsString(data);
        byte[] response = json.getBytes();
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private void sendError(com.sun.net.httpserver.HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, Object> error = new HashMap<>();
        error.put("code", statusCode);
        error.put("message", message);
        sendJsonResponse(exchange, statusCode, error);
    }

    @Override
    public void close() {
        if (httpServer != null) {
            httpServer.stop(0);
            LOGGER.info("Management API server stopped");
        }
    }

    /**
     * Check if management API is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get the management port
     */
    public int getPort() {
        return port;
    }

    /**
     * Get the migration controller instance
     */
    public StorageModeMigrationController getMigrationController() {
        return migrationController;
    }
}
