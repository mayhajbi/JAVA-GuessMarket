# Screens and features

## Screens

* **Header** - *Load File...* opens a file chooser (XML files only). The file is loaded in the
  background with a progress bar; a failed load shows the detailed reason and keeps the previous data.
* **Events** - every event with its status, type, commission, market maker and account balance,
  filtered by type, status and commission method (each with *All*).
* **Users** - every user with the balance; for the selected user, the events the user is the market
  maker of or takes part in, and *Balance over time* - the amount the user started with and every
  change since then.
* **Event details** (on both screens) - an LMSR event shows its option values and trading history;
  an order book event shows the order book of every option with LAST / BID / ASK / MID / SPREAD, the
  participants with their holdings and the trades, and the position of the acting user. Below them,
  *Price over time* draws the value of one share of every option, from the moment the event was
  opened until it was closed (where the winning option is worth its full payout and the other one
  nothing). An event that was never opened, and an order book option that never had both a bid and
  an ask, have no prices to draw yet.
* **Actions** - performed by the acting user (chosen on the events screen, the selected user on the
  users screen): *Open event* and *Close event* for the market maker, *Buy* shares of an LMSR event,
  *Place order* (buy or sell, quantity, option, price) in an order book event.

Every window can be resized freely; when it is small, the screens scroll instead of cutting content.

## Additional features

* **Graphs** - a price over time graph for every event and a balance over time graph for every
  user. Always on: the price graph is part of the event details on both screens, and the balance
  graph is part of the users screen. The engine records a point whenever the value actually changes,
  so the graphs describe the whole session from the moment the data file was loaded.
* **Creating an event** - *New event...* on the events screen opens a form where a user creates an
  event of their own and becomes its market maker. The form asks for the details every event has,
  and then only the fields of the trading method that was chosen: the liquidity (b) of an LMSR
  event, or the base value (d), the initial investment and whether minting is allowed of an order
  book event. The event is created inactive with an id no other event uses, and the same user opens,
  trades in and closes it like any other event of theirs. Every rule a data file has to obey is
  checked here as well - the engine shares one validator between the two ways in. The form stays
  open until the engine accepted the event, so a rejected event keeps whatever was already typed.
* **Skins** - *Skin* in the header switches the whole window between three looks: the regular one,
  *Midnight* (dark, sans serif) and *Parchment* (warm paper, serif). Each changes the background,
  the buttons, and the font and its size of every label - including the message dialogs and the form
  of a new event. The application starts in the regular look.
* **Animations** - *Animations* in the header turns on three animations: the window fades in when a
  data file finished loading (0.8s), the name of an event is pulsed when it is opened or closed
  (0.6s), and the details of an event slide in when a different event is chosen (0.5s). The
  application starts with the animations turned off.
