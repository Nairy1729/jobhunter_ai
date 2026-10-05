const fs = require('fs');

const clean = str => str.replace(/^\uFEFF/, '');
const jobs = JSON.parse(clean(fs.readFileSync('all_jobs_dump.json', 'utf8')));
const p1 = JSON.parse(clean(fs.readFileSync('pass1_results.json', 'utf8')));

jobs.forEach((j, idx) => {
  const res = p1.find(p => p.Id === j.id);
  console.log(`\n======================================================`);
  console.log(`[Job ${idx + 1}/29] ID: ${j.id}`);
  console.log(`COMPANY: ${j.companyName} | TITLE: ${j.title}`);
  console.log(`LOCATION: ${j.location} | WORKMODE: ${j.workMode} | MIN_YOE: ${j.minExperienceYears}`);
  console.log(`POSTING DATE: ${j.postingDate}`);
  console.log(`SYSTEM PASS 1: Category=${res ? res.PriorityCategory : 'N/A'}, Score=${res ? res.PriorityScore : 'N/A'}, Freshness=${res ? res.Freshness : 'N/A'}`);
  console.log(`RECOMMENDATION: ${res ? res.Recommendation : 'N/A'}`);
  console.log(`HARD VIOLATIONS: ${res ? res.HardViolations : 'N/A'}`);
  console.log(`WHY: ${res ? res.WhyThisJob : 'N/A'}`);
  console.log(`CONCERNS: ${res ? res.PotentialConcerns : 'N/A'}`);
  const snippet = j.rawDescriptionMarkdown ? j.rawDescriptionMarkdown.substring(0, 350).replace(/[\r\n]+/g, ' ') : 'N/A';
  console.log(`SNIPPET: ${snippet}...`);
});
