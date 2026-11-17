# Peercall Android

This is the Android-only native Kotlin starter for the Peercall dialer and balance experience.

## Structure

- `app/` contains the Android application module.
- `MainActivity.kt` hosts the Compose entry point.
- `DialerScreen.kt` contains the dialer UI and quota display.
- `QuotaRepository.kt` reads the last server-confirmed quota from encrypted storage.
- `PeercallInCallService.kt` observes Telecom call state through Android's public APIs.

## Run in Android Studio

1. Open the `peercall-android` folder in Android Studio.
2. Let Gradle sync.
3. Run the app on a device or emulator.

## Notes

This is an Android app using the platform Telecom APIs. Calls reach the EPC server only if the target device's OEM telephony stack is configured to route them there. EPC destination signaling, server quota refresh, SMS reconciliation, and server-confirmed balance updates are not implemented because their contracts are not present in this project. A cached balance is shown only when explicitly marked server-confirmed; it is not used to authorize or charge calls.
