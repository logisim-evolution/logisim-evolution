[![Logisim-evolution](artwork/logisim-evolution-logo.svg)](https://github.com/logisim-evolution/logisim-evolution)

---

Branch [master](https://github.com/logisim-evolution/logisim-evolution/tree/master): [![Build](https://github.com/logisim-evolution/logisim-evolution/actions/workflows/gradle.yml/badge.svg?branch=master)](https://github.com/logisim-evolution/logisim-evolution/actions/workflows/gradle.yml)
![Code style](https://github.com/logisim-evolution/logisim-evolution/actions/workflows/checkstyle.yml/badge.svg?branch=master)
![Markdown Lint](https://github.com/logisim-evolution/logisim-evolution/actions/workflows/markdown.yml/badge.svg?branch=master)

Branch [develop](https://github.com/logisim-evolution/logisim-evolution/tree/develop): [![Java CI with Gradle](https://github.com/logisim-evolution/logisim-evolution/actions/workflows/gradle.yml/badge.svg?branch=develop)](https://github.com/logisim-evolution/logisim-evolution/actions/workflows/gradle.yml)
![Code style](https://github.com/logisim-evolution/logisim-evolution/actions/workflows/checkstyle.yml/badge.svg?branch=develop)
![Markdown Lint](https://github.com/logisim-evolution/logisim-evolution/actions/workflows/markdown.yml/badge.svg?branch=develop)

---

# Logisim-evolution #

* **Table of contents**
  * [Features](#features)
  * [Requirements](#requirements)
  * **[Downloads](#download)**
  * [Pictures of Logisim-evolution](docs/pics.md)
  * [More Information](docs/docs.md)
  * [Bugs reports & feature requests](https://github.com/logisim-evolution/logisim-evolution/issues)
  * [For developers](docs/developers.md)
  * [How to contribute](docs/developers.md#how-to-contribute)
  * [Credits](docs/credits.md)

---

## Features ##

`Logisim-evolution` is educational software for designing and simulating digital logic circuits.
`Logisim-evolution` is [free](#license), [open-source](https://github.com/logisim-evolution), and [cross-platform](#requirements).

Project highlights:

* easy to use circuit designer,
* logic circuit simulations,
* chronogram (to see the evolution of signals in your circuit),
* electronic board integration (schematics can be simulated on real hardware),
* VHDL components (components behavior can be specified in VHDL!),
* TCL/TK console (interfaces between the circuit and the user),
* huge library of components (LEDs, TTLs, switches, SoCs),
* supports [multiple languages](docs/docs.md#translations),
* and more!

[![Logisim-evolution](docs/img/logisim-evolution-01-small.png)](docs/pics.md)
[![Logisim-evolution](docs/img/logisim-evolution-02-small.png)](docs/pics.md)

---

## Requirements ##

`Logisim-evolution` is a Java application; therefore, it can run on any operating system supporting the Java runtime enviroment.
It requires [Java 11 (or newer)](https://www.oracle.com/java/technologies/javase-downloads.html).

## Download ###

`Logisim-evolution` is available for
[download in compiled form](https://github.com/logisim-evolution/logisim-evolution/releases)
with ready to use installable packages for Windows, macOS, and Linux
or in [source code form](https://github.com/logisim-evolution), which you can [build yourself](docs/developers.md).

The following [platform specific packages](https://github.com/logisim-evolution/logisim-evolution/releases)
include the Java runtime and do not require it to be installed separately:

* `logisim-evolution_<version>-1_amd64.deb`: Debian package (also suitable for Ubuntu and derivatives),
* `logisim-evolution-<version>-1.x86_64.rpm`: Package for Fedora/Redhat/CentOS/SuSE Linux distributions,
* `logisim-evolution-<version>.msi`: Installer package for Microsoft Windows,
* `Logisim-evolution-<version>.dmg`: macOS package. Note that `Logisim-evolution` may also be installed
  using [MacPorts](https://www.macports.org/) (by typing `sudo port install logisim-evolution`)
  or via [Homebrew](https://brew.sh/) (by typing `brew install --cask logisim-evolution`).

The Java JAR [`logisim-evolution-<version>-all.jar`](https://github.com/logisim-evolution/logisim-evolution/releases)
is also available and can be run on any system with a Java 11 or newer runtime installed.

---

## License ##

* `Logisim-evolution` is copyrighted ©2001-2021 by Logisim-evolution [developers](docs/credits.md).
* This is free software licensed under [GNU General Public License v3](https://www.gnu.org/licenses/gpl-3.0.en.html).
