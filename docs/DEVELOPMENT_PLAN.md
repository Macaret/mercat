# Construction Prize Wheel: Development Plan

Single Android tablet, landscape only, fully offline. Package `com.smartcodeapps.constructionprizewheel`, minSdk 26, targetSdk 37.
Terms are defined in [GLOSSARY.md](../GLOSSARY.md).

## Settled decisions

- One `active` flag per prize. Active means shown on the wheel. At most 8 active prizes.
- Activating a 9th prize is blocked. Activating appends the prize to the end of the wheel order.
- Admin edits are staged and validated together on Save. Nothing is applied if any rule fails.
- Save requires: every active prize has an image; active probabilities are whole numbers from 0 to 100 and total exactly 100; at least one active prize can win when spinning.
- Inactive prizes keep their probability but don't count toward the total.
- Failed PIN attempts are not rate-limited.
- No default PIN. The admin sets one on first run. The PIN is exactly 4 digits.
- Restore configuration resets prizes, branding, and PIN to factory state. It does not clear spin history.
- Deleting a prize keeps its spin records, using the stored name and ID.
- Images are center-cropped to a square and stored in internal storage. Deleting a prize deletes its image.
- Spin button is disabled from spin start until the customer returns to the wheel.
- Congratulations screen waits for CONTINUE. No timeout.
- Admin dashboard has an explicit "Exit to wheel" action. No timeout.
- Redemption status can be toggled both ways.
- English only.

## Stack

Per `CLAUDE.md`: Kotlin, Jetpack Compose, MVVM with StateFlow in ViewModels, Hilt, Navigation Compose, coroutines with injected dispatchers. Room for prizes and spin records. DataStore for settings and PIN hash. Canvas for the wheel, Animatable for the spin. Android Photo Picker for images.

## Stages

Each stage ends with a build and its tests passing. Stages 1 and 2 are the domain core and are tested without UI.

**Stage 0: Foundation**
- Add dependencies: Hilt, Room, DataStore, Navigation Compose, lifecycle ViewModel Compose.
- Lock orientation to landscape in the manifest.
- Set up Hilt application and an empty Navigation graph.
- Test: app builds and launches to a placeholder screen.

**Stage 1: Domain model and selection engine**
- Pure Kotlin: `Prize`, `SpinRecord`, `RedemptionStatus`.
- Validation of the active set and probabilities, returning every failing rule.
- `PrizeSelectionEngine`: weighted random pick over active prizes, using an injected random source.
- Test: unit tests for validation rules, the 8-active cap, the 100% total, 0% prizes never winning, and weighted selection distribution with a seeded random source.

**Stage 2: Persistence**
- Room entities and DAOs for prizes and spin records. Spin records store prize name and ID as a snapshot.
- Repositories with injected dispatchers.
- Image storage: center-crop, save to internal storage, delete on prize delete.
- DataStore for branding, PIN hash, and settings.
- Test: Turbine tests on repository flows; instrumented test for Room migrations and persistence across process restart.

**Stage 3: Wheel screen (static)**
- Logo, headline, `WheelCanvas` drawing 1 to 8 equal segments with prize images centered in each, fixed pointer, SPIN button.
- Zero active prizes: spin disabled with an "add prizes" message.
- Test: Compose UI test for segment count and disabled state; ViewModel state test.

**Stage 4: Spin animation and selection**
- On SPIN: pick the winner first with `PrizeSelectionEngine`, then compute the landing angle at a random point in the middle 60% of that segment.
- `Animatable`: 5 full rotations over about 4 seconds, ending at the computed angle. The animation never decides the result.
- Button disabled from start until return to the wheel.
- Test: ViewModel test that the winner is chosen before animation starts; test that the final angle falls in the winning segment's middle 60%.

**Stage 5: Congratulations screen**
- After the animation, save the spin record, then navigate to the result screen with the winner.
- Show heading, message, prize image, name, optional description, CONTINUE.
- CONTINUE returns to the wheel and re-enables the spin button.
- Test: spin record saved before navigation; CONTINUE resets wheel state.

**Stage 6: Admin entry and PIN**
- 3-second press-and-hold on the logo. Releasing early cancels silently.
- First run: force PIN setup, exactly 4 digits, stored as a salted hash.
- PIN entry screen with keypad. No rate limiting.
- Test: hold-duration logic; hash verification; setup forced when no PIN exists.

**Stage 7: Admin dashboard and prize management**
- Dashboard with sections and "Exit to wheel".
- Prize list, add/edit/delete prize, name, description, image picker.
- Test: CRUD in the repository; delete removes image file.

> Prizes screen revision: the list has two sections, ON THE WHEEL and NOT ON THE WHEEL, with a drag handle on every row (only the handle starts a drag). Dropping inside the first section sets the wheel order. Dropping across sections activates or deactivates the prize. With 8 active, dropping an inactive prize pushes the prize that was last on the wheel into the inactive section and shows a message. The inactive section stays sorted A to Z. Rows have EDIT and DELETE buttons, and tapping a row does nothing. The edit form still blocks activation at the limit. Uses the Reorderable library (sh.calvin.reorderable 3.1.0).
>
> Stage 7 as built: edits happen on a draft copy shared across admin screens. The prize edit form covers name, description, image, active flag and probability. The list screen has SAVE and DISCARD, and Save runs the full validation. The sample-prize seed was removed.

**Stage 8: Wheel configuration and probabilities (done, folded into stage 7)**
- Wheel order, active flags, and probabilities are edited on the Prizes screen (drag sections) and the edit form.
- A separate Wheel Setup screen was built and then removed.
- Save applies all changes only if validation passes; otherwise shows every failing rule.

**Stage 9: Spin history (done)**
- SPIN HISTORY screen from the dashboard: newest first, with date, time, prize name, a Redeemed switch that works both ways, and a "Total spins / Not redeemed" summary.
- Records keep the prize name snapshot, so deleted prizes still show correctly.
- Test: formatting, ViewModel state and toggle calls, Compose screen test.

> Stage 10 as built: a SETTINGS screen with branding (logo, main title, congratulations title and message, saved with SAVE BRANDING and not allowed to be empty), CHANGE PIN (reuses the PIN screen in setup mode, new PIN plus confirmation), RESET SPIN HISTORY and RESTORE CONFIGURATION, each behind a confirmation dialog. Restore deletes prizes and images, resets branding and removes the PIN, keeps the history, then returns to the wheel. Old logo files are deleted only when branding is saved.

**Stage 10: Settings**
- Branding: logo, main headline, congratulations heading and message.
- Change PIN.
- Reset spin history, with confirmation.
- Restore configuration, with confirmation. Clears PIN and forces setup on next admin entry.
- Test: confirmation required before each destructive action; restore leaves spin history intact.

> Stage 11 as built: keyboard insets are handled in `MainActivity` (needed under edge-to-edge), the result screen scrolls its text and pins CONTINUE below it, and `TabletLayoutTest` checks that key controls stay on screen at 800x480, 960x600, 1280x800 and 1920x1200 dp, plus 800x360 for the PIN keypad.

**Stage 11: Tablet QA**
- Test on a landscape tablet emulator at common sizes.
- Verify golden path: spin, win, continue, admin entry, edit, return.
- Verify persistence across app kill and restart.

## Notes

- Photo Picker: `ActivityResultContracts.PickVisualMedia` falls back on older devices. minSdk 26 is fine, but test the picker on an API 26 device or emulator.
- `Greeting` and the template `MainActivity` content are placeholders and get replaced in stage 0.
- ADRs: not written yet. Hilt is recorded in `CLAUDE.md`, so no ADR is needed for it.
