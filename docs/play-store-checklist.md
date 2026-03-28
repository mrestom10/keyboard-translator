# Google Play Store Publishing Checklist

## Pre-Build Setup

### 1. Generate Upload Keystore
Run in terminal:
```bash
keytool -genkey -v -keystore android/upload-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```
SAVE YOUR PASSWORD SECURELY. You need it for every future update.

### 2. Configure Signing
Edit `android/key.properties` and replace `REPLACE_WITH_YOUR_PASSWORD` with your actual keystore password.

### 3. Build Release AAB
```bash
flutter build appbundle --release
```
Output: `build/app/outputs/bundle/release/app-release.aab`

## Google Play Console Setup

### 4. Create Developer Account
- Go to https://play.google.com/console
- Pay $25 one-time registration fee
- Verify your identity

### 5. Create New App
- App name: **Translator Keyboard - Type & Translate**
- Default language: English (US)
- App type: App
- Free / Paid: Free

### 6. Store Listing (use docs/store-listing.md)
- [x] App name
- [x] Short description
- [x] Full description
- [ ] App icon (512x512 PNG) — generate from the adaptive icon or use a design tool
- [ ] Feature graphic (1024x500 PNG) — create a banner showing the keyboard
- [ ] Phone screenshots (minimum 2) — take screenshots of the keyboard in action
- [ ] Tablet screenshots (optional but recommended)

### 7. Required Screenshots
Take these screenshots on your phone:
1. The companion app home screen (setup page)
2. The keyboard open in a chat app (showing QWERTY)
3. The keyboard with translation (typed text + Translate button)
4. The Arabic keyboard layout
5. The GIF panel open
6. The emoji picker open

### 8. Content Rating
- Go to "Content rating" section
- Fill the IARC questionnaire
- Answer: No violence, no sexual content, no gambling
- Users can communicate: No (keyboard doesn't enable direct communication)
- This app is a tool: Yes

### 9. Privacy & Data Safety
- Privacy policy URL: Host `docs/privacy-policy.html` somewhere public
  - Easy option: GitHub Pages (push to a gh-pages branch)
  - Easy option: Firebase Hosting (free)
  - Easy option: Any static host (Netlify, Vercel)
- Data safety form:
  - Data collected: None
  - Data shared: Text sent to translation APIs (only when user taps Translate)
  - Data encrypted in transit: Yes (HTTPS)
  - Data deletion: N/A (no data stored)

### 10. Target Audience
- Target age group: 13+ (app accesses internet)
- Not designed for children

### 11. App Access
- No restricted access (no login required)

### 12. Ads
- Does not contain ads

### 13. App Category
- Category: Tools
- Tags: translator, keyboard, translation, multilingual

## Publishing

### 14. Create Release
- Go to Production > Create new release
- Upload the `.aab` file
- Release name: "1.0.0"
- Release notes: "Initial release of Translator Keyboard"

### 15. Review & Publish
- Review all sections (green checkmarks needed)
- Submit for review
- Typical review time: 1-3 days for new apps

## Post-Publishing

### 16. Monitor
- Check for crashes in Android Vitals
- Monitor reviews and ratings
- Respond to user feedback

## Files Created
- `docs/privacy-policy.html` — Privacy policy page
- `docs/store-listing.md` — Store listing text
- `docs/play-store-checklist.md` — This checklist
- `android/app/proguard-rules.pro` — ProGuard rules
- `android/key.properties` — Signing config (EDIT WITH YOUR PASSWORD)
- `android/app/src/main/res/drawable/ic_launcher_foreground.xml` — Adaptive icon foreground
- `android/app/src/main/res/drawable/ic_launcher_background.xml` — Adaptive icon background
- `android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` — Adaptive icon definition
