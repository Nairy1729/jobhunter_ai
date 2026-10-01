const fs = require('fs');
const clean = str => str.replace(/^\uFEFF/, '');
const jobs = JSON.parse(clean(fs.readFileSync('all_jobs_dump.json', 'utf8')));
const p1 = JSON.parse(clean(fs.readFileSync('pass1_results.json', 'utf8')));

const idx = parseInt(process.argv[2] || '1', 10) - 1;
const j = jobs[idx];
const r = p1.find(p => p.Id === j.id);

console.log(`=== JOB #${idx + 1}: ${j.title} (${j.companyName}) ===`);
console.log(`Location: ${j.location} | WorkMode: ${j.workMode} | Min YOE: ${j.minExperienceYears}`);
console.log(`Pass 1: Category=${r?.PriorityCategory} | Score=${r?.PriorityScore} | Freshness=${r?.Freshness}`);
console.log(`Recommendation: ${r?.Recommendation}`);
console.log(`Why: ${r?.WhyThisJob}`);
console.log(`Concerns: ${r?.PotentialConcerns}`);
console.log(`Hard Violations: ${r?.HardViolations}`);
console.log(`\n--- FULL TEXT ---\n${j.rawDescriptionMarkdown}`);
