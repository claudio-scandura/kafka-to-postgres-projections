import org.gradle.api.tasks.testing.logging.*

plugins {
    application
    java
    id("io.freefair.lombok") version "5.2.1"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    jcenter()
    mavenCentral()
}

val akkaVersion = "2.6.10"

dependencies {
    implementation("com.typesafe:config:1.4.1")
    implementation("com.mylaesoftware:config-composer:0.0.4")
    annotationProcessor("com.mylaesoftware:config-composer-processor:0.0.4")
    implementation("com.typesafe.akka:akka-actor-typed_2.13:$akkaVersion")
    implementation("com.typesafe.akka:akka-stream_2.13:$akkaVersion")
    implementation("com.typesafe.akka:akka-stream-kafka_2.13:2.0.7")
    implementation("com.lightbend.akka:akka-projection-core_2.13:1.1.0")
    implementation("com.lightbend.akka:akka-projection-kafka_2.13:1.1.0")
    implementation("com.lightbend.akka:akka-projection-jdbc_2.13:1.1.0")
    implementation("org.postgresql:postgresql:42.2.19")
    implementation("org.flywaydb:flyway-core:7.5.3")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("com.typesafe.akka:akka-slf4j_2.13:$akkaVersion")
    implementation("com.zaxxer:HikariCP:4.0.2")

    testImplementation("org.mockito:mockito-core:3.9.0")
    testImplementation("org.mockito:mockito-junit-jupiter:3.9.0")
    testImplementation("com.typesafe.akka:akka-actor-testkit-typed_2.13:$akkaVersion")
    testImplementation("com.typesafe.akka:akka-stream-testkit_2.13:$akkaVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.1")
    testImplementation("org.assertj:assertj-core:3.19.0")
    testImplementation("org.testcontainers:junit-jupiter:1.15.2")
    testImplementation("org.testcontainers:postgresql:1.15.2")
    testImplementation("org.testcontainers:kafka:1.15.2")
    testImplementation("org.awaitility:awaitility:4.0.3")
}

application {
    mainClassName = "com.mylaesoftware.App"
}

tasks.withType<Test> {
    useJUnitPlatform()

    outputs.upToDateWhen { false }

    testLogging {
        lifecycle {
            events = mutableSetOf(TestLogEvent.FAILED, TestLogEvent.PASSED, TestLogEvent.SKIPPED)
            exceptionFormat = TestExceptionFormat.FULL
            showExceptions = true
            showCauses = true
            showStackTraces = true
            showStandardStreams = true
        }
        info.events = lifecycle.events
        info.exceptionFormat = lifecycle.exceptionFormat
    }

    addTestListener(object : TestListener {
        override fun beforeSuite(suite: TestDescriptor) {}
        override fun beforeTest(testDescriptor: TestDescriptor) {}
        override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {}

        override fun afterSuite(suite: TestDescriptor, result: TestResult) {
            if (suite.parent == null) {
                logger.lifecycle("----")
                logger.lifecycle("Test result: ${result.resultType}")
                logger.lifecycle("Test summary: ${result.testCount} tests, " +
                        "${result.successfulTestCount} succeeded, " +
                        "${result.failedTestCount} failed, " +
                        "${result.skippedTestCount} skipped")
            }
        }
    })

}