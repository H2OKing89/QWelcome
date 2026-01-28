plugins {
    `java-library`
    id("com.google.protobuf")
}

val protobufVersion = "3.25.3"

dependencies {
    api("com.google.protobuf:protobuf-javalite:$protobufVersion")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
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
