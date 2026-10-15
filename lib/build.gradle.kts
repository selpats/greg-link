plugins {
    id("java-library")
    id("maven-publish")
    id("com.diffplug.spotless") version "6.25.0"
}

repositories {
    mavenCentral()
    mavenLocal()
}

group = "cafe.zach"

apply(from = "${rootProject.projectDir.parentFile}/gradle/shared/spotless.gradle")

tasks.withType<JavaCompile> {
    options.release.set(8)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}

dependencies {
    implementation("net.dv8tion:JDA:6.3.1") {
        exclude(module="opus-java")
        exclude(module="tink")
    }
    implementation("com.google.code.gson:gson:2.8.0")
}

configurations.named("runtimeElements") {
    attributes {
        attribute(
            Attribute.of("com.gtnewhorizons.retrofuturagradle.obfuscation", String::class.java),
            "mcp"
        )
        attribute(
            Attribute.of("rfgDeobfuscatorTransformed", Boolean::class.javaObjectType),
            true
        )
    }
}

configurations.named("apiElements") {
    attributes {
        attribute(
            Attribute.of("com.gtnewhorizons.retrofuturagradle.obfuscation", String::class.java),
            "mcp"
        )
        attribute(
            Attribute.of("rfgDeobfuscatorTransformed", Boolean::class.javaObjectType),
            true
        )
    }
}
