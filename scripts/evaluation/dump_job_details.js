const fs = require('fs');
const clean = str => str.replace(/^\uFEFF/, '');
const jobs = JSON.parse(clean(fs.readFileSync('all_jobs_dump.json', 'utf8')));

jobs.forEach((j, i) => {
  console.log(`\n======================================================`);
  console.log(`### JOB #${i+1}: ${j.id}`);
  console.log(`Company: ${j.companyName}`);
  console.log(`Title: ${j.title}`);
  console.log(`Location: ${j.location} | WorkMode: ${j.workMode}`);
  console.log(`Min YOE: ${j.minExperienceYears}`);
  console.log(`Posting Date: ${j.postingDate}`);
  console.log(`Job URL: ${j.jobUrl}`);
  console.log(`--- RAW TEXT ---`);
  console.log(j.rawDescriptionMarkdown || 'NO DESCRIPTION');
});
