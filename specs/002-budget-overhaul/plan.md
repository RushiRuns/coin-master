# Implementation Plan: Budget Overhaul

**Branch**: `002-budget-overhaul` | **Date**: 2026-06-17 | **Spec**: [spec.md](file:///c:/Users/H%20P/AndroidStudioProjects/CoinMaster/specs/002-budget-overhaul/spec.md)

**Input**: Feature specification from `/specs/002-budget-overhaul/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary
The goal is to overhaul the Coin Master application to introduce a nested Category -> Envelope model, classify envelopes by "Expense Type" (Fixed vs Variable), separate the Savings management view from the main budget screen, and add helper interfaces for Notes, Transfer targets, and Income streams. We will maintain zero-based budgeting logic in the repository layer and adhere strictly to localized, offline-first Material Design 2 principles.

## Technical Context

**Language/Version**: Kotlin 1.9+, Java/JVM 17

**Primary Dependencies**: Android Jetpack (Room, Lifecycle, ViewModel, Navigation Component), Hilt (Dependency Injection), Google Material Components for Android (Material Design 2), Kotlin Coroutines, and Flow.

**Storage**: Local SQLite via Room Database.

**Testing**: JUnit 4 (for ViewModels/Repositories unit testing), Mockk (mocking libraries), Room in-memory test databases (for DAO integration testing), and Espresso (for UI/onboarding flows).

**Target Platform**: Android (API 24+)

**Project Type**: Mobile App

**Performance Goals**: UI rendering at stable 60 fps, Room query responses in < 100ms.

**Constraints**: Offline-first (no internet permission), local-only storage, zero-based budgeting constraint (Allocations == Income) enforced prior to active budget month persistence.

**Scale/Scope**: Single user, offline device database, 12 top-level and secondary UI fragments.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Principle I: Architecture (MVVM/Clean)**: ViewModels access Repositories; views observe ViewModel streams. -> **PASS**
- **Principle II: Database (Room)**: Room is the sole database persistence engine. -> **PASS**
- **Principle III: Data Integrity (No Floating Point)**: All money fields use `Long` paise. -> **PASS**
- **Principle IV: Budgeting Engine (ZBB)**: Formula `Income - Needs - Wants - Savings = 0` checked in Repository. -> **PASS**
- **Principle V: Design System (MD2)**: Theme-based Material Design 2, BottomNavigationView has 3 tabs, FAB present. -> **PASS**
- **Principle VI: UX Simplicity (3 Taps/Accessibility)**: Core screens accessible in under 3 taps, targets >= 48dp. -> **PASS**
- **Principle VII: Localization**: Strings in res/values/strings.xml, local number formatting applied. -> **PASS**
- **Principle VIII: Local-First**: No internet permission or sync. -> **PASS**
- **Principle IX: Reactive Flow**: Observing Flows/LiveData. -> **PASS**
- **Principle X: Code Quality**: MVVM test coverage, DAO tests. -> **PASS**

## Project Structure

### Documentation (this feature)

```text
specs/002-budget-overhaul/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
└── checklists/
    └── requirements.md  # Spec quality checklist
```

### Source Code (repository root)

```text
app/src/main/java/com/rushi/coinmaster/
├── data/
│   ├── local/
│   │   ├── dao/
│   │   │   ├── AccountDao.kt
│   │   │   ├── ExpenseCategoryDao.kt      # [NEW]
│   │   │   ├── CategoryDao.kt             # (Envelopes)
│   │   │   ├── TransactionDao.kt
│   │   │   ├── IncomeStreamDao.kt         # [NEW]
│   │   │   ├── NoteDao.kt                 # [NEW]
│   │   │   └── TransferRecipientDao.kt    # [NEW]
│   │   ├── database/
│   │   │   └── CoinMasterDatabase.kt
│   │   └── entity/
│   │       ├── AccountEntity.kt
│   │       ├── ExpenseCategoryEntity.kt   # [NEW]
│   │       ├── CategoryEntity.kt          # (Envelopes)
│   │       ├── TransactionEntity.kt
│   │       ├── IncomeStreamEntity.kt      # [NEW]
│   │       ├── NoteEntity.kt              # [NEW]
│   │       └── TransferRecipientEntity.kt # [NEW]
│   └── repository/
├── domain/
│   └── usecase/
└── ui/
    ├── MainActivity.kt
    ├── onboarding/
    ├── dashboard/                         # (Home)
    ├── transactions/
    ├── budget/
    ├── categories/                        # [NEW]
    ├── expensetype/                       # [NEW]
    ├── income/                            # [NEW]
    ├── savings/                           # [NEW]
    ├── accounts/                          # (Moved to sidebar navigation target)
    ├── notes/                             # [NEW]
    ├── transfers/                         # [NEW]
    ├── goals/
    └── settings/
```

**Structure Decision**: Monolith Android Application layout, matching MVVM. Created new fragments/packages for all new sidebar navigation destinations and updated the navigation graph.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| None      | N/A        | N/A                                 |
