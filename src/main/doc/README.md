# Documentation generation

This directory contains authored metadata used to generate JavaHelp resources. The metadata itself
is not packaged as application help content.

`help-sets.xml` defines the complete set of locale-specific HelpSet descriptors. The
`generateHelpSets` task validates every referenced map, table of contents, and search database,
then writes the six descriptors under `build/generated/documentation-resources/doc`. Gradle adds
that generated directory to the application's main resources, so `processResources` and packaged
JARs always contain the generated descriptors.

`guide-memory.xml` is the canonical English topic tree for the bounded Memory guide prototype.
Locale overlays provide translated titles and may explicitly select a reviewed localized page. If
an overlay omits a topic or its `path`, the generator inherits the corresponding English value;
the presence of a similarly named file does not select it automatically.

Run `./gradlew generateDocumentationPrototype` to write standalone English and German map and TOC
artifacts under `build/generated/documentation-prototype`. These prototype artifacts do not yet
replace the complete hand-maintained maps and tables of contents packaged by the application.
