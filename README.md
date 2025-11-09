QueueCTL — CLI Background Job Queue (Java + Spring Boot)
A minimal, production-style CLI job queue with workers, retries (exponential backoff), a Dead Letter Queue (DLQ), and persistent storage.

Built for the Backend Developer Internship assignment.
This README is copy-paste ready for GitHub.


✨ Highlights


🧰 CLI-first: enqueue, worker start/stop, status, list, dlq list/retry, config get/set/show


🔁 Automatic retries with exponential backoff (delay = base^attempts)


🪦 Dead Letter Queue (DLQ) after max retries


💾 Persistence via MySQL + JPA/Hibernate (survives restarts)


🧵 Multiple workers with safe locking and heartbeats


🛑 Graceful shutdown (finishes current job)


⚙️ Runtime config: max retries, backoff base, poll/heartbeat intervals


✅ Smoke tests via scripts/validate.ps1 (Windows) & scripts/validate.sh (Linux/macOS)



🧱 Project Structure (key parts)
queuectl/
├─ src/main/java/com/queuectl/
│  ├─ Application.java
│  ├─ cli/                  # CLI command handlers
│  ├─ domain/               # JPA entities (Job, WorkerHeartbeat, KeyValueConfig)
│  ├─ repo/                 # Spring Data repositories
│  └─ service/              # JobService, WorkerService, Backoff/Locking logic
├─ src/main/resources/
│  └─ application.properties
├─ scripts/
│  ├─ validate.ps1          # Windows end-to-end smoke test
│  └─ validate.sh           # Linux/macOS end-to-end smoke test
├─ pom.xml
└─ README.md


🏗️ Architecture Overview
Storage


jobs – queue storage (pending/processing/completed/failed/dead), scheduling fields, priority, locking


worker_heartbeats – liveness/host/pid/threads + last seen timestamps


kv_config – runtime config (max_retries, backoff_base, workers.stop, etc.)


Core flow


enqueue adds a PENDING job (run_at / next_run_at control schedule).


worker polls the earliest runnable job (state=PENDING AND next_run_at<=NOW()), atomically locks it, and sets state=PROCESSING.


Job command is executed via OS shell; exit code 0 ⇒ COMPLETED.


On failure, attempts++ and next_run_at += base^attempts.
When attempts > max_retries ⇒ DEAD (goes to DLQ).


Worker heartbeats are stored in worker_heartbeats and graceful stop is controlled by kv_config.workers.stop=true.


Backoff
delay_seconds = backoff_base ^ attempts


🗄️ Database Schema (MySQL)
Tables (simplified):


jobs


id (PK), command, state (PENDING|PROCESSING|COMPLETED|FAILED|DEAD),
attempts, max_retries, backoff_base, priority,
run_at, next_run_at, created_at, updated_at,
locked_by, locked_at, exit_code, last_error, log_path


Indexes: idx_jobs_state, idx_jobs_next_run_at, idx_jobs_priority, idx_jobs_locked(locked_by, locked_at)




worker_heartbeats


worker_id (PK), host, pid, threads, created_at, updated_at




kv_config


k (PK), v (text)





Note: Entity fields are explicitly mapped to snake_case columns (e.g., next_run_at, created_at) to match SQL.


🔧 Prerequisites


JDK 17+


Maven 3.9+


MySQL 8+


Shell: PowerShell (Windows) or Bash (Linux/macOS)


Configure DB in src/main/resources/application.properties:
spring.datasource.url=jdbc:mysql://localhost:3306/queuectl?useSSL=false&serverTimezone=UTC
spring.datasource.username=YOUR_USER
spring.datasource.password=YOUR_PASS
spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false


▶️ Build & Run
# From project root
mvn -DskipTests package

# Run CLI
java -jar target/queuectl-1.0.0.jar --help


🖱️ CLI Usage (Examples)
Enqueue jobs
# Simple echo
java -jar target/queuectl-1.0.0.jar enqueue "echo hello from job 1"

# Failing job with custom retry/backoff
java -jar target/queuectl-1.0.0.jar enqueue "cmd /c exit 1" --id bad1 --max-retries 2 --backoff-base 2   # Windows
# or
java -jar target/queuectl-1.0.0.jar enqueue "sh -c 'echo will fail; exit 1'" --id bad1 --max-retries 2 --backoff-base 2   # Linux/macOS

# Slow job with priority
java -jar target/queuectl-1.0.0.jar enqueue "timeout /T 2" --priority 5  # Windows
# or
java -jar target/queuectl-1.0.0.jar enqueue "sleep 2" --priority 5       # Linux/macOS

Start / Stop workers
# Start two workers
java -jar target/queuectl-1.0.0.jar worker start --count 2

# Request graceful stop (workers finish current job)
java -jar target/queuectl-1.0.0.jar worker stop

Status & Listing
java -jar target/queuectl-1.0.0.jar status
java -jar target/queuectl-1.0.0.jar list
java -jar target/queuectl-1.0.0.jar list --state pending

DLQ management
java -jar target/queuectl-1.0.0.jar dlq list
java -jar target/queuectl-1.0.0.jar dlq retry bad1

Runtime config
# Show all
java -jar target/queuectl-1.0.0.jar config show

# Get single key
java -jar target/queuectl-1.0.0.jar config get max_retries

# Set values
java -jar target/queuectl-1.0.0. jar config set max_retries 3
java -jar target/queuectl-1.0.0. jar config set backoff_base 2
java -jar target/queuectl-1.0.0. jar config set workers.stop true   # signal graceful stop


🔬 One-Command Smoke Tests
Windows (PowerShell)
# Allow script for this session only
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass

# From project root:
powershell -ExecutionPolicy Bypass -File .\scripts\validate.ps1

Linux/macOS (Bash)
chmod +x scripts/validate.sh
./scripts/validate.sh

The validation scripts:


build the JAR,


enqueue a success, a failure with retries, and a slow job,


start workers, then stop gracefully,


print status, DLQ, and job list.



🧪 Expected Scenarios (covered)


✅ Basic job completes (exit code 0)


✅ Failing job retries with exponential backoff


✅ Moves to DLQ after max_retries


✅ Multiple workers without double-processing (lock + heartbeat)


✅ Invalid commands fail gracefully and are retried/moved to DLQ


✅ Persistence: jobs/config/heartbeats survive restarts


✅ CLI: all required commands implemented and documented



🧠 Assumptions & Trade-offs


Commands are executed through the host shell (cmd on Windows, sh on *nix).
Callers provide OS-appropriate commands in enqueue.


Scheduling uses run_at / next_run_at; workers poll by next_run_at <= NOW().


Minimal logging is kept in DB (exit_code, last_error) + console logs.


DB is single instance; for multi-node workers, MySQL row locks prevent contention.



🧭 Roadmap (nice-to-have)


Job timeouts and kill


Output capture and persisted logs


Priority queues and partitions


Web dashboard & metrics


Batch retry / bulk operations


More unit/integration tests



❓ Troubleshooting


mvn not found → Install Maven and ensure it’s on PATH.


DB connection errors → Check application.properties URL/creds; ensure MySQL is running and DB exists.


Workers don’t stop → config set workers.stop true, then check status for updated flag.


Indexes already exist → Safe to ignore; DDL is idempotent in scripts.



📜 License
MIT 

🙌 Credits
Designed & implemented by Aman Kumar (SKYTOX07) as part of the internship assignment.