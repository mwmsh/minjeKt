plugins {
    kotlin("jvm") version "2.1.10"
}

group = "com.minjeKt"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(kotlin("reflect"))

}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(22)
}