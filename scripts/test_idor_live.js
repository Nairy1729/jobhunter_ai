const http = require('http');

function request(options, body = null) {
  return new Promise((resolve) => {
    const req = http.request(options, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => resolve({ status: res.statusCode, headers: res.headers, body: data }));
    });
    req.on('error', err => resolve({ error: err.message }));
    if (body) req.write(body);
    req.end();
  });
}

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function run() {
  console.log('=== IDOR LIVE CROSS-TENANT TEST ===');

  // 1. Login as Candidate A
  const loginA = await request({
    host: 'localhost',
    port: 8085,
    path: '/api/auth/login',
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  }, JSON.stringify({ email: 'candidate@jobhunter.ai', password: 'password123' }));

  const dataA = JSON.parse(loginA.body);
  const tokenA = dataA.token || (dataA.data && dataA.data.token);
  console.log('Candidate A Login Status:', loginA.status, tokenA ? 'Token Acquired' : 'Failed');

  // 2. Fetch Candidate A's resumes
  const resumeListResp = await request({
    host: 'localhost',
    port: 8085,
    path: '/api/resumes',
    method: 'GET',
    headers: { 'Authorization': `Bearer ${tokenA}` }
  });
  console.log('Candidate A Resumes Status:', resumeListResp.status);
  const parsedResumes = JSON.parse(resumeListResp.body);
  const resumeId = parsedResumes.data && parsedResumes.data.length > 0 ? parsedResumes.data[0].id : null;
  console.log('Candidate A Resume ID:', resumeId);

  // 3. Register or Login as Candidate B (Attacker)
  const regB = await request({
    host: 'localhost',
    port: 8085,
    path: '/api/auth/register',
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  }, JSON.stringify({
    email: 'attacker_test_' + Date.now() + '@jobhunter.ai',
    password: 'AttackerPassword123!',
    firstName: 'Attacker',
    lastName: 'User'
  }));

  const dataB = JSON.parse(regB.body);
  const tokenB = dataB.token || (dataB.data && dataB.data.token);
  console.log('Attacker User B Register Status:', regB.status, tokenB ? 'Token Acquired' : 'Failed');

  if (resumeId && tokenB) {
    // 4. Attacker attempts to fetch Candidate A's resume by ID
    const idorAttack = await request({
      host: 'localhost',
      port: 8085,
      path: `/api/resumes/${resumeId}`,
      method: 'GET',
      headers: { 'Authorization': `Bearer ${tokenB}` }
    });
    console.log(`\n=== IDOR EXPLOIT ATTEMPT ===`);
    console.log(`Attacker GET /api/resumes/${resumeId} Status: ${idorAttack.status}`);
    console.log(`Response Body: ${idorAttack.body}`);

    if (idorAttack.status === 403) {
      console.log('\n[PASS] IDOR DEFENSE VERIFIED: Candidate A resume protected against cross-tenant access with HTTP 403 Forbidden!');
    } else {
      console.log('\n[FAIL] Unexpected status code:', idorAttack.status);
    }
  }
}

run();
