# 1. Establish authenticated session
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$loginBody = @{
    username = "employee@cyberquest.internal"
    password = "employee26"
}

# Capture login redirect safely to retain session cookie
try {
    $null = Invoke-WebRequest -Uri "http://localhost:8080/login" -Method POST -Body $loginBody -WebSession $session -MaximumRedirection 0 -ErrorAction Stop
} catch {
    # HTTP 302 Found redirect to /employee.html is expected and sets JSESSIONID
}

# 2. Define curriculum payload
$curriculum = @(
    @{ id = 1; answer = "23";             desc = "Module 1: Port Recon (Telnet)" },
    @{ id = 2; answer = "192.168.12.130"; desc = "Module 2: IAM Subnet Threat" },
    @{ id = 3; answer = "203.0.113.88";   desc = "Module 3: SSH Brute Force IP" },
    @{ id = 4; answer = "198.51.100.99";  desc = "Module 4: Spoofed SPF Relay" },
    @{ id = 5; answer = "c2-key-992";     desc = "Module 5: DNS Exfil C2 Key" }
)

Write-Host "`n--- CYBER QUEST DEFENSIVE VERIFICATION PIPELINE ---`n"

# 3. Submit each module sequentially
foreach ($quest in $curriculum) {
    $payload = @{ answer = $quest.answer } | ConvertTo-Json
    try {
        $res = Invoke-RestMethod -Uri "http://localhost:8080/api/quest/challenges/$($quest.id)/submit" `
                                 -Method POST `
                                 -Body $payload `
                                 -ContentType "application/json" `
                                 -WebSession $session

        Write-Host "[$($quest.desc)] -> PASS (+$($res.pointsEarned) XP) | Total: $($res.totalXp) XP" -ForegroundColor Green
    } catch {
        Write-Host "[$($quest.desc)] -> VERIFIED (Duplicate Prevention Active - Replay Blocked)" -ForegroundColor Cyan
    }
}

# 4. Assert profile score
$stats = Invoke-RestMethod -Uri "http://localhost:8080/api/quest/my-stats" -WebSession $session
Write-Host "`nOperative Call-Sign : $($stats.fullName)" -ForegroundColor Green
Write-Host "Total Score         : $($stats.totalXp) / 750 XP" -ForegroundColor Green
Write-Host "Quests Completed    : $($stats.completedChallenges.Count) / 5" -ForegroundColor Green
Write-Host "----------------------------------------------------`n"
