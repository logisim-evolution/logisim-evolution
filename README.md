logisim-evolution
=================

Branch master : [![Build Status](https://travis-ci.org/reds-heig/logisim-evolution.svg?branch=master)](https://travis-ci.org/reds-heig/logisim-evolution)

Logisim is an educational tool for designing and simulating digital logic circuits.
It was originally created by [Dr. Carl Burch](http://www.cburch.com/logisim/) and actively developed until 2011.
After this date the author focused on other projects, and recently the development has been officially stopped  [(see his message here)](http://www.cburch.com/logisim/retire-note.html).

In the meantime, people from a group of swiss institutes ([Haute École Spécialisée Bernoise](http://www.bfh.ch), [Haute École du paysage, d'ingénierie et d'architecture de Genève](http://hepia.hesge.ch), and [Haute École d'Ingénierie et de Gestion du Canton de Vaud](http://www.heig-vd.ch)) started developing a version of Logisim that fitted their courses, integrating several tools -- for instance a chronogram, the possibility to test the schematics directly on an electronic board, TCL/TK consoles, ...

We have decided to release this new Logisim version under the name logisim-evolution, to highlight the large number of changes made during these years, and **we actively seek the contribution of the community**.

## Languages

Logisim supports many languages. Many of them are automatically translated by deepl. If you detect bizarre translations please do not hesitate to correct them in the corresponding property files and make a pull-request.

## What's new in logisim-evolution

* chronogram -- to see the evolution of signals in your circuit
* electronic board integration -- schematics can now be simulated on real hardware!
* board editor -- to add new electronic boards
* VHDL component -- a new component type whose behavior is specified in VHDL
* TCL/TK console -- interfaces between the circuit and the user
* DIP switches
* RGB LEDs
* large number of bug-fixes
* GUI improvements
* automatic updates
* code refactoring
* ...

## Running logisim-evolution

You can find compiled versions of the code [here](https://github.com/reds-heig/logisim-evolution/releases).
Starting from V3.4.1, the following compiled versions are available:
* logisim-evolution_`<version>`-1_amd64.deb  : Self contained debian installer (also ubuntu).
* logisim-evolution_`<version>`-1.x86_64.rpm : Self contained Redhat installer.
* logisim-evolution_`<version>`.dmg          : Self contained Mac OsX installer.
* logisim-evolution_`<version>`.msi          : Self contained Windows installer.

If you want to have the latest development version you can build/run it by cloning the repository on your local machine and ensuring that at least [OpenJDK](https://adoptopenjdk.net/) 9 is installed.
Once this is done, enter the directory and execute:
```bash
./gradlew run
```
or on windows:
```bash
gradlew run
```

If you wish to create a distribution which can then be run without gradle, execute:
```bash
./gradlew shadowJar
```
or on windows:
```bash
gradlew shadowJar
```
which will create a new jar file in `build/libs` called `logisim-evolution-<version>-all.jar` that you can distribute freely.

On macOS, you can build a native app bundle `Logisim-evolution.app` using:
```bash
./gradlew createApp
```
which you will find afterwards in the folder `build/macApp/`. This has the advantage that `.circ` files are automatically associated with the `Logisim-evolution.app` so that you can open them directly in the Finder. *Note:* Curently, the app needs a separately installed compatible JDK/JRE to execute. You may also build a DMG image `logisim-evolution-<version>.dmg` containing `Logisim-evolution.app` for distribution:
```
./gradlew createDmg
```
which you will find in the folder `build/distribution`.

For all platforms you can now generate a platform specific installer, saved in `build/dist`, by running (you need at least V14.0 of a Java JRE):
```bash
./gradlew jpackage
```

or on windows:
```
gradlew jpackage
```

## Testing logisim-evolution

As logisim-evolution needs updates (new features and patches) and currently lacks unit tests, the *development* branch was created.
The goal of this branch is to add new features/patches without affecting the release on branch master.
Users who are willing to test new features should checkout the development branch. Feedback from users is really appreciated as it makes logisim-evolution better. Feel free to use the issue tab to report bugs and suggest features.

## Contributing to logisim-evolution

If you want to contribute to logisim-evolution, this is how to do it:
* Make a local *fork* of logisim-evolution by clicking the *Fork* button.
* Fix the bugs you want to fix on your local fork.
* Add the features you want to add on your local fork.
* Add/modify the documentation/language support on your local fork.

Once it is running without bugs on your local fork request a *Pull request* by:
* Go to the *Pull request*-tab and click the button *New pull request*
* Click on *compare across forks*
* On the right hand side select your fork, for example: *head repository: BFH-ktt1/logisim-evolution*
* On the right hand side select your branch, for example: *base: bugfixes*
* On the left hand side select the development branch *base : develop* (important: all push request must be on the develop-branch as the master branch only holds the code of the latest release).
* Make sure that there are no conflicts reported.

## Code style
All logisim java files have been converted using google-java-format. If you are using eclipse there is a plugin available to enforce this standard. More information on the google java format can be found [here](https://github.com/google/google-java-format). At the moment version 1.6 is used.

## Documentation

[Here](http://reds-data.heig-vd.ch/logisim-evolution/IntroToLogisimEnglish.pdf) you can find a tutorial (French version [here](http://reds-data.heig-vd.ch/logisim-evolution/tutoLogisim.pdf)) that explains some basic usage of Logisim. The electronic card referenced in the tutorial is a small card we use in our laboratories -- you won't be able to buy it in a store -- but the descriptions should be good enough to be used for another generic board.

Another good reference is [this book](https://github.com/grself/CIS221_Text/raw/master/dl.pdf), the accompanying [lab manual](https://github.com/grself/CIS221_Lab_Manual/raw/master/dl_lab.pdf), and [YouTube channel](http://bit.ly/2KLMcoc), where basic electronics is explained with the help of Logisim.


## Development

Logisim-evolution uses gradle for project management which means it can be easily imported into most modern IDEs.

Instructions on how to import a gradle project into Eclipse can be found [here](https://www.eclipse.org/community/eclipse_newsletter/2018/february/buildship.php).

Instructions on how to import a gradle project into IntelliJ IDEA can be found [here](https://www.jetbrains.com/help/idea/gradle.html) under "Importing a project from a Gradle model".


## Retro-compatibility

We cannot guarantee backwards compatibility of logisim-evolution with files created by the original Logisim.
We have incorporated a parser that alters the name of the components to satisfy VHDL requirements for variable names,
but components have evolved in shape since then (e.g. RAM and counters).
You might need to rework your circuits a bit when opening them with logisim-evolution -- but the changes will be stored
in the new format, therefore you have to do your work only once.


## Wish-list

Logisim-evolution is continuously-growing software, and we have several ideas we would like to implement. In particular, we would like to have

* unit tests for the code
* extensive documentation
* test circuits
* ...

If you are willing to contribute to any of these, please feel free to contact us!


## How to get support for logisim-evolution

Unfortunately, we do not have enough resources to provide direct support for logisim-evolution.
We will, however, try to deal with the raised issues in a *best-effort* way.

If you find a bug or have an idea for an interesting feature, please do not hesitate to open a ticket!


## License

The code is licensed under [GPLv3](https://www.gnu.org/licenses/gpl-3.0.en.html).


## Credits

The following institutions/people actively contributed to logisim-evolution:

* Carl Burch - Hendrix College - USA
* Kevin Walsh [College of the Holy Cross](http://www.holycross.edu) - USA
* [Haute École Spécialisée Bernoise](http://www.bfh.ch) - Switzerland
* [Haute École du paysage, d'ingénierie et d'architecture de Genève](http://hepia.hesge.ch) - Switzerland
* [Haute École d'Ingénierie et de Gestion du Canton de Vaud](http://www.heig-vd.ch) - Switzerland
* Theldo Cruz Franqueira - Pontifícia Universidade Católica de Minas Gerais - Brasil
* Moshe Berman - Brooklyn College

If you feel that your name should be in this list, please feel free to send us a [mail](mailto:ktt1@bfh.ch)!


## Other Logisim forks available on the net

* [Logisim holycross](https://github.com/kevinawalsh/logisim-evolution) - a branch from logisim evolution(2.13.14) with several great enhancements made by Kevin Walsh. Currently there is an effort ongoing to merge these features in evolution.
* [Logisim by Joseph Lawrance et al.](https://github.com/lawrancej/logisim) - they have started from Burch's original code and integrated it in several open-source development frameworks, cleaning up the code. We have taken a few code cleanups and the redo functionality from their code.
* [logisim-iitd](https://code.google.com/p/logisim-iitd) - IIT Delhi version of Logisim, it integrates the floating-point components within the arithmetic unit.
* [Logisim for the CS3410 course, Cornell University](http://www.cs.cornell.edu/courses/cs3410/2015sp/) - they have a very interesting test vector feature, that was only recently integrated into logisim-evolution.


## Alternatives

* A complete rewriting of Logisim, called [Digital](https://github.com/hneemann/Digital), has been developed by Prof. Helmut Neemann of the Baden-Württemberg Cooperative State University Mosbach.
