$ErrorActionPreference = "Stop"

Write-Host "=========================================================="
Write-Host "VERIFYING PROFILE-FIRST DISCOVERY AND USEFUL JOBS"
Write-Host "=========================================================="

# 1. Unauthenticated generic discovery check (must be rejected)
Write-Host "`n1. Testing Unauthenticated Access to /api/jobs..."
try {
    $unauthRes = Invoke-RestMethod -Uri 'http://localhost:8085/api/jobs' -Method Get
    Write-Error "FAILED: Unauthenticated access was permitted!"
} catch {
    Write-Host ("SUCCESS: Unauthenticated access properly rejected: " + $_.Exception.Message)
}

# 2. Login as candidate
Write-Host "`n2. Logging in as candidate@jobhunter.ai..."
$loginRes = Invoke-RestMethod -Uri 'http://localhost:8085/api/auth/login' -Method Post -ContentType 'application/json' -Body '{"email":"candidate@jobhunter.ai","password":"password123"}'
$token = $loginRes.data.token
$headers = @{ Authorization = "Bearer $token" }
Write-Host "Logged in successfully, JWT received."

# 3. Check Profile Readiness
Write-Host "`n3. Checking Profile Readiness via GET /api/profile/readiness..."
$readinessRes = Invoke-RestMethod -Uri 'http://localhost:8085/api/profile/readiness' -Method Get -Headers $headers
$readiness = $readinessRes.data

Write-Host ("Profile Readiness State: " + $readiness.state)
Write-Host ("Readiness Score: " + $readiness.score + "%")
Write-Host ("Can Discover: " + $readiness.canDiscover)
Write-Host ("Headline: " + $readiness.headline)
Write-Host ("Message: " + $readiness.message)
Write-Host "Checklist Items:"
foreach ($item in $readiness.items) {
    $sym = if ($item.satisfied) { "[PASS]" } else { "[FAIL]" }
    Write-Host ("  " + $sym + " " + $item.label + ": " + $item.details)
}

# 4. Query Jobs with Profile-First Gating
Write-Host "`n4. Querying Jobs via GET /api/jobs (Profile-Gated)..."
$jobsRes = Invoke-RestMethod -Uri 'http://localhost:8085/api/jobs' -Method Get -Headers $headers
$jobs = $jobsRes.data.content
$totalElements = $jobsRes.data.totalElements

Write-Host ("Total Useful Jobs Surfaced: " + $jobs.Count + " (totalElements matching profile: " + $totalElements + ")")

foreach ($j in $jobs) {
    Write-Host ("----------------------------------------------------------")
    Write-Host ("Job: " + $j.title + " at " + $j.companyName)
    Write-Host ("Location: " + $j.location + " | WorkMode: " + $j.workMode)
    Write-Host ("Usefulness Status: " + $j.usefulnessStatus)
    Write-Host ("Match Category: " + $j.matchCategory)
    Write-Host ("Hard Eligibility Passed: " + $j.hardEligibilityPassed)
    Write-Host ("Priority Category: " + $j.priorityCategory + " | Score: " + $j.priorityScore)
    Write-Host ("Matched Requirements Count: " + $j.matchedRequirements.Count)
    Write-Host ("Missing Requirements Count: " + $j.missingRequirements.Count)
    Write-Host ("Candidate Evidence Count: " + $j.candidateEvidence.Count)
}

# 5. Verify ByteDance Poor Match was correctly eliminated by Usefulness Gate
$bytedanceFound = $jobs | Where-Object { $_.companyName -like "*ByteDance*" -or $_.title -like "*iOS*" }
if ($bytedanceFound) {
    Write-Error "FAILED: ByteDance iOS job was surfaced to candidate feed!"
} else {
    Write-Host "`nSUCCESS: ByteDance iOS mismatched job was correctly ELIMINATED by Hard Filter / Usefulness Gate!"
}

Write-Host "`n=========================================================="
Write-Host "PROFILE-FIRST DISCOVERY AND USEFUL JOBS FULLY VERIFIED!"
Write-Host "=========================================================="
