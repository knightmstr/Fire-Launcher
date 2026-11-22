# Debloat (optional)

[← Back to README](../README.md)

[`AI_STEPS.md`](../AI_STEPS.md) is a step-by-step guide you can follow yourself or hand to an AI coding agent (Claude Code, Gemini CLI…). It installs FireLauncher and disables five Amazon ad and telemetry services. Everything can be undone.

## The commands

```bash
# Disable
for p in com.amazon.advertising.firetv com.amazon.device.sale.service \
         com.amazon.client.metrics com.amazon.device.crashmanager com.amazon.ftv.screensaver; do
  adb shell pm disable-user --user 0 "$p"
done

# Undo
for p in com.amazon.advertising.firetv com.amazon.device.sale.service \
         com.amazon.client.metrics com.amazon.device.crashmanager com.amazon.ftv.screensaver; do
  adb shell pm enable "$p"
done
```


The fifth package, `com.amazon.ftv.screensaver`, is Amazon's screensaver. Leave it enabled if you don't use FireLauncher's [screensaver](EXTRAS.md#screensaver).
