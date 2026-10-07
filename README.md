# StudyTube

YouTube learning companion: track channels, take timestamped notes, resume where you left off.
One codebase → **web app (PC + iPad, installable PWA)** and **Android APK (Capacitor)**.

All app code lives in `www/index.html` (single file). No build step.

## Run locally
    npm run serve        # http://localhost:5173

## 1. Web app on PC and iPad
Host the `www/` folder on any HTTPS static host (Cloudflare Pages, Netlify, GitHub Pages — drag-and-drop works).
- PC (Chrome/Edge): open the URL → install icon in the address bar, or Settings → Install in the app.
- iPad (Safari): open the URL → Share → Add to Home Screen.

Data is stored per device (localStorage). Use Settings → Export / Import Backup to move data between devices.

### Optional: your own relay (recommended for the web version)
Browsers can't read YouTube directly (CORS), so the web version uses public relays that sometimes rate-limit.
For a stable setup, deploy this tiny Cloudflare Worker and paste its URL in Settings → "Your own relay"
as `https://YOUR-WORKER.workers.dev/?url={url}`:

    export default {
      async fetch(request) {
        const target = new URL(request.url).searchParams.get("url");
        if (!target || !/^https:\/\/(www\.)?youtube\.com\//.test(target)) {
          return new Response("Not allowed", { status: 400 });
        }
        const res = await fetch(target, { headers: { "User-Agent": "Mozilla/5.0" } });
        return new Response(res.body, {
          status: res.status,
          headers: { "Access-Control-Allow-Origin": "*", "Content-Type": res.headers.get("Content-Type") || "text/plain" },
        });
      },
    };

The Android app does not need a relay (CapacitorHttp makes requests natively).

## 2. Android APK
**Via GitHub Actions (no Android Studio):** push this folder to a GitHub repo → Actions → "Build Android APK"
→ download `StudyTube-debug-apk` from the run's artifacts → install on the phone.

**Locally:**
    npm install
    npx cap add android
    npx cap sync android
    npx cap open android      # build/run from Android Studio

Re-run `npx cap sync android` after changing anything in `www/`.
Change the app id in `capacitor.config.json` (`com.studytube.app`) before publishing.
