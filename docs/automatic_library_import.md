# Automatically Importing Logisim Libraries

Logisim Evolution supports loading custom libraries at startup, contained in Logisim `.circ` files.
To do this, create a directory in your **home directory** named `logisim-defaults` and insert any Logisim libraries you
would like to load every time Logisim Evolution starts. **Every circuit must have a unique name, and must not be called
"main"**. This is to avoid conflicts caused by loading libraries with the same name.

Please note that any files added using the `logisim-defaults` folder are third-party,
and therefore will need referencing for any projects you use them in.

As of present, libraries are only automatically imported on *startup*. By creating a new file through the user interface,
only built-in libraries will be imported.

![Logisim-defaults](img/logisim-defaults.png)