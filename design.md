# Design Notes

## Core Entities
- **Job**: id, command, state, attempts, maxRetries, backoffBase, (runAt, nextRunAt), lockedBy/At, exitCode, lastError, logPath, priority.
- **KeyValueConfig**: simple shared config (e.g., `max_retries`, `backoff_base`, `workers.stop`).
- **WorkerHeartbeat**: `workerId`, host, pid, threads, timestamps.

## Worker Loop
- findNextRunnable() — SQL pick of (PENDING|FAILED) with `nextRunAt <= now` ordered by `priority DESC, createdAt ASC`.
- claimById(id, workerId) — conditional update ensuring only one worker wins.
- execute via ProcessBuilder, log to `logs/<id>.log`.
- success: COMPLETED, failure: FAILED with backoff or DEAD after exceeding maxRetries.

## Graceful Stop
- `worker stop`: set config key `workers.stop=true`.  
- Worker threads poll `stopRequested()` between jobs, ensuring graceful exit.

## Concurrency Safety
- Atomic claim via conditional UPDATE ensures single ownership under SQLite's serialization of writers.

## Extensibility Ideas
- Job timeouts (Process.waitFor(timeout)).
- Scheduled jobs (supported via `--run-at`, `nextRunAt`).
- Priority queues (already present).
- Minimal web dashboard (Spring MVC + Thymeleaf).
