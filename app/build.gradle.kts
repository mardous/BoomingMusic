import java.util.Properties

plugins {
    alias(libs.plugins.android)
    alias(libs.plugins.android.safeargs)
    id("kotlin-parcelize")
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.aboutlibraries)
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
    versionMinor = 2,
    versionPatch = 0,
    versionBuild = 7
)
val currentVersionCode = currentVersion.code

android {
    compileSdk = 36
    namespace = "com.mardous.booming"

    defaultConfig {
        minSdk = 28
        targetSdk = 36

        applicationId = namespace
        versionCode = 1200107
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
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
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
    implementation(libs.material.components)
    implementation(libs.androidx.core)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.concurrent.futures)
    implementation(libs.androidx.mediarouter)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.palette)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.viewpager)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.runtime.livedata)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.bundles.kotlinx)
    implementation(libs.bundles.lifecycle)
    implementation(libs.bundles.media3)
    implementation(libs.bundles.navigation)
    implementation(libs.bundles.glance)
    implementation(libs.bundles.koin)
    implementation(libs.bundles.coil)
    implementation(libs.bundles.ktor)
    implementation(libs.bundles.markwon)

    implementation(libs.room)
    ksp(libs.room.compiler)

    implementation(libs.m3color)
    implementation(libs.coil.transformations)

    implementation(libs.balloon)
    implementation(libs.compose.markdown)
    implementation(libs.aboutlibraries)

    implementation(libs.fadingedgelayout)
    implementation(libs.advrecyclerview) {
        isTransitive = true
    }
    implementation(libs.fastscroll)

    implementation(libs.customactivityoncrash)
    implementation(libs.keyboardvisibilityevent)

    implementation(libs.taglib)
    implementation(libs.jaudiotagger)

    implementation(libs.versioncompare)
    implementation(libs.commons.text)
    implementation(libs.juniversalchardet)
}