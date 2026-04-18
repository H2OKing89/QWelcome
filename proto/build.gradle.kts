plugins {
    `java-library`
    alias(libs.plugins.protobuf)
}

dependencies {
    api(libs.protobuf.javalite)
}

protobuf {
    protoc {
        // The protobuf plugin requires standard artifact spec format to not trigger gradle deprecations
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    generateProtoTasks {
        all().forEach {
            it.builtins {
                named("java") {
                    option("lite")
                }
            }
        }
    }
}
