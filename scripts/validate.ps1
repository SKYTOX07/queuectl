# QueueCTL Validation Script (Windows PowerShell)
Write-Host "Building project..." -ForegroundColor Cyan
cd ..
mvn -q -DskipTests package
cd target

$jar = "queuectl-1.0.0.jar"

# Clean old data
Remove-Item "..\queuectl.db","..\queuectl.log" -ErrorAction SilentlyContinue
Write-Host "Old database/logs cleaned" -ForegroundColor Yellow

# Enqueue jobs
java -jar $jar enqueue "echo hello from job 1"
java -jar $jar enqueue "cmd /c exit 1" --id bad1 --max-retries 2 --backoff-base 2
java -jar $jar enqueue "timeout /T 2" --priority 5

# Start workers
Write-Host "Starting workers..." -ForegroundColor Cyan
Start-Job { java -jar "queuectl-1.0.0.jar" worker start --count 2 } | Out-Null
Start-Sleep -Seconds 10
java -jar $jar worker stop
Start-Sleep -Seconds 2

# Show status and DLQ
Write-Host "`n--- STATUS ---" -ForegroundColor Green
java -jar $jar status

Write-Host "`n--- DLQ ---" -ForegroundColor Green
java -jar $jar dlq list

Write-Host "`n--- JOB LIST ---" -ForegroundColor Green
java -jar $jar list
