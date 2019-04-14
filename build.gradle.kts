import org.jetbrains.grammarkit.tasks.GenerateLexer
import org.jetbrains.grammarkit.tasks.GenerateParser
import org.gradle.language.base.internal.plugins.CleanRule
import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.*
import java.nio.file.*

val isCI = !System.getenv("CI").isNullOrBlank()
val commitHash = kotlin.run {
	val process: Process = Runtime.getRuntime().exec("git rev-parse --short HEAD")
	process.waitFor()
	@Suppress("RemoveExplicitTypeArguments")
	val output = process.inputStream.use {
		process.inputStream.use {
			it.readBytes().let<ByteArray, String>(::String)
		}
	}
	process.destroy()
	output.trim()
}

val pluginComingVersion = "0.2.2"
val pluginVersion = if (isCI) "$pluginComingVersion-$commitHash" else pluginComingVersion
val packageName = "rs.pest"

group = packageName
version = pluginVersion

plugins {
	java
	id("org.jetbrains.intellij") version "0.4.7"
	id("org.jetbrains.grammarkit") version "2019.1"
	kotlin("jvm") version "1.3.30"
}

fun fromToolbox(path: String) = file(path).listFiles().orEmpty().filter { it.isDirectory }.maxBy {
	val (major, minor, patch) = it.name.split('.')
	String.format("%5s%5s%5s", major, minor, patch)
}

allprojects {
	apply { plugin("org.jetbrains.grammarkit") }

	intellij {
		updateSinceUntilBuild = false
		instrumentCode = true
		val user = System.getProperty("user.name")
		when (System.getProperty("os.name")) {
			"Linux" -> {
				val root = "/home/$user/.local/share/JetBrains/Toolbox/apps"
				val intellijPath = fromToolbox("$root/IDEA-C/ch-0")
					?: fromToolbox("$root/IDEA-U/ch-0")
					?: fromToolbox("$root/IDEA-JDK11/ch-0")
				intellijPath?.absolutePath?.let { localPath = it }
				val pycharmPath = fromToolbox("$root/PyCharm-C/ch-0")
					?: fromToolbox("$root/IDEA-C/ch-0")
					?: fromToolbox("$root/IDEA-JDK11/ch-0")
				pycharmPath?.absolutePath?.let { alternativeIdePath = it }
			}
		}

		setPlugins("org.rust.lang:0.2.96.2122-191")
	}
}

java {
	sourceCompatibility = JavaVersion.VERSION_1_8
	targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<PatchPluginXmlTask> {
	changeNotes(file("res/META-INF/change-notes.html").readText())
	pluginDescription(file("res/META-INF/description.html").readText())
	version(pluginComingVersion)
	pluginId(packageName)
}

sourceSets {
	main {
		withConvention(KotlinSourceSet::class) {
			listOf(java, kotlin).forEach { it.srcDirs("src", "gen") }
		}
		resources.srcDirs("res")
	}

	test {
		withConvention(KotlinSourceSet::class) {
			listOf(java, kotlin).forEach { it.srcDirs("test") }
		}
		resources.srcDirs("testData")
	}
}

repositories { mavenCentral() }

dependencies {
	compile(kotlin("stdlib-jdk8"))
	compile("org.eclipse.mylyn.github", "org.eclipse.egit.github.core", "2.1.5") {
		exclude(module = "gson")
	}
	testCompile(kotlin("test-junit"))
	testCompile("junit", "junit", "4.12")
}

task("displayCommitHash") {
	group = "help"
	description = "Display the newest commit hash"
	doFirst { println("Commit hash: $commitHash") }
}

task("isCI") {
	group = "help"
	description = "Check if it's running in a continuous-integration"
	doFirst { println(if (isCI) "Yes, I'm on a CI." else "No, I'm not on CI.") }
}

fun path(more: Iterable<*>) = more.joinToString(File.separator)

val genParser = task<GenerateParser>("genParser") {
	group = tasks["init"].group!!
	description = "Generate the Parser and PsiElement classes"
	source = "grammar/pest.bnf"
	targetRoot = "gen/"
	val parserRoot = Paths.get("rs", "pest")!!
	pathToParser = path(parserRoot + "PestParser.java")
	pathToPsiRoot = path(parserRoot + "psi")
	purgeOldFiles = true
}

val genLexer = task<GenerateLexer>("genLexer") {
	group = genParser.group
	description = "Generate the Lexer"
	source = "grammar/pest.flex"
	targetDir = path(Paths.get("gen", "rs", "pest", "psi"))
	targetClass = "PestLexer"
	purgeOldFiles = true
	dependsOn(genParser)
}

tasks.withType<KotlinCompile> {
	dependsOn(genParser, genLexer)
	kotlinOptions {
		jvmTarget = "1.8"
		languageVersion = "1.3"
		apiVersion = "1.3"
		freeCompilerArgs = listOf("-Xjvm-default=enable")
	}
}
