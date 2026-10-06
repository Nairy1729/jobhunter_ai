const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');
const os = require('os');
const http = require('http');

const chromePath = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
const outDir = path.resolve(__dirname, 'frames');
if (!fs.existsSync(outDir)) fs.mkdirSync(outDir, { recursive: true });

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

function httpGet(url) {
  return new Promise((resolve, reject) => {
    http.get(url, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try { resolve(JSON.parse(data)); } catch (e) { resolve(data); }
      });
    }).on('error', reject);
  });
}

async function run() {
  const tempProfile = path.join(os.tmpdir(), 'ch_cdp_' + Date.now());
  const port = 9222;

  console.log('Launching headless Chrome on port 9222...');
  const chromeProcess = spawn(chromePath, [
    '--headless=new',
    '--disable-gpu',
    '--hide-scrollbars',
    '--no-sandbox',
    '--no-first-run',
    '--no-default-browser-check',
    `--remote-debugging-port=${port}`,
    `--user-data-dir=${tempProfile}`,
    '--window-size=1080,1920',
    'about:blank'
  ]);

  chromeProcess.on('error', err => console.error('Chrome spawn error:', err));

  // Wait for CDP to be available
  let version = null;
  for (let i = 0; i < 30; i++) {
    try {
      version = await httpGet(`http://127.0.0.1:${port}/json/version`);
      if (version && version.webSocketDebuggerUrl) break;
    } catch (e) {}
    await sleep(200);
  }

  if (!version || !version.webSocketDebuggerUrl) {
    console.error('Could not connect to Chrome DevTools Protocol');
    chromeProcess.kill();
    return;
  }

  console.log('Connected to Chrome CDP!');

  const targets = await httpGet(`http://127.0.0.1:${port}/json/list`);
  const pageTarget = targets.find(t => t.type === 'page') || targets[0];
  const wsUrl = pageTarget.webSocketDebuggerUrl;
  const ws = new WebSocket(wsUrl);

  let idCounter = 1;
  const callbacks = new Map();

  function sendCommand(method, params = {}) {
    return new Promise((resolve, reject) => {
      const id = idCounter++;
      callbacks.set(id, { resolve, reject });
      ws.send(JSON.stringify({ id, method, params }));
    });
  }

  await new Promise((resolve, reject) => {
    ws.onopen = resolve;
    ws.onerror = reject;
  });

  ws.onmessage = (event) => {
    try {
      const msg = JSON.parse(event.data);
      if (msg.id && callbacks.has(msg.id)) {
        const cb = callbacks.get(msg.id);
        callbacks.delete(msg.id);
        if (msg.error) cb.reject(new Error(msg.error.message));
        else cb.resolve(msg.result);
      }
    } catch (e) {}
  };

  await sendCommand('Page.enable');
  await sendCommand('Runtime.enable');
  await sendCommand('Emulation.setDeviceMetricsOverride', {
    width: 1080,
    height: 1920,
    deviceScaleFactor: 1,
    mobile: false
  });

  async function takeScreenshot(fileName) {
    const res = await sendCommand('Page.captureScreenshot', { format: 'png' });
    const buffer = Buffer.from(res.data, 'base64');
    const outPath = path.join(outDir, fileName);
    fs.writeFileSync(outPath, buffer);
    console.log(`Saved screenshot ${fileName} (${buffer.length} bytes)`);
  }

  // 1. Capture Landing Hero
  console.log('Navigating to Landing Page...');
  await sendCommand('Page.navigate', { url: 'http://localhost:5173/' });
  await sleep(2500);
  await takeScreenshot('01_hero.png');

  // Authenticate candidate
  console.log('Authenticating candidate via backend API...');
  const loginRes = await new Promise((resolve) => {
    const req = http.request({
      host: 'localhost',
      port: 8085,
      path: '/api/auth/login',
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    }, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => resolve(JSON.parse(data)));
    });
    req.write(JSON.stringify({ email: 'candidate@jobhunter.ai', password: 'password123' }));
    req.end();
  });

  const token = loginRes.token || (loginRes.data && loginRes.data.token);
  console.log('Acquired candidate auth token:', !!token);

  if (token) {
    // Inject token as 'jh_token'
    await sendCommand('Runtime.evaluate', {
      expression: `localStorage.setItem('jh_token', '${token}'); location.reload();`
    });
    await sleep(3500);

    // 2. Capture Private Jobs Feed
    console.log('Capturing Jobs Feed...');
    await takeScreenshot('02_jobs_feed.png');

    // Click first job card
    console.log('Opening job detail...');
    await sendCommand('Runtime.evaluate', {
      expression: `
        const card = document.querySelector('[data-job-id]') || Array.from(document.querySelectorAll('div, article')).find(el => el.textContent.includes('Software Engineer') || el.textContent.includes('Engineer'));
        if (card) card.click();
      `
    });
    await sleep(2000);
    await takeScreenshot('03_job_detail_why.png');

    // Click Tailor tab
    console.log('Opening Tailor tab...');
    await sendCommand('Runtime.evaluate', {
      expression: `
        const tailorBtn = Array.from(document.querySelectorAll('button')).find(b => b.textContent.toLowerCase().includes('tailor'));
        if (tailorBtn) tailorBtn.click();
      `
    });
    await sleep(2000);
    await takeScreenshot('04_resume_tailoring.png');

    // Close detail modal if open
    await sendCommand('Runtime.evaluate', {
      expression: `
        const closeBtn = document.querySelector('button[aria-label="Close"]') || Array.from(document.querySelectorAll('button')).find(b => b.querySelector('svg.lucide-x') || b.textContent === '✕');
        if (closeBtn) closeBtn.click();
      `
    });
    await sleep(1000);

    // 3. Navigate to Government Jobs
    console.log('Navigating to Government Jobs...');
    await sendCommand('Runtime.evaluate', {
      expression: `
        const govTab = Array.from(document.querySelectorAll('button')).find(b => b.textContent.includes('Government'));
        if (govTab) govTab.click();
      `
    });
    await sleep(2500);
    await takeScreenshot('05_government_feed.png');

    // Open first government job detail
    console.log('Opening government job detail...');
    await sendCommand('Runtime.evaluate', {
      expression: `
        const detailBtn = Array.from(document.querySelectorAll('button')).find(b => b.textContent.includes('Notification') || b.textContent.includes('Details'));
        if (detailBtn) detailBtn.click();
      `
    });
    await sleep(2000);
    await takeScreenshot('06_government_detail.png');

    // Close modal & navigate to Profile
    await sendCommand('Runtime.evaluate', {
      expression: `
        const closeBtn = Array.from(document.querySelectorAll('button')).find(b => b.textContent.includes('Close') || b.querySelector('svg.lucide-x'));
        if (closeBtn) closeBtn.click();
      `
    });
    await sleep(1000);

    console.log('Navigating to Profile...');
    await sendCommand('Runtime.evaluate', {
      expression: `
        const profTab = Array.from(document.querySelectorAll('button')).find(b => b.textContent.includes('Profile'));
        if (profTab) profTab.click();
      `
    });
    await sleep(2500);
    await takeScreenshot('07_profile_hub.png');
  }

  ws.close();
  chromeProcess.kill();
  try { fs.rmSync(tempProfile, { recursive: true, force: true }); } catch (e) {}
  console.log('All screens captured successfully!');
}

run().catch(console.error);
