# Data Model: Coin Master

**Plan**: specs/001-coin-master-core/plan.md

## Database Schema (Room)

### Table: `accounts`
Stores financial account metadata and current balances.
- `id`: INTEGER (PK, autoGenerate)
- `name`: TEXT (NOT NULL)
- `type`: TEXT (Enum: `CASH`, `BANK_ACCOUNT`, `CREDIT_CARD`, `INVESTMENTS`)
- `balance_paise`: INTEGER (NOT NULL, DEFAULT 0) - Stored in Long.
- `color_hex`: TEXT (NOT NULL)
- `icon_name`: TEXT (NOT NULL)
- `is_deleted`: INTEGER (0/1) - Soft delete flag.

### Table: `categories` (Envelopes)
Stores budget categories and their master bucket assignment.
- `id`: INTEGER (PK, autoGenerate)
- `name`: TEXT (NOT NULL)
- `bucket_type`: TEXT (Enum: `NEEDS`, `WANTS`, `SAVINGS`)
- `color_hex`: TEXT (NOT NULL)
- `icon_name`: TEXT (NOT NULL)
- `display_order`: INTEGER
- `is_deleted`: INTEGER (0/1)

### Table: `transactions`
Individual financial records.
- `id`: INTEGER (PK, autoGenerate)
- `amount_paise`: INTEGER (NOT NULL)
- `type`: TEXT (Enum: `INCOME`, `EXPENSE`, `TRANSFER`, `BALANCE_CORRECTION`)
- `account_id`: INTEGER (FK -> accounts.id)
- `transfer_to_account_id`: INTEGER (FK -> accounts.id, NULLABLE)
- `category_id`: INTEGER (FK -> categories.id, NULLABLE)
- `budget_month_id`: INTEGER (FK -> budget_months.id, NULLABLE)
- `date`: INTEGER (Epoch millis)
- `note`: TEXT (NULLABLE)
- `is_deleted`: INTEGER (0/1)

### Table: `budget_months`
Monthly budget configuration.
- `id`: INTEGER (PK)
- `month`: INTEGER (1-12)
- `year`: INTEGER
- `income_paise`: INTEGER (Declared income)
- `needs_percent`: INTEGER (Default 50)
- `wants_percent`: INTEGER (Default 30)
- `savings_percent`: INTEGER (Default 20)
- `is_active`: INTEGER (0/1)

### Table: `envelope_allocations`
Joins categories to budget months with specific allocation amounts.
- `id`: INTEGER (PK)
- `budget_month_id`: INTEGER (FK)
- `category_id`: INTEGER (FK)
- `allocated_amount_paise`: INTEGER

### Table: `sinking_funds`
Savings goals tracked within the Savings bucket.
- `id`: INTEGER (PK)
- `name`: TEXT
- `target_amount_paise`: INTEGER
- `saved_amount_paise`: INTEGER
- `target_date`: INTEGER (Epoch millis)
- `category_id`: INTEGER (FK -> categories.id)
- `is_completed`: INTEGER (0/1)

## Data Integrity Rules
1. **Zero-Based Budgeting**: `SUM(envelope_allocations.allocated_amount_paise) == budget_months.income_paise` for active months.
2. **Smallest Unit Storage**: All financial fields use `Long` to store paise. No floating point.
3. **Atomic Writes**: `AddTransactionUseCase` updates both `transactions` and `accounts` in a single Room transaction.
