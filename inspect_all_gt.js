const fs = require('fs');
const clean = str => str.replace(/^\uFEFF/, '');
const jobs = JSON.parse(clean(fs.readFileSync('all_jobs_dump.json', 'utf8')));
const p1 = JSON.parse(clean(fs.readFileSync('pass1_results.json', 'utf8')));

console.log("================================================================================");
console.log("DETAILED INSPECTION OF ALL 29 JOBS FOR GROUND TRUTH DETERMINATION");
console.log("================================================================================");

jobs.forEach((j, i) => {
  const res = p1.find(p => p.Id === j.id);
  const desc = j.rawDescriptionMarkdown || '';
  
  // Detect seniority words in title
  const t = j.title.toLowerCase();
  let level = "MID/UNSPECIFIED";
  if (t.includes("intern") || t.includes("graduate") || t.includes("entry") || t.includes("junior")) level = "ENTRY/JUNIOR";
  else if (t.includes("staff") || t.includes("principal") || t.includes("director") || t.includes("lead")) level = "STAFF/PRINCIPAL/LEAD";
  else if (t.includes("senior") || t.includes("sr.")) level = "SENIOR";
  else if (t.includes(" ii") || t.includes(" 2")) level = "MID (LEVEL II)";

  // Detect tech stack
  const hasJava = /java\b/i.test(desc) || /spring\b/i.test(desc);
  const hasDotNet = /\.net\b|c#\b/i.test(desc);
  const hasPython = /python\b/i.test(desc);
  const hasNode = /node\.js\b|node\b/i.test(desc);
  const hasGo = /\bgo\b|\bgolang\b/i.test(desc);
  const hasSwift = /swift\b|ios\b|objective-c/i.test(desc);

  console.log(`\n[${i+1}] ${j.companyName} | ${j.title}`);
  console.log(`    Level: ${level} | MinYOE: ${j.minExperienceYears || 'N/A'} | Location: ${j.location || 'N/A'} | WorkMode: ${j.workMode || 'N/A'}`);
  console.log(`    Tech: Java/Spring: ${hasJava} | .NET: ${hasDotNet} | Python: ${hasPython} | Node: ${hasNode} | Go: ${hasGo} | Swift/iOS: ${hasSwift}`);
  console.log(`    Pass 1 System: Cat=${res?.PriorityCategory}, Score=${res?.PriorityScore}, Freshness=${res?.Freshness}`);
  console.log(`    Pass 1 Rec: ${res?.Recommendation} | HardViolations: ${res?.HardViolations ? res.HardViolations.substring(0, 100) : 'None'}`);
});
