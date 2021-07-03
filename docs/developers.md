[![Logisim-evolution](../artwork/logisim-evolution-logo.svg)](https://github.com/logisim-evolution/logisim-evolution)

---

# Developers #

* [« Go back](../README.md)
* **Developer's corner**
  * [Requirements](#requirements)
  * [Gradle](#gradle)
  * [Import project into IDE](#import-project-into-ide)
    * [Eclipse](#import-project-into-ide)
    * [InteliJ IDEA](#import-project-into-ide)
  * [Building from sources](#building-from-sources)
  * [Testing development code](#testing-development-code)
  * [Code style](style.md)
  * [How to contribute](#how-to-contribute)

---

## Requirements ##

`Logisim-evolution` is written in Java 11, so to build it from sources you need JDK 11 (or equivalent, i.e. OpenJDK). To build
Linux packages, JDK 14+ is required. We recommend using JDK 16 for your development works with `Logisim-evolution`.

**NOTE:** ensure your `$JAVA_HOME` enviromental variable points proper JDK version.

## Gradle ##

`Logisim-evolution` uses [Gradle](https://gradle.org) as build system, which means it can be easily imported into most modern IDEs
that supports it, incl. [Eclipse](https://www.eclipse.org) and [IntelliJ IDEA](https://www.jetbrains.com/idea/).

## Import project into IDE ##

How to import Gradle project:

* [How to import Gradle project into Eclipse](https://www.eclipse.org/community/eclipse_newsletter/2018/february/buildship.php),
* [How to import Gradle project into IntelliJ IDEA](https://www.jetbrains.com/help/idea/gradle.html) (section "Importing a project
  from a Gradle model").

## Building from sources ##

To build and run `Logisim-evolution` application invoke `Gradle` build system and pass specified task name as argument.
`Logisim-evolution` comes with Gradle wrapper script, which can be invoked as `./gradlew <ARGS>` on Linux or macOS, and
`gradlew <ARGS>` on Windows.

To build and run on Linux and macOS:

```bash
./gradlew run
```

or on Windows:

```bash
gradlew run
```

If you wish to create a Java JAR package, which can then be run without [Gradle](https://gradle.org), execute:

```bash
./gradlew shadowJar
```

which will create `logisim-evolution-<version>-all.jar` in `build/libs/` folder. To use this package
you need any Java runtime environments (JRE or JDK) v11 or newer, and then type:

```bash
java -jar logisim-evolution-<version>-all.jar
```

You can also generate a platform-specific installer, which gets saved in `build/dist`. This feature requires using OpenJDK 14 or
newer. Packages can be built by running `jpackage` task:

```bash
./gradlew jpackage
```

> **NOTE:** `jpackage` creates the installer for the platform that builds it. Building cross-platform installers is not supported
> by Java's `jpackage` utility. You may also need to install additional developer tools for the platform in order to build the
> installer. See Java's [jpackage documentation](https://docs.oracle.com/en/java/javase/14/jpackage/packaging-overview.html)
> for more details of tool requirements.

To see all available tasks `./gradlew tasks --all`

## Testing development code ##

`Logisim-evolution` is often updated, the [branch `develop`](https://github.com/logisim-evolution/logisim-evolution/tree/develop)
is the place where all the works on next release happen. Once the code reach the point it's ready for next public release, it will
be merged into [`master` branch](https://github.com/logisim-evolution/logisim-evolution/tree/master) and released. But if you want
to contribute, or even just see what we are currently working on just checkout the `develop` and build `Logisim-evolution` from
source as described above.

**If you see any issues or got improvement ideas, please [create a ticket](https://github.com/logisim-evolution/logisim-evolution/issues)
to make `Logisim-evolution` better!**

---

## How to contribute ##

If you want to contribute to Logisim-evolution, this is how to do it:

* Make a local *fork* of `Logisim-evolution` by clicking the *Fork* button on [project GitHub page](https://github.com/logisim-evolution/logisim-evolution).
* Fix the bugs you want to fix on your local fork.
* Add the features you want to add on your local fork.
* Add/modify the documentation/language support on your local fork.

Once it is running without bugs on your local fork request a *Pull request* by:

* Go to the *Pull request*-tab and click the button *New pull request*.
* Click on *compare across forks*.
* On the right hand side select your fork, for example: *head repository: BFH-ktt1/logisim-evolution*
* On the right hand side select your branch, for example: *base: bugfixes*
* On the left hand side select the development branch *base: develop* (**Important:** All pull requests must be on
  the [branch `develop`](https://github.com/logisim-evolution/logisim-evolution/tree/develop) as
  the [branch `master`](https://github.com/logisim-evolution/logisim-evolution/tree/master) only holds the code of the latest stable
  release!)
* Make sure that there are no conflicts reported.
