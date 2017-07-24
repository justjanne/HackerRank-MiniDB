plugins {
    kotlin("jvm")
    application
}

application {
    mainClassName = "MainKt"
}

repositories {
    jcenter()
}

dependencies {
    compile(kotlin("stdlib"))
    testCompile(kotlin("test-junit"))
}