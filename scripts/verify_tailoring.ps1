$ErrorActionPreference = "Stop"
$loginRes = Invoke-RestMethod -Uri 'http://localhost:8085/api/auth/login' -Method Post -ContentType 'application/json' -Body '{"email":"candidate@jobhunter.ai","password":"password123"}'
$token = $loginRes.data.token
Write-Host "Logged in successfully, token received."

$headers = @{ Authorization = "Bearer $token" }
$jobsRes = Invoke-RestMethod -Uri 'http://localhost:8085/api/jobs' -Method Get -Headers $headers
$jobs = $jobsRes.data.content
Write-Host ("Total jobs found: " + $jobs.Count)

$job = $jobs[0]
Write-Host ("Testing with Job ID: " + $job.id + " | Title: " + $job.title + " at " + $job.company)

$planRes = Invoke-RestMethod -Uri ("http://localhost:8085/api/jobs/" + $job.id + "/tailor/plan") -Method Get -Headers $headers
$plan = $planRes.data
Write-Host ("Plan summary: " + $plan.tailoringSummary)
Write-Host ("Skills prioritized count: " + $plan.prioritizedSkillKeywords.Count)
Write-Host ("Bullets sharpened count: " + $plan.bulletSharpeningProposals.Count)
if ($plan.bulletSharpeningProposals.Count -gt 0) {
    Write-Host ("Sample sharpened bullet:")
    Write-Host ("  Original: " + $plan.bulletSharpeningProposals[0].originalBullet)
    Write-Host ("  Tailored: " + $plan.bulletSharpeningProposals[0].tailoredBullet)
    Write-Host ("  Rationale: " + $plan.bulletSharpeningProposals[0].rationale)
}

$tailorRes = Invoke-RestMethod -Uri ("http://localhost:8085/api/jobs/" + $job.id + "/tailor") -Method Post -Headers $headers
$tailor = $tailorRes.data
Write-Host ("Tailor result status: " + $tailor.status)
Write-Host ("PDF Available: " + $tailor.pdfAvailable)
Write-Host ("Audited Claims count: " + $tailor.validationReport.auditedClaims.Count)
Write-Host ("Directly Supported Claims: " + $tailor.validationReport.directlySupportedClaims)
Write-Host ("Derived Supported Claims: " + $tailor.validationReport.derivedSupportedClaims)
Write-Host ("Unsupported Claims: " + $tailor.validationReport.unsupportedClaims)
Write-Host ("Validation Passed: " + $tailor.validationReport.passed)

$downloadRes = Invoke-WebRequest -Uri ("http://localhost:8085/api/tailored-resumes/" + $tailor.id + "/download") -Method Get -Headers $headers
Write-Host ("Downloaded Content-Type: " + $downloadRes.Headers['Content-Type'] + " | Content-Length: " + $downloadRes.RawContentLength)
Write-Host "SUCCESS: OfferPilot-style tailoring and PDF verification complete."
