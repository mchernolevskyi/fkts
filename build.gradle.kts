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
    implementation("org.apache.commons:commons-lang3:3.9")
    implementation("org.slf4j:slf4j-simple:1.7.26")
    compileOnly("org.projectlombok:lombok:1.18.8")
}

version = "1.2.1"
