const fs = require('fs');
const start = parseInt(process.argv[2] || '1', 10);
const end = parseInt(process.argv[3] || '5', 10);

const clean = str => str.replace(/^\uFEFF/, '');
const jobs = JSON.parse(clean(fs.readFileSync('all_jobs_dump.json', 'utf8')));
console.log(`Total jobs loaded: ${jobs.length}`);

for (let i = start - 1; i < Math.min(end, jobs.length); i++) {
  const j = jobs[i];
  console.log(`\n======================================================`);
  console.log(`[Job ${i + 1}/29] ID: ${j.id}`);
  console.log(`Company: ${j.companyName} | Title: ${j.title}`);
  console.log(`Location: ${j.location} | WorkMode: ${j.workMode} | Min YOE: ${j.minExperienceYears}`);
  console.log(`Posting Date: ${j.postingDate} | URL: ${j.jobUrl}`);
  console.log(`--- DESCRIPTION ---`);
  console.log((j.rawDescriptionMarkdown || 'NO DESCRIPTION').substring(0, 1500));
}
