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

**Alternative: run with Gradle directly**

```sh
# Provider app
cd apps/provider-app && ./gradlew installDebug

# Customer app
cd apps/customer-app && ./gradlew installDebug
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

While you could add new projects to your workspace manually, you might want to leverage [Nx plugins](https://nx.dev/concepts/nx-plugins?utm_source=nx_project&utm_medium=readme&utm_campaign=nx_projects) and their [code generation](https://nx.dev/features/generate-code?utm_source=nx_project&utm_medium=readme&utm_campaign=nx_projects) feature.

To install a new plugin you can use the `nx add` command. Here's an example of adding the React plugin:
```sh
yarn nx add @nx/react
```

Use the plugin's generator to create new projects. For example, to create a new React app or library:

```sh
# Generate an app
yarn nx g @nx/react:app demo

# Generate a library
yarn nx g @nx/react:lib some-lib
```

You can use `yarn nx list` to get a list of installed plugins. Then, run `yarn nx list <plugin-name>` to learn about more specific capabilities of a particular plugin. Alternatively, [install Nx Console](https://nx.dev/getting-started/editor-setup?utm_source=nx_project&utm_medium=readme&utm_campaign=nx_projects) to browse plugins and generators in your IDE.

[Learn more about Nx plugins &raquo;](https://nx.dev/concepts/nx-plugins?utm_source=nx_project&utm_medium=readme&utm_campaign=nx_projects) | [Browse the plugin registry &raquo;](https://nx.dev/plugin-registry?utm_source=nx_project&utm_medium=readme&utm_campaign=nx_projects)


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
