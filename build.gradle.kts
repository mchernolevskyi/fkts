plugins {
    java
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.neuronrobotics:nrjavaserial:3.15.0")
    implementation("commons-io:commons-io:2.6")
    implementation("org.apache.commons:commons-compress:1.18")
    implementation("org.tukaani:xz:1.8")
    implementation("org.apache.commons:commons-lang3:3.9")
}

version = "1.2.1"
