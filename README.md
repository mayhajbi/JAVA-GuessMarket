# Guess Market

A prediction market: events with two possible outcomes and the users who trade in them are loaded
from an XML file. Every event has a market maker who opens it, funds it and finally closes and
decides it. An event is traded either with the LMSR pricing rule or through an order book (bids,
asks and minting, in the spirit of Polymarket). The current stage (exercise 2) is a JavaFX
application on top of a passive engine; exercise 1 was a console application.

## Features

* Loading and validating a data file, with a detailed message for every fault
* Users and their accounts, events with a life cycle and a market maker
* LMSR trading and order book trading
* Graphs of prices and balances, user created events, skins and animations

## Requirements

* JDK 25
* JAXB 4.x jars under `lib/` and JavaFX SDK 22.0.2 under `lib/javafx-sdk-22.0.2/` (both bundled)

## Build and run

```
build.bat        compiles into out\ and creates jars\ (gm-dto.jar, gm-engine.jar, gm-ui-fx.jar + lib\)
run.bat          runs the JavaFX application; works from the project folder or from inside jars\
```

## Project structure

| Module   | Package      | Role |
|----------|--------------|------|
| `dto`    | `gm.dto`     | Immutable data transfer objects (records) between the engine and any user interface |
| `engine` | `gm.engine`  | The events, users, pricing rules, loading and validating the data file, and the engine interface |
| `ui-fx`  | `gm.ui.fx`   | The JavaFX application: FXML screens with their controllers |
| `ui`     | `gm.ui`      | The console application of exercise 1, kept for reference and no longer built |

A user interface talks to the engine only through `gm.engine.api.GuessMarketEngine` and receives
only `gm.dto` objects. The engine stays passive - it never reaches back into a user interface.

## Documentation

* [Screens and features](docs/screens.md)
* [Assumptions](docs/assumptions.md)
