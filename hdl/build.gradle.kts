dependencies {
    
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=hdl.InternalHdlApi")
    }
}