# Screens and features

## Screens

* **Login** - the user types a name and logs in. A name that is already taken, an empty name or a
  name that is not in English is refused with a message, and the user can try again. After the
  login the events screen is shown, and the title of the window carries the name of the user.
* **Events** - every event with its status, type, commission, market maker and account balance,
  filtered by type, status and commission method (each with *All*).
* **Account** - the screen of the user who is logged in:
  * *Load file* opens a file chooser (XML files only). The file is uploaded to the server in the
    background, with a progress bar and a message while it is on its way; only another upload waits
    until it is finished. The events of the file are added to the system and the user becomes their
    market maker; a refused file shows the reason the server gave.
  * *Users* - the other users, with the name, the balance and whether the user is a market maker.
  * *Account details* - every movement of money in the account (type, amount and the balance after
    it), the latest one first.
  * *Load funds* adds an amount to the balance. A user who is blocked can load funds as well: it
    is the way out of the block.
  * *Balance over time* - the balance of the user after every change.
  * *Events - market maker / participant* - the events the user is the market maker of or takes
    part in, and the details of the selected one.
* **Chat** - a third tab: everything the users wrote, each line with its time and its writer, and a
  field with *Send* (or Enter) to add a line. Every user sees what every other user writes, within
  about half a second while the tab is shown. *Auto scroll* follows the last line; turned off, the
  screen stays where the user is reading. A user who logs in later sees the whole chat.
* **Event details** (on the Events and Account screens), laid out as in the sketch of the exercise - the name, the
  description and the summary of the event, then an LMSR event shows its option values and an order
  book event the order books of its two options side by side (with LAST / BID / ASK / MID / SPREAD)
  and the participants with their holdings. Next come the actions (with the position of the acting
  user in an order book event), and then the trading history or the trades. Below them,
  *Price over time* draws the value of one share of every option, from the moment the event was
  opened until it was closed (where the winning option is worth its full payout and the other one
  nothing). An event that was never opened, and an order book option that never had both a bid and
  an ask, have no prices to draw yet.
* **Actions** - performed by the user who is logged in: *Open event* and *Close event* for the market maker, *Buy* shares of an LMSR event,
  *Place order* (buy or sell, quantity, option, price) in an order book event.

* **Automatic updates** - what the other users do (a file uploaded, an event opened or closed, a
  purchase, an order, funds loaded) appears on the shown screen by itself within about half a second,
  without losing what was typed or selected.
* **Status line** - *Server not reachable* at the bottom of the window while the server does not
  answer; the screens are locked until it does. After a restart of the server the client returns to the
  login screen.

Every window can be resized freely; when it is small, the screens scroll instead of cutting content.

## Additional features

* **Graphs** - a price over time graph for every event and a balance over time graph for the user
  who is logged in. Always on: the price graph is part of the event details on both screens, and the
  balance graph is part of the Account screen. The engine records a point whenever the value
  actually changes.
* **Creating an event** - *New event* on the events screen opens a form where a user creates an
  event of their own and becomes its market maker. The form asks for the details every event has,
  and then only the fields of the trading method that was chosen: the liquidity of an LMSR
  event, or the base value, the initial investment and whether minting is allowed of an order
  book event. The event is created inactive with an id no other event uses, and the same user opens,
  trades in and closes it like any other event of theirs. Every rule a data file has to obey is
  checked here as well - the engine shares one validator between the two ways in. The form stays
  open until the engine accepted the event, so a rejected event keeps whatever was already typed.
* **Skins** - *Skin* in the header switches the whole window between three looks: the regular one,
  *Midnight* (dark, sans serif) and *Parchment* (warm paper, serif). Each changes the background,
  the buttons, and the font and its size of every label - including the message dialogs and the form
  of a new event. The application starts in the regular look.
* **Animations** - *Animations* in the header turns on three animations: the window fades in once a
  file was uploaded successfully (0.9s), the status of an event pulses after it was opened or
  closed, as soon as the confirmation message is closed (0.7s), and the details of an event slide in
  from the side when a different event is chosen (0.45s). A file that was refused plays none of
  them. The application starts with the animations turned off.
