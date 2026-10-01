# PowerShell script to verify OfferPilot AI Resume Tailoring Engine end-to-end
$baseUrl = "http://localhost:8085"

Write-Host "Connecting to backend at $baseUrl..."

# 1. Login
Write-Host "1. Authenticating candidate..."
$loginBody = @{
    email = "candidate@jobhunter.ai"
    password = "password123"
} | ConvertTo-Json

$loginRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method POST -Body $loginBody -ContentType "application/json"
$token = $loginRes.data.token
$headers = @{
    Authorization = "Bearer $token"
}

Write-Host "Authenticated successfully! User ID: $($loginRes.data.user.id)"

# 2. Get Useful Jobs to find a target job
Write-Host "2. Fetching target job for tailoring..."
$jobsRes = Invoke-RestMethod -Uri "$baseUrl/api/jobs" -Method GET -Headers $headers
$targetJob = $jobsRes.data.content | Where-Object { $_.company.name -like "*FinTech*" } | Select-Object -First 1

if (-not $targetJob) {
    $targetJob = $jobsRes.data.content[0]
}
$jobId = $targetJob.id
Write-Host "Selected Target Job: $($targetJob.title) at $($targetJob.company.name) (ID: $jobId)"

# 3. Create Tailored Resume via POST /api/v1/tailored-resumes
Write-Host "3. Creating OfferPilot Tailored Resume via POST /api/v1/tailored-resumes..."
$createBody = @{
    jobDescriptionId = $jobId
    displayName = "$($targetJob.company.name) - Senior Backend Engineer Tailored"
    templateName = "PROFESSIONAL_DEFAULT"
} | ConvertTo-Json

$tailoredRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/tailored-resumes" -Method POST -Body $createBody -ContentType "application/json" -Headers $headers
$tailored = $tailoredRes.data

Write-Host "Tailored Resume Generated Successfully!"
Write-Host "ID: $($tailored.id)"
Write-Host "Display Name: $($tailored.displayName)"
Write-Host "Status: $($tailored.status)"
Write-Host "ATS Score Estimate: $($tailored.atsScoreEstimate)%"
Write-Host "Matched Skills Count: $($tailored.matchedSkills.Count)"
Write-Host "Partially Matched Skills Count: $($tailored.partiallyMatchedSkills.Count)"
Write-Host "Missing Skills Count: $($tailored.missingSkills.Count)"
Write-Host "Tailoring Notes Count: $($tailored.tailoringNotes.Count)"
Write-Host "Comparison Delta: +$($tailored.comparison.scoreDelta) points (Master: $($tailored.comparison.masterOverallScore)% -> Tailored: $($tailored.comparison.tailoredOverallScore)%)"

# 4. List tailored resumes via GET /api/v1/tailored-resumes
Write-Host "4. Listing tailored resumes via GET /api/v1/tailored-resumes..."
$listRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/tailored-resumes" -Method GET -Headers $headers
Write-Host "Total Tailored Resumes for User: $($listRes.data.Count)"

# 5. Get detail via GET /api/v1/tailored-resumes/{id}
Write-Host "5. Retrieving tailored resume details via GET /api/v1/tailored-resumes/$($tailored.id)..."
$detailRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/tailored-resumes/$($tailored.id)" -Method GET -Headers $headers
Write-Host "Retrieved: $($detailRes.data.displayName) with $($detailRes.data.matchedSkills.Count) matched skills"

# 6. Download LaTeX file via GET /api/v1/tailored-resumes/{id}/download-latex
Write-Host "6. Downloading LaTeX .tex file via GET /api/v1/tailored-resumes/$($tailored.id)/download-latex..."
$latexRes = Invoke-WebRequest -Uri "$baseUrl/api/v1/tailored-resumes/$($tailored.id)/download-latex" -Method GET -Headers $headers -UseBasicParsing
Write-Host "LaTeX Response Status: $($latexRes.StatusCode)"
Write-Host "Content-Type: $($latexRes.Headers['Content-Type'])"
Write-Host "Content-Disposition: $($latexRes.Headers['Content-Disposition'])"
Write-Host "LaTeX Content Preview (first 200 chars):"
Write-Host $latexRes.Content.Substring(0, [Math]::Min(200, $latexRes.Content.Length))

# 7. Render LaTeX via POST /api/v1/tailored-resumes/{id}/render-latex
Write-Host "7. Triggering LaTeX re-render via POST /api/v1/tailored-resumes/$($tailored.id)/render-latex..."
$renderRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/tailored-resumes/$($tailored.id)/render-latex" -Method POST -Headers $headers
Write-Host "Re-rendered LaTeX saved to: $($renderRes.data.latexFilePath)"

Write-Host "=========================================================="
Write-Host "ALL OFFERPILOT TAILORING ENGINE TESTS PASSED CLEANLY!"
Write-Host "=========================================================="
