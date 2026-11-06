import JSZip from 'jszip';
import { ANDROID_PROJECT_FILES } from '../data/projectFiles';

export async function downloadAndroidProjectZip(): Promise<void> {
  const zip = new JSZip();

  // Add all Android Studio project files
  for (const file of ANDROID_PROJECT_FILES) {
    zip.file(file.path, file.content);
  }

  // Also include gradlew bash script and gradlew.bat
  zip.file(
    'gradlew',
    `#!/bin/sh
# Gradle startup script for UN*X
exec gradle "$@"
`
  );
  zip.file(
    'gradlew.bat',
    `@rem Gradle startup script for Windows
gradle %*
`
  );

  const content = await zip.generateAsync({ type: 'blob' });
  const url = URL.createObjectURL(content);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'FireLauncher-Android-Project.zip';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}
