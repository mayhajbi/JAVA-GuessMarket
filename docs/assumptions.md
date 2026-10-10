# Assumptions

The choices made wherever the exercise did not decide.

* **Data file** - a file describes events only (`Guess-Market` with `GM-events`). A file with users
  (`GM-users`) or with event ids is refused with a message that names the element. The events of a
  file are added only if the whole file is valid; a refused file changes nothing.
* **Validation order** - the file is checked top to bottom and the upload stops at the first fault.
  After the file itself is valid, the names of its events are checked against each other and against
  the events that already exist: an event name is unique, compared without case.
* **Users** - a user registers by logging in with a name that is not taken (compared without case)
  and starts with an empty account. The name stays taken until the server stops.
* **Market maker** - exactly one user per event: the user who uploaded its file, or who created it.
  Only that user may open and close the event. The market maker may also trade in the own
  event like any other user.
* **Event life cycle** - every event starts as *inactive* (no trading). The market maker opens
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
  buyer below zero, as the exercise says; while the balance is below zero that user is blocked from
  opening and creating events, buying shares and placing orders. The waiting buy orders of a blocked
  user are cancelled, since they can no longer be paid for. A blocked user still receives money (a
  sale, a payout, a commission, a returned subsidy), may still upload files, and a blocked market
  maker may still close the own event, so that the winners can always be paid.
* **Loading funds** - any positive amount may be loaded, by a blocked user as well. The block is
  decided by the balance of the moment: funds that bring the balance back to zero or more remove it.
* **English only** - the name of a user and the name, the description and the options of an event
  (in a file or in the form of a new event) may hold only English letters, digits and common
  punctuation marks; anything else is refused with a message.
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
* **Account screen** - lists every event the user is the market maker of or has taken part
  in, in any status and not only the active ones: the details the exercise asks for include those of
  a closed event (the shares of every option and the winner, or the profit / loss), so a closed
  event stays in the list.
* **The sketch** - the box *Account Details* at the bottom left of the account screen is read as the
  lines of the account: every movement of money, of the user or caused by another user (a commission,
  a payout), in a line of its own, with its type (Deposit, Event, Payout, Commission), its amount and
  the balance after it. Of another user only the name, the balance and whether the user is a market
  maker are shown. The login screen is not in the sketch; it is a small window of its own.
* **Messages** - a refused action, a refused file and a completed action are reported in a message
  window that names the action and the reason.
* **Environment** - the server runs on Tomcat 10.1 (`jakarta.servlet`), and the client expects it at
  `http://localhost:8080/guessmarket`. An action of the user waits for the server up to 10 seconds, and an
  automatic update up to 5. Nothing is kept beyond the running server: when it stops, the users, the events and the history are gone.
* **Text** - every textual value is compared without case, and whitespace at the edges (or line
  breaks and tabs inside a value, including a value typed into the form of a new event) is ignored.
* **Server write requests** - every request that changes data (`/login`, `/upload`, `/account/deposit`,
  `/event/create`, `/event/open`, `/event/buy`, `/event/order`, `/event/close`, `/chat/send`) is accepted only as
  POST; any other method gets 400 with the reason. The acting user is always the one of the session:
  a user name sent in the query or in the body is ignored.
* **Server request details** - a JSON body must have every field it needs: a missing field, or a
  value that does not fit (for example an unknown order side), gets 400 naming the field. When an
  event is created, `liquidity` is needed only for an LMSR event, and `baseValue`, `allowMint` and
  `initialInvestment` only for an order book event; the fields of the other method are ignored.
  The option of a buy, an order or a close is sent as 0 or 1 (the first or the second option).
* **Automatic updates** - the client pulls its data from the server again every half a second (the
  exercise allows up to 2 seconds). Every pull brings the whole data of what it refreshes: the events,
  the account, the other users, and the event that is shown. Only the screen that is shown is refreshed,
  and the details of an event only while an event is selected. An action of the user who is logged in
  is seen at once, without waiting for the next pull.
* **Chat** - the chat lives on the server and is shared by all the users; it is gone when the server
  stops. Only the lines that are new are pulled: the client remembers the version of the chat it
  shows (the amount of lines) and asks for what came after it. A line is cleaned like every other
  text, has to be in English and may not be empty; a blocked user may write as well. The time of a
  line is the moment the server received it.
* **Refresh keeps the screen** - a pull that brought nothing new changes nothing on the screen. What
  the user typed into a field, and the selected row of a table, survive every pull; a table whose rows
  did change is filled again, and its selected row stays selected when the same row is still there.
* **Server not reachable** - after 3 pulls in a row without an answer (about a second and a half) the
  client shows a status line and locks its screens; it keeps trying, and continues by itself once the
  server answers. A server that was restarted has no users, so a client that finds out it is no longer
  known goes back to the login screen with an explanation.
* **Upload** - the file travels as a multipart field and is read straight from memory, never written
  to the disk of the server; it may not be bigger than 1MB, and a bigger one is refused with a
  message. The events of a valid file are added to the existing ones, and the uploader becomes their
  market maker.
