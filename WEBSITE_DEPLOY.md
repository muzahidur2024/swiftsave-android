# DownMe — website APK distribution

Everything under `website/` is ready to upload to **tefarab.xyz**. The app already links to your privacy page and support email.

---

## What we prepared (already in the repo)

| Item | Purpose |
|------|---------|
| `website/downme/index.html` | Public download page (install steps + download button) |
| `website/downloads/` | Folder for the APK (APK files are gitignored) |
| `scripts/package-website-release.ps1` | Builds release APK, copies it here, updates version/SHA on the page |

**Target URLs after upload:**

- Page: `https://www.tefarab.xyz/downme/`
- APK: `https://www.tefarab.xyz/downloads/DownMe-1.0.1-arm64.apk` (name updates with each release)

---

## Your part (on tefarab.xyz)

### 1. Build the APK package locally

From PowerShell:

```powershell
cd "d:\swiftsave app\swiftsave-android"
.\scripts\package-website-release.ps1
```

This creates `website/downloads/DownMe-<version>-arm64.apk` and refreshes `website/downme/index.html` with size and SHA-256.

**Before going public:** create `keystore.properties` + a `.jks` file and run the script again **without** debug signing (see `keystore.properties.example` below).

### 2. Upload two folders to your host

Use **FTP**, **cPanel File Manager**, or your site builder’s file upload:

| Upload this local folder | To this path on the server |
|--------------------------|----------------------------|
| `website/downme/` (contains `index.html`) | `public_html/downme/` (or equivalent) |
| `website/downloads/` (contains the `.apk`) | `public_html/downloads/` |

So both URLs work:

- `https://www.tefarab.xyz/downme/` → shows the download page  
- `https://www.tefarab.xyz/downloads/DownMe-1.0.1-arm64.apk` → direct file download  

**Important:** Large APK (~67 MB). If upload fails, zip the APK or use your host’s “large file” / SFTP option.

### 3. Confirm in the browser

- Open `https://www.tefarab.xyz/downme/` — page loads, button downloads the APK.  
- On a phone, install from that APK and test one download.  
- Privacy link works: `https://www.tefarab.xyz/privacy-downme`

### 4. Link the page from your main site

Add a button on **tefarab.xyz** (home or “Work” section), e.g. **“Get DownMe (Android)”** → `https://www.tefarab.xyz/downme/`.

### 5. Optional: HTTPS / MIME

Most hosts serve `.apk` correctly. If download opens as text, set MIME type `application/vnd.android.package-archive` for `.apk` in cPanel or server config.

---

## Release keystore (your part — do before wide release)

Do **not** commit real passwords. Create once:

```powershell
cd "d:\swiftsave app\swiftsave-android"
keytool -genkeypair -v `
  -keystore downme-release.p12 `
  -storetype PKCS12 `
  -alias downme `
  -keyalg RSA `
  -keysize 2048 `
  -validity 10000
```

Copy `keystore.properties.example` → `keystore.properties` and fill in paths/passwords. Re-run `package-website-release.ps1`.

**Already created `downme-release.jks`?** Migrate once (use a new filename — do not use the same name for src and dest):

```powershell
keytool -importkeystore `
  -srckeystore downme-release.jks `
  -destkeystore downme-release.p12 `
  -deststoretype PKCS12
```

Enter the old JKS password, then set the new PKCS12 password (same password is fine). Point `storeFile` in `keystore.properties` at `swiftsave-android/downme-release.p12` and keep the `.jks` only as a backup.

---

## Updating to a new version

1. Bump `versionCode` / `versionName` in `app/build.gradle.kts`.  
2. Run `.\scripts\package-website-release.ps1`.  
3. Upload the **new** APK to `downloads/` (keep old APK optional for rollback).  
4. Re-upload `downme/index.html` if the script updated it.

---

## Not included (you do later)

- Google Play Console listing  
- Custom domain DNS (if not already on tefarab.xyz)  
- Analytics on the download page  
- iOS build  

For Play Store, use the same `applicationId`: `com.downme.app` and a production keystore.
