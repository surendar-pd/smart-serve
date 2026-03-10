# SmartServe — Product Requirements Document (PRD)

**Version:** 1.0  
**Status:** Approved  
**Source:** [DRAFT_PRD.md](./DRAFT_PRD.md) (Group 7 – CSI 5175 Mobile Commerce Technologies, March 2026)

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Problem Statement](#2-problem-statement)
3. [Proposed Solution & Target Audience](#3-proposed-solution--target-audience)
4. [Application Scope](#4-application-scope)
5. [User Roles, Authentication & Privacy](#5-user-roles-authentication--privacy)
6. [User Interface & Screen Descriptions](#6-user-interface--screen-descriptions)
7. [Technical Architecture](#7-technical-architecture)
8. [Success Criteria & Out of Scope](#8-success-criteria--out-of-scope)
9. [References](#9-references)

---

## 1. Executive Summary

**SmartServe** is a local service marketplace Android application for the Ottawa region. It connects customers who need home, education, and student-life services with nearby providers. The product differentiates through **smart recurring suggestions**, **location and time-aware recommendations**, and **preference-aware quick booking** to reduce friction for repeat bookings.

- **Platform:** Native Android (Kotlin), Material Design  
- **Backend:** Firebase (Auth, Firestore, FCM; optional Cloud Functions)  
- **User types:** Customer and Provider (single account can hold both roles)  
- **Geography:** Ottawa-area only (defined radius)

---

## 2. Problem Statement

Across the Ottawa area, a wide range of people—university students, young professionals, and long-time residents—depend on recurring local services such as cleaning, tutoring, moving help, and minor repairs. Existing platforms do not support this use case well:

- Users must **search from scratch** each time, compare options manually, re-enter preferences, and re-verify availability.
- **Previous bookings are not remembered**; there are no reminders when a service might be due again.
- Rebooking a known provider (e.g., a cleaner used two weeks ago) takes too long for busy users.

SmartServe directly targets this friction by remembering history, surfacing one-tap rebook options, and pre-filling preferences.

---

## 3. Proposed Solution & Target Audience

### 3.1 Core Value Propositions

| Idea | Description |
|------|-------------|
| **Smart Recurring Suggestions** | App tracks booking history and detects patterns (e.g., cleaning every two Saturdays). Surfaces a one-tap rebook card before the next expected date (e.g., "Rebook your usual cleaner for this Saturday at 10:00?" with Confirm / Change Time). No search or form-filling. |
| **Location & Time-Aware Recommendations** | Home content adapts to where the user is and what time it is. Near campus on a weekday → tutoring and assignment help promoted. At saved home on weekend evening → cleaning and repairs first. Banner color reflects context. |
| **Preference-Aware Quick Booking** | Saves default address, preferred time slots, and budget range. Booking forms are pre-filled; user confirms in seconds. |

### 3.2 Target Audience

- University students (e.g., Carleton, uOttawa)  
- Young professionals in the Ottawa area  
- Local residents who want faster recurring service booking  

The app stays within a defined Ottawa-area radius to keep the provider pool relevant.

---

## 4. Application Scope

### 4.1 Service Categories (In Scope)

- **Home Services:** Cleaning, basic handyman work, small repairs  
- **Education:** Tutoring, assignment help, language practice  
- **Student Life Services:** Moving help, furniture assembly, tech repair  

### 4.2 Product Boundaries

- **Two apps, one auth:** Customer and Provider are separate user-facing experiences sharing a common authentication layer.
- **Customer:** Browse services (map + personalized feed), smart suggestions, book with pre-filled preferences, cart, upcoming/past bookings, rate and review, call provider, share booking.
- **Provider:** Profile (category, description, hourly rate, service radius on map), view/accept/decline requests, see customer location (map), mark jobs done, view ratings, past bookings, earnings.

---

## 5. User Roles, Authentication & Privacy

### 5.1 Roles

- **Customer:** Browse, book, cart, rate providers, call providers, share bookings.  
- **Provider:** List services, respond to requests, mark complete, view earnings.  
- **Dual role:** One user can hold both roles and switch between them.

### 5.2 Authentication (Firebase)

- **Email/password:** Mandatory email verification; password reset via time-limited token (e.g., 15 minutes).  
- **Google Sign-In:** OAuth 2.0.  
- **Phone (SMS OTP):** Required for providers; optional for customers; rate-limited.

**Flow:** Introduction (role choice) → Sign Up or Login → first-time Profile Setup → then direct to Customer or Provider Home.

### 5.3 Privacy

- **Data:** Name, email, phone (providers), service preferences. Location only while app is in use (no background); precision limited to neighborhood level for recommendations.
- **Controls:** Profile → "Privacy and Data" — individual toggles for Smart Suggestions, Location Awareness, Voice search; option to request account deletion.

---

## 6. User Interface & Screen Descriptions

*Wireframes are referenced in the draft; see `docs/DRAFT_PRD.md` for visual placeholders.*

### 6.1 Auth & Profile Setup (Shared)

| Screen | Description |
|--------|-------------|
| **Introduction (Customer)** | Logo, tagline, short description; "Get Started" → Customer Sign Up; "Already have an account? Log In". |
| **Introduction (Provider)** | Same layout; "Join as Provider" → Provider Sign Up; "Already a provider? Log In". |
| **Login** | Email, password, "Log In", "Continue with Google", "Forgot Password?". Routes to Customer or Provider Home by role. |
| **Sign Up (Customer)** | Full name, email, password, confirm password; "Create Account" (email verification); "Continue with Google". |
| **Sign Up (Provider)** | As Customer + required phone number (SMS OTP). |
| **Forgot Password** | Back, instructions, email field, "Send Reset Link" (token expiry 15 min). |
| **Customer Profile Setup** | Once after first login: optional photo, optional phone, home address (default for bookings), toggles for location-based recommendations and push notifications, "Start". |
| **Provider Profile Setup** | Once after first login: photo, service category (Home / Education / Student Life), description, hourly rate, map-based service radius (Google Maps circle), availability (days/times), "Start". |

### 6.2 Customer App Screens

**Navigation:** Bottom bar — Home, Search, Cart, Bookings.

| Screen | Description |
|--------|-------------|
| **Home (Smart Feed)** | Search bar + microphone (voice search); horizontal smart suggestion cards (e.g., rebook usual cleaner); category chips (Cleaning, Tutoring, Moving, Repairs); list of recommended providers (name, type, rating, price, thumbnail). Context-aware ordering and banner. |
| **Category List** | Back, search, sort (Services, Trending, Nearby, Rating); provider cards → Service List. |
| **Service List** | Rows: service name, provider, rating, price, description, "Add to Cart"; budget filter (pre-filled from saved range). |
| **Booking Flow** | Date picker (default next available), time slots (by provider availability; preferred highlighted), address (pre-filled), "Add to Cart", budget, "Confirm Booking". Provider gets push on confirm. |
| **Cart** | "My Cart (N Items)", remove per item, promo code, "Proceed to Checkout". |
| **Manage Bookings** | Tabs: Upcoming, Confirmed. Card: service, provider, date, time, status; tap → contact + "Call"; actions: Cancel, Reschedule, Review. |
| **Past Bookings** | Completed list; "Rebook" opens Booking Flow with previous preferences. |
| **Profile** | Edit Profile; My Addresses, Notification Settings, Privacy and Data; toggles: Smart Suggestions, Location Awareness; Log Out. |

### 6.3 Provider App Screens

**Navigation:** Bottom bar — Home, Bookings.

| Screen | Description |
|--------|-------------|
| **Provider Home** | Header (SmartServe - Provider, photo, notification bell); filter chips (New, Active, Completed, All); list of request cards (customer first name, service, date/time, neighborhood). Tap → Request Detail. |
| **Request Detail & Map** | Customer avatar/name, service, date, time, neighborhood, special instructions; map with customer pin; "Accept" (push to customer) / "Decline" (notify customer). |
| **Active / Completed Job** | "Status: In Progress", customer details, map; "Call Customer" (call logged), "Mark Done" (notify customer, prompt rating). |
| **Past Bookings (Provider)** | Completed jobs, ratings; earnings summary (e.g., week/month). |

---

## 7. Technical Architecture

### 7.1 Frontend

- **Native Android**, Kotlin, Material Design.  
- Customer and Provider flows: separate activities/fragments; shared auth module.

### 7.2 Backend (Firebase)

- **Firebase Authentication:** Email/password, Google OAuth, phone OTP.  
- **Cloud Firestore:** Profiles, listings, bookings, ratings, preferences.  
- **FCM:** Push notifications (e.g., new request, acceptance).  
- **Cloud Functions (optional):** Server-side recommendation logic.

### 7.3 APIs & SDKs

- **Google Maps SDK for Android:** Provider radius selection, customer location display, nearby provider pins.  
- **Android SpeechRecognizer:** Voice search.  
- **Android Telephony / Share:** Call initiation and logging; share booking (e.g., WhatsApp, SMS).

### 7.4 Data Flow (High Level)

1. User authenticates (Firebase Auth); profile/preferences stored in Firestore.  
2. On app open: client reads GPS + time, compares with booking history → generates suggestion cards.  
3. Customer books → record in Firestore → FCM to provider.  
4. Provider accepts/declines → status updates via Firestore listeners to customer in real time.

---

## 8. Success Criteria & Out of Scope

### 8.1 Success Criteria

- Customers can complete a rebook in minimal steps using smart suggestions and pre-filled preferences.  
- Providers receive and respond to requests with clear location and job details.  
- Auth (email, Google, phone for providers) and profile setup work end-to-end.  
- Location and time context influence home feed and suggestions when enabled.

### 8.2 Out of Scope (for initial release)

- Payments in-app (handled offline or future phase).  
- Services outside the three categories above.  
- Regions outside the defined Ottawa-area radius.  
- Background location tracking.

---

## 9. References

- [Firebase Documentation](https://firebase.google.com/docs)  
- [Google Maps SDK for Android](https://developers.google.com/maps/documentation/android-sdk)  
- [Android SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer)  
- [Material Design Guidelines](https://material.io/design)  
- [MIT App Inventor Tutorials](https://appinventor.mit.edu/explore/ai2/tutorials)
