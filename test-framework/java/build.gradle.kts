import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsTask
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.Pmd
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    id("java")
    id("checkstyle")
    id("pmd")
    id("jacoco")
    id("com.github.spotbugs") version "6.5.8"
}

group = "com.streaminglab.testframework"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

checkstyle {
    toolVersion = "13.7.0"
    configDirectory.set(rootProject.layout.projectDirectory.dir("../../config/checkstyle"))
}

pmd {
    toolVersion = "7.24.0"
    isConsoleOutput = true
    rulesMinimumPriority = 5
    ruleSetFiles = files(rootProject.layout.projectDirectory.file("../../config/pmd/ruleset.xml"))
    ruleSets = emptyList()
}

jacoco {
    toolVersion = "0.8.15"
}

spotbugs {
    toolVersion = "4.10.2"
    effort = Effort.MAX
    reportLevel = Confidence.MEDIUM
    excludeFilter.set(rootProject.layout.projectDirectory.file("../../config/spotbugs/exclude.xml"))
}

val bddTest = sourceSets.create("bddTest") {
    java.srcDir("src/bddTest/java")
    resources.srcDir("src/bddTest/resources")

    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += output + compileClasspath
}

configurations[bddTest.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())
configurations[bddTest.runtimeOnlyConfigurationName].extendsFrom(configurations.testRuntimeOnly.get())


dependencies {
    // Framework implementation dependencies
    // yaml parser
    // Source: https://mvnrepository.com/artifact/org.yaml/snakeyaml
    implementation("org.yaml:snakeyaml:2.6")
    // Source: https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind
    implementation("com.fasterxml.jackson.core:jackson-databind:2.22.0")
    // Source: https://mvnrepository.com/artifact/com.microsoft.playwright/playwright
    implementation("com.microsoft.playwright:playwright:1.61.0")

    // Unit test dependencies
    testImplementation(platform("org.junit:junit-bom:6.1.1"))
    testImplementation(platform("org.assertj:assertj-bom:3.27.7"))
    testImplementation(platform("org.mockito:mockito-bom:5.20.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // BDD/Cucumber dependencies
    add(bddTest.implementationConfigurationName, platform("org.junit:junit-bom:6.1.1"))
    add(bddTest.implementationConfigurationName, platform("io.cucumber:cucumber-bom:7.34.4"))
    add(bddTest.implementationConfigurationName, platform("org.assertj:assertj-bom:3.27.7"))

    add(bddTest.implementationConfigurationName, "org.junit.platform:junit-platform-suite")
    add(bddTest.implementationConfigurationName, "io.cucumber:cucumber-java")
    add(bddTest.implementationConfigurationName, "io.cucumber:cucumber-junit-platform-engine")
    add(bddTest.implementationConfigurationName, "io.cucumber:cucumber-picocontainer")
    add(bddTest.implementationConfigurationName, "org.assertj:assertj-core")

    add(bddTest.runtimeOnlyConfigurationName, "org.junit.platform:junit-platform-launcher")
}

tasks.register<JavaExec>("playwrightInstall") {
    group = "verification"
    description = "Installs Playwright browser binaries."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.microsoft.playwright.CLI")
    args("install", "chromium")
}

// sets location of cucumber features
tasks.named<ProcessResources>("processBddTestResources") {
    from("../features") {
        into("features")
    }
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)

    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.withType<Checkstyle>().configureEach {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.withType<Pmd>().configureEach {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.withType<SpotBugsTask>().configureEach {
    reports {
        create("xml") {
            required.set(true)
        }
        create("html") {
            required.set(true)
        }
    }
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.test)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.45".toBigDecimal()
            }
        }
        rule {
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.40".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

tasks.register("qualityCheck") {
    group = "verification"
    description = "Runs static analysis, unit tests, and coverage verification."
    dependsOn(tasks.check)
    dependsOn(tasks.jacocoTestReport)
}

tasks.register<Test>("bddTest") {
    description = "Runs shared BDD scenarios using the Java test-framework implementation."
    group = "verification"

    testClassesDirs = bddTest.output.classesDirs
    classpath = bddTest.runtimeClasspath

    useJUnitPlatform {
        System.getProperty("cucumber.features")?.let {
            includeEngines("cucumber")
        }
    }

    System.getProperty("cucumber.features")?.let {
        systemProperty("cucumber.features", it)
    }

    System.getProperty("cucumber.filter.tags")?.let {
        systemProperty("cucumber.filter.tags", it)
    }

    System.getProperty("cucumber.filter.name")?.let {
        systemProperty("cucumber.filter.name", it)
    }

    System.getProperty("cucumber.plugin")?.let {
        systemProperty("cucumber.plugin", it)
    }

    systemProperty("cucumber.junit-platform.naming-strategy", "long")

    systemProperty(
        "test.framework.profile",
        System.getProperty("test.framework.profile", "local")
    )

    testLogging {
        events("passed", "skipped", "failed")
    }
}
