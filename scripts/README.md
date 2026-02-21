# Google Photos Downloader Script

A [jbang](https://www.jbang.dev/) script that authenticates with Google Photos via OAuth2, presents a photo picker with thumbnails, and downloads selected photos to a local directory.

The script opens an **embedded browser** (powered by [jcefmaven](https://github.com/jcefmaven/jcefmaven)) inside a Swing window — no system browser is launched.

## Prerequisites

### 1. Install jbang

```bash
curl -Ls https://sh.jbang.dev | bash -s - app setup
```

Or see [jbang installation docs](https://www.jbang.dev/documentation/guide/latest/installation.html).

### 2. Enable Google Photos API & Create Credentials

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project (or select an existing one)
3. Enable the **Google Photos Library API**:
   - Navigate to *APIs & Services → Library*
   - Search for "Google Photos Library API" and enable it
4. Create OAuth 2.0 credentials:
   - Navigate to *APIs & Services → Credentials*
   - Click *Create Credentials → OAuth client ID*
   - Choose **Desktop app** as the application type
   - Download the credentials JSON file
5. Save the downloaded file as **`credentials.json`** in the directory where you will run the script

### 3. Linux: Display Server

On Linux, a display server (X11 or XWayland) is required for the Swing/JCEF window.

---

## Running the Script

Run from the repository root (where `credentials.json` is placed):

```bash
jbang scripts/downloadPhotos.java
```

> **First run**: jcefmaven will download the Chromium Embedded Framework binaries (~150 MB) into a `jcef-bundle/` directory. This is a one-time operation.

---

## Usage

1. The app opens a window with an embedded browser at `http://localhost:8090`
2. Click **Sign in with Google** and complete the OAuth flow
3. Your photos are displayed as a **thumbnail grid** (newest first)
4. **Select photos** by clicking thumbnails (multi-select is supported)
5. Use **Select All** to select the entire current page
6. Set a **folder name** in the input field (defaults to `photos-YYYY-MM-DD`)
7. Click **⬇ Download** — selected photos are saved to the named folder in full resolution
8. Click **Load More Photos** to fetch additional pages from your library

---

## Notes

- Only image files (not videos) are shown in the picker
- Photos are downloaded in full resolution by appending `=d` to the Google Photos base URL
- The `jcef-bundle/` directory and `credentials.json` are excluded from git (see `.gitignore`)
- The access token is held in memory only and is not persisted to disk
