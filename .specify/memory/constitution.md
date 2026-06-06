# Coin Master — Project Constitution

<!-- SYNC IMPACT REPORT
Version change: N/A → 1.0.0 (initial ratification)
List of modified principles: All (initial creation: I-XI)
Added sections: Project Overview, Principles I-XI, Governance
Removed sections: None
Templates requiring updates:
- ✅ .specify/templates/plan-template.md (updated Gates)
- ✅ .specify/templates/tasks-template.md (updated test requirements)
Follow-up TODOs: None
-->

| Field | Value |
|----------------------|-----------------------------------------------|
| **Project** | Coin Master — Personal Budget Planner |
| **Platform** | Android (Kotlin) |
| **Constitution Version** | 1.0.0 |
| **Ratification Date**| 2026-06-06 |
| **Last Amended** | 2026-06-06 |
| **Status** | Active |

---

## Project Overview

Coin Master is a personal, single-user Android application for managing individual wealth and budgeting. It combines the 50/30/20 rule, zero-based budgeting, and the envelope method into a unified, opinionated system. The app is designed to be used by any individual regardless of technical proficiency, including elderly users. All data is stored locally on-device. There is no authentication, cloud sync, or backend dependency. The primary goal of every design and implementation decision is: **simple enough for anyone, powerful enough for serious budgeting.**

---

## Principle I: Architecture — MVVM with Clean Layers

**The app MUST follow the MVVM (Model-View-ViewModel) pattern with a Repository layer separating data from business logic.**

- ViewModels MUST NOT directly access the database (Room DAOs). All data access MUST go through Repository classes.
- Repositories MUST be the single source of truth for their domain (accounts, transactions, budget months, envelopes, goals).
- Activities and Fragments MUST only interact with ViewModels. They MUST NOT hold business logic.
- The domain layer (Repository + use cases) MUST be independently testable without Android framework dependencies.
- Hilt MUST be used for dependency injection to keep constructors clean and improve testability.
- Navigation Component MUST be used for all screen-to-screen transitions. No manual Fragment transactions except where Navigation Component is genuinely insufficient.

**Rationale:** Clean separation ensures the app remains maintainable as features grow. MVVM with Repository is the officially recommended Android architecture and integrates seamlessly with Kotlin Coroutines and Room.

---

## Principle II: Database — Room is the Only Persistence Layer

**Room (Jetpack) MUST be the sole persistence mechanism for all structured financial data.**

- No raw SQLite usage. Every database interaction MUST go through Room DAOs.
- All Room queries returning lists or objects that update the UI MUST return `Flow<T>` or `LiveData<T>`, never plain return types, to enable reactive UI updates.
- DataStore (Preferences) MAY be used only for lightweight app-level settings (selected language, currency symbol, onboarding completion flag). It MUST NOT store financial data.
- SharedPreferences MUST NOT be used anywhere in the app.
- Database schema migrations MUST be written explicitly using Room's `Migration` class. Destructive migrations are FORBIDDEN in production builds.
- All DAO methods that write data MUST be `suspend` functions and called from Coroutine contexts only.

**Canonical Database Tables:**

| Table | Purpose |
|------------------------|---------------------------------------------------------------|
| `accounts` | Cash, bank, credit card accounts with balance and metadata |
| `categories` | Budget envelopes with bucket type (NEED / WANT / SAVING) |
| `transactions` | Individual income, expense, and transfer records |
| `budget_months` | Monthly budget instance with total income |
| `envelope_allocations` | Per-month allocation amounts per category |
| `sinking_funds` | Named savings goals with target amount and target date |
| `app_settings` | Key-value pairs for non-financial app configuration |

**Rationale:** Room provides compile-time query verification, seamless Kotlin Coroutines and Flow integration, and robust migration support — making it the right and only database choice for this app.

---

## Principle III: Financial Data Integrity — No Floating Point Money

**All monetary values MUST be stored and computed using `Long` (representing the smallest currency unit, e.g., paise for INR) or `BigDecimal`. `Float` and `Double` MUST NEVER be used to represent money.**

- The canonical internal representation is `Long` in the smallest unit (paise for ₹, cents for $). Example: ₹1,500.75 is stored as `150075L`.
- All arithmetic on monetary values (addition, subtraction, percentage splits) MUST use `Long` or `BigDecimal` operations. Never `Double`.
- Display formatting MUST use `NumberFormat.getCurrencyInstance(currentLocale)` so currency symbols, separators, and decimal formatting adapt to the active locale automatically.
- The 50/30/20 split MUST be computed using integer division with a remainder-assignment rule: any unallocated paise from rounding MUST be added to the Savings bucket to preserve zero-balance integrity.

**Rationale:** Floating-point arithmetic produces rounding errors that are unacceptable in financial calculations. A ₹0.01 discrepancy in a budget is a bug. Using Long in smallest units eliminates this entirely.

---

## Principle IV: Budgeting Engine — Zero-Balance Must Always Be Enforced

**The budgeting engine MUST enforce that total envelope allocations for a month equal exactly the month's declared income (zero-based budgeting). No budget month may be "saved" or "activated" with an unallocated balance.**

- Income is split into three master buckets: **Needs (50%)**, **Wants (30%)**, **Savings & Goals (20%)**. These percentages are the default; the user MAY adjust them, but the total MUST always equal 100%.
- Within each master bucket, the user assigns money to named envelopes. The sum of all envelopes within a bucket MUST equal that bucket's allocated amount.
- The sum of all three buckets MUST equal the declared monthly income. This is the zero-balance invariant. It MUST be enforced at the Repository layer before any budget month is persisted.
- A transfer between accounts MUST NOT be recorded as an expense or income transaction. It MUST be recorded as a `TRANSFER` type and have no effect on envelope balances.
- Sinking funds (monthly contributions toward a future goal) MUST live inside the Savings bucket as sub-envelopes, counted against the 20% allocation.

**Rationale:** The zero-based budgeting philosophy — every rupee assigned a job — is the core financial principle of this app. Allowing unallocated balances defeats the purpose and produces inaccurate budget health metrics.

---

## Principle V: Design System — Material Design 2, No Exceptions

**The app's visual design MUST strictly follow Material Design 2 (MD2) using the official Material Components for Android library (`com.google.android.material`). Material Design 3 (Material You) components MUST NOT be used.**

- The color system MUST define: `colorPrimary`, `colorPrimaryDark`, `colorAccent`, `colorSurface`, `colorBackground`, and `colorError` in the app's theme. All components MUST derive their colors from this theme — no hardcoded colors in layouts or code.
- Typography MUST follow the MD2 type scale: `headline1`–`headline6`, `subtitle1`, `subtitle2`, `body1`, `body2`, `caption`, `overline`. Roboto is the base font.
- Elevation and card shadows MUST follow MD2 elevation guidelines. Cards used for account summaries, envelope items, and transactions MUST use `MaterialCardView`.
- The Floating Action Button (FAB) is the primary action trigger on transactional screens. It MUST be present and prominent on the Transactions screen and the Add Account screen.
- Bottom Navigation (`BottomNavigationView`) MUST be used for top-level screen navigation. It MUST have no more than 4 tabs. Every tab MUST have both an icon and a text label.
- Color coding is a first-class design element: every category/envelope MUST have an associated color. This color MUST appear consistently in the list item, chart slice, and any transaction row for that category.
- Status colors for envelope health MUST follow this system: Green (< 70% spent), Amber (70–90% spent), Red (≥ 90% spent). These MUST be applied consistently everywhere envelope health is shown.

**Rationale:** MD2 provides a familiar, trusted visual language that reduces the learning curve for all users, including non-technical and elderly users. Strict adherence to a single design system prevents visual inconsistency and reduces decision fatigue during implementation.

---

## Principle VI: UX Simplicity — Designed for Everyone

**Every primary user task MUST be completable in 3 taps or fewer from the app's home screen. No exceptions without documented justification.**

- Every tappable element (buttons, list items, FABs) MUST have a minimum touch target of 48dp × 48dp. Preferred size for primary actions is 56dp × 56dp.
- Icons MUST always be paired with a text label in navigation and actions. Standalone icons without labels are FORBIDDEN for any user-facing interactive element.
- Text sizes MUST never go below 14sp for any user-visible content. Primary financial figures (balances, totals) MUST use 24sp or larger.
- Every destructive action (delete transaction, remove account, clear month) MUST show a confirmation dialog before executing. The confirmation MUST clearly describe what will be permanently deleted.
- After any deletion, a Snackbar with an "Undo" option MUST appear with a minimum 5-second window. Undoing MUST fully restore the deleted item.
- Empty states MUST never be blank screens. Every empty state MUST display a contextual illustration, a plain-language description, and a clear call-to-action (e.g., "No accounts yet. Tap + to add your first account.").
- Onboarding (first launch only) MUST be a linear, 3-step flow: (1) Set your name and currency, (2) Add your first account, (3) Set your monthly income. No step may be skipped on first launch.
- Error messages MUST be written in plain language. Technical error codes, stack traces, or jargon MUST NEVER appear in user-facing UI.

**Rationale:** The target audience explicitly includes older and less tech-savvy users. Simplicity is not a "nice to have" — it is a hard product requirement. Every UX deviation that adds friction must be justified against this principle.

---

## Principle VII: Localization — Strings-First, Always

**Every single piece of user-facing text MUST be defined in `res/values/strings.xml`. Hardcoded strings in Kotlin source files or XML layouts are FORBIDDEN.**

- The default language is English (`res/values/strings.xml`).
- Hindi (`res/values-hi/strings.xml`) and Marathi (`res/values-mr/strings.xml`) MUST be supported from the first release.
- In-app language switching MUST be supported independent of the device's system language. The selected language MUST be persisted in DataStore and applied at app startup before the first Activity is shown.
- `AppCompatDelegate.setApplicationLocales(LocaleListCompat)` MUST be used for in-app language switching on Android 13+ (API 33+). A `LocaleHelper` utility MUST handle backward compatibility for earlier API levels.
- Number and currency formatting MUST always use `NumberFormat` instances constructed with the currently active `Locale`. The Indian numbering system (lakh, crore separators) MUST be applied automatically when the locale is `hi_IN` or `mr_IN`.
- Date formatting MUST use `DateTimeFormatter` with the active locale. Hardcoded date format strings (e.g., `"dd/MM/yyyy"`) MUST NOT be used in the display layer.
- Plural forms (e.g., "1 transaction" vs "5 transactions") MUST use `res/values/plurals.xml`, not string concatenation in code.

**Rationale:** The app's target market is Indian users across linguistic backgrounds. Localization is a first-class feature, not an afterthought. Strings-first architecture ensures every string is translatable by design.

---

## Principle VIII: Single User, Local-First, No Network Dependency

**Coin Master is a single-user, fully offline application. It MUST function completely without any network connectivity. No core feature may require internet access.**

- There is no user authentication, login, or session management. The app opens directly to the home screen.
- There MUST be no outbound network calls for core functionality (budgeting, transactions, accounts, goals).
- The app MUST support local data export (JSON or CSV) as a backup mechanism. This feature MAY be deferred to a later version but the data schema MUST be designed with exportability in mind from the start.
- No analytics SDKs, crash reporting SDKs, or ad SDKs that make network calls MUST be included unless explicitly decided by the project maintainer and documented here.
- `INTERNET` permission MUST NOT be declared in `AndroidManifest.xml` unless a deliberate, documented decision is made to add an online feature.

**Rationale:** Privacy and reliability are non-negotiable for a personal finance app. The user's financial data must never leave their device unless they explicitly choose to export it.

---

## Principle IX: Reactive Data Flow — UI Observes, Never Queries

**The UI layer (Activities, Fragments) MUST NEVER query the database directly. All data displayed in the UI MUST flow through ViewModel → Repository → Room DAOs using Kotlin `Flow` or `LiveData`.**

- ViewModels MUST expose data to the UI as `StateFlow<T>`, `SharedFlow<T>`, or `LiveData<T>`.
- When a transaction is added, deleted, or modified, the affected UI screens (dashboard, envelope list, account list) MUST update automatically through reactive streams. No manual "refresh" calls from the UI after write operations.
- UI state (loading, success, error, empty) MUST be modeled as a sealed class or data class and exposed from the ViewModel as a single state stream.
- Long-running database operations MUST be executed on `Dispatchers.IO`. ViewModel methods that trigger database writes MUST launch coroutines using `viewModelScope`.
- `collectAsState()` or `observeAsState()` patterns MUST be used in the UI. Direct `runBlocking` calls from the UI thread are FORBIDDEN.

**Rationale:** Reactive data flow eliminates entire classes of bugs (stale UI, missed updates, threading issues) and is the foundation of a predictable, testable app. This is non-negotiable for a financial app where data consistency is critical.

---

## Principle X: Code Quality — Readable, Tested, Minimal

**Code MUST be readable by a developer unfamiliar with the codebase. Clarity over cleverness, always.**

- Every ViewModel and Repository class MUST have corresponding unit tests. Test coverage for these layers MUST be maintained.
- Room DAOs MUST have integration tests using an in-memory Room database (not mocks).
- All Kotlin files MUST follow the official Kotlin coding conventions. No magic numbers or strings in business logic — use named constants or enums.
- Functions longer than 40 lines are a warning sign and MUST be refactored unless there is a documented reason.
- No `TODO` comments MAY be committed to the main branch without an associated issue/ticket reference.
- The `BuildConfig` class MUST be used for any environment-specific constants (e.g., debug flags). No hardcoded environment-specific values in source code.
- ProGuard/R8 rules MUST be configured and tested before any release build is produced.

**Rationale:** This app will be built incrementally over time. Code that is readable and tested today stays maintainable tomorrow. Financial calculation bugs caused by untested code have real consequences for users.

---

## Principle XI: Documentation Separation of Concerns

**Specifications describe WHAT and WHY. Plans describe HOW. These MUST never be mixed.**

- `spec.md` files MUST remain technology-agnostic. No implementation details, library names, or architecture patterns belong in a spec. The audience is a product stakeholder, not a developer.
- `plan.md` files contain ALL technical decisions: architecture choices, library selections, data models, API contracts. The audience is a developer.
- A feature is not ready for implementation planning until its `spec.md` has zero `[NEEDS CLARIFICATION]` markers.
- Every implementation plan MUST include a **Constitution Compliance** section listing which principles are relevant and confirming they are satisfied. Any deviation from this constitution MUST be documented with explicit rationale in the plan.

**Rationale:** Mixing product intent with implementation details in the same document degrades both. Keeping them separate forces clarity of thought at each stage of development.

---

## Governance

### Amendment Process

Amendments to this constitution REQUIRE:

1. A written rationale explaining why the existing principle is insufficient or incorrect.
2. An impact assessment: which existing specs, plans, and code are affected by the change.
3. A version bump following semantic versioning:
   - **MAJOR** (X.0.0): Removal or redefinition of an existing principle that breaks backward compatibility.
   - **MINOR** (1.X.0): Addition of a new principle or material expansion of an existing one.
   - **PATCH** (1.0.X): Clarifications, wording improvements, typo fixes.
4. The `Last Amended` date MUST be updated to the ISO date of the amendment.

### Compliance Review

Every `plan.md` created using `/speckit.plan` MUST include a **Constitution Compliance** checklist. Implementation MUST NOT begin on any feature whose plan has unresolved constitution violations.

### Non-Negotiable Principles

The following principles are **immutable** and MUST NOT be amended without a full review:

- Principle III (No Floating Point Money) — financial correctness
- Principle IV (Zero-Balance Enforcement) — core product philosophy
- Principle VIII (Local-First, No Network) — user privacy

All other principles MAY be amended following the process above.
