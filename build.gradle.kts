import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
    }
}

allprojects {
    group = "io.github.rk012"
    version = "0.1"
}

subprojects {
    repositories {
        mavenCentral()
    }

    apply(plugin = "org.jetbrains.kotlin.jvm")

    val testImplementation by configurations

    dependencies {
        testImplementation(kotlin("test"))
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    configure<KotlinJvmProjectExtension> {
        jvmToolchain(11)
    }
}
