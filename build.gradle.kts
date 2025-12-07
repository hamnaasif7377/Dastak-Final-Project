buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath ("com.android.tools.build:gradle:8.1.0") // or your version
        classpath ("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0") // or your version
        classpath ("com.google.gms:google-services:4.4.0") // or your version
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}