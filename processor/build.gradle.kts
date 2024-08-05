dependencies {
    implementation(project(":hardware"))
    implementation(project(":common"))
    testImplementation(project(":common", configuration = "testArtifacts"))
}
