$REGION = "ap-south-1"

# Switch ALL target group health checks to /actuator/health (returns 200, publicly accessible)
$tgs = @{
  "cryptovault-dev-tg-gateway"      = @{ ARN = "arn:aws:elasticloadbalancing:ap-south-1:469935552565:targetgroup/cryptovault-dev-tg-gateway/aecbc5433b6c4790";      Path = "/actuator/health" }
  "cryptovault-dev-tg-auth"         = @{ ARN = "arn:aws:elasticloadbalancing:ap-south-1:469935552565:targetgroup/cryptovault-dev-tg-auth/36bc15e6880dd318";         Path = "/actuator/health" }
  "cryptovault-dev-tg-wallet"       = @{ ARN = "arn:aws:elasticloadbalancing:ap-south-1:469935552565:targetgroup/cryptovault-dev-tg-wallet/5db03460adb6daea";       Path = "/actuator/health" }
  "cryptovault-dev-tg-transaction"  = @{ ARN = "arn:aws:elasticloadbalancing:ap-south-1:469935552565:targetgroup/cryptovault-dev-tg-transaction/ddea13bdc0fc7c95";  Path = "/actuator/health" }
  "cryptovault-dev-tg-notification" = @{ ARN = "arn:aws:elasticloadbalancing:ap-south-1:469935552565:targetgroup/cryptovault-dev-tg-notification/fc8ed02d717b8c20"; Path = "/actuator/health" }
  "cryptovault-dev-tg-risk"         = @{ ARN = "arn:aws:elasticloadbalancing:ap-south-1:469935552565:targetgroup/cryptovault-dev-tg-risk/7dd54a4a4bedbf2f";         Path = "/actuator/health" }
  "cryptovault-dev-tg-audit"        = @{ ARN = "arn:aws:elasticloadbalancing:ap-south-1:469935552565:targetgroup/cryptovault-dev-tg-audit/685916756ef24d67";        Path = "/actuator/health" }
  "cryptovault-dev-tg-kyc"          = @{ ARN = "arn:aws:elasticloadbalancing:ap-south-1:469935552565:targetgroup/cryptovault-dev-tg-kyc/bde59587b3df0675";          Path = "/actuator/health" }
}

foreach ($name in $tgs.Keys) {
  $tg = $tgs[$name]
  Write-Host "Updating $name -> $($tg.Path)" -ForegroundColor Cyan

  $payload = @{
    TargetGroupArn        = $tg.ARN
    HealthCheckPath       = $tg.Path
    Matcher               = @{ HttpCode = "200" }
    HealthCheckIntervalSeconds  = 30
    HealthCheckTimeoutSeconds   = 10
    HealthyThresholdCount       = 2
    UnhealthyThresholdCount     = 3
  }
  $tmpFile = [System.IO.Path]::GetTempFileName() + ".json"
  $payload | ConvertTo-Json | Set-Content $tmpFile

  $result = aws elbv2 modify-target-group --cli-input-json "file://$tmpFile" --region $REGION --query "TargetGroups[0].{Name:TargetGroupName,Path:HealthCheckPath,Matcher:Matcher}" --output json 2>&1
  Remove-Item $tmpFile -Force
  Write-Host "  -> $result" -ForegroundColor Green
}

Write-Host "`nAll health checks updated to /actuator/health!" -ForegroundColor Green
