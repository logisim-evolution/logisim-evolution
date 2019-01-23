
plugins {
    java
    application
    id("com.github.johnrengelman.shadow") version "4.0.1"
    id("edu.sc.seis.macAppBundle") version "2.3.0"
}

repositories {
    jcenter()
    mavenCentral()
}

application {
    mainClassName = "com.cburch.logisim.Main"
}

dependencies {
    implementation(fileTree("lib") {
        include("**/*.jar")
    })
    implementation("org.hamcrest:hamcrest-core:1.3")
    implementation("javax.help:javahelp:2.0.05")
//  implementation("com.fifesoft:rsyntaxtextarea:2.6.1") // Currently using 3.0.0-SNAPSHOT
    implementation("org.slf4j:slf4j-api:1.7.8")
    implementation("org.slf4j:slf4j-simple:1.7.8")

    testImplementation("ch.qos.logback:logback-classic:1.1.2")
    testImplementation("ch.qos.logback:logback-core:1.1.2")
    testImplementation("junit:junit:4.12")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

task<Jar>("sourcesJar") {
    group = "build"
    description = "Creates a source jar archive."
    dependsOn.add("classes")
    classifier = "src"

    from(sourceSets.main.get().allSource)
}

tasks {
    jar {
        manifest {
            attributes.putAll(mapOf(
                    "Implementation-Title" to name,
                    "Implementation-Version" to version
            ))
        }

        from(".") {
            include("LICENSE")
            include("README.md")
        }
    }
    shadowJar {
        from(".") {
            include("LICENSE")
            include("README.md")
        }
    }
    macAppBundle {
        mainClassName = "com.cburch.logisim.Main"
        runtimeConfigurationName = "shadow"
        jarTask = "shadowJar"
        bundleJRE = false
        highResolutionCapable = true
        appName = "Logisim-evolution"
        appStyle = "universalJavaApplicationStub"
        bundleIdentifier = "com.cburch.logisim"
        creatorCode = "????"
        icon = "src/main/resources/resources/logisim/img/Logisim-evolution.icns"
        // backgroundImage = "src/main/resources/resources/logisim/img/logisim-icon-128.png"
        // javaProperties.put("apple.laf.useScreenMenuBar", "true")
        bundleExtras.put("CFBundleDisplayName", "Logisim-evolution")
        bundleExtras.put(
            "CFBundleDocumentTypes",
            arrayOf(
                mapOf(
                    "LSItemContentTypes" to arrayOf("com.cburch.logisim.circ"),
                    "CFBundleTypeName" to "Logisim-evolution circuit file",
                    "LSHandlerRank" to "Owner",
                    "CFBundleTypeRole" to "Editor",
                    "LSIsAppleDefaultForType" to true
                )
            )
        )
        bundleExtras.put(
            "UTExportedTypeDeclarations",
            arrayOf(
                mapOf(
                    "UTTypeIdentifier" to "com.cburch.logisim.circ",
                    "UTTypeDescription" to "Logisim-evolution circuit file",
                    "UTTypeConformsTo" to arrayOf("public.data"),
                    "UTTypeTagSpecification" to
                    mapOf(
                        "public.filename-extension" to arrayOf("circ"),
                        "public.mime-type" to arrayOf("application-prs.cburch.logisim")
                    )
                )
            )
        )
        bundleExtras.put("LSApplicationCategoryType", "public.app-category.education")
        bundleExtras.put("NSHumanReadableCopyright", "Copyright © 2001–2019 Carl Burch, BFH, HEIG-VD, HEPIA, et al.")
        bundleExtras.put("NSSupportsAutomaticGraphicsSwitching", "true")
    }
}
