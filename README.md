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

**Inventory and assets.** A second ledger for the things themselves, kept in the
same database so a purchase can point at the expense that bought it. Two kinds:
consumables that get used up and can expire, and assets that are kept and can sit
unused. Each carries a quantity and unit, a storage location, what it cost, and —
depending on kind — a use-by date, a warranty end, or when you last used it.

**Expiry checklist and reminder.** The overview leads with what is going off:
expired, today, next three days, and so on, each line one tap from "used",
"binned", or "+7 days" when it is still fine. Snoozing something already overdue
pushes it forward from today rather than from its stale date. A daily
notification at an hour you pick names what is close to its date; it stays silent
on days when nothing is. The full checklist lives on the Inventory tab.

**Declutter, without the nagging.** Three things the data can say on its own:

- **You own more than one** — held items grouped by name, showing the total
  quantity, where the copies are, and what they came to.
- **Untouched for months** — assets still in stock whose last use, or purchase
  date when they have never been used, is older than a threshold you set.
  Consumables are left out; a stale jar is the expiry rule's business.
- **Expired but still here** — gone off and still on the shelf.

Each suggestion offers donate, bin, or keep. "Keep" records that you used it
today, which is what stops it being suggested again.

**Donation and throw-away checklist.** Anything you flag queues up under "ready
to go", split into donate and throw away. Tick a line once the thing has actually
left the house and it moves out of stock into history; "keep" puts it back.

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

Unit tests cover the period engine, money parsing and formatting, recurrence
scheduling, expiry buckets and reminder timing:

```bash
./gradlew testDebugUnitTest
```

Instrumented tests need a running emulator or a connected device. They cover the
version 1 to version 2 database migration against real data, and run the reminder
worker end to end against the real notification manager:

```bash
./gradlew connectedDebugAndroidTest
```

## How it is put together

```
core/          Money (minor-unit arithmetic and formatting), Period (the period
               engine), PeriodController (the selection shared across tabs),
               Expiry (urgency buckets and countdown wording)
data/db/       Room entities, DAOs and the SQL behind every report
data/repo/     FinanceRepository — the single entry point onto the database
data/prefs/    DataStore-backed settings
data/recurring/RecurringEngine — posts scheduled transactions that came due
data/reminder/ ExpiryReminderWorker and the notification it posts
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
- **Dependency injection is a plain container**, reachable through
  `AppContainer.get(context)` so background workers can get at it without
  casting their context to the `Application` class. With this few objects, a DI
  framework would cost more in build time than it saves in wiring.
- **Duplicate detection groups on a stored `nameKey`**, not on `lower(name)`, so
  it uses an index instead of scanning every row.
- **The reminder is a WorkManager periodic job**, so the schedule survives
  reboots and app updates without a `BOOT_COMPLETED` receiver.

## Data and privacy

Everything lives in a local SQLite database. There is no network permission in
the manifest, no analytics, and no account. The only permission requested is
`POST_NOTIFICATIONS`, asked for at the moment you switch the expiry reminder on
and needed for nothing else. CSV export is the backup story, and Android's own
backup rules cover the database and preferences.
