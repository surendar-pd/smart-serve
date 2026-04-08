# SmartServeApp

<a alt="Nx logo" href="https://nx.dev" target="_blank" rel="noreferrer"><img src="https://raw.githubusercontent.com/nrwl/nx/master/images/nx-logo.png" width="45"></a>

Nx workspace with two Android apps: **provider-app** and **customer-app** (Jetpack Compose, Kotlin).

## Running the apps

**Prerequisites**

- [Node.js](https://nodejs.org/) (LTS)
- [Yarn](https://yarnpkg.com/) (v4; enable with `corepack enable` if needed)
- [Android Studio](https://developer.android.com/studio) (or Android SDK + emulator / device with USB debugging)

**1. Install dependencies**

From the repository root:

```sh
yarn install
```

The workspace uses **Yarn 4** (see `packageManager` in `package.json`). There are **no `scripts` in `package.json`** for the Android apps—invoke Gradle through **Nx** with `yarn nx …` as below. Nx is configured in `nx.json`; app targets live in each app’s `project.json` (e.g. `build`, `installDebug`).

**2. Build an app**

```sh
# Provider app
yarn nx build provider-app

# Customer app
yarn nx build customer-app
```

**3. Install and run on a device or emulator**

Start an Android emulator or connect a device, then:

```sh
# Provider app
yarn nx run provider-app:installDebug

# Customer app
yarn nx run customer-app:installDebug
```

**4. Hot reload (app stays running, no full restart)**

For the smoothest experience—apply code changes without reinstalling or restarting the app—run the app from **Android Studio**:

1. Open the app folder in Android Studio (e.g. `File → Open → apps/provider-app`).
2. Run the app (green Play or Debug).
3. With the app running, use **Apply Code Changes** (⌃F10 / Ctrl+F10) or **Apply Changes and Restart Activity** (⌃⌥F10) after editing code. Only changed code is built and pushed to the device; the app stays running (or only the activity restarts).

**Alternative: run with Gradle directly**

```sh
# Provider app
cd apps/provider-app && ./gradlew installDebug

# Customer app
cd apps/customer-app && ./gradlew installDebug
```

On **Windows**, use the wrapper batch file:

```sh
cd apps\\provider-app && gradlew.bat installDebug
cd apps\\customer-app && gradlew.bat installDebug
```

## Finish your remote caching setup

[Click here to finish setting up your workspace!](https://cloud.nx.app/connect/EQEanaB6kr)

## Run tasks

To run tasks with Nx use:

```sh
yarn nx <target> <project-name>
```

For example:

```sh
yarn nx build provider-app
```

These targets are either [inferred automatically](https://nx.dev/concepts/inferred-tasks?utm_source=nx_project&utm_medium=readme&utm_campaign=nx_projects) or defined in the `project.json` or `package.json` files.

[More about running tasks in the docs &raquo;](https://nx.dev/features/run-tasks?utm_source=nx_project&utm_medium=readme&utm_campaign=nx_projects)

## Add new projects

To add libraries or apps, use [Nx generators](https://nx.dev/features/generate-code) and the right plugin for your stack (this repo is Android/Kotlin). Run `yarn nx list` to see installed plugins, or use [Nx Console](https://nx.dev/getting-started/editor-setup) in the IDE.

[Learn more about Nx plugins &raquo;](https://nx.dev/concepts/nx-plugins)


[Learn more about Nx on CI](https://nx.dev/ci/intro/ci-with-nx#ready-get-started-with-your-provider?utm_source=nx_project&utm_medium=readme&utm_campaign=nx_projects)

## Install Nx Console

Nx Console is an editor extension that enriches your developer experience. It lets you run tasks, generate code, and improves code autocompletion in your IDE. It is available for VSCode and IntelliJ.

[Install Nx Console &raquo;](https://nx.dev/getting-started/editor-setup?utm_source=nx_project&utm_medium=readme&utm_campaign=nx_projects)

## Useful links

Learn more:

- [Learn about Nx on CI](https://nx.dev/ci/intro/ci-with-nx?utm_source=nx_project&utm_medium=readme&utm_campaign=nx_projects)
- [Releasing Packages with Nx release](https://nx.dev/features/manage-releases?utm_source=nx_project&utm_medium=readme&utm_campaign=nx_projects)
- [What are Nx plugins?](https://nx.dev/concepts/nx-plugins?utm_source=nx_project&utm_medium=readme&utm_campaign=nx_projects)

And join the Nx community:
- [Discord](https://go.nx.dev/community)
- [Follow us on X](https://twitter.com/nxdevtools) or [LinkedIn](https://www.linkedin.com/company/nrwl)
- [Our Youtube channel](https://www.youtube.com/@nxdevtools)
- [Our blog](https://nx.dev/blog?utm_source=nx_project&utm_medium=readme&utm_campaign=nx_projects)
