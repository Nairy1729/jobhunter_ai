const fs = require('fs');
const clean = str => str.replace(/^\uFEFF/, '');
const jobs = JSON.parse(clean(fs.readFileSync('all_jobs_dump.json', 'utf8')));
const p1 = JSON.parse(clean(fs.readFileSync('pass1_results.json', 'utf8')));

console.log('| # | Company | Title | Location | WorkMode | MinYOE | Pass 1 Cat | Pass 1 Score | Pass 1 Freshness |');
console.log('|---|---|---|---|---|---|---|---|---|');
jobs.forEach((j, i) => {
  const res = p1.find(p => p.Id === j.id);
  const loc = (j.location || 'N/A').replace(/\|/g, '/');
  const tit = (j.title || 'N/A').replace(/\|/g, '/');
  console.log(`| ${i+1} | ${j.companyName} | ${tit} | ${loc} | ${j.workMode || 'N/A'} | ${j.minExperienceYears || 'N/A'} | ${res?.PriorityCategory} | ${res?.PriorityScore} | ${res?.Freshness} |`);
});
