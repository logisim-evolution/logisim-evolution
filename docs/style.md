![Logisim-evolution](../src/main/resources/resources/logisim/img/logisim-evolution-logo.svg)

---

* [« Go back](developers.md)
* **Source code quality**
  * [Coding style](#coding-style)
  * [Checking code style with InteliJ IDEA](#checking-code-style-with-intelij-idea)
    * [Editing built-in config](#editing-built-in-config)
    * [Using cloned config](#using-cloned-config)
  * [Using Gradle plugin](#using-gradle-plugin)
    
---

# Coding style #

`Logisim-evolution` uses `Google Java Style` code style as provided with the `CodeStyle`
static anaylyzer tool, with a few checks disabled as specified in `suppressions.xml`
config file.

# Checking code style with InteliJ IDEA #

As we use suppression config file, you can set up InteliJ's Checkstyle plugin in
two way - one adds our suppressions to built-in "Google Checks" config, other
creates compltely new config using copy from Checkstyle source archive.

In both ways you need to install `CheckStyle-IDEA` plugin first:

* Go to `Settings -> Plugins`,
* Install [CheckStyle-IDEA plugin](https://plugins.jetbrains.com/plugin/1065-checkstyle-idea) (by Jamie Shiell) from the Marketplace,

## Editing built-in config ##

Edit existing Google Checks configuration:

* Open `Tools -> Checkstyle` plugin settings,
* Activate `Google Checks` configuration,
* Now highlight `Google Checks` row and click `Pen` icon above the list to edit it
* Look for `org.checkstyle.google.suppressionfilter.config` named property and set its
  value to `config/checkstyle/suppressions.xml`
* Click `Finish`
* From now on you can run CheckStyle using `Checkstyle` command or directly from CheckStyle tab
* Ensure `Rules:` shown in the scan result window read `Google Checks`

## Using cloned config ##

You can configure InteliJ's CheckStyle plugin to behave exactly as we configure it:

* Open `Tools -> Checkstyle` plugin settings.
* Select `Checkstyle version` to your liking.
* Go to [Checkstyle GitHub page](https://github.com/checkstyle/checkstyle/releases) and look
  for release matching selected `Checkstyle version` and download source archive.
* Unpack it and copy out `src/main/resources/google_checks.xml` file to Logisim's `config/checkstyle/`.
* Go back to plugin configuration and add new "Configuration file":
  * Click `+` icon,
  * Set description to `Logisim-evolution`,
  * Select `Use a local Checkstyle file`,
  * Click `Browse` and point to `config/checkstyle/google_checks.xml` file,
  * Enable `Store relative to project location` and click `Next`,
  * On `Property` table look for property named `org.checkstyle.google.suppressionfilter.config`
    and set the value to `config/checkstyle/suppressions.xml` and click `Next,
  * Click "Finish",
* From now on you can run CheckStyle using `Checkstyle` command or directly from CheckStyle tab,
* Ensure `Rules:` shown in the scan result window read `Logisim-evolution`.

---

# Using Gradle plugin #

CheckStyle is also plugged into project's Gradle and exposes `checkstyleMain` and `checkstyleTest` tasks:

```bash
$ ./gradlew checkstyleMain
```
