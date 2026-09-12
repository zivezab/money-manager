# Money Manager

An offline-first Android app for tracking income and expenses, built with Kotlin,
Jetpack Compose and Room. Every figure is derived from a single local ledger —
nothing leaves the device and there is no account to create.

## What it does

**Income, split by where it came from.** Income categories are tagged as coming
from work or not, so the overview can answer "how much of this period did I earn,
and how much came from benefits, investments or side income?" — useful when
employment is intermittent. The seeded tree covers salary, bonus and freelance on
the employment side, and unemployment benefit, government support, investment,
interest, rental, gifts and refunds on the other.

**Expenses, by category.** A two-level category tree ships preloaded with the
usual suspects — home loan (principal / interest / property tax), rent, travel
(flights / hotels / tours), food, groceries, transport, utilities, healthcare,
insurance, education, shopping, entertainment, subscriptions, kids, pets, fees
and taxes. Subcategory spend rolls up into the parent in reports, so a "Travel"
slice includes its flights and hotels.

**Every period.** Daily, weekly, monthly, quarterly, yearly and all-time. The
period you pick follows you between tabs. Two settings bend the boundaries to fit
your life rather than the calendar:

- **Month starts on day N** — set it to your pay day and "this month" runs the
  25th to the 24th.
- **Week starts on** — any day of the week.

**Accounts.** Cash, bank, card, savings and investment accounts, each with an
opening balance. Balances are derived from the ledger rather than stored, so they
can never drift out of sync with the transactions behind them. Transfers move
money between accounts without counting as income or spending.

**Budgets.** Weekly, monthly, quarterly or yearly caps on a category or on total
spending. Each one compares how much you have spent against how far through the
period you are, so it can say "spending faster than the period is elapsing"
before you have actually gone over, and tells you the daily rate that would still
land on budget.

**Scheduled payments.** Salary, rent, loan instalments — anything that repeats on
a daily, weekly, monthly, quarterly or yearly cycle. Occurrences post
automatically the next time you open the app, catching up everything that fell
due while it was closed. A rule that starts on the 31st keeps landing on month
end rather than drifting down to the 28th after it passes through February.

**Stats.** A donut of the period's spending or income by category, a bar chart
comparing income against expenses across the period's own sub-ranges (days in a
week, weeks in a month, months in a year), category rankings with share-of-total
meters, and drill-down into subcategories.

**CSV import and export**, through the system file picker — no storage
permission. The exported header is also the import contract; unknown accounts and
categories in an imported file are created as they are encountered.

**Look and feel.** Material 3 with dynamic colour on Android 12+, a full dark
theme, and twelve currencies.

## Running it

Requires JDK 17 and the Android SDK (compileSdk 35, minSdk 26).

```bash
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`. To install on a connected device
or emulator:

```bash
./gradlew installDebug
```

Unit tests cover the period engine, money parsing and formatting, and recurrence
scheduling:

```bash
./gradlew testDebugUnitTest
```

## How it is put together

```
core/          Money (minor-unit arithmetic and formatting), Period (the period
               engine), PeriodController (the selection shared across tabs)
data/db/       Room entities, DAOs and the SQL behind every report
data/repo/     FinanceRepository — the single entry point onto the database
data/prefs/    DataStore-backed settings
data/recurring/RecurringEngine — posts scheduled transactions that came due
data/csv/      CSV round-trip
ui/            One package per screen, each a Compose screen plus its ViewModel
di/            AppContainer — hand-rolled dependency graph
```

A few decisions worth knowing about:

- **Amounts are `Long` minor units** (cents) everywhere. No floating point
  touches a monetary value.
- **Aggregation happens in SQL**, not in Kotlin. Totals, category rollups, daily
  series and the job/non-job split are each one query, so the UI holds only what
  it draws.
- **Balances are derived, never stored.** An account's balance is its opening
  balance plus everything paid in, minus everything paid out.
- **Charts are hand-drawn** on Compose `Canvas`. The two shapes needed are simple
  enough that a charting dependency would cost more than it saves.
- **Enums persist by name**, not ordinal, so reordering a declaration cannot
  silently reinterpret existing rows.
- **Dependency injection is a plain container.** With this few objects, a DI
  framework would cost more in build time than it saves in wiring.

## Data and privacy

Everything lives in a local SQLite database. There is no network permission in
the manifest, no analytics, and no account. CSV export is the backup story, and
Android's own backup rules cover the database and preferences.
