## Fixes

- Removed `POST_NOTIFICATIONS` from required permissions and deleted `getNecessaryPermissions()`. Declining notifications or Bluetooth no longer blocks the app.
- `AbsBaseActivity` now requests storage permissions only. Bluetooth does not block app entry.
- `Preferences.requestedPermissions` now saves which permissions were requested. This keeps the Settings option available after a cold start.
- Permanently denied permissions now show "Settings" and open the App info screen with `Context.openAppDetailsSettings()`. "Grant access" previously did nothing.
- Moved backup restore to `OnboardViewModel`, with a blocking dialog and restart after success. This prevents the next preference write from erasing a restored backup.
- Split `welcome_to_x` around its placeholder to preserve word order in translations.
- Switch rows now use `toggleable`, the "Granted" badge is not a button, and the CTA uses `heightIn`. This fixes accessibility roles and large-font clipping.

## Performance

- Reading background rotation inside `graphicsLayer` no longer recomposes every frame.
- The minimum-duration slider saves once on release and only when changed. It previously wrote the full preference file at every drag step.
- Portrait list padding is remembered. This prevents remeasuring on every recomposition because `PaddingValues.plus` has no `equals`.
- `languageEntries()` is resolved once and only when needed. It previously ran 41 ICU lookups per composition.

## Other

- Both language dialogs now share `LanguageList`, so they show the same flags and English subtitles.
- `strings.xml` restores `permissions_needed` and `welcome_to_x`, adds notification permission strings, and removes unused `permission_bluetooth_denied`.
- Restored the same two keys in 39 translation files and removed the unused key.
- Moved `shapes` and `accompanist` to `version.ref` in `libs.versions.toml`.
