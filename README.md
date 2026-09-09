# Guess Market - Exercise 1 (Console application)

A prediction market engine: events with two possible outcomes are loaded from an XML file, traded
with the LMSR pricing rule, and finally closed and decided. This repository holds the first stage of
the project - the system engine and a console user interface on top of it.

## Requirements

* JDK 25
* The JAXB 4.x jar files under `lib/`: `jakarta.xml.bind-api`, `jaxb-core`, `jaxb-impl`,
  `jakarta.activation-api` and `angus-activation` (the JAXB reference implementation distribution).
  Both the build and the run scripts read the whole folder, so file names and versions are not
  hard coded anywhere.

## Project structure

| Module   | Package      | Role |
|----------|--------------|------|
| `dto`    | `gm.dto`     | Immutable data transfer objects (records) that carry information between the engine and any user interface |
| `engine` | `gm.engine`  | The system itself: the events, the pricing rules, loading and validating the data file, and the engine interface |
| `ui`     | `gm.ui`      | The console application of exercise 1: the menu, reading the input of the user and printing the output |
| `ui-fx`  | `gm.ui.fx`   | The JavaFX application of exercise 2 (in progress) |

Every user interface module talks to the engine only through the `gm.engine.api.GuessMarketEngine`
interface, and receives answers only as `gm.dto` objects, so the inner objects of the engine are
never exposed. The engine stays passive - it never reaches back into a user interface.

JavaFX is bundled under `lib/javafx-sdk-22.0.2/` (base, controls, graphics, fxml); the build and run
scripts pass it as `--module-path lib/javafx-sdk-22.0.2/lib --add-modules javafx.controls,javafx.fxml`.

## Build and run

```
build.bat        creates out\ (compiled classes) and jars\ (gm-dto.jar, gm-engine.jar,
                 gm-ui.jar, gm-ui-fx.jar), and copies lib\ next to them
run.bat          runs the JavaFX application (exercise 2)
run-console.bat  runs the console application (exercise 1)
```

The run scripts also work from inside the `jars` folder, which is the folder that gets submitted.

## Menu

1. Load an events file (XML)
2. Show all the events in the system
3. Show the trading state of an event
4. Participate in an event (buy shares)
5. Close an event and decide its result
6. Save the current state of the system to a file
7. Load a saved state of the system from a file
8. Exit

## Sample data files

`samples/` holds one valid file and several invalid ones (duplicate event id, an illegal commission,
a single option, a zero liquidity value and a file that is not XML at all), for checking the error
messages of the system.
