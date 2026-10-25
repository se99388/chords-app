# Chords (Tab4u)

Android app: type a song name, get the chords from Tab4u.
Personal use only - respect Tab4u's terms of use.

## Features
- Search by song name (Hebrew or English); one match opens directly, several show a list
- Chords aligned above lyrics, monospace, RTL for Hebrew songs
- "Chords only" toggle, A− / A+ text size, back button returns to results

## Build with GitHub Actions (no Android Studio needed)
1. Create a GitHub repo and push this folder to the `main` branch.
2. The **Build APK** workflow runs automatically (Actions tab).
3. On your phone, open the repo's **Releases** page → *Latest build* → download `app-debug.apk`.
4. Allow "Install unknown apps" for your browser when Android asks, then install.

Every push to `main` rebuilds and replaces the APK in the *Latest build* release.

## Build with Android Studio
Open this folder in Android Studio, let it sync, plug in your phone (USB debugging on) and press Run.

## If results or chords come back empty
Tab4u may have changed its HTML. The parsing lives in
`app/src/main/java/com/ofir/chords/Tab4uClient.kt`:
- search results: links containing `tabs/songs/`
- song content: `#songContentTPL`, `td.chords*` (chord lines), `td.song` (lyric lines)
