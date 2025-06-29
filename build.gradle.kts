import org.gradle.internal.os.OperatingSystem

plugins {
    application
    id("org.openjfx.javafxplugin") version "0.0.14"
    id("org.beryx.runtime") version "1.12.7"
}

application {
    mainClass.set("com.ibrasoft.jdriveclonr.Launcher")
    applicationName = "jDriveClonr"
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml")
}

repositories {
    mavenCentral()
}

dependencies {
    // Google API Client
    implementation("com.google.api-client:google-api-client:1.35.2")
    
    // Google OAuth
    implementation("com.google.oauth-client:google-oauth-client:1.34.1")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.25.0")
    
    // Google Guava
    implementation("com.google.guava:guava:33.4.8-jre")
    
    // Google Drive API
    implementation("com.google.apis:google-api-services-drive:v3-rev197-1.25.0")
    
    // Google Photos API
    implementation("com.google.photos.library:google-photos-library-client:1.7.2")
    
    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")
    
    // Google Sheets
    implementation("com.google.apis:google-api-services-sheets:v4-rev614-1.18.0-rc")
    
    // Google Slides
    implementation("com.google.apis:google-api-services-slides:v1-rev399-1.25.0")
    
    // Google HTTP Client
    implementation("com.google.http-client:google-http-client:1.43.3")
    implementation("com.google.http-client:google-http-client-gson:1.43.3")
    implementation("com.google.http-client:google-http-client-jackson2:1.43.3")
    
    // SLF4J Logging
    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("org.slf4j:slf4j-simple:2.0.7")
    
    // Test Dependencies
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}

runtime {
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))

    modules.set(
        listOf(
            "java.base",
            "java.desktop",
            "java.logging",
            "java.net.http",
            "java.xml",
            "jdk.crypto.ec",
            "java.security.sasl",
            "java.scripting",
            "jdk.unsupported",
            "jdk.httpserver"
        )
    )

    launcher {
        noConsole = true
    }

    jpackage {
        val currentOs = org.gradle.internal.os.OperatingSystem.current()
        val imgType = when {
            currentOs.isWindows -> "ico"
            currentOs.isMacOsX -> "icns"
            else -> "png"
        }

        imageOptions.addAll(listOf("--icon", "src/main/resources/DriveClonrLogo.$imgType"))
        installerOptions.addAll(listOf("--resource-dir", "src/main/resources"))
        installerOptions.addAll(listOf("--vendor", "IbraSoft"))

        if (currentOs.isWindows) {
            installerOptions.addAll(
                listOf("--win-per-user-install", "--win-dir-chooser", "--win-menu", "--win-shortcut")
            )
        } else if (currentOs.isLinux) {
            installerOptions.addAll(
                listOf("--linux-package-name", "jDriveClonr", "--linux-shortcut")
            )
        } else if (currentOs.isMacOsX) {
            installerOptions.addAll(
                listOf("--mac-package-name", "jDriveClonr")
            )
        }
    }
}
