# Weight Tracking App — Enhancement Summary (CS 499)

This document maps every enhancement described in the code‑review packet to the work
implemented in the project, organized by the three review categories and the SNHU
CS 499 course outcomes. It is written as a before/after narrative for the ePortfolio.

## Snapshot

| Area | Before | After |
|------|--------|-------|
| Accounts / login | None — app opened straight to a single shared profile | Full multi‑user auth: register, log in, log out, per‑account data |
| Passwords | (Original artifact stored plain text) | PBKDF2‑HMAC‑SHA256 with a random per‑user salt; plain text never stored or compared |
| Data ownership | One global profile + one flat entry list | Each user owns their own profile and weight history (foreign key + index) |
| "Current weight" | Assumed newest row id | Ordered by real parsed date, so back‑dated entries can't distort it |
| Analytics | None | Moving average, min/max, and a linear‑regression trend line |
| List updates | `notifyDataSetChanged()` (full rebind) | `DiffUtil` — only changed rows rebind |
| Goal celebration | None | SEND_SMS congratulations text on reaching goal, with graceful degradation |
| Referential integrity | FK declared but not enforced | `PRAGMA foreign_keys=ON` on every connection + cascade delete |
| Input validation | Date checked; weight lower‑bound only | Date format + weight upper/lower bounds |
| Tests | Default empty sample | Real JUnit tests for hashing, analytics, dates, conversions |
| SMS reliability | (planned only) | Always fires a goal notification; sends SMS on top; in-app **test message** button + live status |
| Insights | None | Dedicated **Insights** tab: stats + regression-projected goal date |
| Account security | None | **Change password** (re-hash) and **delete account** (live FK cascade) |
| Finding entries | Scroll only | **Search** history by date or notes |

---

## Category 1 — Software Engineering & Design

**Outcomes 3 & 4.**

The original artifact was a single ~665‑line `MainActivity` (a "God‑Activity"). The project
now uses a layered **MVVM** architecture:

- **Model** — Room entities `User`, `UserProfile`, `WeightEntry` and the read‑model `WeightSummary`.
- **View** — focused fragments (`LoginFragment`, `RegisterFragment`, `DashboardFragment`,
  `HistoryFragment`, `ProfileFragment`, `EntryEditorFragment`) with a reusable `item_entry.xml` row.
- **ViewModel** — `WeightViewModel` exposes state through `LiveData`; the UI observes rather than manages.
- **Repository** — `WeightRepository` owns all persistence and business logic on a background executor.

Screen navigation uses the Navigation component and named destination IDs instead of raw
ViewFlipper indices, addressing the "symbolic constants rather than magic numbers" finding.

## Category 2 — Algorithms & Data Structures

**Outcomes 3 & 4.**

- **Correctness fix (sort key):** history is ordered by the parsed `entryDate`
  (`ORDER BY entryDate DESC, createdAt DESC`), not by row id. A back‑dated weigh‑in no longer
  masquerades as the current weight.
- **Analytics layer** (`util/Analytics.java`):
  - *Moving average* over a configurable window of the most recent entries.
  - *Min / max* across the history.
  - *Trend line* via least‑squares **linear regression** of weight against day‑offset
    (`slope = (nΣxy − ΣxΣy) / (nΣx² − (Σx)²)`), reported as kg/lb per week.
  - Float comparisons use a documented tolerance (`TREND_TOLERANCE_KG_PER_DAY`) rather than equality.
- **Efficiency:** `EntryAdapter` now uses `DiffUtil` to compute the minimal row updates; a full
  rebind is only used when the display unit changes (which affects every row's rendered text).

## Category 3 — Databases

**Outcome 5 (plus 3 & 4).**

- **Password security (top priority):** `util/PasswordUtils.java` hashes with
  PBKDF2WithHmacSHA256 (120,000 iterations, 256‑bit key) and a random 16‑byte salt.
  Login hashes the entered password with the stored salt and compares digests in constant time.
- **Foreign‑key enforcement:** `AppDatabase` runs `PRAGMA foreign_keys=ON` in an `onOpen`
  callback; `WeightEntry` and `UserProfile` declare `onDelete = CASCADE`, so deleting a user
  removes their rows instead of orphaning them.
- **Indexing:** `WeightEntry` has an `@Index("userId")`, so per‑user lookups don't scan the table.
- **Room + background threads:** all data access already runs off the UI thread via a single‑thread
  executor, avoiding ANR risk; cursors are managed by Room.
- **Migration:** the schema is versioned (v2). For this course build it uses destructive fallback,
  which is called out as a deliberate trade‑off.
- **Least privilege:** `SEND_SMS` is requested at runtime and the app degrades gracefully
  (`SmsHelper` returns `false`) when the permission is denied, the number is blank, or the device
  has no telephony. `android.hardware.telephony` is marked `required="false"`.
- **Input validation:** dates must match `yyyy-MM-dd`; weights must be within realistic
  bounds (0–1500 lb / 0–700 kg).
- **Account lifecycle:** `changePassword` verifies the current password and stores a fresh
  salt + hash; `deleteAccount` removes the user row and lets the foreign-key cascade delete the
  profile and every weight entry — a live, on-camera demonstration that FK enforcement is real.

## SMS — done as robustly as possible

The goal-reached celebration is now layered so it works in every environment:

1. **Guaranteed local notification** fires on the device the moment the goal is reached, so the
   celebration never silently fails (emulator with no SIM, permission denied, etc.).
2. **Real SMS on top** via `SmsManager.sendTextMessage`, sent only when the number is valid
   (7–15 digits) and `SEND_SMS` is granted.
3. **In-app test tool** — Profile has a live "SMS alerts" status line and a **Send test message**
   button that requests the permission if needed and confirms delivery (or falls back to a test
   notification), so you can prove the pipeline works before recording the real goal moment.

To see a real text delivered, use a physical device with a SIM, or two emulators addressed by
their console port (e.g. `5556`). On a single emulator the call succeeds and the notification/toast
confirm the path even though nothing is delivered to a real handset.

## Added feature layer (all fully wired)

- **Insights tab** (`InsightsFragment`): entries tracked, moving average, min/max, signed weekly
  rate, the regression trend line, and a **projected goal date** extrapolated from the least-squares
  slope. Demonstrates applying the algorithms layer to real user value (Outcomes 3 & 4).
- **History search**: an in-memory filter over the observed list by date or notes (data structures).
- **Change password / delete account**: account-security controls in Profile (Outcome 5).

---

## Course‑outcome mapping

1. **Collaborative strategies for diverse audiences** — clear commenting and this before/after narrative.
2. **Professional communications** — the review video plus documented, refactored code.
3. **Design & evaluate computing solutions, managing trade‑offs** — MVVM architecture, the
   date‑sort correctness fix, analytics, DiffUtil, and the documented migration/regression choices.
4. **Well‑founded, innovative techniques and tools** — MVVM/LiveData, Room, DiffUtil, PBKDF2.
5. **Security mindset** — password hashing + salting, foreign‑key enforcement, parameterized
   queries, and least‑privilege permission handling.

## New / changed files

**New:** `model/User.java`, `data/UserDao.java`, `util/PasswordUtils.java`,
`util/SessionManager.java`, `util/Analytics.java`, `notifications/SmsHelper.java`,
`ui/LoginFragment.java`, `ui/RegisterFragment.java`, `res/layout/fragment_login.xml`,
`res/layout/fragment_register.xml`, plus unit tests under `app/src/test`.

**Changed:** `MainActivity.java` (login gate + permissions), `AppDatabase.java` (User entity + FK
enforcement), `UserProfileDao`/`WeightEntryDao` (per‑user queries), `WeightRepository.java`
(auth, analytics, SMS), `WeightViewModel.java` (current‑user session), `EntryAdapter.java`
(DiffUtil), `DashboardFragment.java` (analytics + goal event), `ProfileFragment.java`
(phone + logout), `EntryEditorFragment.java` (weight bounds), `model/*` (userId/FK + analytics
fields), `AndroidManifest.xml` (SEND_SMS), `res/navigation/nav_graph.xml`, and the dashboard/profile layouts.

## How to test the video demo

1. Launch → **Login** screen (no session).
2. **Create an account** (username, password, optional phone) → lands on Dashboard.
3. Set profile/goal and phone; in Profile tap **Send test message** to prove SMS setup on camera.
4. Add a few entries (try a **back‑dated** one to show the date‑sort fix).
5. Open the **Insights** tab → moving average, weekly rate, and the projected goal date.
6. Open **History** → **search** by date/notes; Update/Delete rows (DiffUtil animates only the changed row).
7. Add an entry at/under the goal → **goal notification** fires immediately and the **SMS** sends
   (with permission); a toast confirms which happened.
8. In Profile, **change password**, then **delete account** to show the foreign‑key cascade wiping
   the profile and entries. Finally **log out** → returns to Login; other accounts' data is preserved.
