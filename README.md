# Guess Market - Exercise 2 (JavaFX application)

A prediction market: events with two possible outcomes and the users who trade in them are loaded
from an XML file. Every event has a market maker who opens it, funds it and finally closes and
decides it. An event is traded either with the LMSR pricing rule or through an order book (bids,
asks and minting, in the spirit of Polymarket). This stage of the project replaces the console of
exercise 1 with a JavaFX application on top of the same passive engine.

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
| `ui`     | `gm.ui`      | The console application of exercise 1. Kept for reference only and not built since exercise 2: the engine now requires a user for every action, which that console does not have |
| `ui-fx`  | `gm.ui.fx`   | The JavaFX application of exercise 2: a header that loads a data file in the background, the events screen (filters, table, event details) and the users screen (balances, the events of a user, event details). The event details show the options and history of an LMSR event, or the order book of every option (with LAST/BID/ASK/MID/SPREAD), the participants and trades of an order book event, and offer the actions of the acting user: open, buy, place an order, close. Every screen is an FXML file with its own controller, connected by `app.AppController` |

Every user interface module talks to the engine only through the `gm.engine.api.GuessMarketEngine`
interface, and receives answers only as `gm.dto` objects, so the inner objects of the engine are
never exposed. The engine stays passive - it never reaches back into a user interface.

JavaFX is bundled under `lib/javafx-sdk-22.0.2/` (base, controls, graphics, fxml); the build and run
scripts pass it as `--module-path lib/javafx-sdk-22.0.2/lib --add-modules javafx.controls,javafx.fxml`.

## Build and run

```
build.bat        creates out\ (compiled classes) and jars\ (gm-dto.jar, gm-engine.jar,
                 gm-ui-fx.jar), and copies lib\ next to them
run.bat          runs the JavaFX application (exercise 2)
```

The run scripts also work from inside the `jars` folder, which is the folder that gets submitted.

## Screens

* **Header** - *Load File...* opens a file chooser (XML files only). The file is loaded in the
  background with a progress bar; a failed load shows the detailed reason and keeps the previous data.
* **Events** - every event with its status, type, commission, market maker and account balance,
  filtered by type, status and commission method (each with *All*).
* **Users** - every user with the balance; for the selected user, the events the user is the market
  maker of or takes part in.
* **Event details** (on both screens) - an LMSR event shows its option values and trading history;
  an order book event shows the order book of every option with LAST / BID / ASK / MID / SPREAD, the
  participants with their holdings and the trades, and the position of the acting user.
* **Actions** - performed by the acting user (chosen on the events screen, the selected user on the
  users screen): *Open event* and *Close event* for the market maker, *Buy* shares of an LMSR event,
  *Place order* (buy or sell, quantity, option, price) in an order book event.

Every window can be resized freely; when it is small, the screens scroll instead of cutting content.

## Sample data files

`samples/` holds the data files of exercise 1 (one valid file and several invalid ones: a duplicate
event id, an illegal commission, a single option, a zero liquidity value and a file that is not XML
at all). They use the format of exercise 1, without users, so the application of exercise 2
rejects every one of them with a detailed message. They are kept as a record of the first stage.

## Assumptions

* **Data file** - only the exercise 2 schema is accepted (`Guess-Market` with `GM-events` and
  `GM-users`). A file is loaded only if it is completely valid; a failed load leaves the previous
  system untouched.
* **Validation order** - the file is checked top to bottom and the load stops at the first fault:
  the events, then the users (unique name, positive initial cash), then that every market maker
  reference points to an event that exists, and finally that every event has exactly one market
  maker.
* **Market maker** - exactly one user per event, taken from the `GM-market-maker` blocks of the
  file. Only that user may open and close the event. The market maker may also trade in the own
  event like any other user.
* **Event life cycle** - every event is loaded as *inactive* (no trading). The market maker opens
  it (*active*) and later closes it (*closed*); an event cannot be reopened.
* **Opening an LMSR event** - the market maker pays the initial subsidy (`b * ln(2)`) from the own
  account into the event account. Opening must be fully covered: without enough money the event
  stays inactive and nothing is charged.
* **Commissions** - both commission types are paid straight to the market maker: an on-purchase
  commission by the buyer on every purchase, an on-close commission by every winner out of the
  payout.
* **Closing an event** - every share of the winning option pays its holder 1 (LMSR) or `d` (order
  book). Whatever is left in the event account afterwards (the unused part of an LMSR subsidy)
  returns to the market maker, and the waiting orders of an order book are cancelled.
* **Negative balance** - a purchase or a trade is carried out even if it brings the balance of the
  buyer below zero; from then on that user is blocked from opening events, buying shares and placing
  orders. The waiting buy orders of a blocked user are cancelled, since they can no longer be paid
  for. A blocked user still receives money (a sale, a payout, a commission, a returned subsidy), and
  a blocked market maker may still close the own event, so that the winners can always be paid.
* **Order book file values** - `d` must be a positive integer, `initial` must not be negative and
  must divide by `d` (the market maker receives whole pairs of shares), and `allow-mint` must be
  `true` or `false`.
* **Opening an order book event** - the market maker pays `initial` into the event account and
  receives `initial / d` pairs (one share of each option per pair), which the market maker may sell.
* **Order prices** - in whole cents, from 0.01 up to `d - 0.01`. A user may sell only shares the
  user holds and has not already offered in another order.
* **Matching** - a new order is matched right away, best price first and then by arrival time, and
  may consume several waiting orders. Every trade happens at the price of the waiting order. A buy
  order is served at the cheapest price available: an existing share at an ask price, or - when
  minting is allowed - a new pair minted with a waiting buy order of the other option whose price
  completes it to at least `d` (the waiting order pays its own price, the new order pays the rest of
  `d`). On an equal price the existing share is sold first. What is not matched waits in the book.
* **Order book statistics** - BID and ASK are the best waiting buy and sell orders of the option
  itself, MID is their average and SPREAD their difference; LAST is the price of the last trade.
  The value of a holding is shares times MID (or LAST when there is no MID); after closing, `d` for
  the winning option and 0 for the other.
* **Text** - every textual value is compared without case, and whitespace at the edges (or line
  breaks inside a value) is ignored.
