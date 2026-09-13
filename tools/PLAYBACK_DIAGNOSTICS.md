# Local playback diagnostics

The fork already collects detailed Media3/Torbox playback diagnostics. When
**Playback diagnostics** is enabled in Advanced settings, the app now uploads a
snapshot every 30 seconds while a playback session is active. Manual report
buttons remain available for startup failures and playback errors.

## Start the collector

Keep the TV and this Mac on the same trusted LAN, then run from the repository:

```bash
./tools/start-playback-diagnostics.sh
```

Open the dashboard on the Mac at <http://127.0.0.1:8787/>. Reports are stored
under `diagnostics-data/` (ignored by Git) as JSON files.

The current build configuration points the app at `192.168.0.237:8787`. If the
Mac's LAN address changes, update `PLAYBACK_REPORTS_BASE_URL` in the ignored
`local.dev.properties` file and rebuild the APK. The bearer token in that file
must match the server token.

## What to inspect

Useful fields include `stream.host`, `stream.addonName`, `player.isTorrentStream`,
`playbackAnalytics.rebufferCount`, `rebufferTotalMs`, `healthSnapshots`,
`bandwidthEstimateBps`, `totalBytesLoaded`, `lastLoadError`, `droppedFrames`,
and the active video/audio formats.

These reports intentionally omit stream URLs and header values. Raw diagnostic
lines are redacted before upload, including URLs, cookies, authorization, and
API-token patterns. Do not expose this collector to the public internet; it is a
small temporary debugging server with local file storage.

Disable the setting after reproducing the problem and stop the collector with
Ctrl-C when finished.
