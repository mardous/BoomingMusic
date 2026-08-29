# Requested permissions

Booming Music requests the following permissions:

| Permission                               | Description                                                                                 | Android version |
|------------------------------------------|---------------------------------------------------------------------------------------------|-----------------|
| **FOREGROUND\_SERVICE\_MEDIA\_PLAYBACK** | Essential for continuous playback in the background on Android 14 and higher.               | From 14         |
| **READ\_MEDIA\_AUDIO**                   | Allows access to audio files on the device. Required starting from Android 13.              | From 13         |
| **READ\_MEDIA\_IMAGES**                  | Allows the app to read image files (such as album covers) stored on the device.             | From 13         |
| **BLUETOOTH\_CONNECT**                   | Replaces `BLUETOOTH` on Android 12+ for detecting and interacting with Bluetooth devices.   | From 12         |
| **READ\_EXTERNAL\_STORAGE**              | Needed to list music files on devices running Android 12 or lower.                          | Up to 12        |
| **BLUETOOTH**                            | Used on Android 11 and below to detect Bluetooth devices (e.g., headphones).                | Up to 11        |
| **WRITE\_EXTERNAL\_STORAGE**             | Required on older versions to fully access external storage.                                | Up to 10        |
| **FOREGROUND\_SERVICE**                  | Allows the app to run a foreground service, essential for continuous playback.              | All             |
| **MODIFY\_AUDIO\_SETTINGS**              | Allows the app to manage audio focus and system audio settings for optimal playback.        | All             |
| **INTERNET**                             | Used to download artist images, artwork, lyrics, and check for updates.                     | All             |
| **ACCESS\_NETWORK\_STATE**               | Checks internet connection status before downloading content.                               | All             |
| **WAKE\_LOCK**                           | Prevents the device from sleeping during playback.                                          | All             |

If you still have questions about the permissions requested by the app, or if we have missed placing any on this list, feel free to contact us through this email: support@boomingmusic.org