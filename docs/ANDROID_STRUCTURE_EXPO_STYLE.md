# Android (Compose) structure — Expo-style groups & layouts

This doc shows how to mirror Expo’s **(auth)**, **(app)**, **layouts**, and **screens** in an Android Kotlin/Compose app **before** implementing it.

---

## 1. What Expo does (for reference)

Typical Expo Router structure:

```
app/
├── (auth)/                    # Route group — not in URL
│   ├── _layout.tsx            # Layout for this group (e.g. centered card)
│   ├── login.tsx              # Screen → /login
│   └── signup.tsx             # Screen → /signup
├── (app)/                     # Route group — main app
│   ├── _layout.tsx            # Layout (e.g. tabs / bottom nav)
│   ├── index.tsx              # Screen → /
│   ├── home.tsx
│   └── profile.tsx
├── _layout.tsx                # Root layout (theme, providers)
└── layouts/                   # Reusable layout components
    ├── AuthLayout.tsx
    └── AppLayout.tsx
```

- **Groups**: `(auth)` and `(app)` — different layouts and screen sets.
- **Layouts**: `_layout.tsx` per group + shared `layouts/`.
- **Screens**: One file per screen under each group.

---

## 2. Android/Compose equivalent (no file-based routing)

In Android we use:

- **One (or few) Activity** with a single `NavHost`.
- **Navigation graphs** = “groups” (auth graph vs main graph).
- **Packages** = “folders” for auth, app, layouts.
- **Composables** = screens and layout wrappers.

So the same idea looks like this:

```
app/src/main/java/com/smartserve/providerapp/
├── MainActivity.kt                    # Single Activity → setContent { RootLayout() }
├── navigation/
│   ├── AppNavigation.kt              # NavHost + routes (auth vs app graph)
│   └── Routes.kt                     # Sealed class or object with route strings
│
├── ui/
│   ├── layouts/                      # Shared layout composables (like Expo layouts/)
│   │   ├── AuthLayout.kt             # Wrapper for auth screens (e.g. centered card)
│   │   └── AppLayout.kt             # Wrapper for main app (e.g. Scaffold + bottom nav)
│   │
│   ├── auth/                         # Group (auth) — same as Expo (auth)/
│   │   ├── AuthScreen.kt             # “Layout” for auth group (uses AuthLayout)
│   │   ├── LoginScreen.kt            # Screen
│   │   └── SignupScreen.kt           # Screen
│   │
│   ├── app/                          # Group (app) — same as Expo (app)/
│   │   ├── AppScreen.kt              # “Layout” for app group (uses AppLayout, e.g. tabs)
│   │   ├── HomeScreen.kt             # Screen
│   │   └── ProfileScreen.kt         # Screen
│   │
│   └── theme/
│       ├── Theme.kt
│       ├── Color.kt
│       └── Type.kt
```

So:

- **`(auth)`** → package **`ui.auth`** + **auth navigation graph**.
- **`(app)`** → package **`ui.app`** + **app navigation graph**.
- **Layouts** → **`ui.layouts`** (`AuthLayout`, `AppLayout`).
- **Screens** → one Composable per screen under **`ui.auth`** and **`ui.app`**.

---

## 3. How navigation “groups” work in Compose

We don’t have URLs; we have a **single NavHost** and **routes**:

- **Routes** (e.g. `Routes.kt`):
  - `"login"`, `"signup"` → auth group.
  - `"home"`, `"profile"` → app group.

- **NavHost** in `AppNavigation.kt`:
  - Start with **auth graph** if not logged in (e.g. `login` → `signup`).
  - After login, **replace** with **app graph** (e.g. `home` → `profile`), so the back stack doesn’t go back to login.

So:

- **Auth group** = set of screens (login, signup) shown in **AuthLayout**, with their own back stack.
- **App group** = set of screens (home, profile) shown in **AppLayout** (e.g. bottom nav), with their own back stack.

Visually:

```
┌─────────────────────────────────────────────────────────────┐
│  MainActivity                                               │
│  setContent { ProviderAppTheme { AppNavigation() } }         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  AppNavigation (NavHost)                                    │
│  • if (!isLoggedIn) → auth graph (AuthLayout)                │
│      • LoginScreen, SignupScreen                            │
│  • else → app graph (AppLayout)                             │
│      • HomeScreen, ProfileScreen                            │
└─────────────────────────────────────────────────────────────┘
                              │
         ┌────────────────────┴────────────────────┐
         ▼                                          ▼
┌─────────────────────┐                  ┌─────────────────────┐
│  AuthLayout         │                  │  AppLayout           │
│  (centered card,    │                  │  (Scaffold +         │
│   no bottom nav)    │                  │   bottom nav / tabs) │
│  • LoginScreen      │                  │  • HomeScreen        │
│  • SignupScreen     │                  │  • ProfileScreen     │
└─────────────────────┘                  └─────────────────────┘
```

---

## 4. Side-by-side mapping

| Expo                         | Android (Compose)                          |
|-----------------------------|--------------------------------------------|
| `app/(auth)/`               | Package `ui.auth` + auth NavGraph          |
| `app/(app)/`                | Package `ui.app` + app NavGraph            |
| `app/(auth)/_layout.tsx`    | `AuthLayout` + `AuthScreen` (container)    |
| `app/(app)/_layout.tsx`     | `AppLayout` + `AppScreen` (container)      |
| `app/layouts/`              | Package `ui.layouts` (AuthLayout, AppLayout) |
| `app/(auth)/login.tsx`      | `ui.auth.LoginScreen`                      |
| `app/(app)/home.tsx`        | `ui.app.HomeScreen`                        |
| Root `_layout.tsx`         | `ProviderAppTheme` in `MainActivity`        |

---

## 5. Summary before implementing

- **Yes, it’s possible** to have the same mental model as Expo: **(auth)**, **(app)**, **layouts**, and **screens**.
- **Groups** = packages (`ui.auth`, `ui.app`) + separate navigation graphs.
- **Layouts** = `ui.layouts` (AuthLayout, AppLayout) wrapping each group’s screens.
- **Screens** = one Composable per screen under `ui.auth` and `ui.app`.

Next step is to add this package structure and navigation in **provider-app** (and optionally mirror in **customer-app**). If you want, we can implement this step by step in the repo.
