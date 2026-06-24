param([string]$ServiceName = "auth-service")

$REGION = "ap-south-1"
$logGroup = "/ecs/cryptovault-dev-$ServiceName"

Write-Host "=== CloudWatch Logs: $logGroup ===" -ForegroundColor Cyan

# Get latest stream
$streamsJson = aws logs describe-log-streams `
  --log-group-name $logGroup `
  --region $REGION `
  --order-by LastEventTime `
  --descending `
  --max-items 1 `
  --output json 2>&1
$streams = $streamsJson | ConvertFrom-Json
$streamName = $streams.logStreams[0].logStreamName
Write-Host "Stream: $streamName" -ForegroundColor Yellow

# Fetch last 40 log events
$eventsJson = aws logs get-log-events `
  --log-group-name $logGroup `
  --log-stream-name $streamName `
  --region $REGION `
  --limit 60 `
  --output json 2>&1
$events = ($eventsJson | ConvertFrom-Json).events
$last30 = $events | Select-Object -Last 30
foreach ($e in $last30) {
  Write-Host $e.message
}
