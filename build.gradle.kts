plugins {
    java
    id("io.papermc.paperweight.userdev") version "1.6.0"
}

// 配置Java工具链
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

// 配置Java编译选项
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

group = "com.sagecraft"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    paperweight.paperDevBundle("1.20.4-R0.1-SNAPSHOT")
}

tasks.withType<ProcessResources> {
    filesMatching("plugin.yml") {
        expand(
            "version" to project.version
        )
    }
}
