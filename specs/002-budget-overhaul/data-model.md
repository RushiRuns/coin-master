# Data Model: Budget Overhaul

This document details the database schema updates (Room) for the Coin Master application to support the Budget Overhaul feature.

## Database Schema (Room)

### New Table: `expense_categories`
Stores expense categories that act as parent groupings for envelopes.
- `id`: INTEGER (PK, autoGenerate)
- `name`: TEXT (NOT NULL)
- `color_hex`: TEXT (NOT NULL)
- `icon_name`: TEXT (NOT NULL)
- `is_deleted`: INTEGER (0/1, DEFAULT 0)

### Modified Table: `categories` (Envelopes)
Added columns to link to a parent category and record the expense type.
- `id`: INTEGER (PK, autoGenerate)
- `name`: TEXT (NOT NULL)
- `bucket_type`: TEXT (Enum: `NEEDS`, `WANTS`, `SAVINGS`)
- `color_hex`: TEXT (NOT NULL)
- `icon_name`: TEXT (NOT NULL)
- `display_order`: INTEGER
- `is_deleted`: INTEGER (0/1, DEFAULT 0)
- **[NEW]** `expense_category_id`: INTEGER (FK -> `expense_categories.id`, NULLABLE)
- **[NEW]** `expense_type`: TEXT (Enum: `FIXED`, `VARIABLE`, DEFAULT `VARIABLE`, NOT NULL)

### New Table: `income_streams`
Stores user declared income sources for onboarding and settings management.
- `id`: INTEGER (PK, autoGenerate)
- `name`: TEXT (NOT NULL)
- `amount_paise`: INTEGER (NOT NULL)
- `account_id`: INTEGER (FK -> `accounts.id`, NULLABLE)
- `is_deleted`: INTEGER (0/1, DEFAULT 0)

### New Table: `notes`
Stores simple notes created by the user.
- `id`: INTEGER (PK, autoGenerate)
- `title`: TEXT (NOT NULL)
- `content`: TEXT (NOT NULL)
- `updated_at`: INTEGER (Epoch millis, NOT NULL)
- `is_deleted`: INTEGER (0/1, DEFAULT 0)

### New Table: `transfer_recipients`
Stores details of banks or people to whom the user transfers money.
- `id`: INTEGER (PK, autoGenerate)
- `name`: TEXT (NOT NULL)
- `type`: TEXT (Enum: `PERSON`, `BANK`, NOT NULL)
- `bank_details`: TEXT (NULLABLE) - UPI ID, IFSC, or account number.
- `is_deleted`: INTEGER (0/1, DEFAULT 0)

### Modified Table: `transactions`
Modified to support transfers to external recipients.
- `id`: INTEGER (PK, autoGenerate)
- `amount_paise`: INTEGER (NOT NULL)
- `type`: TEXT (Enum: `INCOME`, `EXPENSE`, `TRANSFER`, `BALANCE_CORRECTION`, `EXTERNAL_TRANSFER`)
- `account_id`: INTEGER (FK -> `accounts.id`)
- `transfer_to_account_id`: INTEGER (FK -> `accounts.id`, NULLABLE)
- **[NEW]** `transfer_recipient_id`: INTEGER (FK -> `transfer_recipients.id`, NULLABLE)
- `category_id`: INTEGER (FK -> `categories.id`, NULLABLE)
- `budget_month_id`: INTEGER (FK -> `budget_months.id`, NULLABLE)
- `date`: INTEGER (Epoch millis)
- `note`: TEXT (NULLABLE)
- `is_deleted`: INTEGER (0/1, DEFAULT 0)

## Data Integrity Rules

1.  **Zero-Based Budgeting Integrity**: 
    The ZBB check must validate:
    `SUM(envelope_allocations.allocated_amount_paise) == budget_months.income_paise`
    This includes allocations to all envelopes (Needs, Wants, and Savings), ensuring that the unallocated savings displayed in the Savings screen matches the calculated remainder.
2.  **Parent-Child Delete Rule**: 
    When an `expense_categories` entry is soft-deleted, any associated `categories` (Envelopes) must have their `expense_category_id` set to `NULL` (uncategorized).
3.  **Atomic Transfers**: 
    External transfers (type `EXTERNAL_TRANSFER`) decrease the source `account_id` balance but do not increase any internal `transfer_to_account_id`.
