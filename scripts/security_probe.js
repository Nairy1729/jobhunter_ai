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

async function runProbes() {
  console.log('=== 1. ACTUATOR SECURITY TEST ===');
  const health = await request({ host: 'localhost', port: 8085, path: '/actuator/health', method: 'GET' });
  console.log('/actuator/health status:', health.status, health.body);

  const beans = await request({ host: 'localhost', port: 8085, path: '/actuator/beans', method: 'GET' });
  console.log('/actuator/beans status:', beans.status, '(Expected 401 or 403)');

  console.log('\n=== 2. GOVERNMENT JOBS AUTHORIZATION TEST ===');
  const discover = await request({ host: 'localhost', port: 8085, path: '/api/government/jobs/discover', method: 'POST', headers: { 'Content-Type': 'application/json' } }, '{}');
  console.log('Unauthenticated POST /api/government/jobs/discover status:', discover.status, '(Expected 401 or 403)');

  const pubSearch = await request({ host: 'localhost', port: 8085, path: '/api/government/jobs?query=officer', method: 'GET' });
  console.log('Public GET /api/government/jobs?query=officer status:', pubSearch.status, '(Expected 200)');

  console.log('\n=== 3. SECURITY HEADERS TEST ===');
  console.log('X-Frame-Options:', health.headers['x-frame-options']);
  console.log('X-Content-Type-Options:', health.headers['x-content-type-options']);
  console.log('Referrer-Policy:', health.headers['referrer-policy']);
  console.log('Permissions-Policy:', health.headers['permissions-policy']);

  // Wait 60s for rate limit window to expire from previous run
  console.log('\nWaiting for rate limit window to reset (up to 60s)...');
  await sleep(61000);

  console.log('\n=== 4. IDOR RESUME PROTECTION TEST ===');
  const loginResp = await request({
    host: 'localhost',
    port: 8085,
    path: '/api/auth/login',
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  }, JSON.stringify({ email: 'candidate@jobhunter.ai', password: 'CandidatePassword123!' }));

  let token = null;
  try {
    const parsed = JSON.parse(loginResp.body);
    token = parsed.token || (parsed.data && parsed.data.token);
  } catch (e) {}

  if (token) {
    console.log('Logged in candidate successfully. Token acquired.');
    // Attempt to access an unauthorized / non-existent UUID resume
    const fakeResumeId = '00000000-0000-0000-0000-000000000000';
    const idorProbe = await request({
      host: 'localhost',
      port: 8085,
      path: `/api/resumes/${fakeResumeId}`,
      method: 'GET',
      headers: { 'Authorization': `Bearer ${token}` }
    });
    console.log(`GET /api/resumes/${fakeResumeId} status: ${idorProbe.status} (${idorProbe.body})`);
  } else {
    console.log('Candidate login failed:', loginResp.body);
  }

  console.log('\n=== 5. AUTH RATE LIMITING (SLIDING WINDOW 15/MIN) ===');
  let rateLimitHit = false;
  for (let i = 1; i <= 18; i++) {
    const login = await request({
      host: 'localhost',
      port: 8085,
      path: '/api/auth/login',
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    }, JSON.stringify({ email: 'attacker@example.com', password: 'WrongPassword123!' }));
    if (login.status === 429) {
      console.log(`Attempt ${i} intercepted with HTTP 429 Too Many Requests! Retry-After: ${login.headers['retry-after']}`);
      rateLimitHit = true;
      break;
    }
  }
  if (rateLimitHit) {
    console.log('[PASS: Rate Limiting active]');
  } else {
    console.log('[FAIL: Rate Limiting not triggered]');
  }
}

runProbes();
