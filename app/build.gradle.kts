import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.roborazzi)
}

// A chave de assinatura fica fora do git: quem a tiver assina em nome da marca.
val ficheiroChave = rootProject.file("keystore.properties")
val chave = Properties().apply {
    if (ficheiroChave.exists()) FileInputStream(ficheiroChave).use { load(it) }
}

android {
    namespace = "pt.caramelo.tv"
    compileSdk = 35

    defaultConfig {
        applicationId = "pt.caramelo.tv"
        minSdk = 23
        targetSdk = 35
        // Cada compilacao do GitHub tem um numero maior (4/10/2026): e' o que a app compara para se atualizar sozinha (Atualizacao.kt).
        val compilacao = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionCode = compilacao
        versionName = "1.0.$compilacao"
        // A variante "nova" (22/11/2026) desliga estas duas coisas: ver o build type abaixo.
        buildConfigField("boolean", "ATUALIZA", "true")
        manifestPlaceholders["arranque"] = "true"
    }

    signingConfigs {
        if (chave.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(chave.getProperty("storeFile"))
                storePassword = chave.getProperty("storePassword")
                keyAlias = chave.getProperty("keyAlias")
                keyPassword = chave.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Sem chave propria, a release assina com a de debug: continua a instalar-se
            // por ADB, mas com o R8 e sem as verificacoes de debug do Compose, que numa
            // TCL fazem a diferenca entre fluido e aos solavancos (29/9/2026).
            signingConfig = if (chave.isNotEmpty()) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
        // A app nova ao lado da antiga, para experimentar na TV sem a apagar (22/11/2026, Francisco: "por a app nova na TV, sem
        // apagar a antiga"). Outro pacote (pt.caramelo.tv.nova) e outro nome no ecra inicial; nao se atualiza pelo site (instalava
        // a antiga por cima de si) nem abre quando a TV liga (essa e' a antiga). Compila-se com ./scripts/install-tv.sh <IP> --nova.
        create("nova") {
            initWith(getByName("release"))
            applicationIdSuffix = ".nova"
            versionNameSuffix = "-nova"
            matchingFallbacks += listOf("release")
            buildConfigField("boolean", "ATUALIZA", "false")
            manifestPlaceholders["arranque"] = "false"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        // Os testes de imagem desenham os ecras com os recursos da app (letras, cartazes do Caramelo).
        unitTests.isIncludeAndroidResources = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.tv.material)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.ui)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
