# Tests

Regression checks of the whole system. They are not a build step and are not run automatically. From
this folder run:

```
run-all.bat
```

It prints one line per check (passed or failed), a summary at the end, and, if anything failed, the
full detail (expected vs. actual) of every failed check in `output.txt`, which is emptied at the start
of each run. Needs JDK 25 (`javac`, `java`) on the PATH; `run-all.bat` also tries the JDK 25 folder of
the development machine.

There are two kinds of checks:

- **Engine checks** (`engine\*Test.java`) need nothing running. `compile.bat` first compiles the `dto`
  and `engine` sources together with the checks straight from the sources (into `%TEMP%\gm-tests`), so
  they always check the current code: no `build.bat` and nothing in IntelliJ is needed. If that
  compilation fails, the engine checks are skipped.
- **Server checks** (`server\test-*.bat`) talk to a running server with `curl.exe`: start Tomcat with
  the `guessmarket` WAR deployed (from IntelliJ) first. After a change in the server or the engine,
  rebuild the artifact in IntelliJ and restart Tomcat. If the server does not respond at all, these
  checks are skipped (not counted as failed), so a downed server is never mistaken for a real failure.

Each check runs on its own (a failure in one does not stop the others). Test data files are in `data\`
(`ex2\` and `ex3\` are the file formats of the two exercises).

## What each check covers

| Check | Covers |
|---|---|
| `UsersAndUploadsTest` | users registering by name, deposits, account entries, the block while the balance is negative, uploading event files (piling up, the uploader as market maker, refused files change nothing) |
| `LmsrLifecycleTest` | the life cycle of an LMSR event: open, buy (commission on purchase or on close), going below zero, close and the payout, money conserved |
| `OrderBookTest` | the order book: a replay of `data\ex2\clob_simulation.html` in both commission modes, rejected orders, blocked users, minting, the order book parameters |
| `EventFilterTest` | filtering events by type, status and commission |
| `HistoryTest` | the price history of an event and the balance history of a user |
| `CreateEventTest` | a user creating an event and becoming its market maker, and the rules a created event obeys |
| `EventRulesTest` | text cleanup of a created event, the active events, a blocked user creating an event, options of the same name in a file |
| `server\test-login.bat` | `/login`: a valid login (200 + session cookie), the same name from another session (401), a missing name (409) |
| `server\test-read-api.bat` | the read endpoints: refused without a session, wrong parameters (400), a user sees only the own account |
| `server\test-upload.bat` | `/upload`: a valid file, the same file again, a broken file, no file, a file over 1MB, and that the upload leaves no new file in the folders of Tomcat |
| `server\test-deposit.bat` | `/account/deposit`: wrong amounts (400 with the reason), the new balance, the movement in the account log |
| `server\test-write-api.bat` | create, open, buy, order and close end to end with three users: every wrong request (400 with the reason) and every balance, compared with a number computed from the LMSR formula and the order book rules |

## Adding a check

Name decides, `run-all.bat` needs no edit:

- an engine check: a class `engine\<Name>Test.java` that extends `Check` and gives its assertions to
  `run(...)` (see any existing one);
- a server check: a script `server\test-<name>.bat` with the same [PASS]/[FAIL] output, which exits
  with the failure count. A check that has to read JSON or compute decimal numbers (not possible in a
  plain batch file) keeps its logic in `server\<name>-check.ps1`, which sends every request with
  `curl.exe`; `test-<name>.bat` then only runs it and exits with its exit code (see
  `test-write-api.bat`).
