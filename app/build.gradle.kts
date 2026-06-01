plugins {
    id("com.android.application")
    kotlin("android")
    id("com.google.dagger.hilt.android")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
    kotlin("plugin.compose")
}

android {
    namespace = "com.example.noteshare"
    compileSdk = 35
    val noteshareBaseUrl = providers
        .gradleProperty("noteshareBaseUrl")
        .orElse("http://127.0.0.1:8080/")
        .get()

    defaultConfig {
        applicationId = "com.example.noteshare"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        vectorDrawables {
            useSupportLibrary = true
        }
        buildConfigField("String", "BASE_URL", "\"$noteshareBaseUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

val ensureNoteshareServer by tasks.registering(Exec::class) {
    group = "noteshare"
    description = "Start the Spring Boot backend on host tcp:8080 if it is not already running."
    val serverDir = rootProject.layout.projectDirectory.dir("noteshare-server").asFile.absolutePath
    val logDir = rootProject.layout.buildDirectory.dir("noteshare-server").get().asFile.absolutePath
    commandLine(
        "powershell.exe",
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-Command",
        """
        ${'$'}ErrorActionPreference = 'Stop'
        function Test-LocalPort {
            param([int]${'$'}Port)
            ${'$'}client = [System.Net.Sockets.TcpClient]::new()
            try {
                ${'$'}connect = ${'$'}client.BeginConnect('127.0.0.1', ${'$'}Port, ${'$'}null, ${'$'}null)
                if (-not ${'$'}connect.AsyncWaitHandle.WaitOne(500, ${'$'}false)) {
                    return ${'$'}false
                }
                ${'$'}client.EndConnect(${'$'}connect)
                return ${'$'}true
            } catch {
                return ${'$'}false
            } finally {
                ${'$'}client.Close()
            }
        }
        ${'$'}portOpen = Test-LocalPort -Port 8080
        if (-not ${'$'}portOpen) {
            ${'$'}serverDir = '$serverDir'
            ${'$'}logDir = '$logDir'
            New-Item -ItemType Directory -Force -Path ${'$'}logDir | Out-Null
            ${'$'}outLog = Join-Path ${'$'}logDir 'spring-boot.out.log'
            ${'$'}errLog = Join-Path ${'$'}logDir 'spring-boot.err.log'
            Start-Process `
                -FilePath 'cmd.exe' `
                -WorkingDirectory ${'$'}serverDir `
                -WindowStyle Hidden `
                -ArgumentList '/c', 'mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"' `
                -RedirectStandardOutput ${'$'}outLog `
                -RedirectStandardError ${'$'}errLog

            ${'$'}deadline = (Get-Date).AddSeconds(45)
            do {
                Start-Sleep -Seconds 1
                ${'$'}portOpen = Test-LocalPort -Port 8080
            } while (-not ${'$'}portOpen -and (Get-Date) -lt ${'$'}deadline)
        }
        if (-not ${'$'}portOpen) {
            throw "NoteShare backend did not start on 127.0.0.1:8080. Check $logDir\spring-boot.err.log"
        }
        """.trimIndent()
    )
}

val adbReverseDebug by tasks.registering(Exec::class) {
    group = "noteshare"
    description = "Forward Android device tcp:8080 to host tcp:8080 before debug install."
    val adbExecutable = android.sdkDirectory.resolve("platform-tools/adb.exe")
    commandLine(adbExecutable.absolutePath, "reverse", "tcp:8080", "tcp:8080")
    isIgnoreExitValue = true
    dependsOn(ensureNoteshareServer)
}

tasks.matching { it.name == "preDebugBuild" || it.name == "installDebug" }.configureEach {
    dependsOn(adbReverseDebug)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Retrofit + OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.logging)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    // Coil
    implementation(libs.coil.compose)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
