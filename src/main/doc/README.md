# Documentation generation prototype

This directory contains authored metadata for the incremental multilingual JavaHelp migration. It
is not packaged as application help content.

`guide-memory.xml` is the canonical English topic tree for the bounded Memory guide prototype.
Locale overlays provide translated titles and may explicitly select a reviewed localized page. If
an overlay omits a topic or its `path`, the generator inherits the corresponding English value;
the presence of a similarly named file does not select it automatically.

Run `./gradlew generateDocumentationPrototype` to write standalone English and German map and TOC
artifacts under `build/generated/documentation-prototype`. These prototype artifacts do not yet
replace the complete hand-maintained JavaHelp files packaged by the application.
