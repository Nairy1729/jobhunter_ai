const fs = require('fs');
const data = JSON.parse(fs.readFileSync('pass2_results.json', 'utf8'));
const jobs = [2, 4, 10, 11, 18, 19];
jobs.forEach(id => {
  const j = data.find(x => x.jobId === id);
  if (j) {
    console.log(`=== Job ${j.jobId}: ${j.title} @ ${j.company} ===`);
    console.log(`Location: ${j.location}, WorkMode: ${j.workMode}`);
    console.log(`Score: ${j.score}, Category: ${j.category}`);
    console.log(`Violations: ${JSON.stringify(j.constraintViolations)}`);
    console.log(`Match Breakdown:`, j.matchBreakdown);
    console.log(`Gaps (${j.gaps ? j.gaps.length : 0}):`, (j.gaps || []).map(g => g.name || g.requirementName));
    console.log(`-----------------------------------------------`);
  }
});
