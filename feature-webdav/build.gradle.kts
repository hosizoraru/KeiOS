plugins {
    id("keios.android.library")
}

android {
    namespace = "os.kei.feature.webdav"

}

dependencies {
    implementation(project(":core-concurrency"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(platform(libs.ktor.bom))
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.cio)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit4)
    testImplementation(libs.ktor.client.mock)
    // Test service metadata selects kxml2's parser and serializer instead of xmlpull-min's
    // unavailable MXSerializer. Android supplies its own XmlPullParser.
    testImplementation(libs.xmlpull)
    testRuntimeOnly(libs.kxml2)

    // Retained for directory browsing; known-file synchronization uses the direct transport.
    // Preserve server href encoding; decoded names are for display, not URL reconstruction.
    // Exclude xpp3 — Android has built-in XmlPullParser.
    implementation(libs.dav4jvm) {
        exclude(group = "org.ogce", module = "xpp3")
    }
}
