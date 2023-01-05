[![Logisim-evolution](img/logisim-evolution-logo.png)](https://github.com/logisim-evolution/logisim-evolution)

---

* [« Go back](developers.md)
* **Source code quality**
  * [Coding style](#coding-style)
  * [Checking code style with IntelliJ IDEA](#checking-code-style-with-intellij-idea)
    * [Editing built-in config](#editing-built-in-config)
    * [Using cloned config](#using-cloned-config)
  * [Using Gradle plugin](#using-gradle-plugin)
  * [Using `pre-commit`](#using-pre-commit-hooks)
    * [Disabling hooks for a commit](#disabling-hooks-for-a-commit)
    * [Updating hooks](#updating-hooks)

---

# Coding style #

`Logisim-evolution` uses `Google Java Style` code style as provided with the `Checkstyle`
static analyzer tool, with a few checks disabled as specified in the `checkstyle-suppressions.xml`
config file located in project root directory (from where it should be automatically picked
by Checkstyle).

---

# Checking code style with IntelliJ IDEA #

As we use a suppression config file, you can set up IntelliJ's Checkstyle plugin in
two ways: one adds our suppressions to the built-in "Google Checks" config, the other
creates a completely new config using a copy from the Checkstyle source archive.
Either way, you need to install the `CheckStyle-IDEA` plugin first:

* Go to `Settings -> Plugins`.
* Install [CheckStyle-IDEA plugin](https://plugins.jetbrains.com/plugin/1065-checkstyle-idea)
  (by Jamie Shiell) from the JetBrains' Marketplace repository.

## Editing built-in config ##

Edit the existing Google Checks configuration:

* Open the `Tools -> Checkstyle` plugin settings.
* Activate the `Google Checks` configuration.
* Some rules should be disabled, so we also ship a `checkstyle-suppressions.xml` config file. It
  lives in the project's root directory and should be picked up automatically. It should not be
  necessary to do anything else with it for it to be used.
* You can now run Checkstyle using the `Checkstyle` command or directly from the CheckStyle tab.
* Ensure `Rules:`, shown in the scan result window, reads `Google Checks`.

## Using cloned config ##

You can configure InteliJ's CheckStyle plugin to behave exactly as we configure it:

* Open the `Tools -> Checkstyle` plugin settings.
* Set the `Checkstyle version` to your liking.
* Go to the [Checkstyle GitHub page](https://github.com/checkstyle/checkstyle/releases) and look
  for a release matching the selected `Checkstyle version` and download the source archive.
* Unpack it and copy out the `src/main/resources/google_checks.xml` file to project root directory.
* Go back to the plugin configuration and add a new "Configuration file":
  * Click the `+` icon.
  * Set the description to `Logisim-evolution`.
  * Select `Use a local Checkstyle file`.
  * Click `Browse` and point to the `google_checks.xml` file.
  * Enable `Store relative to project location` and click `Next`.
  * Some rules should be disabled, so we also ship a `checkstyle-suppressions.xml` config file. It
    lives in the project's root directory and should be picked up automatically. It should not be
    necessary to do anything else with it for it to be used. If for any reason you need that changed,
    edit configuration and look for a property named `org.checkstyle.google.suppressionfilter.config`,
    then set its value to `checksyle-suppressions.xml` and click `Next.
  * Click "Finish".
* You can now run Checkstyle using the `Checkstyle` command or directly from the CheckStyle tab.
* Ensure `Rules:`, shown in the scan result window, reads `Logisim-evolution`.

---

# Using Gradle plugin #

Checkstyle is also plugged into the project's Gradle build system and provides the `checkstyleMain`
and `checkstyleTest` tasks:

```bash
$ ./gradlew checkstyleMain
```

This should kick in automatically for some build tasks but if for any reason you want Checkstyle
to not be run during your builds, exclude these tasks with `-x`, i.e:

```bash
$ ./gradlew build -x checkstyleMain -x checkstyleTest
```

---

# Using pre-commit hooks #

To improve quality of your commit, it's recommended to use [pre-commit](http://pre-commit.com) hooks, that will block
your commits unless all pre-commit tests pass. `Logisim-evolution` comes with predefined `.pre-commit-config.yaml`
config file for your convenience.

Brief installation instruction (see `pre-commit` [official installation docs](https://pre-commit.com/#install) too):

* Ensure you got Python installed
* Install pre-commit: `pip install pre-commit`
* Go to your Logisim-evolution source code root directory
* Copy provided config file template: `cp .pre-commit-config.yaml.dist .pre-commit-config.yaml`
* Plug hooks into Git pipeline: `pre-commit install`

## Disabling hooks for a commit ##

Not all hooks are perfect, so sometimes you may need to skip execution of one or more (or even all) hooks.
`pre-commit` solves this in two ways. To disable `pre-commit` completely, pass `--no-verify` to `git`:

```bash
$ git commit -a --no-verify
```

Alternatively, you may disable specific hooks only by using a `SKIP` environment variable, that is holds a comma separated
list of hook IDs to be omitted:

$ SKIP=checkstyle-jar git commit -a -m "Some changes"

## Updating hooks ##

Please remember to keep used hooks up to date, by periodically running

```bash
$ pre-commit autoupdate
```
