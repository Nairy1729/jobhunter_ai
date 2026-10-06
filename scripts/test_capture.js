const { execFileSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const os = require('os');

const chromePath = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
const outDir = path.resolve(__dirname, 'frames');
if (!fs.existsSync(outDir)) fs.mkdirSync(outDir, { recursive: true });

function capture(url, outFile, width = 1080, height = 1920) {
  const tempProfile = path.join(os.tmpdir(), 'ch_prof_' + Math.random().toString(36).slice(2));
  console.log(`Capturing ${url} -> ${outFile}...`);
  const outPath = path.join(outDir, outFile);
  try {
    execFileSync(chromePath, [
      '--headless=new',
      '--disable-gpu',
      '--hide-scrollbars',
      '--no-sandbox',
      '--no-first-run',
      '--no-default-browser-check',
      '--disable-background-networking',
      '--disable-default-apps',
      '--disable-extensions',
      '--disable-sync',
      '--disable-translate',
      '--mute-audio',
      '--safebrowsing-disable-auto-update',
      '--virtual-time-budget=2500',
      `--user-data-dir=${tempProfile}`,
      `--window-size=${width},${height}`,
      `--screenshot=${outPath}`,
      url
    ], { timeout: 10000 });
    if (fs.existsSync(outPath)) {
      console.log(`Saved ${outFile} (${fs.statSync(outPath).size} bytes)`);
    } else {
      console.log(`File not created: ${outFile}`);
    }
  } catch (err) {
    console.error(`Error capturing ${outFile}:`, err.message);
  } finally {
    try { fs.rmSync(tempProfile, { recursive: true, force: true }); } catch (e) {}
  }
}

capture('http://localhost:5173/', 'landing_hero.png', 1080, 1920);
