const fs = require('fs');

const clean = str => str.replace(/^\uFEFF/, '');
const jobs = JSON.parse(clean(fs.readFileSync('all_jobs_dump.json', 'utf8')));
const p1 = JSON.parse(clean(fs.readFileSync('pass1_results.json', 'utf8')));
const p2 = JSON.parse(clean(fs.readFileSync('pass2_results.json', 'utf8')));

// Ground truth mapping
const groundTruthMap = {
  "Lingaro - Java Developer": { gt: "HIGH_PRIORITY", group: "Group A (Strong Match)", rationale: "Java Developer in India (Remote) matching candidate 2.5 YOE Java/Spring Boot/Postgres/Git stack." },
  "Software Engineer (Java / Spring Boot)": { gt: "HIGH_PRIORITY", group: "Group A (Strong Match) & F (Fresh)", rationale: "Exact core match: 2-4 YOE, Java 17, Spring Boot, Postgres, JWT, Docker, Bangalore, FinTech domain." },
  "Senior Software Engineer - Java, Node.js & AWS": { gt: "LOW_PRIORITY", group: "Group C (Borderline)", rationale: "Senior level role in UK requiring AWS mastery and mentoring not held by candidate." },
  "Senior Software Engineer": { gt: "LOW_PRIORITY", group: "Group C (Borderline) & G (Dedup)", rationale: "Senior level US Remote position requiring extensive generalist ownership." },
  "Software Engineer - Parsing (Principal/Staff/Senior)": { gt: "NOT_RECOMMENDED", group: "Group D (Unusual) & B (Poor)", rationale: "Principal/Staff/Senior tier; compiler/EDA hardware CAD parsing far outside candidate experience." },
  "Software Engineer – New College Graduate": { gt: "LOW_PRIORITY", group: "Group C (Borderline) & D (Unusual)", rationale: "New grad title for C++/Rust telemetry systems at SpaceX spin-off in California." },
  "Staff Software Engineer, Data Platform": { gt: "NOT_RECOMMENDED", group: "Group B (Poor Match)", rationale: "Staff tier requiring 8+ years leading Kafka data platform infrastructure." },
  "Backend Engineer": { gt: "NOT_RECOMMENDED", group: "Group B (Poor Match)", rationale: "Strict on-site presence in San Francisco, CA incompatible with candidate location." },
  "Software Engineer - Branching": { gt: "MEDIUM_PRIORITY", group: "Group C (Borderline)", rationale: "Remote global role at Supabase; 3 YOE, Docker/Postgres overlap; good stretch role." },
  "Software Engineer II": { gt: "LOW_PRIORITY", group: "Group G (Dedup) & C (Borderline)", rationale: "US Remote .NET Core role; secondary framework match with US remote restriction." },
  "Backend Engineer - Collaboration Services": { gt: "NOT_RECOMMENDED", group: "Group B (Poor) & F (Fresh)", rationale: "Strict on-site presence in New York, NY." },
  "Find your role: Open positions": { gt: "NOT_RECOMMENDED", group: "Group D (Misleading Title)", rationale: "Generic career hub title; actual JD is Technical Due Diligence Manager - Data Centers (10+ YOE)." },
  "Senior Software Engineer Java": { gt: "MEDIUM_PRIORITY", group: "Group C (Borderline)", rationale: "Java/Spring Boot FinTech in Noida, India; strong tech match but Senior 5 YOE makes it stretch." },
  "Senior Distributed Systems Engineer": { gt: "NOT_RECOMMENDED", group: "Group E (Keyword-Heavy) & F (Fresh)", rationale: "Netflix streaming platform in Los Gatos, CA requiring 5+ YOE distributed systems." },
  "Staff Software Engineer": { gt: "NOT_RECOMMENDED", group: "Group B (Poor Match)", rationale: "Staff tier requiring 8+ YOE." },
  "Senior Software Engineer": { gt: "NOT_RECOMMENDED", group: "Group B (Poor Match)", rationale: "London UK hybrid Senior role." },
  "Staff Software Engineer, Payments Platform": { gt: "NOT_RECOMMENDED", group: "Group B (Poor) & F (Fresh)", rationale: "Stripe Staff level payments role requiring 6+ YOE." },
  "Software Engineer - Security": { gt: "NOT_RECOMMENDED", group: "Group B (Poor Match)", rationale: "Strict on-site in New York, NY with 5+ YOE security focus." },
  "Staff Software Engineer - Java, Springboot, SAAS": { gt: "NOT_RECOMMENDED", group: "Group E (Keyword-Heavy)", rationale: "Staff tier (7+ YOE) at Celonis despite Java/Spring Boot/Bangalore keywords." },
  "Senior iOS Engineer (Swift / Objective-C)": { gt: "NOT_RECOMMENDED", group: "Group B (Poor Match)", rationale: "Non-transferable native iOS/Swift stack; on-site in Singapore." },
  "Senior Software Engineer I (Backend Engineer)": { gt: "MEDIUM_PRIORITY", group: "Group C (Borderline)", rationale: "Remote Java backend position with 3 YOE requirement accessible to candidate." },
  "Senior Backend Engineer": { gt: "NOT_RECOMMENDED", group: "Group B (Poor Match)", rationale: "Hardware/drone robotics with C++/Linux on-site in Oakland, CA." },
  "Senior Backend Engineer - Adtech / SSP": { gt: "NOT_RECOMMENDED", group: "Group B (Poor Match)", rationale: "Real-time AdTech SSP with mandatory 3-days/week in Bellevue, WA office." },
  "Sr./Staff Backend Engineer - Java (India)": { gt: "NOT_RECOMMENDED", group: "Group E (Keyword-Heavy)", rationale: "Staff tier (5+ YOE) at Oscilar despite Java/India keywords." },
  "Senior Backend Engineer": { gt: "NOT_RECOMMENDED", group: "Group B (Poor Match)", rationale: "Autonomous AI software platform requiring on-site presence in Cambridge, MA." },
  "Staff Backend Engineer, Hierarchy": { gt: "NOT_RECOMMENDED", group: "Group B (Poor Match)", rationale: "Staff tier at ClickUp." },
  "Senior Backend Engineer": { gt: "MEDIUM_PRIORITY", group: "Group C (Borderline)", rationale: "Remote fraud prevention backend; strong Node.js, Postgres, FinTech alignment." },
  "Senior Backend Engineer - (Java/Spring Boot)": { gt: "NOT_RECOMMENDED", group: "Group E (Keyword-Heavy)", rationale: "Onsite 3 days per week in New York, NY." },
  "Backend Engineer - Security Cleared": { gt: "NOT_RECOMMENDED", group: "Group D (Misleading Title)", rationale: "Requires active US Government Security Clearance and Denver, CO hybrid presence." }
};

console.log("| # | Company | Title | Ground Truth | Pass 1 Cat (Score) | Pass 2 Cat (Score) | Delta / Status |");
console.log("|---|---|---|---|---|---|---|");

let pass1Correct = 0;
let pass2Correct = 0;
let pass1HighCorrect = 0;
let pass2HighCorrect = 0;
let totalHigh = 2; // Lingaro and FinTech Solutions

p2.forEach((j2, i) => {
  const j1 = p1.find(p => p.Id === j2.Id);
  const title = j2.Title;
  const company = j2.Company;

  // Find ground truth
  let gtObj = null;
  for (const [k, v] of Object.entries(groundTruthMap)) {
    if (title.includes(k) || k.includes(title)) {
      gtObj = v;
      break;
    }
  }
  if (!gtObj) {
    gtObj = { gt: "LOW_PRIORITY", group: "General", rationale: "Standard evaluation" };
  }

  const p1Cat = j1 ? j1.PriorityCategory : "N/A";
  const p1Score = j1 ? j1.PriorityScore : 0;
  const p2Cat = j2.PriorityCategory;
  const p2Score = j2.PriorityScore;

  if (p1Cat === gtObj.gt) pass1Correct++;
  if (p2Cat === gtObj.gt) pass2Correct++;

  if (gtObj.gt === "HIGH_PRIORITY") {
    if (p1Cat === "HIGH_PRIORITY") pass1HighCorrect++;
    if (p2Cat === "HIGH_PRIORITY") pass2HighCorrect++;
  }

  let status = "";
  if (p1Cat !== p2Cat) {
    if (p2Cat === gtObj.gt) status = "IMPROVED (Resolved to GT)";
    else status = `Changed: ${p1Cat} -> ${p2Cat}`;
  } else {
    status = p2Cat === gtObj.gt ? "CONFIRMED (Exact GT)" : "Unchanged";
  }

  console.log(`| ${i+1} | ${company} | ${title.substring(0, 35)} | **${gtObj.gt}** | ${p1Cat} (${p1Score}) | **${p2Cat} (${p2Score})** | ${status} |`);
});

console.log("\n=======================================================");
console.log(`PASS 1 Exact GT Accuracy: ${pass1Correct}/29 (${(pass1Correct/29*100).toFixed(1)}%)`);
console.log(`PASS 2 Exact GT Accuracy: ${pass2Correct}/29 (${(pass2Correct/29*100).toFixed(1)}%)`);
console.log(`PASS 1 High-Priority Recall: ${pass1HighCorrect}/${totalHigh} (${(pass1HighCorrect/totalHigh*100).toFixed(1)}%)`);
console.log(`PASS 2 High-Priority Recall: ${pass2HighCorrect}/${totalHigh} (${(pass2HighCorrect/totalHigh*100).toFixed(1)}%)`);
console.log("=======================================================");
