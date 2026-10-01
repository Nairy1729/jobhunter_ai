const http = require('http');

async function getJobAnalysis(jobId) {
  // Login
  const loginData = JSON.stringify({ email: "candidate@jobhunter.ai", password: "password123" });
  const loginRes = await fetch("http://localhost:8085/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: loginData
  });
  const loginJson = await loginRes.json();
  const token = loginJson.data.token;

  const res = await fetch(`http://localhost:8085/api/jobs/${jobId}/analyze`, {
    method: "POST",
    headers: { "Authorization": `Bearer ${token}` }
  });
  const json = await res.json();
  return json.data;
}

getJobAnalysis("2b9a7658-94b3-4441-a299-6828ba344c65").then(data => {
  console.log("Total Requirements:", data.requirementCoverage.length);
  data.requirementCoverage.forEach((rc, i) => {
    console.log(`[${i+1}] ${rc.importance} | ${rc.coverage} | ${rc.candidateEvidence} | REQ: ${rc.requirement} | EXP: ${rc.explanation}`);
  });
}).catch(console.error);
