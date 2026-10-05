const fs = require('fs');

async function runPass2() {
  console.log("=== EXECUTING SECOND VALIDATION PASS (PASS 2) ===");

  // 1. Authenticate
  const loginRes = await fetch("http://localhost:8085/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: "candidate@jobhunter.ai", password: "password123" })
  });
  const loginJson = await loginRes.json();
  const token = loginJson.data.token;
  console.log("Authenticated as candidate@jobhunter.ai");

  // 2. Fetch all jobs
  const jobsRes = await fetch("http://localhost:8085/api/jobs?size=100", {
    headers: { "Authorization": `Bearer ${token}` }
  });
  const jobsJson = await jobsRes.json();
  const jobs = jobsJson.data.content;
  console.log(`Total jobs fetched from database: ${jobs.length}`);

  const pass2Results = [];

  for (let i = 0; i < jobs.length; i++) {
    const j = jobs[i];
    console.log(`Analyzing [${i+1}/${jobs.length}] ${j.companyName} - ${j.title}...`);
    try {
      const res = await fetch(`http://localhost:8085/api/jobs/${j.id}/analyze?forceRecompute=true`, {
        method: "POST",
        headers: { "Authorization": `Bearer ${token}` }
      });
      const data = (await res.json()).data;
      pass2Results.push({
        Id: j.id,
        Company: j.companyName,
        Title: j.title,
        WorkMode: j.workMode,
        Location: j.location,
        MinYoe: j.minExperienceYears,
        PostingDate: j.postingDate,
        PriorityCategory: data.priorityCategory,
        PriorityScore: data.priorityScore,
        Freshness: data.freshness,
        DaysSincePosted: data.daysSincePosted,
        Recommendation: data.recommendation,
        WhyThisJob: (data.whyThisJob || []).join(" | "),
        PotentialConcerns: (data.potentialConcerns || []).join(" | "),
        HardViolations: (data.hardConstraintViolations || []).join(" | "),
        RequirementCoverageCount: data.requirementCoverage ? data.requirementCoverage.length : 0,
        StrongMatchesCount: data.strongMatches ? data.strongMatches.length : 0,
        GapsCount: data.gaps ? data.gaps.length : 0
      });
    } catch (err) {
      console.error(`Error analyzing job ${j.id}:`, err);
      pass2Results.push({
        Id: j.id,
        Company: j.companyName,
        Title: j.title,
        Error: err.message
      });
    }
  }

  fs.writeFileSync('pass2_results.json', JSON.stringify(pass2Results, null, 2), 'utf8');
  console.log(`Successfully completed Pass 2 and saved to pass2_results.json`);
}

runPass2().catch(console.error);
