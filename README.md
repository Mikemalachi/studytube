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

### Your own relay (needed for the web version)
Browsers can't read YouTube directly (CORS), and free public relays often block or rate-limit. Deploy `worker/relay.js`
as a free Cloudflare Worker:
1. dash.cloudflare.com → Workers & Pages → Create → **Hello World** → name it `studytube-relay` → Deploy.
2. Click **Edit code**, replace everything with the contents of `worker/relay.js`, click **Deploy**.
3. Copy the worker URL (`https://studytube-relay.YOURNAME.workers.dev`).
4. In the app: Settings → Network → "Your own relay" → paste `https://studytube-relay.YOURNAME.workers.dev/?url={url}`
   (keep the `?url={url}` part), then tap **Test relays**.
5. To avoid pasting on every device, put the same URL in `DEFAULT_RELAY` near the top of the relay code in `www/index.html` and push.

The Android app does not need a relay (CapacitorHttp makes requests natively).

### Set up another device with a QR code
Settings → **Set up another device** copies your relay address and channel list between devices (merge, no duplicates).
- On the device that is already set up: **Show QR** (or **Copy link**).
- On the new device: **Scan QR** (camera) or **Paste a setup link**.
- Notes, history and favourites are not included; use **Export Backup / Import Backup** for those.
- iPad: a Home Screen app and Safari keep separate data. Scan from inside the installed app (Settings → Scan QR), not with the Camera app.
- The Android app needs the camera permission; the APK workflow adds it automatically.

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

## Sync across devices (notes, favorites, watch later, completed, history, resume)

Sync is end-to-end encrypted: your device encrypts everything before upload, so the server only stores scrambled text.
Pick ONE storage option.

### Option 1: Cloudflare (uses your existing relay Worker)
1. Cloudflare dashboard > Storage & databases > KV > Create a namespace, name it `studytube-sync`.
2. Workers & Pages > your relay Worker > Settings > Bindings > Add > KV namespace. Variable name must be exactly `SYNC`. Pick `studytube-sync`. Save/Deploy.
3. Edit code on the Worker, paste the new `worker/relay.js`, Deploy.
4. In the app: Settings > Sync across devices > Turn on sync.

Free plan limits (check the dashboard for current numbers): KV allows about 1,000 writes a day. The app only writes when something changed and batches changes (about once a minute while a video is playing).

### Option 2: Google Sheets
1. Create a new Google Sheet. Extensions > Apps Script. Delete the sample code and paste `worker/google-sheets-sync.gs`. Save.
2. Deploy > New deployment > type Web app. Execute as: Me. Who has access: Anyone. Deploy, then Authorize (Google shows an "unverified app" warning for your own script: Advanced > Go to project).
3. Copy the Web app URL (ends in `/exec`).
4. In the app: Settings > Sync across devices > Use Google Sheets instead > paste the URL, then Turn on sync.

### Adding more devices
On a device that already syncs: Settings > Set up another device > Show QR. Scan it on the new device. It carries the relay (or Sheets address), channels and the sync key. Keep that QR private: the sync key opens your synced data.

### How conflicts are handled
Every item has a timestamp. The newest edit wins per item, and deletions are remembered, so editing on two devices while offline merges cleanly. Device clocks should be roughly correct.

## Player, batch add and feed limit

- **Add many channels at once:** Add channel now takes a list (one per line or comma separated). Failed lines stay in the box so you can retry.
- **Videos per channel:** Settings, Videos per channel. Only the newest N per channel show on Home and the Video Feed. Nothing is deleted.
- **Player:** video on top, Notes and Checkpoints as tabs, note box pinned at the bottom. The note's timestamp is captured when you start typing. "Pause while typing" is optional.
- **Focus:** the Focus button (or Settings, Player) hides the StudyTube bar while watching. Use the back arrow to leave.
- **Mini player:** the Mini button, or leaving the Player tab, keeps the video playing in a draggable mini player. Tap its bar to expand, drag to move, X to close.

## Handwriting and export
- In the player, tap ✍️ next to Add to write a note by hand (Apple Pencil on iPad; fingers can be enabled with ☝️). Pen, highlighter, eraser, colors, sizes, undo/redo, lined/grid/plain paper. ⛶ makes the editor full screen and floats the video as the mini player. Handwritten notes sync like text notes.
- Export (⬇️ in the player, Export in Library, or Settings): PDF or PNG image with typed and handwritten notes; Markdown, HTML and JSON also available. Every timestamp is a link like https://youtu.be/ID?t=SECONDS that opens the video at that moment.

## Study tools
- Player: speed (tap 1×, or [ and ] on a keyboard), skip −10/−5/+5/+10 (arrow keys), A–B loop, a watch heat-strip under the video, pause points on checkpoints (⏸ in the Checkpoints tab), watch log, rewind 7 s after a break of 15+ minutes.
- Notes: wrap words as {{c1::answer}} (the [...] button does it for the selection) or write "question :: answer" on a line. Library → Review quizzes them with spaced repetition. 📋 inserts note templates. 🔗 Link copies an app link that opens that moment.
- Library: In progress (closest to done first), Courses (ordered paths, optional locks, share as a page), Matrix (Eisenhower sort of Watch Later), Stats (activity heat map, channel health).
- Home: Review, Courses, One video (study a link without adding its channel), Local file (video or audio kept on this device).
- Settings: hide titles by keyword, blurred or text-only thumbnails.
- Everything except local file contents, A–B loops and settings syncs between devices.

## Look and icon
- Settings → Appearance: accent colour (10 presets or any colour) and 6 backgrounds. Stored on each device.
- APK icon: `android-icons/` holds the launcher icons. The GitHub workflow copies them over the default icon after `cap add android`. Regenerate from `android-icons/icon-1024.png` if you want a different design. The web/iPad icons are in `www/icons/`.
