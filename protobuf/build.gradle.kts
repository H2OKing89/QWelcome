plugins {
    kotlin("jvm")
    id("com.google.protobuf") version "0.9.4"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("androidx.datastore:datastore-core:1.2.0")
    implementation("com.google.protobuf:protobuf-kotlin:4.27.2")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.27.2"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                java { option("lite") }
                kotlin { option("lite") }
            }
        }
    }
}
