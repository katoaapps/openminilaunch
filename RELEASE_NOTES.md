Open 3.1.0 makes MinkSpace a cleaner platform for focused Mini-Apps and moves the virtual contact card into the stack.

- **A dedicated Mini-Apps section:** Enable or disable Mini-Apps, drag them into order, and open each app's own settings from one focused Settings tab.
- **vCard in MinkSpace:** The compact card keeps your photo, name, company, bio, and links close by. Expand it to reveal the QR code, then tap the code for its full-screen sharing view. Moving vCard into MinkSpace also frees its former fixed Home shortcut.
- **A consistent expansion model:** Mini-Apps now declare their capabilities through a common MinkSpace contract. Apps can request compact or expanded presentation, MinkSpace supplies the standard expansion action when requested, and every expanded app retains a host-owned close control.
- **Improved widget behavior:** A collapsed widget opens its provider app, while expansion exposes the interactive widget surface. Widget replacement remains with the Widget Mini-App rather than the MinkSpace shell.
- **Better physical-keyboard input:** MinkPad now translates local Alt/Sym layers on compact QWERTY phones before sending HID input. Number-layer keys were verified on both Android and macOS without leaking the host's Alt modifier.
- **Clearer contributor architecture:** Built-in Mini-Apps now use the same manifest, registry, scope, and presentation APIs, with updated documentation and contract tests for future contributions.

MinkSpace remains an on-device launcher surface. The vCard stays encrypted locally, and the MinkPad input fix does not record or upload typed text.
