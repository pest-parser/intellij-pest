import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

group = "rs.pest.build"
version = "999"

plugins { kotlin("jvm") version "1.3.30" }
sourceSets {
	main {
		withConvention(KotlinSourceSet::class) {
			listOf(java, kotlin).forEach { it.srcDirs("src") }
		}
		resources.srcDirs("res")
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
	compile("com.github.cretz.asmble:asmble-compiler:0.3.0")
	compile("com.github.cretz.asmble:asmble-annotations:0.3.0")
}
