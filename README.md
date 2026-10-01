# Guess Market

A prediction market as a client-server application. The engine runs on a Tomcat server and is
reached over HTTP; every user runs a JavaFX client of their own, logs in by name, and sees what the
other users do within half a second. Users upload XML files of events with two possible outcomes,
and the uploader becomes the market maker of those events: the one who opens an event, funds it and
finally closes and decides it. An event is traded either with the LMSR pricing rule or through an
order book (bids, asks and minting, in the spirit of Polymarket).

## Features

* Login by name, an account for every user, loading funds and the lines of the account
* Uploading and validating files of events, with a detailed message for every fault
* Events with a life cycle and a market maker, LMSR trading and order book trading
* Automatic updates between the clients, by pulling from the server
* Graphs of prices and balances, user created events, skins and animations

## Requirements

* JDK 25 and Tomcat 10.1
* JAXB 4.x jars under `lib/`, Gson and OkHttp under `lib/gson/` and `lib/okhttp/`, and JavaFX SDK
  22.0.2 under `lib/javafx-sdk-22.0.2/` (all bundled)

## Build and run

The server is the `guessmarket` WAR artifact of the IntelliJ project (the `server` module with the
`dto`, `api` and `engine` jars, JAXB and Gson inside it), deployed to Tomcat as `guessmarket.war`.

```
build.bat        compiles into out\ and creates jars\client\ (gm-dto.jar, gm-api.jar, gm-ui-fx.jar, gm-client-fx.jar + lib\)
run-client.bat   runs the JavaFX client (the server has to be running); works from the project folder or from inside jars\client\
tests\run-all.bat   runs the regression checks: the engine and client checks compile from the sources and need nothing running, the server checks need a running server (see tests\README.md)
```

## Project structure

| Module   | Package      | Role |
|----------|--------------|------|
| `dto`    | `gm.dto`     | Immutable data transfer objects (records) between the engine and any user interface |
| `api`    | `gm.engine`  | The engine interface (`GuessMarketEngine`) and the base exception, shared by the engine and the client so that the client does not depend on the engine |
| `engine` | `gm.engine`  | The events, users, pricing rules, reading and validating an uploaded file, and the engine implementation |
| `server` | `gm.server`  | The web application (WAR): the servlets that expose the engine over HTTP |
| `ui-fx`  | `gm.ui.fx`   | The JavaFX screens the client uses: events, event details, new event, common components |
| `client-fx` | `gm.client` | The JavaFX client: login, main window, and the engine that talks to the server over HTTP |

A user interface talks to the engine only through `gm.engine.api.GuessMarketEngine` and receives
only `gm.dto` objects. The engine stays passive - it never reaches back into a user interface.

## Documentation

* [Screens and features](docs/screens.md)
* [Assumptions](docs/assumptions.md)
