plugins {
    kotlin("jvm") version "1.7.0"
}

group = "bot.inker.onemirror"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(kotlin("stdlib"))

    // https://github.com/undertow-io/undertow/pull/1341
    // Upgrade if this pull request released
    implementation("io.undertow:undertow-core:2.3.0.Alpha2-SNAPSHOT")

    // Logger
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("org.apache.logging.log4j:log4j-api:2.17.2")
    implementation("org.apache.logging.log4j:log4j-core:2.17.2")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.17.2")
    implementation("org.apache.logging.log4j:log4j-jul:2.17.2")
    implementation("org.apache.logging.log4j:log4j-iostreams:2.17.2")
}