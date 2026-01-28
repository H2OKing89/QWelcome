plugins {
    `java-library`
    alias(libs.plugins.protobuf)
}

dependencies {
    api(libs.protobuf.javalite)
}

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
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
