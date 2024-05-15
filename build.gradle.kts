import io.spartan.dependency.Libraries
import org.gradle.api.tasks.wrapper.Wrapper.DistributionType.ALL

plugins {
    id("groovy")
}

group = "io.spartan"
version = "1.0"

repositories {
    mavenCentral()
    maven(url = "https://repo.jenkins-ci.org/releases/")
    maven(url = "https://repo.jenkins-ci.org/public/")
}

dependencies {
    // groovy 3 is only supported by spock 2
    implementation(Libraries.Groovy.GROOVY_ALL)
    implementation(Libraries.Apache.COMMONS_LANG3)

    testImplementation(Libraries.TestLib.SERVLET_API)
    // spock 2 has only milestone releases yet
    testImplementation(Libraries.TestLib.SPOCK_CORE)
    testImplementation(Libraries.TestLib.REFLECTIONS)

    // dependencies to make non-interfaces mockable
    testImplementation(Libraries.TestLib.CGLIB)
    testImplementation(Libraries.TestLib.OBJENESIS)
}

sourceSets.main {
    groovy.srcDirs("src")
    resources.srcDirs("resources")
}

sourceSets.test {
    groovy.srcDirs("test")
}

java {
    withSourcesJar()
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
    }
}

tasks {
    wrapper {
        description = "Update gradle wrapper to specified version."
        gradleVersion = "8.4"
        distributionType = ALL
    }
}
