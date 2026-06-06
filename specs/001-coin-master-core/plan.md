# Implementation Plan: Coin Master Core

**Branch**: `001-coin-master-core` | **Date**: 2026-06-06 | **Spec**: [specs/001-coin-master-core/spec.md](spec.md)

**Input**: Full technical implementation plan for the Coin Master Android app.

## Summary
Coin Master is a single-Activity Android app implementing a zero-based budgeting system with MVVM architecture. It enforces a strict "no floating-point money" rule (using `Long` for paise) and operates fully offline using Room.

## Technical Context

**Language/Version**: Kotlin 2.0 (Targeting JVM 17)

**Primary Dependencies**: Room 2.6.1, Hilt 2.51.1, Navigation Component 2.7.7, MPAndroidChart 3.1.0, DataStore 1.1.1

**Storage**: Room (Financial Data), DataStore (Settings)

**Testing**: JUnit 4, MockK, Turbine, Room in-memory testing

**Target Platform**: Android (Min SDK 24, Target SDK 35)

**Project Type**: Mobile Application

**Performance Goals**: Dashboard load < 2s on mid-range devices; Instant UI updates via reactive Flow.

**Constraints**: Fully offline (No INTERNET permission); Material Design 2 only.

**Scale/Scope**: v1 MVP - Core budgeting, accounts, and dashboard.

## Constitution Compliance

- [x] **Principle I (Architecture)**: Uses MVVM + Repository + Hilt. No DAO access from ViewModels.
- [x] **Principle II (Database)**: Room for financial data; DataStore for settings. No SharedPreferences.
- [x] **Principle III (Integrity)**: All money stored as `Long` (paise). `MoneyMath` utility enforces this.
- [x] **Principle IV (Budgeting)**: `ValidateZeroBalanceUseCase` enforces Zero-Based Budgeting.
- [x] **Principle V (Design)**: Strictly MD2 via Material Components 1.11.0.
- [x] **Principle VI (Simplicity)**: Task-oriented UI designed for ≤ 3 taps.
- [x] **Principle VII (Localization)**: All text in `strings.xml`. Hindi/Marathi supported.
- [x] **Principle VIII (Privacy)**: Fully offline; no internet permission.
- [x] **Principle IX (Reactive)**: Reactive data flow using `Flow<T>` and `StateFlow<T>`.
- [x] **Principle X (Quality)**: VM/Repository tests and DAO integration tests included.
- [x] **Principle XI (Separation)**: Technical HOW is separated from product WHAT.

## Project Structure

### Documentation (this feature)

```text
specs/001-coin-master-core/
├── spec.md              # Feature specification
├── plan.md              # This technical plan
├── research.md          # Technology & clarification resolution
├── data-model.md        # Database schema & entities
├── quickstart.md        # Validation scenarios
└── tasks.md             # Implementation tasks (generated separately)
```

### Source Code (repository root)

```text
app/src/main/java/com/rushi/coinmaster/
├── data/                # Room entities, DAOs, Repositories
├── domain/              # Pure Kotlin models, Use Cases
├── ui/                  # Fragments, ViewModels, Theme
└── util/                # MoneyMath, Formatter, LocaleHelper
```

**Structure Decision**: Single-project Android app following Clean Architecture within the `app` module.

## Implementation Phases

### Phase 1 — Foundation
- Room Database, DAOs, and Hilt setup.
- Core utilities: `MoneyMath`, `LocaleHelper`, `CurrencyFormatter`.

### Phase 2 — Onboarding
- 3-step setup flow (Profile, Account, Income).
- DataStore integration for `ONBOARDING_COMPLETE`.

### Phase 3 — Account Management
- CRUD for accounts; Net Worth calculation logic.
- Soft delete with Undo window.

### Phase 4 — Budget Planning
- Zero-balance enforcement logic (`ValidateZeroBalanceUseCase`).
- Envelope allocation UI and master bucket splitting.

### Phase 5 — Transaction Recording
- Atomic transactions (Update account + record log).
- Support for Expenses, Income, and Transfers.

### Phase 6-7 — Monitoring & Dashboard
- Reactive envelope status updates.
- MPAndroidChart integration for spending breakdown.

### Phase 8-9 — Sinking Funds & Localization
- Goal tracking with automated monthly contribution math.
- Hindi and Marathi string support and number formatting.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| None      | N/A        | N/A                                 |
