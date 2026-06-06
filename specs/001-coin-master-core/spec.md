# Feature Specification: Coin Master — Personal Budget Planner

**Feature Branch**: `001-coin-master-core`

**Created**: 2026-06-06

**Status**: Draft

**Input**: User description for the full "Coin Master" application core.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - First Launch & Onboarding (Priority: P1)

As a first-time user, I want to be guided through a simple setup so that I can start using the app without confusion.

**Why this priority**: Essential for first-time usability and baseline data entry.

**Independent Test**: Can be tested by clearing app data and verifying the 3-step setup flow appears and persists values.

**Acceptance Scenarios**:

1. **Given** the app is launched for the first time, **When** onboarding starts, **Then** the user must complete (1) Name/Currency, (2) First Account, and (3) Monthly Income steps sequentially.
2. **Given** onboarding is partially complete, **When** the app is closed and reopened, **Then** the user must remain on the incomplete step and cannot skip forward.

---

### User Story 2 - Account Management (Priority: P1)

As a user, I want to add multiple financial accounts so that I can track all of my money in one place.

**Why this priority**: Core foundation for tracking net worth and transaction sources.

**Independent Test**: Can be tested by adding various account types (Cash, Bank, Credit Card) and verifying they appear in the Accounts list.

**Acceptance Scenarios**:

1. **Given** the user is on the Accounts screen, **When** they add a new account, **Then** it must include name, type, balance, icon, and color, and update the total net worth.
2. **Given** a credit card account, **When** its balance is added, **Then** it must be treated as a negative value for net worth calculation.

---

### User Story 3 - Monthly Budget Planning (Priority: P1)

As a user, I want to declare my monthly income and assign it to envelopes so that every rupee has a specific purpose.

**Why this priority**: Core product philosophy (Zero-Based Budgeting).

**Independent Test**: Can be tested by setting a monthly income and verifying that the 50/30/20 buckets are calculated and envelope allocations must equal income to "activate".

**Acceptance Scenarios**:

1. **Given** a new budget month, **When** the user assigns money to envelopes, **Then** the app must show a live unallocated counter.
2. **Given** an unbalanced budget (surplus or deficit), **When** the user tries to activate the month, **Then** the app must block activation and show the exact discrepancy.

---

### User Story 4 - Transaction Recording (Priority: P1)

As a user, I want to record expenses and transfers so that my balances stay accurate.

**Why this priority**: Necessary for real-time budget tracking.

**Independent Test**: Can be tested by adding an expense to an envelope and verifying both the envelope remaining balance and the account balance decrease.

**Acceptance Scenarios**:

1. **Given** an active budget, **When** an expense is recorded for an envelope, **Then** the envelope "spent" balance must increase and the account balance must decrease.
2. **Given** two accounts, **When** a transfer is recorded, **Then** money moves between accounts but does NOT affect any envelope balance.

### Edge Cases

- **Rounding in 50/30/20**: What happens when income doesn't split perfectly? (Assumption: Principle III says remainder goes to Savings).
- **Over-spending**: How does the UI handle an envelope going into negative balance? (Assumption: UI turns red, but transaction is allowed).
- **Leap Years/Month Lengths**: Budgeting is per calendar month.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST support a 3-step linear onboarding flow (Profile, Account, Income).
- **FR-002**: System MUST support at least three account types: Cash, Bank, and Credit Card.
- **FR-003**: System MUST [NEEDS CLARIFICATION: Should an "Investments" account type be supported in v1?]
- **FR-004**: System MUST calculate total Net Worth as the sum of all accounts (Credit Cards as negative).
- **FR-005**: System MUST auto-calculate a 50/30/20 split of declared monthly income.
- **FR-006**: System MUST enforce Zero-Based Budgeting (Allocations == Income) before activating a month.
- **FR-007**: System MUST [NEEDS CLARIFICATION: When income is updated mid-month, should allocations auto-recalculate or require manual adjustment?]
- **FR-008**: System MUST support "Sinking Funds" within the Savings bucket with target dates and automated monthly contribution calculation.
- **FR-009**: System MUST allow recording Expenses, Income, and Transfers.
- **FR-010**: System MUST [NEEDS CLARIFICATION: If an account is deleted, should its linked transactions be deleted or retained with a label?]
- **FR-011**: System MUST provide a Dashboard showing Net Worth, Budget Health (Progress bars), and Recent Transactions.
- **FR-012**: System MUST support in-app language switching (English, Hindi, Marathi).
- **FR-013**: System MUST apply Indian numbering format (Lakhs/Crores) for Hindi/Marathi locales.

### Key Entities

- **Account**: Name, Type, Balance (Long), Icon, Color.
- **Transaction**: Amount (Long), Date, Account, Category (Envelope), Type (Expense/Income/Transfer/Correction), Note.
- **Envelope**: Name, Allocation (Long), Spent (Long), Bucket Type (Need/Want/Saving), Icon, Color.
- **BudgetMonth**: Month/Year, Declared Income, Needs%, Wants%, Savings%.
- **SinkingFund**: Name, Target Amount, Target Date, Current Progress.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: First-time users can complete onboarding and add one transaction in under 5 minutes.
- **SC-002**: 100% of financial calculations (Net Worth, Budget Balance) use integer arithmetic to prevent ₹0.01 errors.
- **SC-003**: The Dashboard displays the three critical metrics (Net Worth, Monthly Health, Track Status) without scrolling.
- **SC-004**: Language switching updates 100% of UI strings immediately without requiring an app restart.
- **SC-005**: 100% of core features (budgeting, transactions) work with Airplane Mode enabled (Offline-First).

## Assumptions

- **Target Audience**: Indian users (based on localization and numbering system requirements).
- **Platform**: Android mid-range devices (per NFR-04).
- **Data Persistence**: Local Room database is sufficient; user handles their own backups via local export (JSON/CSV).
- **UI Design**: Strict adherence to Material Design 2 as per the Project Constitution.

## Out of Scope

- Cloud Sync / Remote Backup.
- Multi-currency (beyond formatting).
- Recurring Transactions (Auto-pay).
- Bank Statement Import (CSV/PDF).
- Dark Mode.
- Comparative analytics across multiple years.
