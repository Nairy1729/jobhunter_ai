const fs = require('fs');
const clean = str => str.replace(/^\uFEFF/, '');
const p1 = JSON.parse(clean(fs.readFileSync('pass1_results.json', 'utf8')));
const j2 = p1.find(p => p.Company.includes('FinTech'));
console.log(JSON.stringify(j2, null, 2));
