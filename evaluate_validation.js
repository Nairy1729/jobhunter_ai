const fs = require('fs');

const clean = str => str.replace(/^\uFEFF/, '');
const p1 = JSON.parse(clean(fs.readFileSync('pass1_results.json', 'utf8')));
const p2 = JSON.parse(clean(fs.readFileSync('pass2_results.json', 'utf8')));

// Exact Ground Truth defined per job ID and company based on human review
const groundTruthById = {
  "10db4444-5c8d-4645-b184-7e43af550e19": {
    company: "Lingarogroup",
    title: "Lingaro - Java Developer",
    group: "Group A (Strong Match)",
    gt: "HIGH_PRIORITY",
    rationale: "Java Developer in India (Remote) matching candidate 2.5 YOE Java/Spring Boot/Postgres/Git stack."
  },
  "2b9a7658-94b3-4441-a299-6828ba344c65": {
    company: "FinTech Solutions",
    title: "Software Engineer (Java / Spring Boot)",
    group: "Group A (Strong Match) & F (Freshness)",
    gt: "HIGH_PRIORITY",
    rationale: "Exact core match: 2-4 YOE, Java 17, Spring Boot, Postgres, JWT, Docker, Bangalore, FinTech domain."
  },
  "7eec851d-19d5-4b5c-baa6-be9fac6d1e31": {
    company: "Xdesign",
    title: "Senior Software Engineer - Java, Node.js & AWS",
    group: "Group C (Borderline / Stretch)",
    gt: "MEDIUM_PRIORITY",
    rationale: "Remote Java role with Node.js and AWS requirements; good stretch opportunity."
  },
  "226b0246-d9ed-461c-b53f-293502c2743c": {
    company: "Renaissancelearning Nam",
    title: "Senior Software Engineer",
    group: "Group C (Borderline) & G (Deduplication)",
    gt: "LOW_PRIORITY",
    rationale: "Senior US Remote role requiring extensive AWS and frontend experience outside core candidate profile."
  },
  "88ef85c8-d476-4409-af29-3b2458cf9816": {
    company: "Allspice",
    title: "Software Engineer - Parsing (Principal/Staff/Senior)",
    group: "Group D (Misleading Title) & B (Poor Match)",
    gt: "NOT_RECOMMENDED",
    rationale: "Principal/Staff tier; compiler/EDA hardware CAD parsing far outside candidate experience."
  },
  "cf5cc5e0-5fcc-4447-bb5c-fe0a557ea9ea": {
    company: "Siftstack",
    title: "Software Engineer – New College Graduate",
    group: "Group D (Misleading Title) & C (Borderline)",
    gt: "LOW_PRIORITY",
    rationale: "New grad title for C++/Rust telemetry systems at SpaceX spin-off in California."
  },
  "d2b823ab-3cb4-4db8-99e0-2a08fa7d766b": {
    company: "Beaconbiosignals",
    title: "Staff Software Engineer, Data Platform",
    group: "Group B (Poor Match - Seniority)",
    gt: "NOT_RECOMMENDED",
    rationale: "Staff tier requiring 8+ years leading Kafka data platform infrastructure."
  },
  "2af62bed-bf6e-4426-8289-a45a36d46945": {
    company: "Doss",
    title: "Backend Engineer",
    group: "Group B (Poor Match - Location)",
    gt: "NOT_RECOMMENDED",
    rationale: "Strict on-site presence in San Francisco, CA incompatible with candidate location."
  },
  "28d690b3-d447-422f-91c0-f9a827e0f961": {
    company: "Supabase",
    title: "Software Engineer - Branching",
    group: "Group C (Borderline / Stretch)",
    gt: "MEDIUM_PRIORITY",
    rationale: "Remote global role at Supabase; 3 YOE, Docker/Postgres overlap; solid stretch role."
  },
  "dd234661-4066-4c34-898f-7d261fcd4d6a": {
    company: "Renaissancelearning Nam",
    title: "Software Engineer II",
    group: "Group G (Deduplication) & C (Borderline)",
    gt: "LOW_PRIORITY",
    rationale: "US Remote .NET Core role; secondary framework match with US remote restriction."
  },
  "978b4163-96e5-4bbe-a3d7-e66b08aebb03": {
    company: "Figma",
    title: "Backend Engineer - Collaboration Services",
    group: "Group B (Poor Match) & F (Freshness)",
    gt: "NOT_RECOMMENDED",
    rationale: "Strict on-site presence in New York, NY."
  },
  "efd05447-1af7-4be8-932f-7a1c3b093fad": {
    company: "Nebius",
    title: "Find your role: Open positions",
    group: "Group D (Misleading Title)",
    gt: "NOT_RECOMMENDED",
    rationale: "Generic career hub title; actual JD is Technical Due Diligence Manager - Data Centers (10+ YOE)."
  },
  "96bdd5b0-927c-4f31-98fc-25befab53e7c": {
    company: "Netspend Careers Page",
    title: "Senior Software Engineer Java",
    group: "Group C (Borderline / Stretch)",
    gt: "MEDIUM_PRIORITY",
    rationale: "Java/Spring Boot FinTech in Delhi NCR, India; strong tech match but Senior 5 YOE makes it stretch."
  },
  "eda70d48-cf81-48a5-8d4d-f840f26e6f6e": {
    company: "Netflix",
    title: "Senior Distributed Systems Engineer",
    group: "Group E (Keyword-Heavy) & F (Freshness)",
    gt: "NOT_RECOMMENDED",
    rationale: "Netflix streaming platform in Los Gatos, CA requiring 5+ YOE distributed systems."
  },
  "a343bebb-8f74-497c-a180-b70f4af7e256": {
    company: "Engine",
    title: "Staff Software Engineer",
    group: "Group B (Poor Match - Seniority)",
    gt: "NOT_RECOMMENDED",
    rationale: "Staff tier requiring 8+ YOE."
  },
  "44fb002a-41a7-4633-bb66-a49e3e70e67e": {
    company: "Swap",
    title: "Senior Software Engineer",
    group: "Group B (Poor Match) / C",
    gt: "LOW_PRIORITY",
    rationale: "London UK e-commerce logistics, Node/TypeScript stack with 5+ YOE."
  },
  "a825eb63-843b-43c9-bc8a-9a973deab4ed": {
    company: "Stripe",
    title: "Staff Software Engineer, Payments Platform",
    group: "Group B (Poor Match) & F (Freshness)",
    gt: "NOT_RECOMMENDED",
    rationale: "Stripe Staff level payments role requiring 6+ YOE."
  },
  "a2ef9db9-9a9a-41c5-acbf-d207987b06ab": {
    company: "Thread Ai",
    title: "Software Engineer - Security",
    group: "Group B (Poor Match - Location)",
    gt: "NOT_RECOMMENDED",
    rationale: "Strict on-site in New York, NY with 5+ YOE security focus."
  },
  "8c4643d5-7538-45f5-9e2f-c6fcbc669776": {
    company: "Celonis",
    title: "Staff Software Engineer - Java, Springboot, SAAS",
    group: "Group E (Keyword-Heavy)",
    gt: "NOT_RECOMMENDED",
    rationale: "Staff tier (7+ YOE) at Celonis despite Java/Spring Boot/Bangalore keywords."
  },
  "535e3b08-3adf-4cc4-b72f-442ec97cfbb5": {
    company: "ByteDance",
    title: "Senior iOS Engineer (Swift / Objective-C)",
    group: "Group B (Poor Match - Technology)",
    gt: "NOT_RECOMMENDED",
    rationale: "Non-transferable native iOS/Swift stack; on-site in Singapore."
  },
  "69f4d0c9-b332-4d9e-9e66-ef65932ec0d6": {
    company: "Inductive Automation Llc",
    title: "Senior Software Engineer I (Backend Engineer)",
    group: "Group C (Borderline / Stretch)",
    gt: "MEDIUM_PRIORITY",
    rationale: "Remote Java backend position with 3 YOE requirement accessible to candidate."
  },
  "176bc74d-1e49-43cb-a321-73efeffff38e": {
    company: "Arxlight Ai",
    title: "Senior Backend Engineer",
    group: "Group C (Borderline / Stretch)",
    gt: "MEDIUM_PRIORITY",
    rationale: "Remote fraud prevention backend; strong Node.js, Postgres, FinTech alignment."
  },
  "8a91db45-45a3-4e42-8c1a-d8db3b1b1190": {
    company: "Nexxen",
    title: "Senior Backend Engineer - Adtech / SSP",
    group: "Group B (Poor Match - Location)",
    gt: "NOT_RECOMMENDED",
    rationale: "Real-time AdTech SSP with mandatory 3-days/week in Bellevue, WA office."
  },
  "50cf7df9-6dde-444d-824d-2251c4e3a999": {
    company: "Oscilar",
    title: "Sr./Staff Backend Engineer - Java (India)",
    group: "Group E (Keyword-Heavy)",
    gt: "NOT_RECOMMENDED",
    rationale: "Staff tier (5+ YOE) at Oscilar despite Java/India keywords."
  },
  "9fc42faf-daf7-4430-9103-47943819125e": {
    company: "Blitzy",
    title: "Senior Backend Engineer",
    group: "Group B (Poor Match - Location)",
    gt: "NOT_RECOMMENDED",
    rationale: "Autonomous AI software platform requiring on-site presence in Cambridge, MA."
  },
  "e4f49622-06ff-4049-840a-eeb421bb17a7": {
    company: "Clickup",
    title: "Staff Backend Engineer, Hierarchy",
    group: "Group B (Poor Match - Seniority)",
    gt: "NOT_RECOMMENDED",
    rationale: "Staff tier at ClickUp."
  },
  "9ed28dfe-94fe-4761-82aa-44950c0da50c": {
    company: "Gridverify",
    title: "Senior Backend Engineer",
    group: "Group B (Poor Match) / C",
    gt: "MEDIUM_PRIORITY",
    rationale: "Remote backend role with Go/Python/Distributed systems; stretch opportunity."
  },
  "a6e18c8e-91a2-4033-93ff-b8acb4acc955": {
    company: "Gen Digital",
    title: "Senior Backend Engineer - (Java/Spring Boot)",
    group: "Group E (Keyword-Heavy)",
    gt: "NOT_RECOMMENDED",
    rationale: "Onsite 3 days per week in New York, NY."
  },
  "c54d002a-858f-4d7c-b751-527fd35f038d": {
    company: "Quindar",
    title: "Backend Engineer - Security Cleared",
    group: "Group D (Misleading Title)",
    gt: "NOT_RECOMMENDED",
    rationale: "Requires active US Government Security Clearance and Denver, CO hybrid presence."
  }
};

let pass1Correct = 0;
let pass2Correct = 0;
let pass1HighCorrect = 0;
let pass2HighCorrect = 0;
let totalHigh = 2; // Lingaro and FinTech Solutions

console.log("| # | Company | Title | Group | Ground Truth | Pass 1 Cat (Score) | Pass 2 Cat (Score) | Status |");
console.log("|---|---|---|---|---|---|---|---|");

p2.forEach((j2, i) => {
  const j1 = p1.find(p => p.Id === j2.Id);
  const id = j2.Id;
  const gtObj = groundTruthById[id] || { gt: "LOW_PRIORITY", group: "General", rationale: "Standard evaluation" };

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
    if (p2Cat === gtObj.gt) status = "IMPROVED (Matches GT)";
    else status = `Shifted: ${p1Cat} -> ${p2Cat}`;
  } else {
    status = p2Cat === gtObj.gt ? "CONFIRMED (Matches GT)" : "Unchanged";
  }

  console.log(`| ${i+1} | ${gtObj.company} | ${gtObj.title.substring(0, 32)} | ${gtObj.group.substring(0, 16)} | **${gtObj.gt}** | ${p1Cat} (${p1Score}) | **${p2Cat} (${p2Score})** | ${status} |`);
});

console.log("\n=======================================================");
console.log(`PASS 1 Exact GT Accuracy: ${pass1Correct}/29 (${(pass1Correct/29*100).toFixed(1)}%)`);
console.log(`PASS 2 Exact GT Accuracy: ${pass2Correct}/29 (${(pass2Correct/29*100).toFixed(1)}%)`);
console.log(`PASS 1 High-Priority Recall: ${pass1HighCorrect}/${totalHigh} (${(pass1HighCorrect/totalHigh*100).toFixed(1)}%)`);
console.log(`PASS 2 High-Priority Recall: ${pass2HighCorrect}/${totalHigh} (${(pass2HighCorrect/totalHigh*100).toFixed(1)}%)`);
console.log("=======================================================");
