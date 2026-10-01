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

There are three kinds of checks:

- **Engine checks** (`engine\*Test.java`) need nothing running. `compile.bat` first compiles the `dto`
  and `engine` sources together with the checks straight from the sources (into `%TEMP%\gm-tests`), so
  they always check the current code: no `build.bat` and nothing in IntelliJ is needed. If that
  compilation fails, the engine checks are skipped.
- **Client checks** (`client\*Test.java`) need nothing running either. `client\run-check.bat` compiles
  one check together with the client classes it uses straight from the sources (`dto`, `api`, `ui-fx`
  and `client-fx`, into `%TEMP%\gm-tests\client-classes`) and runs it with the JavaFX of `lib\`. The
  screens work there against a canned engine, and no window is opened.
- **Server checks** (`server\test-*.bat`) talk to a running server with `curl.exe`: start Tomcat with
  the `guessmarket` WAR deployed (from IntelliJ) first. After a change in the server or the engine,
  rebuild the artifact in IntelliJ and restart Tomcat. If the server does not respond at all, these
  checks are skipped (not counted as failed), so a downed server is never mistaken for a real failure.

Each check runs on its own (a failure in one does not stop the others). Test data files are in `data\`:
the example files of the exercise in `ex3\`, and the order book simulation the order book check replays.
An engine check prepares its users and events with `engine\Scenario.java`, the way the users of the
system do: they register, load money and upload files of events.

## What each check covers

| Check | Covers |
|---|---|
| `UsersAndUploadsTest` | users registering by name, deposits, account entries, the block while the balance is negative, uploading event files (piling up, the uploader as market maker, refused files change nothing) |
| `LmsrLifecycleTest` | the life cycle of an LMSR event: open, buy (commission on purchase or on close), going below zero, close and the payout, money conserved |
| `OrderBookTest` | the order book: a replay of `data\clob_simulation.html` in both commission modes, rejected orders, blocked users, minting, the order book parameters |
| `EventFilterTest` | filtering events by type, status and commission |
| `HistoryTest` | the price history of an event and the balance history of a user |
| `CreateEventTest` | a user creating an event and becoming its market maker, and the rules a created event obeys |
| `MessageWordingTest` | the wording of the refusals: no brackets, money with two digits, the name of the XML element or attribute in a message about an uploaded file |
| `EventRulesTest` | text cleanup of a created event, the active events, a blocked user creating an event, options of the same name in a file |
| `ChatTest` | the chat: the lines pile up in order, a caller that knows a version gets only what came after it, an empty line and a line that is not in English are refused |
| `client\ChatViewTest` | the chat screen: new lines are added once, the screen remembers its version, and a line that was sent appears at once and leaves the field empty |
| `server\test-chat.bat` | the chat with the classes of the client (`ChatCheck.java`): refused without a session, a line of one user reaches another as the only new line, the writer is the user of the session, wrong lines are refused with the reason |
| `client\ViewRefreshTest` | what an automatic update may change on the screen: equal rows and equal points leave a table and a graph untouched, the selected row stays selected, and the quantity typed for an event survives every refresh |
| `client\LayoutTest` | the screens in a window of 1200x760, 800x600 and 640x480: a screen scrolls instead of shrinking below its minimal size, fills the width of the window, nothing sticks out of it and no label or button is too narrow for its text |
| `server\test-live-pull.bat` | the automatic updates with the classes of the client (`LivePullCheck.java`): not logged in (401), a logged in client gets its data, what another user does arrives within 2 seconds, nothing is pulled while the updates are off |
| `server\test-login.bat` | `/login`: a valid login (200 + session cookie), the same name from another session (401), a missing name (409) |
| `server\test-read-api.bat` | the read endpoints: refused without a session, wrong parameters (400), a user sees only the own account |
| `server\test-upload.bat` | `/upload`: a valid file, the same file again, a broken file, no file, a file over 1MB, and that the upload leaves no new file in the folders of Tomcat |
| `server\test-deposit.bat` | `/account/deposit`: wrong amounts (400 with the reason), the new balance, the movement in the account log |
| `server\test-history.bat` | `/account/history`: refused without a session, a point with the new balance after every deposit, the user comes from the session |
| `server\test-write-api.bat` | create, open, buy, order and close end to end with three users: every wrong request (400 with the reason) and every balance, compared with a number computed from the LMSR formula and the order book rules |

## Adding a check

Name decides, `run-all.bat` needs no edit:

- an engine check: a class `engine\<Name>Test.java` that extends `Check` and gives its assertions to
  `run(...)` (see any existing one);
- a client check: a class `client\<Name>Test.java` that extends `Check` as well;
- a server check: a script `server\test-<name>.bat` with the same [PASS]/[FAIL] output, which exits
  with the failure count. A check that has to use the classes of the client keeps its logic in
  `server\<Name>Check.java`, and `test-<name>.bat` runs it through `client\run-check.bat` (see
  `test-live-pull.bat`). A check that has to read JSON or compute decimal numbers (not possible in a
  plain batch file) keeps its logic in `server\<name>-check.ps1`, which sends every request with
  `curl.exe`; `test-<name>.bat` then only runs it and exits with its exit code (see
  `test-write-api.bat`).
