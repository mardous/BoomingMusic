import java.util.Properties

val isNormalBuild: Boolean by rootProject.extra

plugins {
    alias(libs.plugins.agp)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.androidx.safeargs)
    alias(libs.plugins.aboutlibraries)
}

if (isNormalBuild) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

sealed class Version(
    private val versionOffset: Long,
    private val versionMajor: Int,
    private val versionMinor: Int,
    private val versionPatch: Int,
    private val versionBuild: Int = 0,
    private val versionType: String = ""
) {
    companion object {
        private const val MAJOR = 1_000_000L
        private const val MINOR = 100_000L
        private const val PATCH = 10_000L
        private const val VARIANT = 100L

        private const val ALPHA = 0L
        private const val BETA = 1L
        private const val RELEASE_CANDIDATE = 2L
        private const val STABLE = 3L
    }

    init {
        require(versionMajor >= 0 && versionMinor >= 0 && versionPatch >= 0 && versionBuild >= 0) {
            "Version numbers must be non-negative"
        }
    }

    class Alpha(versionMajor: Int, versionMinor: Int, versionPatch: Int, versionBuild: Int) :
        Version(ALPHA, versionMajor, versionMinor, versionPatch, versionBuild, "alpha")

    class Beta(versionMajor: Int, versionMinor: Int, versionPatch: Int, versionBuild: Int) :
        Version(BETA, versionMajor, versionMinor, versionPatch, versionBuild, "beta")

    class RC(versionMajor: Int, versionMinor: Int, versionPatch: Int, versionBuild: Int) :
        Version(RELEASE_CANDIDATE, versionMajor, versionMinor, versionPatch, versionBuild, "rc")

    class Stable(versionMajor: Int, versionMinor: Int, versionPatch: Int) :
        Version(STABLE, versionMajor, versionMinor, versionPatch)

    val name: String
        get() {
            val versionName = "${versionMajor}.${versionMinor}.${versionPatch}"
            return if (versionType.isNotEmpty()) "$versionName-${versionType}.$versionBuild" else versionName
        }

    val code: Int
        get() {
            val versionCode = versionMajor * MAJOR +
                    versionMinor * MINOR +
                    versionPatch * PATCH +
                    versionOffset * VARIANT +
                    versionBuild
            require(versionCode <= Int.MAX_VALUE) {
                "Version code exceeds Int.MAX_VALUE"
            }
            return versionCode.toInt()
        }
}

val currentVersion: Version = Version.Beta(
    versionMajor = 1,
    versionMinor = 1,
    versionPatch = 0,
    versionBuild = 4
)
val currentVersionCode = currentVersion.code

android {
    compileSdk = 36
    namespace = "com.mardous.booming"

    defaultConfig {
        minSdk = 26
        targetSdk = 35

        applicationId = namespace
        versionCode = 1100104
        versionName = currentVersion.name
        check(versionCode == currentVersionCode)
    }

    flavorDimensions += "version"
    productFlavors {
        create("normal") {
            dimension = "version"
        }
        create("fdroid") {
            dimension = "version"
        }
    }

    val signingProperties = getProperties("keystore.properties")
    val releaseSigning = if (signingProperties != null) {
        signingConfigs.create("release") {
            keyAlias = signingProperties.property("keyAlias")
            keyPassword = signingProperties.property("keyPassword")
            storePassword = signingProperties.property("storePassword")
            storeFile = file(signingProperties.property("storeFile"))
        }
    } else {
        signingConfigs.getByName("debug")
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            signingConfig = releaseSigning
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = " DEBUG"
            signingConfig = releaseSigning
        }
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }
    androidResources {
        generateLocaleConfig = true
    }
    packaging {
        resources {
            excludes += listOf("META-INF/LICENSE", "META-INF/NOTICE", "META-INF/java.properties")
        }
    }
    lint {
        abortOnError = true
        warning += listOf("ImpliedQuantity", "Instantiatable", "MissingQuantity", "MissingTranslation")
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    applicationVariants.all {
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                "BoomingMusic-${defaultConfig.versionName}-${name}.apk"
        }
    }
}

kotlin {
    compilerOptions {
        optIn.add("kotlin.RequiresOptIn")
    }
    jvmToolchain(21)
}

fun getProperties(fileName: String): Properties? {
    val file = rootProject.file(fileName)
    return if (file.exists()) {
        Properties().also { properties ->
            file.inputStream().use { properties.load(it) }
        }
    } else null
}

fun Properties.property(key: String) =
    this.getProperty(key) ?: "$key missing"

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)

    // Firebase BoM
    "normalImplementation"(platform(libs.firebase.bom))
    "normalImplementation"(libs.firebase.crashlytics)

    // Google/JetPack
    //https://developer.android.com/jetpack/androidx/versions
    implementation(libs.core.ktx)
    implementation(libs.core.splashscreen)
    implementation(libs.appcompat)
    implementation(libs.fragment.ktx)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)

    // Integration with activities, ViewModels and LiveData
    implementation(libs.compose.runtime.livedata)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    // Android Studio Preview support
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    // Coil
    implementation(libs.coil.compose)

    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.common.java8)

    implementation(libs.navigation.common.ktx)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.runtime.ktx)
    implementation(libs.navigation.ui.ktx)

    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.androidx.media)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.palette.ktx)
    implementation(libs.androidx.viewpager)
    implementation(libs.material.components)

    implementation(libs.aboutlibraries)

    implementation(libs.m3color)
    implementation(libs.balloon)
    implementation(libs.fastscroll)
    implementation(libs.fadingedgelayout)
    implementation(libs.advrecyclerview) {
        isTransitive = true
    }
    implementation(libs.customactivityoncrash)
    implementation(libs.versioncompare)
    implementation(libs.keyboardvisibilityevent)

    implementation(libs.compose.markdown)
    implementation(libs.markdown.core)
    implementation(libs.markdown.html)
    implementation(libs.markdown.glide)
    implementation(libs.markdown.linkify)

    implementation(libs.bundles.ktor)

    implementation(libs.koin.core)
    implementation(libs.koin.android)

    implementation(libs.glide)
    implementation(libs.glide.okhttp3)
    ksp(libs.glide.ksp)

    implementation(libs.taglib)
    implementation(libs.jaudiotagger)
}