# Research: Budget Overhaul

This document details the research, evaluation of alternatives, and key decisions for implementing the Budget Overhaul feature.

## Technical Decisions

### Decision 1: Database Mapping for nested Categories and Envelopes

*   **Decision**: Keep the existing `categories` table (representing Envelopes) as-is but add an `expense_category_id` foreign key. Create a new `expense_categories` table to store parent categories.
*   **Rationale**: 
    The current codebase maps the UI term "Envelopes" to the database table `categories` (via `CategoryEntity.kt`). If we were to rename the database table `categories` to `envelopes`, it would require complex Room migration scripts to drop/recreate foreign keys in `transactions` and `envelope_allocations`. By keeping `categories` representing Envelopes, and introducing a new `expense_categories` parent table, we achieve:
    1. A lightweight Room database migration (adding `expense_category_id` and `expense_type` columns).
    2. Preservation of all historical transactional links.
    3. Clean parent-child relationships where an Expense Category has many Envelopes.
*   **Alternatives Considered**:
    *   *Rename categories to envelopes and create a new categories table*: Rejected due to high migration complexity and risk of data loss.

---

### Decision 2: Zero-Based Budgeting (ZBB) with Separate Savings Screen

*   **Decision**: Keep a unified `envelope_allocations` table under the hood. Savings target allocations are still stored as standard allocations for envelopes belonging to the `SAVINGS` bucket, but they are excluded from the main Budget tab UI and managed via the sidebar Savings screen.
*   **Rationale**:
    Zero-Based Budgeting requires `Income - Allocations = 0`. If Savings allocations were stored in a separate table, validating ZBB across multiple tables would lead to race conditions and complex transaction management. Keeping all allocations in the `envelope_allocations` table allows the repository validation check to run in a single Room transaction: `SUM(allocated_amount_paise) == declared_income_paise`.
*   **Alternatives Considered**:
    *   *Create a separate savings_allocations table*: Rejected because it breaks single-source-of-truth allocation calculations and complicates ZBB validation.

---

### Decision 3: Navigation Component and Drawer Layout Integration

*   **Decision**: Implement a `DrawerLayout` wrapping a `BottomNavigationView`. The BottomNavigationView manages the 3 primary bottom tabs (Dashboard, Transactions, Budget). The sidebar drawer handles navigation to the 9 secondary options.
*   **Rationale**:
    This satisfies Android UX guidelines for top-level navigation while adhering to Project Constitution Principle V (BottomNavigationView with <= 4 tabs) and Principle I (Navigation Component).
*   **Alternatives Considered**:
    *   *Manual Fragment Transitions*: Rejected because it directly violates Principle I.
