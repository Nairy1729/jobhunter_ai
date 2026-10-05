const fs = require('fs');
const clean = str => str.replace(/^\uFEFF/, '');
const jobs = JSON.parse(clean(fs.readFileSync('all_jobs_dump.json', 'utf8')));
const p1 = JSON.parse(clean(fs.readFileSync('pass1_results.json', 'utf8')));

const analysis = jobs.map((j, i) => {
  const res = p1.find(p => p.Id === j.id);
  const text = j.rawDescriptionMarkdown || '';
  
  // Extract key sections: Responsibilities, Requirements, Tech mentions
  return {
    num: i + 1,
    id: j.id,
    company: j.companyName,
    title: j.title,
    location: j.location,
    workMode: j.workMode,
    minYoe: j.minExperienceYears,
    postingDate: j.postingDate,
    systemCat: res ? res.PriorityCategory : 'N/A',
    systemScore: res ? res.PriorityScore : 0,
    systemFreshness: res ? res.Freshness : 'N/A',
    systemHardViolations: res ? res.HardViolations : '',
    textLength: text.length,
    first500: text.substring(0, 500).replace(/[\r\n]+/g, ' ')
  };
});

fs.writeFileSync('jobs_summary_analysis.json', JSON.stringify(analysis, null, 2));
console.log('Saved jobs_summary_analysis.json');
