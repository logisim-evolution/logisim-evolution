[![Logisim-evolution](src/main/resources/resources/logisim/img/logisim-evolution-logo.svg)](https://github.com/logisim-evolution/logisim-evolution)

---

Branch [master](https://github.com/logisim-evolution/logisim-evolution/tree/master): [![Build](https://github.com/logisim-evolution/logisim-evolution/actions/workflows/gradle.yml/badge.svg?branch=master)](https://github.com/logisim-evolution/logisim-evolution/actions/workflows/gradle.yml)

Branch [develop](https://github.com/logisim-evolution/logisim-evolution/tree/develop): [![Java CI with Gradle](https://github.com/logisim-evolution/logisim-evolution/actions/workflows/gradle.yml/badge.svg?branch=develop)](https://github.com/logisim-evolution/logisim-evolution/actions/workflows/gradle.yml)

---

# Logisim-evolution #

* **Table of contents**
  * [Features](#features)
  * [Requirements](#requirements)
  * **[Downloads](#download)**
  * [Pictures of Logisim-evolution](docs/pics.md)
  * [Documentation](docs/docs.md)
  * [Bugs reports & feature requests](https://github.com/logisim-evolution/logisim-evolution/issues)
  * [For developers](docs/developers.md)
  * [How to contribute](docs/developers.md#how-to-contribute)
  * [Credits](docs/credits.md)

---

## Features ##

`Logisim-evolution` is a completely free and [open-source](https://github.com/logisim-evolution) educational, cross-platform
software for designing and simulating digital logic circuits.

Project highlights:

* easy to use circuit designer,
* logic circuit simulations,
* chronogram (to see the evolution of signals in your circuit),
* electronic board integration - schematics can now be simulated on real hardware,
* VHDL components (components whose behavior is specified in VHDL!),
* TCL/TK console (interfaces between the circuit and the user),
* huge library of components (LEDs, TTLs, switches, SoCs),
* Supports [multiple languages](docs/docs.md#translations),
* and more!

[![Logisim-evolution](docs/img/logisim-evolution-01-small.webp)](docs/pics.md)
[![Logisim-evolution](docs/img/logisim-evolution-02-small.webp)](docs/pics.md)

---

## Requirements ##

`Logisim-evolution` is Java application, therefore it can run on any operating system supporting Java runtime enviroment. It
requires [Java 11 (or newer)](https://www.oracle.com/java/technologies/javase-downloads.html).

## Download ###

`Logisim-evolution` is available
for [download in binary form, with ready to use installable packages for Windows, macOS and Linux]((https://github.com/logisim-evolution/logisim-evolution/releases))
or in source code form, which you can [build yourself](docs/developers.md). You can find compiled versions of the code
[here](https://github.com/logisim-evolution/logisim-evolution/releases).

The following binary versions are [available](https://github.com/logisim-evolution/logisim-evolution/releases):

* `logisim-evolution_<version>-1_amd64.deb`: Debian package (also suitable for Ubuntu and derrivatives),
* `logisim-evolution_<version>-1.x86_64.rpm`: Package for Fedora/Redhat/CentOS/SuSE Linux distributions,
* `logisim-evolution_<version>.msi`: installler package for Microsoft Windows.
* `logisim-evolution_<version>.dmg`: macOS package. Note that `Logisim-evolution` may be also installed
  using [MacPorts](https://www.macports.org/) (by typing `sudo port install logisim-evolution`)
  or via [Homebrew](https://brew.sh/) (by typing `brew install --cask logisim-evolution`).

---

## License ##

* `Logisim-evolution` is copyrighted ©2021 by Logisim-evolution [developers](docs/credits.md).
* This is free software licensed under [GNU General Public License v3](https://www.gnu.org/licenses/gpl-3.0.en.html).
