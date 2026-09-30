# Tests

Manual regression checks against a running server, using `curl.exe`. They are not a build step and
are not run automatically: start Tomcat with the `guessmarket` WAR deployed (from IntelliJ) first,
then from this folder run:

```
run-all.bat
```

It prints one line per check (passed or failed), a summary at the end, and, if anything failed,
the full detail (expected vs. actual) in `output.txt`. It first checks whether the server responds
at all; if it does not, the server-dependent scripts are skipped (not counted as failed), so a
downed server is never mistaken for a real check failure. Scripts that do not talk to the server
always run.

## What each script checks

| Script | Covers |
|---|---|
| `server\login.bat` | `/login`: a valid login (200 + session cookie), the same name from another session (401), a missing name (409) |

To add a check for a new server action: add a script next to `login.bat` under `server\` (same
[PASS]/[FAIL] output, exits with the failure count), and add one line for it in `run-all.bat`.
