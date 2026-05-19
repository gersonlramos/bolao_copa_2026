// Feature: bolao-copa-2026, Property 4: Validação de intervalo de gols e pontos

plugins {
    kotlin("jvm")
}

dependencies {
    implementation("javax.inject:javax.inject:1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-property:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
