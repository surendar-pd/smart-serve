const { spawn } = require('node:child_process');
const fs = require('node:fs');
const path = require('node:path');

function usage() {
  console.error('Usage: node tools/run-gradle.cjs <task...>');
  console.error('Example: node tools/run-gradle.cjs assembleDebug');
}

const args = process.argv.slice(2);
if (args.length === 0) {
  usage();
  process.exit(1);
}

const isWin = process.platform === 'win32';
const gradleWrapper = isWin ? 'gradlew.bat' : './gradlew';

const wrapperPath = isWin
  ? path.join(process.cwd(), 'gradlew.bat')
  : path.join(process.cwd(), 'gradlew');

if (!fs.existsSync(wrapperPath)) {
  console.error(`Could not find Gradle wrapper at: ${wrapperPath}`);
  console.error(
    'This command must be run from an Android app directory that contains the Gradle wrapper (e.g. apps/provider-app or apps/customer-app).'
  );
  process.exit(1);
}

const child = spawn(gradleWrapper, args, {
  stdio: 'inherit',
  shell: isWin,
  cwd: process.cwd(),
});

child.on('error', (err) => {
  console.error(`Failed to start Gradle wrapper (${gradleWrapper}).`);
  console.error(err?.message ?? String(err));
  process.exit(1);
});

child.on('exit', (code, signal) => {
  if (signal) process.exit(1);
  process.exit(code ?? 1);
});

