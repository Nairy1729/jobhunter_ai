const fs = require('fs');
const path = require('path');

function getApiKey() {
  const envPath = path.resolve(__dirname, '..', '.env');
  if (fs.existsSync(envPath)) {
    const content = fs.readFileSync(envPath, 'utf8');
    for (const line of content.split('\n')) {
      const trimmed = line.trim();
      if (trimmed.startsWith('GEMINI_API_KEY=') || trimmed.startsWith('GOOGLE_API_KEY=')) {
        return trimmed.split('=')[1].trim().replace(/^["']|["']$/g, '');
      }
    }
  }
  return process.env.GEMINI_API_KEY || process.env.GOOGLE_API_KEY;
}

async function generateVoiceover() {
  const apiKey = getApiKey();
  if (!apiKey) {
    console.error('No GEMINI_API_KEY found in .env');
    process.exit(1);
  }

  const scriptText = 
    "Job hunting shouldn't mean searching thousands of irrelevant listings. " +
    "JobHunter learns what you're looking for and surfaces opportunities that actually match. " +
    "For private jobs, AI analyzes the role and tailors your existing resume, without inventing your experience. " +
    "And for government jobs, it discovers verified public opportunities and matches your true eligibility. " +
    "JobHunter. Find jobs worth applying to.";

  console.log('Requesting voiceover from gemini-3.8-flash-tts...');

  const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/interactions?key=${apiKey}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      model: 'gemini-3.8-flash-tts',
      input: [{
        type: 'user_input',
        content: [{
          type: 'text',
          text: scriptText,
          annotations: [{
            type: 'speech_metadata',
            style: 'confident, refined, calm, documentary narrator'
          }]
        }]
      }],
      response_format: { type: 'audio' },
      generation_config: {
        speech_config: [
          { voice: 'Charon' }
        ]
      }
    })
  });

  if (!response.ok) {
    const errorText = await response.text();
    console.error(`API Error ${response.status}:`, errorText);
    process.exit(1);
  }

  const data = await response.json();
  // Find audio in steps
  let audioBase64 = null;
  if (data.steps) {
    for (const step of data.steps) {
      if (step.type === 'model_output' && step.content) {
        for (const item of step.content) {
          if (item.type === 'audio' && item.data) {
            audioBase64 = item.data;
            break;
          }
        }
      }
    }
  }

  if (!audioBase64 && data.output_audio && data.output_audio.data) {
    audioBase64 = data.output_audio.data;
  }

  if (!audioBase64) {
    console.error('No audio data received in response:', JSON.stringify(data, null, 2));
    process.exit(1);
  }

  const audioBuffer = Buffer.from(audioBase64, 'base64');
  const outPath = path.resolve(__dirname, 'voiceover.wav');
  fs.writeFileSync(outPath, audioBuffer);
  console.log(`Voiceover saved successfully to ${outPath} (${audioBuffer.length} bytes)`);
}

generateVoiceover();
