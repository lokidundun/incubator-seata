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
package org.apache.seata.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

/**
 * Seata Server Application
 *
 * Features:
 * - Storage mode migration API (optional, enabled via seata.management.enabled)
 * - Support for FILE, DB, REDIS, RAFT storage modes
 *
 * To enable storage migration management API:
 * - Set system property: -Dseata.management.enabled=true
 * - Or add to application.yml:
 *   seata:
 *     management:
 *       enabled: true
 *       port: 7091
 *
 */
@SpringBootApplication(scanBasePackages = {"org.apache.seata"})
public class ServerApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerApplication.class);

    public static void main(String[] args) throws IOException {
        // Print migration API info
        String managementEnabled = System.getProperty("seata.management.enabled", "false");
        String managementPort = System.getProperty("seata.management.port", "7091");

        if ("true".equalsIgnoreCase(managementEnabled)) {
            LOGGER.info("===============================================");
            LOGGER.info("  Storage Mode Migration API is enabled");
            LOGGER.info("  API URL: http://localhost:{}/api/v1/storage/migration", managementPort);
            LOGGER.info("  Endpoints:");
            LOGGER.info("    - POST /api/v1/storage/migration/start  - Start migration");
            LOGGER.info("    - GET  /api/v1/storage/migration/status - Get status");
            LOGGER.info("    - GET  /api/v1/storage/migration/modes  - List modes");
            LOGGER.info("    - GET  /api/v1/storage/migration/check  - Check support");
            LOGGER.info("===============================================");
        }

        // Run the spring-boot application
        SpringApplication.run(ServerApplication.class, args);
    }
}
