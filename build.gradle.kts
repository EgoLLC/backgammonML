import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    application
}

group = "art.vilolon"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {

    implementation("org.deeplearning4j:deeplearning4j:1.0.0-M2.1")

    implementation("org.deeplearning4j:rl4j:1.0.0-M1.1")
    implementation("org.deeplearning4j:rl4j-core:1.0.0-M1.1")
//    implementation("org.deeplearning4j:resources:1.0.0-M2.1")
    implementation("org.deeplearning4j:rl4j-api:1.0.0-M2.1")
//    implementation("org.deeplearning4j:deeplearning4j-ui:1.0.0-M2.1")

//    implementation("org.nd4j:nd4j-native:1.0.0-M2.1:linux-x86_64")
    implementation("org.nd4j:nd4j-native:1.0.0-M2.1:macosx-x86_64")
    implementation("org.nd4j:nd4j-native-platform:1.0.0-M2.1")

    implementation("org.nd4j:nd4j-cuda-11.6:1.0.0-M2.1") {
        exclude("nd4j-cuda-11.6-1.0.0-M2.1-windows-x86_64")
    }
    implementation("org.nd4j:nd4j-cuda-11.6:1.0.0-M2.1") {
        exclude("nd4j-cuda-11.6-1.0.0-M2.1-windows-x86_64")
    }
    implementation("org.nd4j:nd4j-cuda-11.6-platform:1.0.0-M2.1")
    implementation("org.nd4j:nd4j-cuda-11.6-preset:1.0.0-M2.1")
//
//    implementation("org.bytedeco:cuda-platform-redist:11.6-8.3-1.5.7:linux-x86_64-redist")
//    implementation("org.bytedeco:javacpp:1.5.7")
//    implementation("org.bytedeco:javacpp-platform:1.5.7")
//    implementation("org.bytedeco:openblas:0.3.26-1.5.7")
//    implementation("org.bytedeco:openblas-platform:0.3.26-1.5.7")

//    implementation("org.bytedeco:opencv:4.9.0-1.5.10")
//    implementation("org.bytedeco:opencv-platform:")
//    implementation("org.bytedeco:javacv:1.5.10")
//    implementation("org.bytedeco:javacv-platform:1.5.10")
//    implementation("org.bytedeco:hdf5:1.12.2-1.5.8")
//    implementation("org.bytedeco:hdf5-platform:1.12.1-1.5.7")
//    implementation("org.bytedeco:leptonica-platform:1.82.0-1.5.7")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.3")

    implementation("org.apache.logging.log4j:log4j-api:2.17.2")
    implementation("org.apache.logging.log4j:log4j-core:2.17.2")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.17.2")

}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

application {
    mainClass.set("MainKt")
}