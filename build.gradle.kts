
plugins {
    java
    application
    id("com.github.johnrengelman.shadow") version "4.0.1"
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
 }
