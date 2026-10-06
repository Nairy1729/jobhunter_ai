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
  const tempProfile = path.join(os.tmpdir(), 'ch_cdp_v2_' + Date.now());
  const port = 9223;

  console.log(`Launching headless Chrome on port ${port}...`);
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

  // 1. Landing Hero
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
    await sendCommand('Runtime.evaluate', {
      expression: `localStorage.setItem('jh_token', '${token}'); location.href = 'http://localhost:5173/';`
    });
    await sleep(4000);

    // 2. Private Jobs Feed
    console.log('Capturing 02_jobs_feed.png...');
    // Scroll down slightly so jobs and cards are in prominent view
    await sendCommand('Runtime.evaluate', {
      expression: `window.scrollTo({ top: 750, behavior: 'instant' });`
    });
    await sleep(1000);
    await takeScreenshot('02_jobs_feed.png');

    // Reset scroll
    await sendCommand('Runtime.evaluate', {
      expression: `window.scrollTo({ top: 0, behavior: 'instant' });`
    });
    await sleep(500);

    // Click INSPECT button on first job
    console.log('Clicking INSPECT button...');
    const inspectResult = await sendCommand('Runtime.evaluate', {
      expression: `
        (() => {
          const btns = Array.from(document.querySelectorAll('button'));
          const inspectBtn = btns.find(b => b.textContent.includes('INSPECT'));
          if (inspectBtn) {
            inspectBtn.click();
            return 'clicked inspect';
          }
          return 'inspect button not found';
        })()
      `,
      returnByValue: true
    });
    console.log('Inspect result:', inspectResult.result.value);
    await sleep(2500);

    // 3. Job Detail Modal - WHY THIS JOB
    console.log('Capturing 03_job_detail_why.png...');
    await takeScreenshot('03_job_detail_why.png');

    // Click RESUME TAILORING tab inside modal
    console.log('Clicking RESUME TAILORING tab...');
    const tailorResult = await sendCommand('Runtime.evaluate', {
      expression: `
        (() => {
          const btns = Array.from(document.querySelectorAll('button'));
          const tailorBtn = btns.find(b => b.textContent.includes('RESUME TAILORING'));
          if (tailorBtn) {
            tailorBtn.click();
            return 'clicked resume tailoring';
          }
          return 'resume tailoring tab not found';
        })()
      `,
      returnByValue: true
    });
    console.log('Tailor tab result:', tailorResult.result.value);
    await sleep(2500);

    // 4. Resume Tailoring view
    console.log('Capturing 04_resume_tailoring.png...');
    await takeScreenshot('04_resume_tailoring.png');

    // Close the Job Intelligence modal
    console.log('Closing Job Detail modal...');
    await sendCommand('Runtime.evaluate', {
      expression: `
        (() => {
          const closeBtns = Array.from(document.querySelectorAll('button')).filter(b => b.querySelector('svg.lucide-x') || b.querySelector('svg'));
          // Find the one in the modal header
          const modalClose = closeBtns.find(b => b.closest('.fixed'));
          if (modalClose) modalClose.click();
          else if (closeBtns[0]) closeBtns[0].click();
        })()
      `
    });
    await sleep(1500);

    // 5. Navigate to Government Jobs
    console.log('Navigating to Government Jobs tab...');
    const govResult = await sendCommand('Runtime.evaluate', {
      expression: `
        (() => {
          const btns = Array.from(document.querySelectorAll('button'));
          const govBtn = btns.find(b => b.textContent.includes('GOVERNMENT JOBS'));
          if (govBtn) {
            govBtn.click();
            return 'clicked gov jobs';
          }
          return 'gov jobs tab not found';
        })()
      `,
      returnByValue: true
    });
    console.log('Gov tab result:', govResult.result.value);
    await sleep(3000);

    // Scroll down slightly to show verified cards
    await sendCommand('Runtime.evaluate', {
      expression: `window.scrollTo({ top: 350, behavior: 'instant' });`
    });
    await sleep(1000);
    console.log('Capturing 05_government_feed.png...');
    await takeScreenshot('05_government_feed.png');

    // 6. Click View Details on first government job
    console.log('Clicking government job card...');
    const govDetailResult = await sendCommand('Runtime.evaluate', {
      expression: `
        (() => {
          const titles = Array.from(document.querySelectorAll('h3'));
          if (titles.length > 0) {
            titles[0].click();
            return 'clicked gov title';
          }
          const detailBtns = Array.from(document.querySelectorAll('button')).filter(b => b.textContent.includes('Details'));
          if (detailBtns.length > 0) {
            detailBtns[0].click();
            return 'clicked details btn';
          }
          return 'no gov job element found';
        })()
      `,
      returnByValue: true
    });
    console.log('Gov detail result:', govDetailResult.result.value);
    await sleep(2500);

    console.log('Capturing 06_government_detail.png...');
    await takeScreenshot('06_government_detail.png');

    // Close gov modal if open
    await sendCommand('Runtime.evaluate', {
      expression: `
        (() => {
          const closeBtns = Array.from(document.querySelectorAll('button')).filter(b => b.querySelector('svg.lucide-x') || b.textContent.includes('✕') || b.textContent.includes('Close'));
          if (closeBtns.length > 0) closeBtns[0].click();
        })()
      `
    });
    await sleep(1500);

    // Reset scroll & navigate to Profile
    console.log('Navigating to Candidate Profile Hub...');
    await sendCommand('Runtime.evaluate', {
      expression: `window.scrollTo({ top: 0, behavior: 'instant' });`
    });
    await sleep(500);

    const profileResult = await sendCommand('Runtime.evaluate', {
      expression: `
        (() => {
          const btns = Array.from(document.querySelectorAll('button'));
          const profBtn = btns.find(b => b.textContent.includes('PROFILE'));
          if (profBtn) {
            profBtn.click();
            return 'clicked profile tab';
          }
          return 'profile tab not found';
        })()
      `,
      returnByValue: true
    });
    console.log('Profile tab result:', profileResult.result.value);
    await sleep(3000);

    console.log('Capturing 07_profile_hub.png...');
    await takeScreenshot('07_profile_hub.png');
  }

  ws.close();
  chromeProcess.kill();
  try { fs.rmSync(tempProfile, { recursive: true, force: true }); } catch (e) {}
  console.log('Done! All screenshots updated.');
}

run().catch(console.error);
