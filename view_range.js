const fs = require('fs');
const clean = str => str.replace(/^\uFEFF/, '');
const jobs = JSON.parse(clean(fs.readFileSync('all_jobs_dump.json', 'utf8')));
const p1 = JSON.parse(clean(fs.readFileSync('pass1_results.json', 'utf8')));

const from = parseInt(process.argv[2] || '1', 10);
const to = parseInt(process.argv[3] || '10', 10);

for (let i = from - 1; i < Math.min(to, jobs.length); i++) {
  const j = jobs[i];
  const res = p1.find(p => p.Id === j.id);
  console.log(`[${i+1}] ${j.companyName} | ${j.title}`);
  console.log(`    Location: ${j.location} | WorkMode: ${j.workMode} | MinYOE: ${j.minExperienceYears}`);
  console.log(`    Pass 1 Cat: ${res?.PriorityCategory}, Score: ${res?.PriorityScore}, Freshness: ${res?.Freshness}`);
  console.log(`    HardViolations: ${res?.HardViolations}`);
}
