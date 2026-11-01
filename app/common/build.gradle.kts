plugins {
    id("simplesoftware.android.library")
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.simplesoftware.browser.common"
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    testImplementation(libs.junit)
}
