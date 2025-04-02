import java.security.MessageDigest
import java.util.*

plugins {
    kotlin("jvm") version "2.1.10"
    id("maven-publish")
    signing
}

val groupName = "io.github.mwmsh.minjekt"
val packageName = "minjekt"
val packageVersion = "0.1.1"

val fullPackageName = "$groupName:$packageName$version"
val fullPackagePath = "${groupName.replace(".", "/")}/$packageName/$packageVersion/"

group = groupName

version = packageVersion

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(kotlin("reflect"))
    testImplementation(project(":"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(22)
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
            from(components["java"])

            pom {
                name = packageName
                description = "Minimal and easy to use dependency injection for Kotlin"
                url = "https://github.com/mwmsh/minjekt"

                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        id = "mwmsh"
                        name = "Mohamed S"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/mwmsh/minjekt.git"
                    developerConnection = "scm:git:ssh://github.com/mwmsh/minjekt.git"
                    url = "https://github.com/mwmsh/minjekt"
                }
            }
        }
    }
}

tasks.register("copyPomFileToLibDir") {
    val publication = publishing.publications.getByName("mavenJava")
    val generatePom: TaskProvider<Task> =
        tasks.named("generatePomFileFor${publication.name.capitalize()}Publication")
    dependsOn(generatePom)
    val output = layout.buildDirectory.file("libs/${project.name}-${version}.pom").get().asFile
    outputs.file(output)
    doLast {
        output.writeBytes(generatePom.get().outputs.files.singleFile.readBytes())
    }
}


tasks.register<Jar>("javadocJar") {
    archiveClassifier = "javadoc"
    from(tasks.javadoc)
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier = "sources"
    from(sourceSets["main"].allSource)
}

artifacts {
    add("archives", tasks["javadocJar"])
    add("archives", tasks["sourcesJar"])
}

signing {

    val signingKeyId = project.properties["signing.signing.keyId"] as String
    val signingPassword = project.properties["signing.password"] as String
    val secretKeyFilePath = project.properties["signing.secretKeyFile"] as String
    val keyContent = File(secretKeyFilePath).readText(Charsets.UTF_8)

    useInMemoryPgpKeys(signingKeyId, keyContent, signingPassword)

    sign(publishing.publications["mavenJava"])

    tasks.register("signJavadocJar") {
        dependsOn(tasks.named("javadocJar"))
        val javadocJarOutput = tasks.getByName("javadocJar").outputs.files.singleFile
        outputs.file(File(javadocJarOutput.absolutePath + ".asc"))

        doLast {
            sign(javadocJarOutput)
        }
    }

    tasks.register("signSourcesJar") {
        dependsOn(tasks.named("sourcesJar"))
        val sourcesJarOutput = tasks.getByName("sourcesJar").outputs.files.singleFile
        outputs.file(File(sourcesJarOutput.absolutePath + ".asc"))

        doLast {
            sign(sourcesJarOutput)
        }
    }

    tasks.register<Exec>("signPomFile") {
        dependsOn(tasks.named("copyPomFileToLibDir"))
        val inputFile = tasks.named("copyPomFileToLibDir").get().outputs.files.singleFile

        val signatureFile = File(inputFile.path + ".asc")
        outputs.file(signatureFile)

        environment("GPG_TTY", "true")

        commandLine(
            "gpg",
            "--batch", "--yes",
            "--armor",
            "--detach-sign",
            "--pinentry-mode", "loopback",  // Use loopback mode
            "--passphrase", signingPassword,  // Replace with your actual passphrase
            "--output",
            signatureFile.absolutePath, inputFile.absolutePath
        )
        doLast {
            println("Signed file created at $signatureFile")
        }
    }
}

tasks.named("publish").configure {
    dependsOn(
        tasks.named("sourcesJar"),
        tasks.named("javadocJar"),
        tasks.named("signSourcesJar"),
        tasks.named("signJavadocJar"),
        tasks.named("copyPomFileToLibDir"),
        tasks.named("signPomFile"),
    )
}

tasks.register("generateChecksums") {
    dependsOn(
        tasks.named("sourcesJar"),
        tasks.named("javadocJar"),
        tasks.named("signSourcesJar"),
        tasks.named("signJavadocJar"),
        tasks.named("copyPomFileToLibDir"),
        tasks.named("signPomFile"),
    )

    val algorithms = listOf("MD5", "SHA-1")

    inputs.files(fileTree(layout.buildDirectory.dir("libs").get().asFile.path).matching { include("*.jar", "*.pom") })
    outputs.dir(layout.buildDirectory.dir("libs"))

    doLast {
        val outputDir = file("${buildDir}/libs")
        outputDir.mkdirs()
        val files = file("build/libs").listFiles()?.filter{it.name.endsWith(".jar") || it.name.endsWith(".pom")}?.filter{!it.name.contains("bundle")}
        files?.forEach { file ->
            algorithms.forEach { algorithm ->
                val checksum = file.inputStream().use { input ->
                    val digest = MessageDigest.getInstance(algorithm)
                    val bytes = digest.digest(input.readBytes())
                    bytes.joinToString("") { "%02x".format(it) }
                }
                val checksumFile = file("${outputDir.path}/${file.name}.${algorithm.lowercase().replace("-", "")}")
                checksumFile.writeText(checksum)
                println("Generated $algorithm checksum for ${file.name}: $checksum")
            }
        }
    }
}

tasks.register<Zip>("createDeploymentZip"){
    dependsOn(tasks.named("generateChecksums"))

    group = "distribution"  // Optional: Categorizes the task
    description = "Zips the build output files."

    archiveFileName.set("artifacts.zip")  // Name of the ZIP file
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))

    from("build/libs") { // Include all files from this directory
        include("*.jar", "*.sha1", "*.md5", "*.asc", "*.pom")    // Only include JAR files
        exclude("**bundle**")
        into(fullPackagePath)
    }
    doLast {
        println("Artifacts successfully zipped!")
    }
}

tasks.register<Exec>("uploadToSonatype") {
    dependsOn(tasks.named("createDeploymentZip"))
    group = "publishing"
    description = "Uploads the bundle to Sonatype Central using curl."


    val apiUrl = "https://central.sonatype.com/api/v1/publisher/upload"

    val username = project.properties["ossrhUsername"] as String?
    val password = project.properties["ossrhPassword"] as String?
    val authToken = Base64.getEncoder().encodeToString("$username:$password".encodeToByteArray())

    val bundlePath = layout.buildDirectory.dir("distributions/artifacts.zip").get().asFile.path

    commandLine = listOf(
        "curl",
        "--request", "POST",
        "--verbose",
        "--header", "accept: text/plain",
        "--header", "Content-Type: multipart/form-data",
        "--header", "Authorization: Bearer $authToken",
        "--form", "bundle=@$bundlePath;type=application/zip",
        apiUrl+"?name=${UUID.randomUUID()}&publishingType=AUTOMATIC"
    )

    doLast {
        println("Bundle uploaded to Sonatype Central.")
    }
}
