# UI Redesign

Applied the **Modern Premium Dark** direction to the Mundo/BGMI VIP Loader source.

## Updated

- Reworked the app background with a deep navy gradient.
- Refreshed cards with tighter corners, cleaner borders, and darker premium surfaces.
- Updated the primary action button with a blue-to-violet gradient and disabled state.
- Refined input, countdown, status, and secondary button colors.
- Improved spacing and contrast on the login and dashboard layouts.
- Preserved all existing view IDs and Java behavior.

## Validation

- All Android XML resources parse successfully.
- Full Gradle build could not run in the sandbox because an Android SDK path is not configured. Configure `ANDROID_HOME` or add `local.properties` with `sdk.dir` before building locally.
