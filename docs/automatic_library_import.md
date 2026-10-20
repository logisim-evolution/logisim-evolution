# Automatically Importing Logisim Libraries

Logisim Evolution supports loading custom libraries at startup, contained in Logisim `.circ` files.

To do this, create a directory named `logisim-defaults` in the program directory used to start Logisim Evolution:

- For an installed application, this is the application directory. On a default Windows install, that may be
  `C:\Program Files\logisim-evolution\app\logisim-defaults`.
- For a standalone `.jar`, place `logisim-defaults` in the same directory as the `.jar` file that starts Logisim Evolution.
  ![Where the logisim-defaults folder goes, if executing from a JAR file.](img/logisim-defaults-jar.png)
- For a Gradle run, place `logisim-defaults` inside `/build/classes/java/`.
  ![Where the logisim-defaults folder goes, if running from Gradle.](img/logisim-defaults-build.png)

Inside the `logisim-defaults` folder should be any `.circ` files which you would like to load automatically at startup.
**Every circuit must have a unique name, and must not be called
"main"**. This is to avoid conflicts caused by loading libraries with the same name.

![An example of a CIRC file in the logisim-defaults folder](img/logisim-defaults-folder.png)

Please note that any files added using the `logisim-defaults` folder are third-party,
and therefore will need referencing for any projects you use them in.

As of present, libraries are only automatically imported on *startup*. By creating a new file through the user interface,
only built-in libraries will be imported.
