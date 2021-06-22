# Coding style #

Logisim-evolution uses code style based on CheckStyle's `Google Java Style` (stored
in `checkstyle/logisim.xml` file), with the following checks currently disabled:

* "MissingJavadocMethod"
* "NeedBraces

# Using Logisim style with InteliJ #

You can use our coding style with InteliJ's CheckStyle plugin:

* Go to Settings -> Plugins,
* Install "CheckStyle-IDEA" plugin (by Jamie Shiell) from Marketplace,
* Open its settings (Tools -> Checkstyle)
* Set correct "Checkstyle version" (see "toolVersion" in "gradle.build.kts" file)
* Add new "Configuration file":
  * Click "+"
  * Set description to "Logisim-evolution"
  * Select "Use a local Checkstyle file"
  * Click "Browse" and locate "logisim.xml" in "checkstyle/" folder in Logisim source tree
  * Enable "Store relative to project location"
  * Click "Next"
  * Click "Next" again on "Property" table.
  * Click "Finish"
* From now on you can run CheckStyle using "Checkstyle" command or directly from CheckStyle tab
* Ensure "Rules:" shown in the scan result window read "Logisim-evolution"

# Checking style with Gradle #

CheckStyle is plugged into project's Gradle and exposes `checkstyleMain` and
`checkstyleTest` tasks:

```bash
$ ./gradlew checkstyleMain
```

# Updating CheckStyle used by Gradle plugin #

If you are going to change version of CheckStyle used by Gradle, the following steps
must be taken care of:

* Edit `build.gradle.kts` and bump `toolVersion` of `checkstyle` task config to desired version
* Upgrade `checkstyle/logisim.xml` configuration. This is important, as certain versions of
  CheckStyle introduces backward incompatible changes and older/different configs may not
  work as expected or cause CheckStyle to fail completely. The simplest way to udpdate 
  `logisim.xml` config is to go to CheckStyle GitHub project release page:
  https://github.com/checkstyle/checkstyle/releases and grab source archive for that particular
  version you set in `toolVersion`. Then get the original `src/main/resources/google_checks.xml`
  file, disable (comment out) checks listed above and write as `logisim.xml`.

