# CB350RS Link — Android starter (v0.1.0)

A phone-first starter project for a Honda CB350RS DLX Pro companion app.

## What works in this first prototype

- Native Android screen (Kotlin; no web wrapper).
- Play/Pause, Previous and Next send standard Android media-key events to the active media session.
- Volume buttons adjust Android media-stream volume.
- BLE scan lists nearby Bluetooth Low Energy devices for initial bike-protocol investigation.

## What is NOT implemented yet

- Pairing/authentication with the CB350RS Bluetooth unit.
- Reading handlebar switch reports or mapping them to media commands.
- Bike instrument/ECU data, speed, fuel level or diagnostics.
- GPS ride recording, trip history, service log and background operation.

Honda's official documentation says RoadSync connects the phone and motorcycle over Bluetooth and uses the handlebar switch for phone functions. It does not publish a public third-party protocol in the material used for this starter. We must inspect the actual bike connection and BLE/GATT services before implementing handlebar input. Do not assume the handlebar controls are standard media keys.

## Build it using only an Android phone

This project includes a GitHub Actions workflow so the phone does not need to compile Android code locally.

1. Download and extract this ZIP on your Android phone.
2. In Chrome, open https://github.com and sign in (create an account if needed).
3. Create a new repository named `CB350RS-Link`.
4. Upload the extracted project files and folders to the repository. Make sure `.github/workflows/build-apk.yml` is included.
5. Open the repository's **Actions** tab. Enable Actions if GitHub asks.
6. Run **Build Android APK** using **Run workflow**, or push a commit to `main`.
7. When the workflow finishes successfully, open its run and download the `CB350RS-Link-debug` artifact.
8. Extract the downloaded artifact ZIP and tap `app-debug.apk` to install. Android may ask you to allow installs from that browser or file manager.

## Test music controls safely

1. Pair your helmet/headphones to the phone.
2. Start playback in a media app such as Spotify, YouTube Music or VLC.
3. Open CB350RS Link and test Previous, Play/Pause and Next while parked.
4. If an app ignores the command, check that it is actively playing and that Android shows its media notification.

## Next development stage: identify the Honda switch connection

- Keep the bike stationary, ignition on, and RoadSync available.
- Do not reset or delete your normal RoadSync pairing yet.
- Use the BLE scan and note any device names/RSSI that appear and disappear with the bike.
- A scan alone does not prove that a device is the bike; the next stage must inspect GATT services and characteristics and test the documented HID-over-GATT possibility.
- Do not attempt to reverse engineer or transmit unknown commands while riding.

## Technical baseline

- Kotlin, native Android Activity
- minSdk 26 / targetSdk 35 / compileSdk 36
- Android Gradle Plugin 8.13.2, Kotlin 2.2.21, Gradle 8.13, JDK 17
