plugins {
    alias(libs.plugins.android.application) apply false
    // Add the dependency for the Google services Gradle plugin
    id("com.google.gms.google-services") version "4.4.3" apply false

}

buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.1")
    }
}