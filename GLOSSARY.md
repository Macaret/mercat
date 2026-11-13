# Glossary

**Prize**: An item a customer can win. Has a name, image, optional description, probability, and an active state.

**Active prize**: A prize shown on the wheel. Only active prizes can be won. Inactive prizes are kept but never shown or won.

**Wheel**: The spinning graphic on the main screen, with one equal segment per active prize.

**Wheel order**: The order of active prizes around the wheel, set by the administrator. Inactive prizes have no wheel order. Activating a prize places it at the end.

**Active limit**: At most 8 prizes can be active at once.

**Probability**: The whole-percent share of spins a prize wins (0 to 100). The probabilities of all active prizes must total exactly 100%. An active prize may have 0%: it stays on the wheel but never wins.

**Spin**: One customer turn of the wheel. Each spin produces exactly one winning prize.

**Spin record**: The saved result of a spin: date, time, winning prize name, and winning prize ID. Kept permanently until the administrator resets spin history. Deleting a prize never removes its spin records.

**Redemption status**: Whether a spin's winning prize has been handed to the customer. Either *Not redeemed* or *Redeemed*. Stored on the spin record.

**Administrator**: A store employee who manages prizes and settings after entering the admin PIN.

**Admin PIN**: The 4-digit number that grants administrator access. There is no default; the administrator sets it on first run.

**Restore configuration**: Resets prizes, branding, and the admin PIN to factory state. Does not clear spin history.

**Reset spin history**: Deletes all spin records.
