import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties()
if (versionPropsFile.exists()) {
    FileInputStream(versionPropsFile).use { versionProps.load(it) }
}

fun getGitCommitCount(): Int {
    return try {
        val process = ProcessBuilder("git", "rev-list", "--count", "HEAD")
            .directory(rootProject.rootDir)
            .redirectErrorStream(true)
            .start()
        val count = process.inputStream.bufferedReader().readLine()?.trim()?.toIntOrNull() ?: 1
        process.waitFor()
        count
    } catch (_: Exception) {
        1
    }
}

fun getGitShortHash(): String {
    return try {
        val process = ProcessBuilder("git", "rev-parse", "--short", "HEAD")
            .directory(rootProject.rootDir)
            .redirectErrorStream(true)
            .start()
        val hash = process.inputStream.bufferedReader().readLine()?.trim() ?: "local"
        process.waitFor()
        hash
    } catch (_: Exception) {
        "local"
    }
}

val vMajor = versionProps.getProperty("VERSION_MAJOR", "1").toInt()
val vMinor = versionProps.getProperty("VERSION_MINOR", "0").toInt()
val vPatch = versionProps.getProperty("VERSION_PATCH", "0").toInt()
val vBuild = versionProps.getProperty("VERSION_BUILD", "101").toInt()

val gitCount = getGitCommitCount()
val gitHash = getGitShortHash()
val appVersionCode = vBuild + gitCount
val appVersionName = "$vMajor.$vMinor.$vPatch.$gitCount"

android {
    namespace = "com.storage.intelligence"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.storage.intelligence"
        minSdk = 29
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName

        buildConfigField("String", "GIT_HASH", "\"$gitHash\"")
        buildConfigField("int", "BUILD_NUMBER", "$appVersionCode")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    val keystoreConfigFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties()
    if (keystoreConfigFile.exists()) {
        keystoreProperties.load(FileInputStream(keystoreConfigFile))
    }

    signingConfigs {
        create("release") {
            if (keystoreConfigFile.exists()) {
                val storeFilePath = keystoreProperties.getProperty("storeFile", "storageiq-release.jks")
                val rawFile = File(storeFilePath)
                val resolvedFile = when {
                    rawFile.isAbsolute -> rawFile
                    rootProject.file(storeFilePath).exists() -> rootProject.file(storeFilePath)
                    else -> project.file(storeFilePath)
                }
                storeFile = resolvedFile.absoluteFile
                storePassword = keystoreProperties.getProperty("storePassword", "")
                keyAlias = keystoreProperties.getProperty("keyAlias", "storageiq")
                keyPassword = keystoreProperties.getProperty("keyPassword", "")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystoreConfigFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs += listOf(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INDEX/{AL2.0,LGPL2.1}"
        }
    }

    applicationVariants.all {
        val variant = this
        outputs.all {
            val output = this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl
            if (output != null) {
                output.outputFileName = "StorageIQ-v${appVersionName}-${variant.name}.apk"
            }
        }
    }
}

tasks.register("incrementBuildNumber") {
    doLast {
        if (versionPropsFile.exists()) {
            val props = Properties()
            FileInputStream(versionPropsFile).use { props.load(it) }
            val current = props.getProperty("VERSION_BUILD", "101").toInt()
            val next = current + 1
            props.setProperty("VERSION_BUILD", next.toString())
            props.setProperty("VERSION_NAME", "$vMajor.$vMinor.$vPatch.$next")
            FileOutputStream(versionPropsFile).use { props.store(it, "Auto-incremented build version") }
            println("Version updated: Build $next ($vMajor.$vMinor.$vPatch.$next)")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose BOM & UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Room DB
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore & WorkManager
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.work.compiler)

    // Media & Image Loading
    implementation(libs.coil.compose)
    implementation(libs.coil.video)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)

    // Serialization & Billing
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.play.billing.ktx)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.compose.ui.tooling)
}
