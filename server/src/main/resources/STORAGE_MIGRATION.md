# Seata Storage Mode Migration Tool

## Overview

This tool helps users migrate Seata's storage mode between different backends (file, db, redis, raft) without data loss.

## Features

- **Command-line tool**: Run migration from command line
- **REST API**: Integrate with Seata Server management API
- **Support all modes**: FILE, DB, REDIS, RAFT
- **Progress tracking**: Real-time migration status
- **Data validation**: Skip completed transactions after 24 hours

## Quick Start

### 1. Enable Management API

Add to your `application.yml`:

```yaml
seata:
  management:
    enabled: true
    port: 7091
```

Or start Seata with system properties:

```bash
java -Dseata.management.enabled=true -Dseata.management.port=7091 -jar seata-server.jar
```

### 2. API Endpoints

#### Start Migration

```bash
POST http://localhost:7091/api/v1/storage/migration/start
Content-Type: application/json

{
  "sourceMode": "FILE",
  "targetMode": "DB",
  "sourceFilePath": "/data/seata/store"
}
```

#### Get Migration Status

```bash
GET http://localhost:7091/api/v1/storage/migration/status
```

#### Get Available Modes

```bash
GET http://localhost:7091/api/v1/storage/migration/modes
```

#### Check Migration Support

```bash
GET http://localhost:7091/api/v1/storage/migration/check?sourceMode=FILE&targetMode=DB
```

### 3. Health Check

```bash
GET http://localhost:7091/health
```

## Migration Process

```
┌─────────────────────────────────────────────────────────────┐
│                    Storage Mode Migration                    │
├─────────────────────────────────────────────────────────────┤
│  1. Stop Seata Server                                       │
│     ./bin/seata-server.sh stop                              │
│                                                              │
│  2. Backup Current Data                                     │
│     - FILE:  Backup store directory                         │
│     - DB:    Backup global_table, branch_table              │
│     - REDIS: Backup related keys                            │
│                                                              │
│  3. Configure Target Storage (application.yml)              │
│     seata:                                                  │
│       store:                                                │
│         mode: db                                            │
│         db:                                                 │
│           datasource: druid                                 │
│           url: jdbc:mysql://localhost:3306/seata            │
│                                                              │
│  4. Start Seata with Management API                         │
│     java -Dseata.management.enabled=true -jar seata-server.jar
│                                                              │
│  5. Execute Migration via API or CLI                        │
│     curl -X POST http://localhost:7091/api/v1/storage/migration/start \
│       -H "Content-Type: application/json" \
│       -d '{"sourceMode":"FILE","targetMode":"DB","sourceFilePath":"/data/seata/store"}'
│                                                              │
│  6. Monitor Migration Status                                │
│     curl http://localhost:7091/api/v1/storage/migration/status
│                                                              │
│  7. Verify and Restart Seata                                │
│     - Check migration results                               │
│     - Restart Seata server with new configuration           │
│     - Verify transactions work correctly                    │
└─────────────────────────────────────────────────────────────┘
```

## Command Line Tool

### Build

```bash
cd seata-server
mvn clean package -DskipTests
```

### Usage

```bash
java -jar seata-server-*.jar <source-mode> <target-mode> [source-file-path] [target-file-path]

# Examples
java -jar seata-server-*.jar file db /path/to/store /tmp/target
java -jar seata-server-*.jar db redis
java -jar seata-server-*.jar redis db
java -jar seata-server-*.jar db raft /tmp/target
java -jar seata-server-*.jar raft db /path/to/store
```

## Supported Migration Paths

| Source \ Target | FILE | DB  | REDIS | RAFT |
|----------------|------|-----|-------|------|
| **FILE**       | -    | ✓   | ✓     | ✓    |
| **DB**         | ✓    | -   | ✓     | ✓    |
| **REDIS**      | ✓    | ✓   | -     | ✓    |
| **RAFT**       | ✓    | ✓   | ✓     | -    |

## Important Notes

1. **Stop Seata First**: Always stop the Seata server before migration
2. **Backup Data**: Always backup your current data before migration
3. **No New Transactions**: Ensure no new transactions during migration
4. **Raft Mode**: Raft mode requires file path for data directory
5. **Completed Transactions**: Transactions older than 24 hours that are already completed will be skipped

## API Response Format

### Success Response

```json
{
  "code": "200",
  "message": "success",
  "data": {
    "status": "RUNNING",
    "sourceMode": "FILE",
    "targetMode": "DB",
    "result": {
      "successCount": 100,
      "failCount": 0,
      "skippedCount": 5,
      "elapsedMillis": 15000,
      "success": true
    }
  }
}
```

### Error Response

```json
{
  "code": "500",
  "message": "Migration from FILE to DB is not supported",
  "data": null
}
```

## Monitoring

### Check Migration Progress

```bash
# Get current status
curl http://localhost:7091/api/v1/storage/migration/status

# Response includes:
# - status: IDLE | RUNNING | COMPLETED | FAILED
# - sourceMode: Source storage mode
# - targetMode: Target storage mode
# - result: MigrationResult with success/fail/skipped counts
```

### View Logs

```bash
tail -f ${user.home}/logs/seata/seata.log | grep -i "migration"
```

## Troubleshooting

### Port Already in Use

If port 7091 is already in use, change the management port:

```yaml
seata:
  management:
    enabled: true
    port: 8092
```

### Migration Fails

1. Check logs for detailed error messages
2. Ensure source data directory/file exists
3. Ensure target database/redis is accessible
4. Verify sufficient disk space

### Data Inconsistency

1. Stop all Seata servers
2. Restore from backup
3. Check source data integrity
4. Retry migration

## Configuration Reference

### application.yml

```yaml
seata:
  # Management API configuration
  management:
    enabled: true           # Enable/disable management API
    port: 7091              # Management API port

  # Storage configuration
  store:
    mode: db               # Current storage mode
    # File mode
    file:
      dir: /data/seata/store
    # DB mode
    db:
      datasource: druid
      db-type: mysql
      url: jdbc:mysql://localhost:3306/seata
      username: root
      password: secret
    # Redis mode
    redis:
      mode: single
      host: localhost
      port: 6379
```
