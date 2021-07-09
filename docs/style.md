[![Logisim-evolution](../artwork/logisim-evolution-logo.svg)](https://github.com/logisim-evolution/logisim-evolution)

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
static anaylyzer tool, with a few checks disabled as specified in the `suppressions.xml`
config file.

# Checking code style with InteliJ IDEA #

As we use a suppression config file, you can set up InteliJ's Checkstyle plugin in
two ways - one adds our suppressions to the built-in "Google Checks" config, the other
creates a completely new config using a copy from the Checkstyle source archive.

Either way, you need to install the `CheckStyle-IDEA` plugin first:

* Go to `Settings -> Plugins`.
* Install [CheckStyle-IDEA plugin](https://plugins.jetbrains.com/plugin/1065-checkstyle-idea) (by Jamie Shiell) from the Marketplace.

## Editing built-in config ##

Edit the existing Google Checks configuration:

* Open the `Tools -> Checkstyle` plugin settings.
* Activate the `Google Checks` configuration.
* Highlight the `Google Checks` row and click the `Pen` icon above the list to edit it.
* Look for the `org.checkstyle.google.suppressionfilter.config` named property and set its
  value to `config/checkstyle/suppressions.xml`.
* Click `Finish`.
* You can now run CheckStyle using the `Checkstyle` command or directly from the CheckStyle tab.
* Ensure `Rules:`, shown in the scan result window, reads `Google Checks`.

## Using cloned config ##

You can configure InteliJ's CheckStyle plugin to behave exactly as we configure it:

* Open the `Tools -> Checkstyle` plugin settings.
* Set the `Checkstyle version` to your liking.
* Go to the [Checkstyle GitHub page](https://github.com/checkstyle/checkstyle/releases) and look
  for a release matching the selected `Checkstyle version` and download the source archive.
* Unpack it and copy out the `src/main/resources/google_checks.xml` file to Logisim's `config/checkstyle/`.
* Go back to the plugin configuration and add a new "Configuration file":
  * Click the `+` icon.
  * Set the description to `Logisim-evolution`.
  * Select `Use a local Checkstyle file`.
  * Click `Browse` and point to the `config/checkstyle/google_checks.xml` file.
  * Enable `Store relative to project location` and click `Next`.
  * On the `Property` table, look for a property named `org.checkstyle.google.suppressionfilter.config`.
    Set the value to `config/checkstyle/suppressions.xml` and click `Next.
  * Click "Finish".
* You can now run CheckStyle using the `Checkstyle` command or directly from the CheckStyle tab.
* Ensure `Rules:`, shown in the scan result window, reads `Logisim-evolution`.

---

# Using Gradle plugin #

CheckStyle is also plugged into the project's Gradle build system and provides the `checkstyleMain` and `checkstyleTest` tasks:

```bash
$ ./gradlew checkstyleMain
```
