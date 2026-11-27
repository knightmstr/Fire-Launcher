# Permissions & privacy

[← Back to README](../README.md)

| Permission | Why |
|---|---|
| Internet | Weather (Open-Meteo), Reddit wallpapers, update checks (GitHub) |
| Photos | Only for your **Onyx TV** album, if you allow it |
| Install packages | Installing updates; Android asks you every time |
| Start at boot | Re-applying your Home button and screensaver choices after a restart |
| Secure settings | Only after you grant it, on the TV or with adb (see [Extras](EXTRAS.md#the-one-time-permission)); every change can be undone |
| Accessibility (Home button) | Only sees which **Amazon launcher** screen opened, never screen content |
| Notification access (Now playing) | Only reads what's playing; never reads notifications |

There's no analytics or tracking, and no accounts. Traffic to the internet is HTTPS only.

**Set up from phone** runs a small web server on your home network, only while that screen is open on the TV (and for at most 10 minutes without use). It answers only requests carrying the one-time code from the TV's QR code, and only when they address the TV by its IP address. That local traffic is plain HTTP, since a TV on a home network has no certificate.
