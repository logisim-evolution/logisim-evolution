logisim-evolution
=================

Branch [`master`](https://github.com/reds-heig/logisim-evolution/tree/master): [![Build Status](https://travis-ci.org/reds-heig/logisim-evolution.svg?branch=master)](https://travis-ci.org/reds-heig/logisim-evolution)

Branch [`develop`](https://github.com/reds-heig/logisim-evolution/tree/develop): [![Build Status](https://travis-ci.org/reds-heig/logisim-evolution.svg?branch=develop)](https://travis-ci.org/reds-heig/logisim-evolution)

Logisim is an educational tool for designing and simulating digital logic circuits. It was originally created by [Dr. Carl Burch](http://www.cburch.com/logisim/) and actively developed until 2011. After this date, the author focused on other projects and the development has been [officially stopped](http://www.cburch.com/logisim/retire-note.html).

In the meantime, people from a group of Swiss higher education institutions ([Haute École Spécialisée Bernoise](http://www.bfh.ch), [Haute École du paysage, d'ingénierie et d'architecture de Genève](http://hepia.hesge.ch), and [Haute École d'Ingénierie et de Gestion du Canton de Vaud](http://www.heig-vd.ch)) started developing a version of Logisim that fitted their courses by integrating several new tools, e.g., a chronogram, the possibility to test the schematics directly on an electronic board, TCL/TK consoles, …

We have decided to release this new Logisim version under the name logisim-evolution to highlight the large number of changes that were made.

**We actively seek the contribution of the community!**


## Languages

Logisim supports many languages. Many of them were automatically translated using [DeepL](https://www.deepl.com/). If you should find bizarre translations, please do not hesitate to correct them in the corresponding property files and to make a pull request!


## What's new in logisim-evolution

* Chronogram -- to see the evolution of signals in your circuit
* Electronic board integration -- schematics can now be simulated on real hardware!
* Board editor -- to add new electronic boards
* VHDL component -- a new component type whose behavior is specified in VHDL
* TCL/TK console -- interfaces between the circuit and the user
* DIP switches
* RGB LEDs
* Large number of bug fixes
* GUI improvements
* Automatic updates
* Code refactoring
* …


## Running logisim-evolution

You can find compiled versions of the code [here](https://github.com/reds-heig/logisim-evolution/releases). Starting with version 3.4.1, the following compiled versions are [available](https://github.com/reds-heig/logisim-evolution/releases):

* `logisim-evolution_<version>-1_amd64.deb`: Self-contained package for Debian-based Linux distributions (e.g., also Ubuntu).
* `logisim-evolution_<version>-1.x86_64.rpm`: Self-contained package for Fedora/Redhat/CentOS/SuSE Linux distributions.
* `Logisim-evolution_<version>.dmg`: Self-contained package for macOS. Note that Logisim-evolution may be also installed using [MacPorts](https://www.macports.org/) using `sudo port install logisim-evolution` or [Homebrew](https://brew.sh/) using `brew install --cask logisim-evolution`.
* `logisim-evolution_<version>.msi`: Self-contained installer for MS Windows.

If you want to have the latest development version, you can build/run it by cloning the repository on your local machine and making sure that at least [OpenJDK](https://adoptopenjdk.net/) 11 is installed. Once this is done, enter the directory and execute:

```bash
./gradlew run
```
or on Windows:

```bash
gradlew run
```

If you wish to create a package, which can then be run without [Gradle](https://gradle.org), execute:

```bash
./gradlew shadowJar
```

or on Windows:

```bash
gradlew shadowJar
```

which will create a new JAR file in `build/libs` called `logisim-evolution-<version>-all.jar` that you can distribute freely and execute with any recent enough Java runtime environment using, e.g.:

```bash
java -jar logisim-evolution-<version>-all.jar
```

For all platforms, you can now generate a platform-specific installer, which gets saved in `build/dist`, by using OpenJDK 14 or later and running:

```bash
./gradlew jpackage
```

or on Windows:

```
gradlew jpackage
```

Note that `jpackage` creates the installer for the platform that builds it.
Building cross-platform installers is not supported by Java's `jpackage` facility. You may need to install additional developer tools for the platform in order to build the installer. See Java's [jpackage documentation](https://docs.oracle.com/en/java/javase/14/jpackage/packaging-overview.html) for more details of tool requirements.


## Testing logisim-evolution

As logisim-evolution is often updated, the [branch `develop`](https://github.com/reds-heig/logisim-evolution/tree/develop), was created. The goal of this branch is to add new features/patches without affecting the stable release on [branch `master`](https://github.com/reds-heig/logisim-evolution/tree/master). Users who are willing to test new features should checkout the [branch `develop`](https://github.com/reds-heig/logisim-evolution/tree/develop) and build logisim-evolution from source as described above. Feedback from users is really appreciated, as it makes logisim-evolution better.

**Feel free to use the [Issues tab](https://github.com/reds-heig/logisim-evolution/issues) to report bugs and suggest features!**


## Contributing to logisim-evolution

If you want to contribute to logisim-evolution, this is how to do it:

* Make a local *fork* of logisim-evolution by clicking the *Fork* button.
* Fix the bugs you want to fix on your local fork.
* Add the features you want to add on your local fork.
* Add/modify the documentation/language support on your local fork.

Once it is running without bugs on your local fork request a *Pull request* by:

* Go to the *Pull request*-tab and click the button *New pull request*.
* Click on *compare across forks*.
* On the right hand side select your fork, for example: *head repository: BFH-ktt1/logisim-evolution*
* On the right hand side select your branch, for example: *base: bugfixes*
* On the left hand side select the development branch *base: develop* (**Important:** All pull requests must be on the [branch `develop`](https://github.com/reds-heig/logisim-evolution/tree/develop) as the [branch `master`](https://github.com/reds-heig/logisim-evolution/tree/master) only holds the code of the latest stable release!)
* Make sure that there are no conflicts reported.


## Code style

All of Logisim's Java files have been formatted using [`google-java-format`](https://github.com/google/google-java-format). If you are using [Eclipse](https://www.eclipse.org/), there is a [plugin](https://github.com/google/google-java-format#eclipse) available to enforce this formatting. At the moment, version 1.6 of the plugin is used.


## Documentation

[Here](http://reds-data.heig-vd.ch/logisim-evolution/IntroToLogisimEnglish.pdf) you can find a tutorial (French version [here](http://reds-data.heig-vd.ch/logisim-evolution/tutoLogisim.pdf)) that explains some basic usage of Logisim. The electronic card referenced in the tutorial is a small card we use in our laboratories -- you won't be able to buy it in a store -- but the descriptions should be good enough to be used for another generic board.

Another good reference is [this book](https://github.com/grself/CIS221_Text/raw/master/dl.pdf), the accompanying [lab manual](https://github.com/grself/CIS221_Lab_Manual/raw/master/dl_lab.pdf), and [YouTube channel](http://bit.ly/2KLMcoc), where basic electronics is explained with the help of Logisim.


## Development

Logisim-evolution uses [Gradle](https://gradle.org) for project management, which means it can be easily imported into most modern IDEs.

Instructions on how to import a Gradle project into [Eclipse](https://www.eclipse.org) can be found [here](https://www.eclipse.org/community/eclipse_newsletter/2018/february/buildship.php).

Instructions on how to import a Gradle project into [IntelliJ IDEA](https://www.jetbrains.com/idea/) can be found [here](https://www.jetbrains.com/help/idea/gradle.html) under "Importing a project from a Gradle model".


## Backward compatibility

We cannot guarantee backward compatibility of logisim-evolution with files created by the original Logisim. We have incorporated a parser that alters the name of the components to satisfy VHDL requirements for variable names, but components have evolved in shape since the original Logisim (e.g. RAM and counters). You might need to rework your circuits a bit when opening them with logisim-evolution, but the changes will be stored in the new format. Therefore, you have to do that work only once.


## Wish list

Logisim-evolution is continuously growing and we have several ideas, which we would like to implement. In particular, we would like to have

* Unit tests for the code
* More extensive documentation
* Test circuits
* …

**If you are willing to contribute to any of these, please feel free to contact us!**


## How to get support for logisim-evolution

Unfortunately, we do not have enough resources to provide direct support for logisim-evolution. However, we will try to deal with the raised issues in a *best-effort* manner.

**If you find a bug or have an idea for an interesting feature, please do not hesitate to [open a ticket](https://github.com/reds-heig/logisim-evolution/issues)!**


## License

The code is licensed under [GPLv3](https://www.gnu.org/licenses/gpl-3.0.en.html).


## Credits

The following institutions/people actively contributed to logisim-evolution:

* Carl Burch - Hendrix College - USA
* Kevin Walsh - [College of the Holy Cross](http://www.holycross.edu) - USA
* [Haute École Spécialisée Bernoise](http://www.bfh.ch) - Switzerland
* [Haute École du paysage, d'ingénierie et d'architecture de Genève](http://hepia.hesge.ch) - Switzerland
* [Haute École d'Ingénierie et de Gestion du Canton de Vaud](http://www.heig-vd.ch) - Switzerland
* Theldo Cruz Franqueira - Pontifícia Universidade Católica de Minas Gerais - Brazil
* Moshe Berman - Brooklyn College - USA

If you feel that your name should be in this list, please feel free to send us a [mail](mailto:ktt1@bfh.ch)!


## Other Logisim forks available on the net

* [Logisim-Evolution (Holy Cross Edition)](https://github.com/kevinawalsh/logisim-evolution) - a branch from logisim-evolution(2.13.14) with several great enhancements made by Kevin Walsh. Currently, there is an ongoing effort to merge these features back into logisim-evolution.
* [Logisim by Joseph Lawrance et al.](https://github.com/lawrancej/logisim) - they have started from Burch's original code and integrated it in several open-source development frameworks, cleaning up the code. We have taken a few code cleanups and the redo functionality from their code.
* [logisim-iitd](https://code.google.com/p/logisim-iitd) - IIT Delhi version of Logisim, it integrates the floating-point components within the arithmetic unit.
* [Logisim for the CS3410 course, Cornell University](http://www.cs.cornell.edu/courses/cs3410/2015sp/) - they have a very interesting test vector feature, that was only recently integrated into logisim-evolution.


## Alternatives

* A complete rewriting of Logisim, called [Digital](https://github.com/hneemann/Digital), has been developed by Prof. Helmut Neemann of the Baden-Württemberg Cooperative State University Mosbach.
