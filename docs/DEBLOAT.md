# Debloat (optional)

[← Back to README](../README.md)

Turns off four Amazon services that show ads and send usage data. They're disabled, not deleted, and you can undo it any time.

| Package | What it does |
|---|---|
| `com.amazon.advertising.firetv` | Ads |
| `com.amazon.device.sale.service` | Deals and sale promotions |
| `com.amazon.client.metrics` | Usage data sent to Amazon |
| `com.amazon.device.crashmanager` | Crash reports sent to Amazon |

## Do it

You need adb, as in the [Install guide](INSTALL.md). Copy the whole block; each line is one package:

> **On a computer:** run these in the `platform-tools` folder and type `.\adb` (Windows) or `./adb` (Mac, Linux) instead of `adb`. See [Get adb](INSTALL.md#1-get-adb).

```bash
adb connect 192.168.1.50:5555    # your TV's IP
adb shell pm disable-user --user 0 com.amazon.advertising.firetv
adb shell pm disable-user --user 0 com.amazon.device.sale.service
adb shell pm disable-user --user 0 com.amazon.client.metrics
adb shell pm disable-user --user 0 com.amazon.device.crashmanager
adb reboot
```

Each line prints `Package … new state: disabled-user`. If one says it can't be disabled or isn't found, your Fire OS doesn't have it or doesn't allow it. That's fine; the others still work.

**Optional:** if you use [Onyx TV's screensaver](EXTRAS.md#screensaver), you can also turn off Amazon's sponsored screensaver. Skip this if you use Amazon's.

```bash
adb shell pm disable-user --user 0 com.amazon.ftv.screensaver
```

## Undo

```bash
adb connect 192.168.1.50:5555    # your TV's IP
adb shell pm enable com.amazon.advertising.firetv
adb shell pm enable com.amazon.device.sale.service
adb shell pm enable com.amazon.client.metrics
adb shell pm enable com.amazon.device.crashmanager
adb shell pm enable com.amazon.ftv.screensaver
adb reboot
```

Each line prints `Package … new state: enabled`. The screensaver line is harmless if you never disabled it.

## Using an AI agent instead

If you use an AI coding agent such as Claude Code or Gemini CLI, it can do all of this for you, along with the install and extras. See [Let an AI agent do it](INSTALL.md#let-an-ai-agent-do-it).
