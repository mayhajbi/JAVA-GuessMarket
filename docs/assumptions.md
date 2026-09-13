# Assumptions

The choices made wherever the exercise did not decide.

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
  buyer below zero; from then on that user is blocked from opening and creating events, buying shares
  and placing orders. The waiting buy orders of a blocked user are cancelled, since they can no longer be paid
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
* **Options** - the two options of an event must have different names (compared without case),
  whether the event comes from a data file or is created by a user.
* **Users screen** - lists every event the selected user is the market maker of or has taken part
  in, in any status and not only the active ones: the details the exercise asks for include those of
  a closed event (the shares of every option and the winner, or the profit / loss), so a closed
  event stays in the list.
* **Text** - every textual value is compared without case, and whitespace at the edges (or line
  breaks and tabs inside a value, including a value typed into the form of a new event) is ignored.
