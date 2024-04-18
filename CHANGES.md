[![Logisim-evolution](docs/img/logisim-evolution-logo.png)](https://github.com/logisim-evolution/logisim-evolution)

# Changes #

* @dev (????-??-??)
  * Added an autosave feature along with preferences for it.
  * Added a new preference to allow the user to choose the action keys for many functions.
  * Changed RAM default output from error to undefined [#1747]
  * Added support for scanning 7-segment display on FPGA-boards
  * Added first support for the openFpga toolchain for the ecp5 famely
    Note that this is experimental for the moment, so use it at your own risk.
  * Improved Chinese localization
    * Changed language code from `cn` to `zh`.
    * Chinese users (also including those who use other forks of Logisim
      that are using `cn` language code) will be required to manually modify language settings.
  * Fixed select port positioning on Multiplexer to be more consistent in some cases [#1734]
  * Fixed appearance of LSe desktop icon [#1662]
  * Update controlled buffer behavior to pass U and E inputs while enabled [#1642]
  * Introduced user-defined color for components.
  * Made component icons more uniform.
  * Added architecture designation to macOS build.
  * Fixed Karnaugh map color index bug.
  * Attribute sheet now honors application color theme.
  * Attribute sheet now displays HEX value of color properties.
  * Added TTL 7487: 4-bit True/complement, zero/one elements
  * Fixed Wrong HDL generation bug in the PortIO component and added the single bit version.
  * Added TTL 74151: 8-line to 1 line data selector
  * Added TTL 74153: dual 4-line to 1 line data selector
  * Added TTL 74181: arithmetic logic unit
  * Added TTL 74182: look-ahead carry generator
  * Added TTL 74299: 8-bit universal shift register with three-state outputs
  * Added TTL 74381: arithmetic logic unit
  * Added TTL 74541: Octal buffers with three-state outputs
  * Added TTL 74670: 4-by-4 register file with three-state outputs
  * Added 16 bit floating point support for floating point arithmetic
  * Added partial support for VHDL time units
  * Fixed the problem of keys getting assigned to focusing on the cell of the table in "properties" section along with its actual intent

* v3.8.0 (2022-10-02)
  * Added reset value attribute to input pins
  * Fixed boolean algebra minimal form bug
  * Fixed random fill Rom bug
  * Added TTL 74164, 74192 and 74193.
  * Fixed off grid components bug that could lead to OutOfMemory error.
  * Removed autolabler for tunnels, such that all get the same label in case of renaming.
  * Fixed bug preventing TTL 7442, 7443 and 7444 from being placed on the circuit canvas.
  * Sub-circuit can now be deleted with `DELETE` key, along with `BACKSPACE` used so far.
  * Fixed `Simulate` -> `Timing Diagram` not opening when using "Nimbus" look and feel.
  * Fixed pressing `CTRL`+`0` selecting the wrong element in the toolbar.
  * Fixed TTL 7485 `7485HdlGenerator` generating wrong HDL type.
  * Fixed TTL 74139, 7447 outputting inverted logic
  * Fixed TTL 74175, CLR inverted
  * Fixed TTL 7436 pin arrangement
  * Added TTL 74138: 3-line to 8-line decoder
  * Added TTL 74240, 74241, 74244: octal buffers with three-state outputs.
  * Added TTL 74245: octal bus transceivers with three-state outputs.
  * Moved TTL 74266 to 747266, correctly reimplemented 74266 with open-collector outputs.
  * Fixed TTL 74165, correct order of inputs, load asynchronously
  * Added TTL 74166: 8-bit parallel-to-serial shift register with clear
  * Removed fixed LM_Licence setting

* v3.7.2 (2021-11-09)
  * Fixed Preferences/Window "Reset window layout to defaults" not doing much.
  * Fixed Gradle builder failing to compile LSe if sources were not checked out from Git.
  * You can now swap the placement of main canvas and component tree/properties pane.
  * Several bug fixes.

* v3.7.1 (2021-10-21)
  * Logisim has now an internal font-chooser to comply to the font-values used.
  * Several bug fixes.

* v3.7.0 (2021-10-12)
  * Reworked the slider component in the I/O extra library.
  * Tick clock frequency display moved to left corner. It's also bigger and text color is configurable.
  * Completely rewritten command line argument parser:
    * All options have both short and long version now,
    * All long arguments require `--` prefix i.e. `--version`,
    * All short arguments require single `-` as prefix i.e. `-v`,
    * `-clearprefs` is now `--clear-prefs`,
    * `-clearprops` option is removed (use `--clear-prefs` instead),
    * `-geom` is now `--geometry`,
    * `-nosplash` is now `--no-splash` or `-ns`,
    * `-sub` is now `--substitute` or `-s`,
    * `-testvector` is now `--test-vector` or `-w`,
    * `-test-fpga-implementation` is now `--test-fpga` or `-f`,
    * `-questa` is removed.
  * PortIO HDL generator and component bug-fixed.
  * Cleanup/rework of the HDL-generation.
  * Each circuit stores/restores the last board used for Download (handy for templates to give to students)
  * Fixed startup crash related to incorrectly localized date format.
  * Added a setting to select lower- or upper-case VHDL keywords.
  * Added project export feature.
  * Cleaned-up the written .circ file.

* v3.6.1 (2021-09-27)
  * Fixed bug in LED-array

* v3.6.0 (2021-09-05)
  * Introducing project logo.
  * Fixed project loader to correctly handle hex values with a 1 in bit 63rd.
  * Added TTL74x34 hex buffer gate.
  * Made pins' tooltips more descriptive for 74161.
  * Added new component LED Bar.
  * Added 74157 and 74158: Quad 2-line to1-line selectors.
  * Added option to configure canvas' and grid's colors.
  * Added DIP switch state visual feedback for ON state.
  * Augmented direction verbal labels (East, North, etc), with corresponding arrow symbols.
  * Application title string now adds app name/version at the very end of the title.
  * Added option to configure size of connection pin markers.
  * Added TTL 74x139: dual 2-line to 4-lines decoders.
  * Fixed missing port on DotMatrix.
  * Combined `Select Location` from Plexers and `Gate Location` from Wiring to one attribute.
    * Breaks backwards comparability for Transistors and Transmission Gates.
      When opening old .circ files, they will have the default `Select Location` ("Bottom/Left").
  * Replace DarkLaf with FlatLaf for better compatibility.
  * Adds "Rotate Left" context menu action.
  * Display "Too few inputs for table" if Karnaugh Map has only 1 input.
  * HexDisplay is stays blank if no valid data is fed instead of showing "H" [#365].
  * Project's "Dirty" (unsaved) state is now also reflected by adding `*` marker to the window title.
  * Support for `AnimatedIcon` has been completely removed.
  * Canvas Zoom controls new offer wider range of zoom and three level of granularity.
  * Added predefined quick zoom buttons.
  * Tons of code cleanup and internal improvements.
  * Added duplicated component placement on same location refusal
  * Fixed pin duplication on load in case a custom apearance is used for a circuit
  * Added LED-array support for FPGA-boards
  * Improved partial placement on FPGA-boards for multi-pin components
  * Fixed several small bugs
  * Each circuit will now remember, restore, and save:
    * The last tick-frequency used for simulation
    * The last download frequency used
  * Removed obsolete VHDL-Architecture attribute from circuit

* v3.5.0 (2021-05-25)
  * Many code-cleanups, bug fixes and again the chronogram.
