[![Logisim-evolution](docs/img/logisim-evolution-logo.png)](https://github.com/logisim-evolution/logisim-evolution)

# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Added a new preference to allow the user to choose the action keys for many functions.
- Added support for scanning 7-segment display on FPGA-boards.
- Added first support for the openFpga toolchain for the ecp5 famely.
  Note that this is experimental for the moment, so use it at your own risk. 
- Introduced user-defined color for components.
- Added architecture designation to macOS build.
- Added TTL 7487: 4-bit True/complement, zero/one elements.
- Added TTL 74151: 8-line to 1 line data selector.
- Added TTL 74153: dual 4-line to 1 line data selector.
- Added TTL 74181: arithmetic logic unit.
- Added TTL 74182: look-ahead carry generator.
- Added TTL 74299: 8-bit universal shift register with three-state outputs.
- Added TTL 74381: arithmetic logic unit.
- Added TTL 74541: Octal buffers with three-state outputs.
- Added TTL 74670: 4-by-4 register file with three-state outputs.

### Changed

- Made component icons more uniform.
- Update controlled buffer behavior to pass U and E inputs while enabled (#1642).
- Attribute sheet now honors application color theme.
- Attribute sheet now displays HEX value of color properties.
- Changed RAM default output from error to undefined (#1747).
- Improved Chinese localization
  - Changed language code from `cn` to `zh`.
  - Chinese users (also including those who use other forks of Logisim
      that are using `cn` language code) will be required to manually modify language settings.

### Fixed

- Fixed select port positioning on Multiplexer to be more consistent in some cases (#1734)
- Fixed appearance of LSe desktop icon (#1662).
- Fixed Karnaugh map color index bug.
- Fixed Wrong HDL generation bug in the PortIO component and added the single bit version.

## [3.8.0] - 2022-10-02

### Added

- Added reset value attribute to input pins.
- Added ability to deleted Sub-circuit with `DELETE` key, along with `BACKSPACE` used so far.
- Added TTL 74164, 74192 and 74193.
- Added TTL 74138: 3-line to 8-line decoder.
- Added TTL 74240, 74241, 74244: octal buffers with three-state outputs.
- Added TTL 74245: octal bus transceivers with three-state outputs.
- Added TTL 74166: 8-bit parallel-to-serial shift register with clear.

### Changed

- Moved TTL 74266 to 747266.

### Fixed

- Fixed `Simulate` -> `Timing Diagram` not opening when using "Nimbus" look and feel.
- Fixed pressing `CTRL`+`0` selecting the wrong element in the toolbar.
- Fixed TTL 7485 `7485HdlGenerator` generating wrong HDL type.
- Fixed TTL 74139, 7447 outputting inverted logic.
- Fixed TTL 74175, CLR inverted.
- Fixed TTL 7436 pin arrangement.
- Fixed bug preventing TTL 7442, 7443 and 7444 from being placed on the circuit canvas.
- Fixed TTL 74165, correct order of inputs, load asynchronously.
- Fixed TTL 74266 correctly reimplemented with open-collector outputs.
- Fixed boolean algebra minimal form bug
- Fixed random fill Rom bug
- Fixed off grid components bug that could lead to OutOfMemory error.

### Removed

- Removed autolabler for tunnels, such that all get the same label in case of renaming.
- Removed fixed LM_Licence setting.

## [3.7.2] - 2021-11-09

## Changed

- You can now swap the placement of main canvas and component tree/properties pane.

### Fixed

- Fixed Preferences/Window "Reset window layout to defaults" not doing much.
- Fixed Gradle builder failing to compile LSe if sources were not checked out from Git.
- Fixed several buga.

## [3.7.1] - 2021-10-21

### Added

- Logisim has now an internal font-chooser to comply to the font-values used.

### Fixed

- Several bug fixes.

## [3.7.0] - 2021-10-12
  * Reworked the slider component in the I/O extra library.
  * Tick clock frequency display moved to left corner. It's also bigger and text color is configurable.

### Added

- Added project export feature.
- Added a setting to select lower- or upper-case VHDL keywords.
- Each circuit stores/restores the last board used for Download (handy for templates to give to students).

### Changed

- Cleanup/rework of the HDL-generation.
- Cleaned-up the written .circ file.
- Completely rewritten command line argument parser:
  - All options have both short and long version now,
  - All long arguments require `--` prefix i.e. `--version`,
  - All short arguments require single `-` as prefix i.e. `-v`,
  - `-clearprefs` is now `--clear-prefs`,
  - `-clearprops` option is removed (use `--clear-prefs` instead),
  - `-geom` is now `--geometry`,
  - `-nosplash` is now `--no-splash` or `-ns`,
  - `-sub` is now `--substitute` or `-s`,
  - `-testvector` is now `--test-vector` or `-w`,
  - `-test-fpga-implementation` is now `--test-fpga` or `-f`,
  - `-questa` is removed.

### Fixed

- Fixed bug in PortIO HDL generator and component.
- Fixed startup crash related to incorrectly localized date format.

## [3.6.1] - 2021-09-27

### Fixed

- Fixed bug in LED-array

## [3.6.0] - 2021-09-05

### Added

- Introducing project logo.
- Added new component LED Bar.
- Added option to configure canvas' and grid's colors.
- Added DIP switch state visual feedback for ON state.
- Added predefined quick zoom buttons.
- Added option to configure size of connection pin markers.
- Added "Rotate Left" context menu action.
- Added duplicated component placement on same location refusal.
- Added LED-array support for FPGA-boards.
- Added TTL 74157 and TTL74158: Quad 2-line to1-line selectors.
- Added TTL 74x34 hex buffer gate.
- Added TTL 74x139: dual 2-line to 4-lines decoders.

### Changed

- Made pins' tooltips more descriptive for TTL 74161.
- Augmented direction verbal labels (East, North, etc), with corresponding arrow symbols.
- Application title string now adds app name/version at the very end of the title.
- Canvas Zoom controls new offer wider range of zoom and three level of granularity.
- Project's "Dirty" (unsaved) state is now also reflected by adding `*` marker to the window title.
- Replace DarkLaf with FlatLaf for better compatibility.
- Display "Too few inputs for table" if Karnaugh Map has only 1 input.
- Improved partial placement on FPGA-boards for multi-pin components.
- HexDisplay is stays blank if no valid data is fed instead of showing "H" (#365).
- Combined `Select Location` from Plexers and `Gate Location` from Wiring to one attribute.
  - Breaks backwards comparability for Transistors and Transmission Gates.
      When opening old .circ files, they will have the default `Select Location` ("Bottom/Left").
- Each circuit will now remember, restore, and save:
  - The last tick-frequency used for simulation
  - The last download frequency used
- Tons of code cleanup and internal improvements.

### Fixed

- Fixed missing port on DotMatrix.
- Fixed pin duplication on load in case a custom apearance is used for a circuit.
- Fixed project loader to correctly handle hex values with a 1 in bit 63rd.
- - Fixed several small bugs.

### Removed

- Removed obsolete VHDL-Architecture attribute from circuit.
- Support for `AnimatedIcon` has been completely removed.

## [3.5.0] - 2021-05-25
- Many code-cleanups, bug fixes and again the chronogram.
