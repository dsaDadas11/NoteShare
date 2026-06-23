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
        .orElse("http://10.0.2.2:8200/")
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

val ensureNoteshareServer by tasks.registering {
    group = "noteshare"
    description = "Start the Spring Boot backend on host tcp:8081 if it is not already running."
    val serverDir = rootProject.layout.projectDirectory.dir("noteshare-server").asFile
    val logDir = rootProject.layout.buildDirectory.dir("noteshare-server").get().asFile

    doLast {
        val port = 8200
        println("Checking if NoteShare backend is running on port $port...")

        // Lightweight TCP probe
        fun isPortOpen(): Boolean {
            return try {
                val process = ProcessBuilder("cmd.exe", "/c", "netstat -an | findstr \":$port\" | findstr \"LISTENING\"")
                    .redirectErrorStream(true)
                    .start()
                val output = process.inputStream.bufferedReader().readText()
                process.waitFor()
                output.isNotBlank()
            } catch (_: Exception) {
                false
            }
        }

        if (!isPortOpen()) {
            if (!serverDir.exists()) {
                println("WARNING: Server directory not found: $serverDir")
                return@doLast
            }
            logDir.mkdirs()
            val outLog = File(logDir, "spring-boot.out.log")
            val errLog = File(logDir, "spring-boot.err.log")
            if (outLog.exists()) outLog.delete()
            if (errLog.exists()) errLog.delete()

            println("Starting NoteShare backend in $serverDir ...")
            ProcessBuilder("cmd.exe", "/c", "mvnw.cmd", "spring-boot:run", "-Dspring-boot.run.profiles=dev")
                .directory(serverDir)
                .redirectOutput(outLog)
                .redirectError(errLog)
                .start()

            println("Waiting for backend to start on 127.0.0.1:$port (timeout 60s)...")
            val deadline = System.currentTimeMillis() + 60_000
            while (!isPortOpen() && System.currentTimeMillis() < deadline) {
                Thread.sleep(2000)
                println("Still waiting...")
            }
        } else {
            println("NoteShare backend is already running on 127.0.0.1:$port")
        }

        if (!isPortOpen()) {
            println("WARNING: NoteShare backend did not start. The app will show network errors until the server is available.")
        } else {
            println("NoteShare backend is ready.")
        }
    }
}

val adbReverseDebug by tasks.registering(Exec::class) {
    group = "noteshare"
    description = "Forward Android device tcp:8200 to host tcp:8200 before debug install."
    val adbExecutable = android.sdkDirectory.resolve("platform-tools/adb.exe")
    commandLine(adbExecutable.absolutePath, "reverse", "tcp:8200", "tcp:8200")
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
    implementation(libs.androidx.material.icons.extended)

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

    // Media3 ExoPlayer (for video playback)
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
