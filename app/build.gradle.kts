import java.util.Properties
import java.net.URI

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Delivery versions are explicit and identical for local/CI builds, independent of Git state.
val deliveryVersion = Properties().apply {
    rootProject.file("version.properties").inputStream().use { load(it) }
}
val deliveryName = requireNotNull(deliveryVersion.getProperty("versionName"))
val deliveryCode = requireNotNull(deliveryVersion.getProperty("versionCode")).toInt()
require(deliveryName.matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+"))) { "Versión no válida: $deliveryName" }
require(deliveryCode > 0) { "versionCode debe ser positivo." }

val distribution = Properties().apply {
    rootProject.file("distribution.properties").inputStream().use { load(it) }
}
fun publicInfo(name: String) = distribution.getProperty(name, "").trim()
val supportLink = publicInfo("supportUrl")
if (supportLink.isNotBlank()) {
    val uri = URI(supportLink)
    require(uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null &&
        uri.host !in listOf("example.com", "example.org", "localhost")) {
        "supportUrl debe ser una página pública HTTPS real o dejarse vacío."
    }
}
fun quoted(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.gtavoapps.portylus"
    compileSdk = 36
    buildToolsVersion = "35.0.0"
    defaultConfig {
        applicationId = "com.gtavoapps.portylus"
        minSdk = 33
        targetSdk = 36
        versionCode = deliveryCode
        versionName = deliveryName
        buildConfigField("String", "DEVELOPER_NAME", quoted(publicInfo("developerName")))
        buildConfigField("String", "WEBSITE_URL", quoted(publicInfo("websiteUrl")))
        buildConfigField("String", "PROJECT_URL", quoted(publicInfo("projectUrl")))
        buildConfigField("String", "SUPPORT_EMAIL", quoted(publicInfo("supportEmail")))
        buildConfigField("String", "PRIVACY_URL", quoted(publicInfo("privacyPolicyUrl")))
        buildConfigField("String", "SOURCE_URL", quoted(publicInfo("sourceCodeUrl")))
        buildConfigField("String", "SUPPORT_URL", quoted(publicInfo("supportUrl")))
        testInstrumentationRunner = "com.gtavoapps.portylus.PortylusTestRunner"
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            val storePath = providers.environmentVariable("PORTYLUS_KEYSTORE_PATH").orElse(providers.environmentVariable("PDF_KEYSTORE_PATH")).orNull
            if (storePath != null) {
                signingConfig = signingConfigs.create("distribution") {
                    storeFile = file(storePath)
                    storePassword = providers.environmentVariable("PORTYLUS_KEYSTORE_PASSWORD").orElse(providers.environmentVariable("PDF_KEYSTORE_PASSWORD")).get()
                    keyAlias = providers.environmentVariable("PORTYLUS_KEY_ALIAS").orElse(providers.environmentVariable("PDF_KEY_ALIAS")).get()
                    keyPassword = providers.environmentVariable("PORTYLUS_KEY_PASSWORD").orElse(providers.environmentVariable("PDF_KEY_PASSWORD")).get()
                }
            }
        }
    }
    // The in-app picker must work offline after a Play install, even if the device
    // initially requests only one language. Keep ES/EN/FR in every base APK.
    bundle { language { enableSplit = false } }
    packaging { jniLibs.useLegacyPackaging = false }
    lint { abortOnError = true }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.08.01"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.2")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("com.artifex.mupdf:fitz:1.28.5")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}

// Explicit publication gate; ordinary debug/unsigned builds remain available.
tasks.register("verifyPlayRelease") {
    doLast {
        require(publicInfo("developerName").isNotBlank()) { "Falta developerName en distribution.properties." }
        require(publicInfo("supportEmail").matches(Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"))) { "Falta un correo de soporte válido." }
        listOf("privacyPolicyUrl", "sourceCodeUrl").forEach { key ->
            val uri = URI(publicInfo(key))
            require(uri.scheme == "https" && !uri.host.isNullOrBlank() &&
                uri.host !in listOf("example.com", "example.org", "localhost")) { "Falta una URL pública HTTPS real para $key." }
        }
        listOf("KEYSTORE_PATH", "KEYSTORE_PASSWORD", "KEY_ALIAS", "KEY_PASSWORD").forEach { key ->
            require(!System.getenv("PORTYLUS_$key").orEmpty().ifBlank { System.getenv("PDF_$key").orEmpty() }.isBlank()) {
                "Falta PORTYLUS_$key para la clave de subida."
            }
        }
        println("Datos locales completos. Verificar URLs, código correspondiente y pruebas antes de subir.")
    }
}
tasks.matching { it.name == "bundleRelease" }.configureEach {
    mustRunAfter("verifyPlayRelease")
}
tasks.register("preparePlayBundle") {
    group = "distribution"
    description = "Valida configuración pública y firma antes de generar el AAB de Google Play."
    dependsOn("verifyPlayRelease", "bundleRelease")
}
