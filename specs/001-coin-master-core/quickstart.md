# Quickstart: Validating Coin Master

This guide describes how to validate the core features of Coin Master through manual and automated testing scenarios.

## 1. Onboarding Validation
**Goal**: Verify that first-time setup correctly initializes the app state.

### Scenario: First-Time Setup
1. **Action**: Launch the app for the first time.
2. **Setup**: Complete the 3-step onboarding (Profile, Add Bank Account, Set ₹50,000 Income).
3. **Check**: 
   - Dashboard shows "Net Worth: ₹X" (where X is the opening balance).
   - Budget screen shows Needs/Wants/Savings buckets pre-calculated.
   - Default categories (Rent, Groceries, etc.) are visible.

## 2. Budget Planning Validation
**Goal**: Verify Zero-Based Budgeting enforcement.

### Scenario: Activate Monthly Budget
1. **Action**: Navigate to Budget tab.
2. **Setup**: Assign ₹25,000 to Needs, ₹15,000 to Wants, and ₹10,000 to Savings envelopes.
3. **Check**: 
   - Unallocated counter shows ₹0 (Balanced).
   - "Activate Budget" button is enabled.
4. **Action**: Increase one envelope by ₹1.
5. **Check**: 
   - Unallocated counter turns red (negative).
   - "Activate Budget" button is disabled.

## 3. Transaction Validation
**Goal**: Verify account/envelope balance updates.

### Scenario: Record Expense
1. **Action**: Tap FAB (+) -> Add Expense.
2. **Setup**: ₹500 from "Bank" for "Groceries".
3. **Check**:
   - Bank Account balance decreases by ₹500.
   - Groceries envelope "Spent" increases by ₹500.
   - Transaction appears in the Dashboard's "Recent Transactions" list.

## 4. Localization Validation
**Goal**: Verify in-app language switching.

### Scenario: Switch to Hindi
1. **Action**: Settings -> Language -> Select "Hindi".
2. **Check**:
   - UI labels (Dashboard, Budget, Accounts) change to Hindi text.
   - Currency formatting uses Indian numbering system (e.g., ₹1,00,000 instead of ₹100,000).

## Automated Test Suites
Run the following commands to verify core logic:

```bash
# Run Unit Tests (MoneyMath, UseCases, ViewModels)
./gradlew testDebugUnitTest

# Run Integration Tests (Room DAOs in-memory)
./gradlew connectedCheck
```
