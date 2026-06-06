# Tasks: Coin Master Core

**Input**: Design documents from `specs/001-coin-master-core/`

**Prerequisites**: plan.md, spec.md, data-model.md, research.md

**Tests**: 
- **Mandatory**: Unit tests for ViewModels/Repositories and integration tests for DAOs (Principle X).
- **Optional**: All other tests are optional.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and core utilities

- [x] T001 Create project package structure as defined in plan.md
- [x] T002 Configure `build.gradle.kts` with Hilt, Room, Navigation, and MPAndroidChart dependencies
- [x] T003 Initialize `CoinMasterApplication` class with `@HiltAndroidApp` in `app/src/main/java/com/rushi/coinmaster/CoinMasterApplication.kt`
- [x] T004 Implement `MoneyMath` utility for `Long` (paise) arithmetic in `app/src/main/java/com/rushi/coinmaster/util/MoneyMath.kt`
- [x] T005 Implement `LocaleHelper` for in-app language switching in `app/src/main/java/com/rushi/coinmaster/util/LocaleHelper.kt`
- [x] T006 Implement `CurrencyFormatter` and `DateFormatter` in `app/src/main/java/com/rushi/coinmaster/util/CurrencyFormatter.kt`
- [x] T007 Configure Hilt `DatabaseModule` and `PreferencesModule` in `app/src/main/java/com/rushi/coinmaster/di/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core database schema and repository infrastructure

- [x] T008 [P] Define `AccountEntity` and `AccountDao` in `app/src/main/java/com/rushi/coinmaster/data/local/`
- [x] T009 [P] Define `CategoryEntity` and `CategoryDao` in `app/src/main/java/com/rushi/coinmaster/data/local/`
- [x] T010 [P] Define `TransactionEntity` and `TransactionDao` in `app/src/main/java/com/rushi/coinmaster/data/local/`
- [x] T011 [P] Define `BudgetMonthEntity`, `EnvelopeAllocationEntity`, and their DAOs in `app/src/main/java/com/rushi/coinmaster/data/local/`
- [x] T012 Initialize `CoinMasterDatabase` with all entities in `app/src/main/java/com/rushi/coinmaster/data/local/database/CoinMasterDatabase.kt`
- [x] T013 Implement `AppPreferences` using DataStore in `app/src/main/java/com/rushi/coinmaster/data/preferences/AppPreferences.kt`
- [x] T014 Create base `nav_graph.xml` with placeholder destinations in `app/src/main/res/navigation/nav_graph.xml`
- [x] T015 [P] Implement integration tests for `AccountDao` and `CategoryDao` (Principle X) in `app/src/androidTest/java/com/rushi/coinmaster/data/`

---

## Phase 3: User Story 1 - Onboarding (Priority: P1) 🎯 MVP

**Goal**: Guide first-time users through name, currency, first account, and income setup.

**Independent Test**: Clear app storage, launch app, complete 3 steps, verify Dashboard appears and data is persisted in Room/DataStore.

- [x] T016 [US1] Create `OnboardingViewModel` to manage 3-step state in `app/src/main/java/com/rushi/coinmaster/ui/onboarding/OnboardingViewModel.kt`
- [x] T017 [US1] Implement `OnboardingFragment` with `ViewPager2` in `app/src/main/java/com/rushi/coinmaster/ui/onboarding/OnboardingFragment.kt`
- [x] T018 [US1] Implement Step 1: `NameCurrencyFragment` for name and currency selection
- [x] T019 [US1] Implement Step 2: `AddFirstAccountFragment` to create initial account via `AccountRepository`
- [x] T020 [US1] Implement Step 3: `SetIncomeFragment` to set monthly income and seed default categories
- [x] T021 [US1] Implement launch logic in `MainActivity` to check `ONBOARDING_COMPLETE` flag
- [x] T022 [P] [US1] Unit test `OnboardingViewModel` ensuring steps validate and persist correctly

---

## Phase 4: User Story 2 - Account Management (Priority: P1)

**Goal**: CRUD for multiple financial accounts and Net Worth tracking.

**Independent Test**: Add 3 accounts (Cash, Bank, Credit Card), verify Net Worth equals (Cash + Bank - Credit Card).

- [x] T023 [US2] Implement `AccountRepository` to handle account CRUD in `app/src/main/java/com/rushi/coinmaster/data/repository/AccountRepository.kt`
- [x] T024 [US2] Implement `GetNetWorthUseCase` to sum active account balances in `app/src/main/java/com/rushi/coinmaster/domain/usecase/GetNetWorthUseCase.kt`
- [x] T025 [US2] Implement `AccountsViewModel` exposing account list and net worth in `app/src/main/java/com/rushi/coinmaster/ui/accounts/AccountsViewModel.kt`
- [x] T026 [US2] Create `AccountsFragment` with `RecyclerView` and Net Worth header
- [x] T027 [US2] Create `AddEditAccountFragment` with form for name, type, color, and icon
- [x] T028 [US2] Implement soft delete and Undo Snackbar logic for accounts
- [x] T029 [P] [US2] Unit test `AccountRepository` and `GetNetWorthUseCase`

---

## Phase 5: User Story 3 - Monthly Budget Planning (Priority: P1)

**Goal**: Zero-based budgeting with 50/30/20 split and envelope allocations.

**Independent Test**: Set income, assign all money to envelopes, activate budget. Try to activate with ₹1 imbalance and verify it's blocked.

- [x] T030 [US3] Implement `BudgetRepository` for budget month and allocation management
- [x] T031 [US3] Implement `ComputeBucketSplitUseCase` (50/30/20 with remainder to Savings) in `app/src/main/java/com/rushi/coinmaster/domain/usecase/ComputeBucketSplitUseCase.kt`
- [x] T032 [US3] Implement `ValidateZeroBalanceUseCase` to enforce zero-based budgeting in `app/src/main/java/com/rushi/coinmaster/domain/usecase/ValidateZeroBalanceUseCase.kt`
- [x] T033 [US3] Implement `BudgetViewModel` with live unallocated counter in `app/src/main/java/com/rushi/coinmaster/ui/budget/BudgetViewModel.kt`
- [x] T034 [US3] Create `BudgetFragment` to display buckets and current activation status
- [x] T035 [US3] Create `AddEditEnvelopeFragment` to manage categories within buckets
- [x] T036 [US3] Create `MonthSetupFragment` for income declaration and percentage adjustment
- [x] T037 [P] [US3] Unit test `ValidateZeroBalanceUseCase` with various allocation scenarios

---

## Phase 6: User Story 4 - Transaction Recording (Priority: P1)

**Goal**: Atomic recording of expenses, income, and transfers.

**Independent Test**: Add ₹500 expense from Bank for Groceries; verify Bank balance decreases and Groceries spent increases.

- [x] T038 [US4] Implement `TransactionRepository` with `@Transaction` atomic write logic in `app/src/main/java/com/rushi/coinmaster/data/repository/TransactionRepository.kt`
- [x] T039 [US4] Implement `AddTransactionUseCase` coupling account and transaction updates
- [x] T040 [US4] Implement `TransactionViewModel` with form validation in `app/src/main/java/com/rushi/coinmaster/ui/transactions/TransactionViewModel.kt`
- [x] T041 [US4] Create `AddTransactionFragment` with amount, type, account, and category selection
- [x] T042 [US4] Implement `Transfer` logic between two accounts (no envelope impact)
- [x] T043 [P] [US4] Integration test `TransactionRepository` ensuring atomic balance updates

---

## Phase 7: Epic 6 - Dashboard (Priority: P1)

**Goal**: Visual summary of net worth, budget health, and recent activity.

**Independent Test**: View Home screen; verify donut chart matches category spending and Net Worth is accurate.

- [ ] T044 [US18] Implement `HomeViewModel` exposing chart data and summary stats in `app/src/main/java/com/rushi/coinmaster/ui/home/HomeViewModel.kt`
- [ ] T045 [US18] Create `HomeFragment` with `MPAndroidChart` PieChart for expense breakdown
- [ ] T046 [US20] Implement horizontal `RecyclerView` for account balance cards on Dashboard
- [ ] T047 [US18] Implement "Recent Transactions" list (last 7) on Dashboard
- [ ] T048 [US19] Implement chart segment tap interaction to show category details

---

## Phase 8: Epic 7 - Language Switching (Priority: P2)

**Goal**: In-app localization for English, Hindi, and Marathi.

**Independent Test**: Change language in Settings to Hindi; verify all labels and currency formatting update immediately.

- [ ] T049 [US21] Populate `strings.xml` and create `values-hi/strings.xml`, `values-mr/strings.xml`
- [ ] T050 [US21] Implement `SettingsViewModel` and `SettingsFragment` for language selection
- [ ] T051 [US21] Integrate `LocaleHelper.applyLocale()` in `MainActivity` and `SettingsFragment`
- [ ] T052 [US21] Verify Indian numbering system (Lakh/Crore) in Hindi/Marathi locales

---

## Phase 9: User Story 12 - Sinking Funds (Priority: P2)

**Goal**: Saving for future goals with automated monthly contribution calculation.

**Independent Test**: Create ₹12,000 goal for 12 months away; verify ₹1,000 monthly contribution is displayed.

- [ ] T053 [US12] Implement `SinkingFundRepository` and `ComputeSinkingFundMonthlyContributionUseCase`
- [ ] T054 [US12] Implement `GoalsViewModel` and `GoalsFragment` with progress cards
- [ ] T055 [US12] Create `AddEditGoalFragment` with target date and amount inputs

---

## Phase 10: Polish & Cross-Cutting Concerns

**Purpose**: Finalizing quality and production readiness

- [ ] T056 [P] Verify accessibility: all touch targets ≥ 48dp as per Principle VI
- [ ] T057 [P] Configure ProGuard/R8 rules for release builds in `app/proguard-rules.pro`
- [ ] T058 [P] Perform final performance audit: verify Dashboard load < 2s on mid-range emulators
- [ ] T059 [P] Conduct final code review for Constitution compliance (Principles I-XI)

---

## Dependencies & Execution Order

### Phase Dependencies

1. **Setup (Phase 1)**: Base utilities and DI.
2. **Foundational (Phase 2)**: Database schema (BLOCKS all user stories).
3. **Onboarding (Phase 3)**: App entry point and initial data seeding.
4. **Accounts (Phase 4)** & **Budget (Phase 5)**: Can run in parallel after Onboarding.
5. **Transactions (Phase 6)**: Depends on Accounts and Budget.
6. **Dashboard (Phase 7)**: Depends on Transactions.
7. **Goals (Phase 9)**: Depends on Budget (Savings bucket).
8. **Polish (Phase 10)**: Final pass.

### Parallel Opportunities

- Foundational entities (T008-T011) can be defined in parallel.
- Accounts (Phase 4) and Budget (Phase 5) implementation can happen in parallel.
- Language switching (Phase 8) is largely independent once infrastructure is in place.
- All tasks marked [P] have no inter-task dependencies within their phase.

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1 & 2 (Foundation).
2. Complete Phase 3 (Onboarding).
3. **STOP and VALIDATE**: Verify the app can successfully capture user profile and seed initial data.

### Incremental Delivery

1. Add Phase 4 (Accounts) → Net Worth tracking active.
2. Add Phase 5 (Budget) → Monthly planning active.
3. Add Phase 6 (Transactions) → Full circular economy (Planning -> Spending -> Tracking) is complete.
4. Add Phase 7 (Dashboard) → Visual feedback loop complete.
