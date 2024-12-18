plugins {
	kotlin("jvm") version "2.0.20"
	kotlin("plugin.serialization") version "2.0.20"
}

repositories {
	mavenCentral()
	maven {
		url = uri("https://plugins.gradle.org/m2/")
	}
	maven {
		url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
	}
    maven {
		url = uri("https://repo.mikeprimm.com/")
	}
	maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
	implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.20")
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
	compileOnly("me.clip:placeholderapi:2.11.6")
    // compileOnly("us.dynmap:dynmap-api:3.4-beta-3")
    // compileOnly("us.dynmap:DynmapCoreAPI:3.4")
	implementation("io.github.agrevster:pocketbase-kotlin:2.6.3")
}

tasks.jar {
	manifest {
		attributes["Main-Class"] = "com.danidipp.sneakymail"
	}
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
	from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

configure<JavaPluginExtension> {
	sourceSets {
		main {
			java.srcDir("src/main/kotlin")
			resources.srcDir(file("src/resources"))
		}
	}
}
