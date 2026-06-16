# Tasks: Budget Overhaul

**Input**: Design documents from `/specs/002-budget-overhaul/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, quickstart.md

**Tests**: Unit testing and instrumented integration testing tasks are included below as part of the implementation cycle.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Contains exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and base entities

- [x] T001 [P] Create local entity classes: ExpenseCategoryEntity, IncomeStreamEntity, NoteEntity, and TransferRecipientEntity in app/src/main/java/com/rushi/coinmaster/data/local/entity/
- [x] T002 [P] Create DAO interfaces: ExpenseCategoryDao, IncomeStreamDao, NoteDao, and TransferRecipientDao in app/src/main/java/com/rushi/coinmaster/data/local/dao/
- [x] T003 [P] Configure Hilt DI module bindings for the new DAOs in app/src/main/java/com/rushi/coinmaster/di/DatabaseModule.kt

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core database migrations and main navigation restructuring

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T004 Implement Room database version 2 schema migration to add new tables and category columns in app/src/main/java/com/rushi/coinmaster/data/local/database/CoinMasterDatabase.kt
- [x] T005 [P] Create automated database migration tests in app/src/androidTest/java/com/rushi/coinmaster/data/local/MigrationTest.kt
- [x] T006 [P] Update AccountType enum and UI spinner values for new account types (Savings, Checking, Fixed Deposits, Wallet, Mutual Fund, Gold) in app/src/main/java/com/rushi/coinmaster/data/local/model/AccountType.kt and app/src/main/res/values/strings.xml
- [x] T007 Restructure main layout with DrawerLayout and NavGraph to include new sidebar destinations in app/src/main/res/layout/activity_main.xml and app/src/main/res/navigation/nav_graph.xml

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Onboarding & Setup (Priority: P1) 🎯 MVP

**Goal**: Setup name, income streams, and first account during first launch

**Independent Test**: Clear app data, run onboarding, enter name/income stream/account, verify details save and onboarding is bypassed on next launch.

### Implementation for User Story 1

- [x] T008 [P] [US1] Create IncomeStream domain model and repository mappings in app/src/main/java/com/rushi/coinmaster/domain/model/IncomeStream.kt and app/src/main/java/com/rushi/coinmaster/data/repository/IncomeStreamRepositoryImpl.kt
- [x] T009 [US1] Update OnboardingFragment to ask for name, income stream(s), and first account in app/src/main/java/com/rushi/coinmaster/ui/onboarding/OnboardingFragment.kt
- [x] T010 [US1] Create onboarding unit tests to verify data persistence in app/src/test/java/com/rushi/coinmaster/ui/onboarding/OnboardingViewModelTest.kt

**Checkpoint**: User Story 1 is fully functional and testable independently.

---

## Phase 4: User Story 2 - Navigation & Sidebar (Priority: P1)

**Goal**: Keep 3 bottom navigation tabs and move secondary options to the sidebar drawer

**Independent Test**: Tap bottom bar to verify only Dashboard, Transactions, and Budget are listed; tap drawer menu to verify the 9 secondary items are accessible.

### Implementation for User Story 2

- [x] T011 [P] [US2] Update BottomNavigationView menu options to show only Dashboard, Transactions, and Budget in app/src/main/res/menu/bottom_nav_menu.xml
- [x] T012 [US2] Bind Navigation Component DrawerLayout to the sidebar menu in app/src/main/java/com/rushi/coinmaster/MainActivity.kt
- [x] T013 [US2] Move AccountsFragment and SettingsFragment navigation destinations to the sidebar drawer in app/src/main/res/navigation/nav_graph.xml

**Checkpoint**: User Stories 1 AND 2 are fully integrated and functional.

---

## Phase 5: User Story 3 - Category-Based Envelopes (Priority: P1)

**Goal**: Group envelopes under parent categories and show categories instead of envelopes in Budget Needs/Wants with a detail popup.

**Independent Test**: Create an expense category, add envelopes, go to Budget tab, verify category displays in Needs/Wants, tap it and verify details popup opens.

### Implementation for User Story 3

- [x] T014 [P] [US3] Create parent category management fragment and list layout in app/src/main/java/com/rushi/coinmaster/ui/categories/ManageCategoriesFragment.kt and app/src/main/res/layout/fragment_manage_categories.xml
- [x] T015 [US3] Modify CategoryEntity (Envelopes) to add nullable expense_category_id FK in app/src/main/java/com/rushi/coinmaster/data/local/entity/CategoryEntity.kt
- [x] T016 [US3] Update BudgetFragment layout and binding to group envelopes by category under Needs/Wants buckets in app/src/main/java/com/rushi/coinmaster/ui/budget/BudgetFragment.kt
- [x] T017 [US3] Implement CategoryDetailDialogFragment popup showing envelopes list with remaining balances in app/src/main/java/com/rushi/coinmaster/ui/budget/CategoryDetailDialogFragment.kt

**Checkpoint**: Category-based grouping is fully functional.

---

## Phase 6: User Story 4 - Expense Type Classification (Priority: P2)

**Goal**: Classify envelopes as Fixed/Variable and select/transfer them in bulk

**Independent Test**: Navigate to Expense Type sidebar option, select multiple envelopes, bulk assign them to Fixed or Variable, and verify state changes.

### Implementation for User Story 4

- [x] T018 [P] [US4] Add expense_type column to CategoryEntity and domain mapping in app/src/main/java/com/rushi/coinmaster/data/local/entity/CategoryEntity.kt
- [x] T019 [US4] Create ExpenseTypeFragment with bulk-selection lists to assign/transfer envelopes between Fixed and Variable in app/src/main/java/com/rushi/coinmaster/ui/expensetype/ExpenseTypeFragment.kt and app/src/main/res/layout/fragment_expense_type.xml

**Checkpoint**: Expense Type classification is fully testable.

---

## Phase 7: User Story 5 - Savings Management (Priority: P2)

**Goal**: Remove Savings from Budget tab and manage it in a separate view with remaining quantity displayed at the top

**Independent Test**: Open Savings from sidebar, verify ZBB validation works, and check "remaining quantity" updates at the top of the screen.

### Implementation for User Story 5

- [x] T020 [US5] Exclude Savings envelopes from Needs/Wants lists in app/src/main/java/com/rushi/coinmaster/ui/budget/BudgetFragment.kt
- [x] T021 [US5] Create SavingsFragment for targets configuration displaying remaining quantity below the top bar in app/src/main/java/com/rushi/coinmaster/ui/savings/SavingsFragment.kt and app/src/main/res/layout/fragment_savings.xml
- [x] T022 [US5] Update BudgetRepository validation engine to enforce zero-based budgeting across Needs, Wants, and Savings allocations in app/src/main/java/com/rushi/coinmaster/data/repository/

---

## Phase 8: User Story 6 - Notes & Transfers (Priority: P3)

**Goal**: Manage simple text notes and transfers to external people/banks in separate sidebar views

**Independent Test**: Create and delete a note; create a transfer recipient and record a transfer transaction.

### Implementation for User Story 6

- [x] T023 [P] [US6] Create NotesFragment for simple text note CRUD operations in app/src/main/java/com/rushi/coinmaster/ui/notes/NotesFragment.kt and app/src/main/res/layout/fragment_notes.xml
- [x] T024 [P] [US6] Create ManageTransfersFragment for managing bank/people recipients in app/src/main/java/com/rushi/coinmaster/ui/transfers/ManageTransfersFragment.kt and app/src/main/res/layout/fragment_manage_transfers.xml
- [x] T025 [US6] Update AddTransactionFragment to support EXTERNAL_TRANSFER transactions with recipient selectors in app/src/main/java/com/rushi/coinmaster/ui/transactions/AddTransactionFragment.kt

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Dashboard improvements, transaction filtering, and validation checks

- [x] T026 [P] Add Floating Action Buttons (FAB) for transaction creation on Dashboard, Transactions, and Budget layouts in app/src/main/res/layout/
- [x] T027 [P] Replace expense breakdown chart with three charts (Want vs Need, Expense Type, and Category breakdown) on Dashboard in app/src/main/java/com/rushi/coinmaster/ui/dashboard/DashboardFragment.kt
- [x] T028 [US2] Remove daily activity list component from dashboard layout/controller and create a Daily Activity sub-tab inside the Transactions screen in app/src/main/res/layout/fragment_dashboard.xml, app/src/main/java/com/rushi/coinmaster/ui/dashboard/DashboardFragment.kt, and app/src/main/java/com/rushi/coinmaster/ui/transactions/
- [x] T029 [P] Add Day, Week, and Month filters to Transactions list in app/src/main/java/com/rushi/coinmaster/ui/transactions/TransactionsFragment.kt
- [x] T030 Run quickstart validation scenarios in specs/002-budget-overhaul/quickstart.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - starts immediately.
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories.
- **User Stories (Phase 3+)**: All depend on Foundational phase completion.
  - Can proceed sequentially in priority order (P1 → P2 → P3).
- **Polish (Phase 9)**: Depends on all user stories being completed.

---

## Parallel Example: User Story 3

```bash
# Launch model updates:
Task: "Modify CategoryEntity (Envelopes) to add nullable expense_category_id FK in app/src/main/java/com/rushi/coinmaster/data/local/entity/CategoryEntity.kt"

# Launch layout development:
Task: "Create parent category management fragment and list layout in app/src/main/java/com/rushi/coinmaster/ui/categories/ManageCategoriesFragment.kt"
```

---

## Implementation Strategy

### MVP First (User Stories 1-3)
1. Setup Phase 1 and Foundational Phase 2 (ZBB DB + Nav Drawer).
2. Complete US1 (Onboarding), US2 (Sidebar layout), and US3 (Category grouping).
3. **STOP and VALIDATE**: Verify Category grouping on the Budget screen.
