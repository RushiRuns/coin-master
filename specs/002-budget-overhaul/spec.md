# Feature Specification: Budget Overhaul

**Feature Branch**: `002-budget-overhaul`

**Created**: 2026-06-17

**Status**: Draft

**Input**: User description of budget, category, navigation, income, savings, notes, and transfers overhaul.

## Clarifications

### Session 2026-06-17
- Q: Where should money be transferred from and to? → A: Both envelope-to-envelope and account-to-account transfers, plus transfers to other people.
- Q: What specific new account types should be added? → A: Savings Account, Checking Account, Fixed Deposits, Wallet, Mutual Fund, and Gold (in addition to Cash and Credit Card).
- Q: What should the 'spending type' parameter be renamed to in the UI? → A: Expense Type (Fixed vs. Variable).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Personalized Onboarding & Setup (Priority: P1)
As a first-time user, I want to see a personalization screen where I can setup my profile, declare my income streams, and add my primary financial account so that I can establish my budgeting foundation immediately.

**Why this priority**: Essential first step for new users to initialize their data before accessing the home dashboard.

**Independent Test**: Can be tested by clearing app storage, launching the app, verifying that onboarding requires entering name, income streams, and first account, and successfully saving.

**Acceptance Scenarios**:
1. **Given** a first launch, **When** onboarding starts, **Then** the user is prompted to enter name, add income streams, and add their first account.
2. **Given** onboarding is complete, **When** the app is reopened, **Then** the onboarding screen is bypassed, and the dashboard is shown.

---

### User Story 2 - Simplified Navigation & Sidebar (Priority: P1)
As a user, I want a simplified bottom navigation bar with only Dashboard, Transactions, and Budget tabs, and all other options in a sidebar drawer so that the main interface feels clean and focused.

**Why this priority**: Core UX layout restructuring that defines the overall navigation flow of the app.

**Independent Test**: Verify that only three tabs are visible in the bottom navigation bar and that the hamburger icon opens a sidebar containing all secondary screens.

**Acceptance Scenarios**:
1. **Given** the app is in any main screen, **When** looking at the bottom bar, **Then** only Dashboard, Transactions, and Budget tabs are visible.
2. **Given** the dashboard, **When** clicking the sidebar menu, **Then** options for Category, Spending Type, Income, Savings, Accounts, Notes, Transfer, Goals, and Settings are shown.

---

### User Story 3 - Category-Based Envelope Grouping (Priority: P1)
As a user, I want to group my envelopes into categories (e.g., Subscriptions, Eating out, Groceries) and see categories in the Needs/Wants buckets on the Budget screen, with the option to tap a category to see its individual envelopes in a popup.

**Why this priority**: Central feature that changes how envelopes are organized, budget balance is displayed, and user details are explored.

**Independent Test**: Create a category, assign envelopes to it, navigate to the Budget tab, verify that categories are shown instead of individual envelopes, and tap a category to verify the envelope popup opens.

**Acceptance Scenarios**:
1. **Given** a budget month setup, **When** viewing the Budget tab, **Then** Needs and Wants sections show the allocated categories instead of raw envelopes.
2. **Given** a category in the Budget tab, **When** the category is tapped, **Then** a popup opens showing the list of envelopes under that category with their respective allocations and remaining balances.

---

### User Story 4 - Expense Type and Bulk Selection (Priority: P2)
As a user, I want to classify my envelopes as either Fixed or Variable expenses and manage these assignments in bulk or transfer envelopes between types.

**Why this priority**: Provides secondary analytical tracking to help users identify fixed commitments vs. adjustable spending.

**Independent Test**: Assign envelopes to Fixed/Variable classes in bulk, verify they update accordingly, and check the dashboard chart representing this distribution.

**Acceptance Scenarios**:
1. **Given** the Expense Type screen, **When** selecting multiple envelopes, **Then** the user can set their type to Fixed or Variable in bulk.
2. **Given** the Expense Type screen, **When** selecting envelopes of one type, **Then** the user can transfer them to the other type.

---

### User Story 5 - Savings Management (Priority: P2)
As a user, I want savings removed from the main budget screen and placed in a dedicated sidebar interface where I can manage my savings policies and accounts, seeing the total remaining amount at the top.

**Why this priority**: Cleans up the main budget tab while preserving savings tracking in a specialized view.

**Independent Test**: Verify savings section is gone from the Budget tab, open Savings screen from sidebar, add savings targets, and check remaining savings balance displays at the top.

**Acceptance Scenarios**:
1. **Given** the Budget tab, **When** viewing allocations, **Then** no savings section is displayed.
2. **Given** the Savings screen, **When** looking below the top bar, **Then** the remaining unallocated savings quantity is shown.

---

### User Story 6 - Notes & Transfer Management (Priority: P3)
As a user, I want to manage simple text notes and transfer targets (banks/people) in dedicated sidebar screens.

**Why this priority**: Auxiliary utility features that add organizational convenience.

**Independent Test**: Create a note, verify it saves and is editable/deletable. Create a transfer target, verify it is listed and manageable.

**Acceptance Scenarios**:
1. **Given** the Notes screen, **When** creating a note, **Then** it is saved with a title and content and listed.
2. **Given** the Transfer screen, **When** adding a recipient (person or bank), **Then** the transfer recipient is saved and editable.

---

### Edge Cases

- **Zero-Based Budgeting Balance Math**: Since Savings is moved out of the Budget tab, how is Zero-Based Budgeting validated? 
  - *Assumption*: The validation formula becomes: `Income - Allocations(Needs + Wants) - Allocations(Savings) = 0`. Month activation is blocked if the sum of all categories plus savings allocations does not equal total declared income.
- **Orphan Envelopes on Category Delete**: What happens to envelopes when their parent category is deleted?
  - *Assumption*: Deleting a category moves its envelopes to a default "Uncategorized" category, or the user is prompted to select a destination category before deletion.
- **Category Popup on Empty Categories**: What happens if a category contains no envelopes?
  - *Assumption*: Tapping an empty category displays a popup with an option to create/add envelopes to it immediately.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST show a 3-step personalization onboarding screen on first launch: (1) Profile details, (2) Income stream declarations, (3) Initial accounts.
- **FR-002**: Bottom navigation bar MUST only display three tabs: Dashboard (Home), Transactions, and Budget.
- **FR-003**: Sidebar navigation MUST contain options for: Category, Expense Type, Income, Savings, Accounts, Notes, Transfer, Goals, and Settings (positioned at the bottom).
- **FR-004**: System MUST support creating, editing, viewing, and deleting (CRUD) expense categories.
- **FR-005**: Budget tab MUST display categories instead of individual envelopes under Needs and Wants buckets.
- **FR-006**: Tapping a category in the Budget tab MUST display a popup showing all envelopes associated with that category.
- **FR-007**: Envelopes MUST support an Expense Type property with values: Fixed and Variable.
- **FR-008**: System MUST provide a bulk-editing screen for Expense Types where envelopes can be selected and modified or transferred between types in bulk.
- **FR-009**: System MUST support listing and managing income streams.
- **FR-010**: Savings section MUST be removed from the Budget tab.
- **FR-011**: System MUST provide a dedicated Savings screen in the sidebar that displays "remaining quantity" directly below the top bar.
- **FR-012**: System MUST support transferring money (envelope-to-envelope, account-to-account, and transfers to other people).
- **FR-013**: System MUST support creating, viewing, editing, and deleting text notes.
- **FR-014**: Daily activity list MUST be removed from the Dashboard.
- **FR-015**: Dashboard MUST display three breakdown charts: (1) Want vs. Need, (2) Variable vs. Fixed, and (3) Expense Categories.
- **FR-016**: Transactions tab MUST show total spending with filter options for Day, Week, and Month.
- **FR-017**: System MUST support managing transfers to people and banks (CRUD operations).
- **FR-018**: System MUST support new account types: Savings Account, Checking Account, Fixed Deposits, Wallet, Mutual Fund, and Gold (in addition to Cash and Credit Card).
- **FR-019**: System MUST rename 'spending type' to 'Expense Type' in the UI (categorized as Fixed or Variable).
- **FR-020**: Floating action button (FAB) for transaction creation MUST be present on Dashboard, Transactions, and Budget tabs.

### Key Entities

- **Category**: Name, color, icon, and display order. Groups multiple Envelopes.
- **Envelope**: (Modified) Now contains `category_id` (FK to Category) and `expense_type` (Enum: `FIXED`, `VARIABLE`).
- **IncomeStream**: Name, amount, frequency, and account link.
- **Note**: Title, content, and last updated timestamp.
- **TransferRecipient**: Name, type (Person or Bank), bank details/UPI ID, and historical transfers log.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Bottom navigation simplicity: 100% of core transactions and budget viewing is completed using only the 3 main tabs.
- **SC-002**: Navigational access: Any sidebar item (e.g., Notes, Transfer, Savings) is accessible in at most 2 taps from the home screen.
- **SC-003**: Enforced ZBB integrity: Budget month activation validation runs in under 100ms, verifying that `income - allocated_categories - allocated_savings == 0`.
- **SC-004**: Performance: Dashboard charts (Want vs Need, Fixed vs Variable, Categories) render within 200ms of tab selection.

## Assumptions

- Offline-first operation: All category, income stream, notes, and transfers data are stored locally in Room.
- Zero-Based Budgeting is still enforced across both the main budget (Needs/Wants) and the separated Savings interface.
- Standard Android Material design guidelines are applied to popups and sidebar menus.
