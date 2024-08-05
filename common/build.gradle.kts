dependencies {}

tasks.register<Jar>("testJar") {
    from(sourceSets.test.get().output)
    archiveClassifier.set("tests")
}

configurations {
    create("testArtifacts") {
        extendsFrom(configurations.testImplementation.get())
    }
}

artifacts {
    add("testArtifacts", tasks.named("testJar"))
}
