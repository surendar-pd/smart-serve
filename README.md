# SmartServe

SmartServe is a local services marketplace focused on Ottawa. The project contains two Android apps built with Kotlin + Jetpack Compose, along with shared modules for common functionality.

## Project overview

This repository includes:

- `apps/customer-app`: Customer-facing Android app used to discover services, browse providers, add services to cart, place bookings, chat with providers, track booking states, and manage favorites/profile data.
- `apps/provider-app`: Provider-facing Android app used to manage services, receive/respond to customer requests, handle active jobs, communicate with customers, and review earnings and booking metrics.
- `libs/shared-auth`: Shared authentication and user-role models used across apps.
- `libs/shared-ui`: Shared reusable Compose UI components and design primitives used by both apps.

## Core capabilities

Customer app capabilities:

- Browse service categories and provider listings
- Search services/providers across categories
- Book services with scheduling and special instructions
- Manage cart and edit booking selections
- Save favorites and revisit preferred services quickly
- View booking history and rate providers
- Chat with providers for active bookings

Provider app capabilities:

- Create and manage service offerings
- Review pending booking requests and accept/decline
- Manage active jobs with navigation and customer contact actions
- Track completed jobs and ratings
- View dashboard metrics for bookings and earnings
- Chat with customers related to active or pending bookings

## Tech stack

- Kotlin + Jetpack Compose (Android)
- Firebase (Auth, Firestore, Storage)
- Nx workspace for monorepo orchestration
- Gradle for Android build and install workflows

## Getting started

Prerequisites:

- Node.js (LTS)
- Yarn (workspace package manager)
- Android Studio (or Android SDK with emulator/device)

Install dependencies from repo root:

```sh
yarn install
```

Build apps:

```sh
yarn nx build provider-app
yarn nx build customer-app
```

Install debug builds to device/emulator:

```sh
yarn nx run provider-app:installDebug
yarn nx run customer-app:installDebug
```

Windows Gradle wrapper alternative:

```sh
cd apps\provider-app && gradlew.bat installDebug
cd apps\customer-app && gradlew.bat installDebug
```

## Contributors

- [Adithiyan](https://github.com/Adithiyan)
- [Vineela34](https://github.com/vineela34)
- [surendar-pd](https://github.com/surendar-pd)
- [VSSheethal](https://github.com/VSSheethal)

