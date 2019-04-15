import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

group = "rs.pest.build"
version = "999"

plugins { kotlin("jvm") version "1.3.30" }
sourceSets {
	main {
		withConvention(KotlinSourceSet::class) {
			listOf(java, kotlin).forEach { it.srcDirs("src") }
		}
		resources.srcDirs()
	}

	test {
		withConvention(KotlinSourceSet::class) {
			listOf(java, kotlin, resources).forEach { it.srcDirs() }
		}
	}
}
repositories { jcenter() }
dependencies {
	compile(kotlin("stdlib-jdk8"))
	compile(kotlin("reflect"))
	compile("com.github.cretz.asmble:asmble-compiler:0.3.0") {
		exclude(module = "kotlin-stdlib")
		exclude(module = "kotlin-reflect")
	}
}
