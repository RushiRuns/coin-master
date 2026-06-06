# Research: Coin Master Technical Decisions

**Plan**: specs/001-coin-master-core/plan.md

## Resolved Clarifications

### Decision 1: Investment Account Support (C-01)
- **Decision**: Include `INVESTMENTS` account type in v1.
- **Rationale**: While liquid cash is primary, tracking SIP/portfolio value is essential for a "Personal Budget Planner" seeking to track net worth comprehensively.
- **Alternatives**: Deferring to v2 was considered, but adding a new enum value and basic balance tracking is low-effort with high user value.

### Decision 2: Mid-Month Income Updates (C-02)
- **Decision**: Manual Adjustment.
- **Rationale**: Proportional auto-allocation might assign money where it's not needed. Zero-based budgeting requires the user to be deliberate. The app will update the "Unallocated" surplus and block budget confirmation until assigned.
- **Alternatives**: Auto-recalculation (rejected for lack of user control).

### Decision 3: Deleted Account Integrity (C-03)
- **Decision**: Preserve Data (Soft Delete + Label).
- **Rationale**: Deleting transactions would break historical budget accuracy. We will add an `account_name_snapshot` to transactions or use a "Deleted Account" label in the UI while retaining the transaction record.
- **Alternatives**: Cascade delete (rejected as it destroys financial history).

### Decision 4: Mid-Month Bonus Allocation (C-04)
- **Decision**: Yes, assignable to envelopes.
- **Rationale**: Financial life is dynamic. Users should be able to receive unexpected income and immediately "give it a job" by assigning it to a Savings goal or a Want envelope.
- **Alternatives**: Month-start only (too restrictive).

## Technology Research

### Android Locale Switching (API < 33)
- **Finding**: `AppCompatDelegate.setApplicationLocales` is the modern way, but `LocaleHelper` with `context.createConfigurationContext()` is necessary for backward compatibility.
- **Strategy**: Implement a utility that handles both paths seamlessly.

### Monetary Arithmetic
- **Finding**: Floating point math (Double/Float) is non-deterministic for currency.
- **Strategy**: Use `Long` (paise) for all internal storage and computation. Use `BigDecimal` only for final display formatting.

### Reactive UI with Room
- **Finding**: Room returns `Flow<T>` which is cold. ViewModels should use `stateIn` to convert to `StateFlow` for UI consumption to ensure configuration changes don't restart queries.
- **Strategy**: Standardize on `StateFlow` in all ViewModels.
