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

  // Get or create page target
  const targets = await httpGet(`http://127.0.0.1:${port}/json/list`);
  const pageTarget = targets.find(t => t.type === 'page') || targets[0];
  const wsUrl = pageTarget.webSocketDebuggerUrl;

  console.log('Connecting WebSocket to page target:', wsUrl);
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
    mobile: true
  });

  // Helper to take screenshot
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
  await sleep(2000);
  await takeScreenshot('01_hero.png');

  // Login programmatically by setting token in localStorage
  // Let's get candidate token from backend API
  console.log('Authenticating candidate...');
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
    // Inject token and refresh
    await sendCommand('Runtime.evaluate', {
      expression: `localStorage.setItem('jobhunter_token', '${token}'); location.reload();`
    });
    await sleep(2500);

    // 2. Capture Private Jobs Feed
    console.log('Capturing Jobs Feed...');
    await takeScreenshot('02_jobs_feed.png');

    // Click first job card to reveal detail
    await sendCommand('Runtime.evaluate', {
      expression: `
        const card = document.querySelector('[data-job-card]') || document.querySelector('.cursor-pointer');
        if (card) card.click();
      `
    });
    await sleep(1500);
    await takeScreenshot('03_job_detail.png');

    // 3. Switch to Government Jobs tab
    console.log('Navigating to Government Jobs tab...');
    await sendCommand('Runtime.evaluate', {
      expression: `
        const btn = Array.from(document.querySelectorAll('button')).find(b => b.textContent.includes('Government'));
        if (btn) btn.click();
      `
    });
    await sleep(2000);
    await takeScreenshot('04_government_feed.png');

    // Click first government job to show official notification & verification
    await sendCommand('Runtime.evaluate', {
      expression: `
        const govCard = Array.from(document.querySelectorAll('button, div')).find(el => el.textContent.includes('View Details') || el.textContent.includes('Notification'));
        if (govCard) govCard.click();
      `
    });
    await sleep(1500);
    await takeScreenshot('05_government_detail.png');

    // 4. Switch to Profile tab
    console.log('Navigating to Profile tab...');
    await sendCommand('Runtime.evaluate', {
      expression: `
        const btn = Array.from(document.querySelectorAll('button')).find(b => b.textContent.includes('Profile'));
        if (btn) btn.click();
      `
    });
    await sleep(2000);
    await takeScreenshot('06_profile_hub.png');
  }

  ws.close();
  chromeProcess.kill();
  try { fs.rmSync(tempProfile, { recursive: true, force: true }); } catch (e) {}
  console.log('CDP Screen Capture completed successfully!');
}

run().catch(console.error);
