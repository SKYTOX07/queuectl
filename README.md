# queuectl — Java Spring Boot + Picocli Job Queue

A minimal, production‑grade **CLI-based background job queue** implemented in **Java (Spring Boot + JPA + SQLite + Picocli)**.

It supports:
- Enqueuing jobs (shell commands)
- Running **multiple worker threads** (and multiple processes if you run `worker start` in several shells)
- **Automatic retries** with **exponential backoff**
- **Dead Letter Queue (DLQ)** after exhausting retries
- **Persistent storage** (SQLite) across restarts
- **Config management** via CLI (max retries, backoff base, poll interval, etc.)
- **Graceful shutdown**: workers finish current job when stop is requested
- **Minimal metrics** via `status` including active workers (heartbeats)

> Bonus: Each job's stdout/stderr is captured to a log file: `./logs/<jobId>.log`

---

## Setup Instructions

### Requirements
- **JDK 17+**
- **Maven 3.9+**

### Build
```bash
mvn -q -DskipTests package
```

### Run
```bash
java -jar target/queuectl-1.0.0.jar --help
```

---

## Usage Examples

### Enqueue
```bash
java -jar target/queuectl-1.0.0.jar enqueue "echo hello"
java -jar target/queuectl-1.0.0.jar enqueue "sleep 2" --id job1 --max-retries 3 --backoff-base 2
java -jar target/queuectl-1.0.0.jar enqueue "echo at 10:05" --run-at "2025-11-09T10:05:00+05:30"
```

### Workers
```bash
# Start 3 threads
java -jar target/queuectl-1.0.0.jar worker start --count 3

# In another shell, stop gracefully (after current jobs finish)
java -jar target/queuectl-1.0.0.jar worker stop
```

### Status
```bash
java -jar target/queuectl-1.0.0.jar status
```

### List Jobs
```bash
java -jar target/queuectl-1.0.0.jar list
java -jar target/queuectl-1.0.0.jar list --state FAILED
```

### Dead Letter Queue
```bash
java -jar target/queuectl-1.0.0.jar dlq list
java -jar target/queuectl-1.0.0.jar dlq retry job1
```

### Config
```bash
java -jar target/queuectl-1.0.0.jar config set max_retries 5
java -jar target/queuectl-1.0.0.jar config get max_retries
```

---

## Architecture Overview

**Storage**: SQLite via Spring Data JPA. Schema auto‑managed (`ddl-auto=update`).  
**Job State Machine**:
- `PENDING` → `PROCESSING` → `COMPLETED` on exit code 0
- else attempts++, compute backoff delay, schedule new `nextRunAt`
  - if attempts ≤ maxRetries → `FAILED` (will be retried)
  - else `DEAD` (DLQ)

**Exponential Backoff**:  
`delaySeconds = backoffBase ^ attempts`

**Duplicate Prevention**:  
Workers use a conditional `UPDATE` to claim a job, so only one wins the race.

**Workers**: N threads per process. Run multiple processes for more parallelism. Output goes to `logs/<id>.log`.

**Graceful Stop**: `worker stop` sets DB flag `workers.stop=true`. Running workers check the flag between jobs and exit cleanly.

**Heartbeats**: One row per `worker start` process; `status` shows those updated in last 10s.

---

## Assumptions & Trade‑offs

- SQLite chosen for simplicity and single-file persistence. For heavy concurrency, prefer PostgreSQL.
- Stop is cooperative, not forced kill.
- No per-job timeout (bonus idea).
- Priority is simple integer (higher first).

---

## Testing Instructions

A quick demo script:
```bash
chmod +x scripts/demo.sh
./scripts/demo.sh
```

What it validates:
- Successful job completes.
- A failing job retries with backoff and moves to DLQ.
- Multiple workers process in parallel without overlap.
- Job data persists in `queuectl.db`.
- Invalid command demonstrates failure handling.

---

## Assumptions & Simplifications

- CLI JSON payload not required—use flags (`--id`, `--max-retries`, etc.).
- Uses the system shell to execute commands (Windows/Unix supported).
- `status` uses heartbeats to infer active workers.

---

## License
MIT


---

## Using MySQL instead of SQLite

1. Create DB & user:

```sql
CREATE DATABASE queuectl CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'queuectl'@'%' IDENTIFIED BY 'queuectl_pw';
GRANT ALL PRIVILEGES ON queuectl.* TO 'queuectl'@'%';
FLUSH PRIVILEGES;
```

2. Build and run:

```bash
mvn -q clean package -DskipTests
java -jar target/queuectl-1.0.0.jar status
```

Configured in `src/main/resources/application.properties`:

```
spring.datasource.url=jdbc:mysql://localhost:3306/queuectl?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata&createDatabaseIfNotExist=true
spring.datasource.username=queuectl
spring.datasource.password=queuectl_pw
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=update
```
