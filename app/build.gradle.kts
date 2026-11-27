import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.security.DigestInputStream
import java.security.MessageDigest

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.abhi.ganasuno"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.abhi.ganasuno"
        // kept at 26 (works with every library below)
        minSdk = 26
        targetSdk = 36
        versionCode = 112
        versionName = "1.1"
    }

    flavorDimensions += "edition"
    productFlavors {
        create("foss") { dimension = "edition" }
        create("full") { dimension = "edition" }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources.excludes +=
            listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/INDEX.LIST",
                "/META-INF/{AL2.0,LGPL2.1}",
            )
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// The extractor brings protobuf-javalite; drop the full protobuf if anything else pulls it (DEX duplicate classes).
configurations.all {
    exclude(group = "com.google.protobuf", module = "protobuf-java")
}

dependencies {
    // Keep the existing app libraries and compile SDK 36.
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")

    // Playback (ExoPlayer + MediaSession service for background play / notification)
    implementation("androidx.media3:media3-exoplayer:1.8.0")
    implementation("androidx.media3:media3-database:1.8.0")
    implementation("androidx.media3:media3-session:1.8.0")
    implementation("androidx.media3:media3-ui:1.8.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.8.0")

    // Network + stream URL extraction (same extractor SimpMusic uses as its fallback)
    // okhttp 5.x needs compileSdk 37; 4.12.0 works with compileSdk 36
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.github.maxrave-dev:BravePipeExtractor:105aad96c7")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.5")

    testImplementation("junit:junit:4.13.2")
}

val releaseArtifacts = {
    listOf("fossReleaseRuntimeClasspath", "coreLibraryDesugaring").flatMap { name ->
        configurations.getByName(name).resolvedConfiguration.resolvedArtifacts.toList()
    }.distinctBy { it.moduleVersion.id.toString() + ":" + it.name }.sortedBy { it.moduleVersion.id.toString() }
}

tasks.register("verifyReleaseDependencies") {
    group = "verification"
    description = "Verify the FOSS release runtime and desugaring artifact SHA-256 checksums"
    doLast {
        val entries = rootProject.file("docs/licenses/RELEASE-DEPENDENCY-SHA256SUMS.txt").readLines()
            .filter { it.isNotBlank() }
        val expected = entries.associate { line ->
            val (hash, coordinate) = line.split("  ", limit = 2)
            require(hash.matches(Regex("[0-9a-f]{64}"))) { "Invalid SHA-256 for $coordinate" }
            coordinate to hash
        }
        val artifacts = releaseArtifacts()
        val coordinates = artifacts.map { it.moduleVersion.id.toString() }
        check(expected.size == entries.size && coordinates.size == expected.size && coordinates.toSet() == expected.keys) {
            "Resolved release coordinates differ from the verified checksum list"
        }
        for (artifact in artifacts) {
            val digest = MessageDigest.getInstance("SHA-256")
            DigestInputStream(artifact.file.inputStream(), digest).use { input ->
                input.transferTo(OutputStream.nullOutputStream())
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            val coordinate = artifact.moduleVersion.id.toString()
            check(hash == expected.getValue(coordinate)) { "Release dependency checksum mismatch: $coordinate" }
        }
        println("Verified SHA-256 checksums for ${artifacts.size} FOSS release and desugaring artifacts")
    }
}

// Optional auditing task: inspect actual resolved runtime archives.
tasks.register("generateDependencyInventory") {
    group = "verification"
    description = "Resolve FOSS release artifacts and collect embedded license and notice text"
    doLast {
        val out = rootProject.file("release-license-inventory.txt")
        val report = buildString {
            appendLine("Resolved dependencies — actual runtime archives")
            appendLine("Embedded notices are not a complete legal review. Missing text must be obtained from upstream.")
            for (artifact in releaseArtifacts()) {
                appendLine("\n${artifact.moduleVersion.id} (${artifact.name})")
                val texts = runCatching {
                    ZipFile(artifact.file).use { zip ->
                        fun notice(name: String): Boolean = name.substringAfterLast('/').uppercase().let { it.startsWith("LICENSE") || it.startsWith("NOTICE") || it.startsWith("COPYING") }
                        val outTexts = mutableListOf<Pair<String, String>>()
                        val entries = zip.entries().asSequence().filter { !it.isDirectory }.take(10000).toList()
                        entries.filter { it.size in 1L..150_000L && notice(it.name) }.take(12).forEach { entry ->
                            outTexts.add(entry.name to zip.getInputStream(entry).use { it.readNBytes(150_000).toString(Charsets.UTF_8) })
                        }
                        entries.filter { it.name.endsWith(".jar") && it.size in 1L..32_000_000L }.take(12).forEach { nested ->
                            ZipInputStream(zip.getInputStream(nested)).use { inner ->
                                var inspected = 0
                                var total = 0L
                                while (inspected++ < 10000 && total < 64_000_000 && outTexts.size < 24) {
                                    val entry = inner.nextEntry ?: break
                                    // Bound decompression for every entry, including skipped classes.
                                    val buf = ByteArray(8192)
                                    val capture = ByteArrayOutputStream()
                                    val wanted = !entry.isDirectory && notice(entry.name)
                                    var count = 0L
                                    while (true) {
                                        val n = inner.read(buf)
                                        if (n < 0) break
                                        count += n; total += n
                                        if (wanted && capture.size() + n <= 150_000) capture.write(buf, 0, n)
                                        if (total > 64_000_000 || count > 16_000_000) break
                                    }
                                    if (total > 64_000_000 || count > 16_000_000) break
                                    if (wanted && count in 1L..150_000L) outTexts.add("${nested.name}!/${entry.name}" to capture.toString(Charsets.UTF_8.name()))
                                    inner.closeEntry()
                                }
                            }
                        }
                        outTexts
                    }
                }.getOrDefault(emptyList())
                if (texts.isEmpty()) appendLine("No embedded notice found within scan limits; check upstream.")
                else texts.forEach { (name, text) -> appendLine(name); appendLine(text) }
            }
        }
        out.writeText(report.take(2_000_000))
        println("Dependency inventory written to ${out.path}")
    }
}
