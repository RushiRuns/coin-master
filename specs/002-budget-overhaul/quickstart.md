# Quickstart: Budget Overhaul Validation

This guide outlines the validation scenarios and test execution steps to verify that the Budget Overhaul features work correctly end-to-end.

## Prerequisites

*   Android SDK installed with emulator/device configured.
*   Latest project build compiles without errors.
*   Test database is initialized.

---

## Validation Scenarios

### Scenario 1: Onboarding Flow and Income Stream Setup
1.  **Action**: Clear app data and launch the app.
2.  **Steps**:
    *   Enter name "John Doe" and currency "INR".
    *   Add income stream "Salary" with amount ₹50,000 linked to first account "HDFC Bank".
    *   Confirm onboarding completion.
3.  **Expected Outcome**: 
    *   The database should contain one record in `accounts` named "HDFC Bank".
    *   The `income_streams` table contains "Salary" with amount `5000000` (paise) pointing to the HDFC Bank account.
    *   Onboarding screen is bypassed on subsequent launches.

### Scenario 2: Nested Category Budget View and Popup
1.  **Action**: Setup a budget for the current month.
2.  **Steps**:
    *   Create Expense Category "Entertainment" and add envelopes "Netflix" and "Cinema" to it.
    *   Go to the Budget tab.
3.  **Expected Outcome**:
    *   The "Wants" card shows the category "Entertainment" instead of the individual envelopes.
    *   Tapping the "Entertainment" card displays a popup listing "Netflix" and "Cinema" with their corresponding allocations.

### Scenario 3: Expense Type Bulk Assignment
1.  **Action**: Classify multiple envelopes.
2.  **Steps**:
    *   Navigate to the Expense Type screen in the sidebar.
    *   Select envelopes "Rent", "Car Loan", and "Insurance" and bulk-assign them to "Fixed".
    *   Select envelopes "Dining Out" and "Groceries" and bulk-assign them to "Variable".
3.  **Expected Outcome**:
    *   The `categories` table updates `expense_type` for "Rent", "Car Loan", and "Insurance" to `FIXED`.
    *   The `categories` table updates `expense_type` for "Dining Out" and "Groceries" to `VARIABLE`.
    *   The dashboard displays a chart representing this distribution.

### Scenario 4: Savings Separation and ZBB Enforcement
1.  **Action**: Balance the month's budget.
2.  **Steps**:
    *   Declare Income = ₹50,000.
    *   Allocate ₹40,000 to Needs and Wants categories.
    *   Navigate to the Savings screen and allocate ₹10,000 to Savings targets.
    *   Attempt to activate the budget.
3.  **Expected Outcome**:
    *   The system successfully activates the budget since `50000 - 40000 - 10000 == 0`.
    *   If allocations are altered to sum to ₹49,000, month activation is blocked with a discrepancy warning.

---

## Verification Commands

### Run Unit Tests
To run repository and ViewModel tests verifying data integrity and ZBB rules:
```bash
./gradlew testDebugUnitTest
```

### Run Instrumented Tests
To verify database migrations (Room migration verification):
```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rushi.coinmaster.data.local.MigrationTest
```
