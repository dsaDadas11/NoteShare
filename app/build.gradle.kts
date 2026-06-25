import java.net.HttpURLConnection
import java.net.URI
import java.util.Properties

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
        .orElse("http://127.0.0.1:8200/")
        .get()

    defaultConfig {
        applicationId = "com.example.noteshare"
        minSdk = 26
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

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

fun localOrEnv(localName: String, envName: String): String? {
    return (localProperties.getProperty(localName) ?: System.getenv(envName))
        ?.takeIf { it.isNotBlank() }
}

val ensureNoteshareServer by tasks.registering {
    group = "noteshare"
    description = "Start the Spring Boot backend on host tcp:8200 if it is not already running."
    val serverDir = rootProject.layout.projectDirectory.dir("noteshare-server").asFile
    val logDir = rootProject.layout.buildDirectory.dir("noteshare-server").get().asFile

    doLast {
        val port = 8200
        println("Checking if NoteShare backend is running on port $port...")

        // 发起 NoteShare 真实接口请求，确认应用和数据库都已可用（不只是端口监听）
        fun isServerReady(): Boolean {
            return try {
                val conn = URI("http://127.0.0.1:$port/api/notes?page=1&size=1").toURL()
                    .openConnection() as HttpURLConnection
                conn.connectTimeout = 2000
                conn.readTimeout = 2000
                conn.requestMethod = "GET"
                conn.connect()
                val code = conn.responseCode
                conn.disconnect()
                code in 200..299
            } catch (_: Exception) {
                false
            }
        }

        if (!isServerReady()) {
            if (!serverDir.exists()) {
                println("WARNING: Server directory not found: $serverDir")
                return@doLast
            }
            logDir.mkdirs()
            val outLog = File(logDir, "spring-boot.out.log")
            val errLog = File(logDir, "spring-boot.err.log")
            if (outLog.exists()) outLog.delete()
            if (errLog.exists()) errLog.delete()

            val dbPassword = localOrEnv("noteshareDbPassword", "DB_PASSWORD")
            val jwtSecret = localOrEnv("noteshareJwtSecret", "JWT_SECRET")
            if (dbPassword == null || jwtSecret == null) {
                println("WARNING: Missing noteshareDbPassword/noteshareJwtSecret in local.properties or DB_PASSWORD/JWT_SECRET environment variables.")
                return@doLast
            }

            println("Starting NoteShare backend in $serverDir using MySQL configuration...")
            val backendProcess = ProcessBuilder("cmd.exe", "/c", "mvnw.cmd", "spring-boot:run")
            backendProcess.environment()["DB_PASSWORD"] = dbPassword
            backendProcess.environment()["JWT_SECRET"] = jwtSecret
            backendProcess.environment()["JPA_DDL_AUTO"] =
                localOrEnv("noteshareJpaDdlAuto", "JPA_DDL_AUTO") ?: "update"
            backendProcess
                .directory(serverDir)
                .redirectOutput(outLog)
                .redirectError(errLog)
                .start()

            println("Waiting for backend to be ready on 127.0.0.1:$port (timeout 90s)...")
            val deadline = System.currentTimeMillis() + 90_000
            while (!isServerReady() && System.currentTimeMillis() < deadline) {
                Thread.sleep(2000)
                println("Still waiting...")
            }
        } else {
            println("NoteShare backend is already running on 127.0.0.1:$port")
        }

        if (!isServerReady()) {
            println("WARNING: NoteShare backend did not fully start. The app will show network errors until the server is available.")
        } else {
            println("NoteShare backend is ready.")
        }
    }
}

val adbReverseDebug by tasks.registering {
    group = "noteshare"
    description = "Forward tcp:8200 to the host for every connected debug device."
    dependsOn(ensureNoteshareServer)
    outputs.upToDateWhen { false }

    doLast {
        val adbName = if (System.getProperty("os.name").startsWith("Windows")) "adb.exe" else "adb"
        val adbExecutable = android.sdkDirectory.resolve("platform-tools/$adbName")
        if (!adbExecutable.exists()) {
            println("WARNING: adb not found at $adbExecutable")
            return@doLast
        }

        fun runAdb(vararg args: String): Pair<Int, String> {
            val process = ProcessBuilder(adbExecutable.absolutePath, *args)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            return exitCode to output
        }

        val (devicesExitCode, devicesOutput) = runAdb("devices")
        if (devicesExitCode != 0) {
            println("WARNING: Unable to list Android devices:\n$devicesOutput")
            return@doLast
        }

        val devices = devicesOutput
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("List of devices") }
            .mapNotNull { line ->
                val parts = line.split(Regex("\\s+"))
                if (parts.size >= 2 && parts[1] == "device") parts[0] else null
            }
            .toList()

        if (devices.isEmpty()) {
            println("No online Android devices found; skipping adb reverse.")
            return@doLast
        }

        devices.forEach { serial ->
            val (reverseExitCode, reverseOutput) = runAdb("-s", serial, "reverse", "tcp:8200", "tcp:8200")
            if (reverseExitCode == 0) {
                println("Configured adb reverse for $serial: tcp:8200 -> tcp:8200")
            } else {
                println("WARNING: Failed to configure adb reverse for $serial:\n$reverseOutput")
            }
        }
    }
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
    implementation(libs.google.material)

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

    // Testing - Unit Tests
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.robolectric:robolectric:4.12.2")
    testImplementation("androidx.test:core:1.6.1")

    // Testing - Instrumented Tests
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
