import de.undercouch.gradle.tasks.download.Download
import org.jetbrains.grammarkit.tasks.GenerateLexer
import org.jetbrains.grammarkit.tasks.GenerateParser
import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.*
import java.nio.file.Paths

val isCI = !System.getenv("CI").isNullOrBlank()
val commitHash = Runtime.getRuntime().exec("git rev-parse --short HEAD").run {
	waitFor()
	val output = inputStream.use { inputStream.use { it.readBytes().let(::String) } }
	destroy()
	output.trim()
}

val pluginComingVersion = "0.4.0"
val pluginVersion = if (isCI) "$pluginComingVersion-$commitHash" else pluginComingVersion
val packageName = "rs.pest"
val asmble = "asmble"
val rustTarget = projectDir.resolve("rust").resolve("target")

group = packageName
version = pluginVersion

plugins {
	// Java support
	java
	// Kotlin support
	kotlin("jvm") version "2.1.0"
	// https://github.com/JetBrains/gradle-intellij-plugin
	id("org.jetbrains.intellij.platform") version "2.2.1"
	// https://github.com/JetBrains/gradle-changelog-plugin
	id("org.jetbrains.changelog") version "2.2.1"
	// https://github.com/JetBrains/gradle-grammar-kit-plugin
	id("org.jetbrains.grammarkit") version "2022.3.2.2"
}

// Configure project's dependencies
repositories {
  mavenCentral()
  intellijPlatform.defaultRepositories()
}

intellijPlatform.pluginConfiguration {
  name = "IntelliJ Pest"
}

intellij {
	if (!isCI) setPlugins("PsiViewer:201.6251.22-EAP-SNAPSHOT.3")
	else version = "2020.1"
	setPlugins("org.rust.lang:251.23774.445", "org.toml.lang:0.2.120.37-193", "java")
}

java {
  withSourcesJar()
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(21))
  }
}

tasks.patchPluginXml {
	changeNotes(file("res/META-INF/change-notes.html").readText())
	pluginDescription(file("res/META-INF/description.html").readText())
	version(pluginVersion)
	pluginId(packageName)
}

sourceSets {
	main {
		withConvention(KotlinSourceSet::class) {
			listOf(java, kotlin).forEach { it.srcDirs("src", "gen") }
		}
		resources.srcDirs("res", rustTarget.resolve("java").absolutePath)
	}

	test {
		withConvention(KotlinSourceSet::class) {
			listOf(java, kotlin).forEach { it.srcDirs("test") }
		}
		resources.srcDirs("testData")
	}
}

repositories {
	mavenCentral()
	jcenter()
}

dependencies {
	implementation(kotlin("stdlib-jdk8"))
	implementation("org.eclipse.mylyn.github", "org.eclipse.egit.github.core", "2.1.5") {
		exclude(module = "gson")
	}
	implementation("org.jetbrains.kotlinx", "kotlinx-html-jvm", "0.7.1") {
		exclude(module = "kotlin-stdlib")
	}
	implementation(files("$projectDir/rust/target/java"))
	testImplementation(kotlin("test-junit"))
	testImplementation("junit", "junit", "4.12")
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

val downloadAsmble = task<Download>("downloadAsmble") {
	group = asmble
	src("https://github.com/pest-parser/intellij-pest/files/3592625/asmble.zip")
	dest(buildDir.absolutePath)
	overwrite(false)
}

val unzipAsmble = task<Copy>("unzipAsmble") {
	group = asmble
	dependsOn(downloadAsmble)
	from(zipTree(buildDir.resolve("asmble.zip")))
	into(buildDir)
}

val wasmFile = rustTarget
	.resolve("wasm32-unknown-unknown")
	.resolve("release")
	.resolve("pest_ide.wasm")
val wasmGcFile = wasmFile.resolveSibling("pest_ide_gc.wasm")

val asmbleExe by lazy {
	buildDir
		.resolve("asmble")
		.resolve("bin")
		.resolve(if (System.getProperty("os.name").startsWith("Windows")) "asmble.bat" else "asmble")
}

val compileRust = task<Exec>("compileRust") {
	val rustDir = projectDir.resolve("rust")
	workingDir(rustDir)
	inputs.dir(rustDir.resolve("src"))
	outputs.dir(rustDir.resolve("target"))
	commandLine("rustup", "run", "nightly", "cargo", "build", "--release")
}

val gcWasm = task/*<Exec>*/("gcWasm") {
	dependsOn(compileRust)
/* Asmble is broken for GCed wasm.
	workingDir(projectDir)
	inputs.file(wasmFile)
	outputs.file(wasmGcFile)
	commandLine("wasm-gc", wasmFile.absolutePath, wasmGcFile.absolutePath)
*/
	doFirst { wasmFile.copyTo(wasmGcFile, overwrite = true) }
}

val translateWasm = task<Exec>("translateWasm") {
	group = asmble
	dependsOn(unzipAsmble, gcWasm)
	workingDir(projectDir)
	val path = wasmGcFile.also { inputs.file(it) }.absolutePath
	val outPath = "${path.dropLast(1)}t".also { outputs.file(it) }
	commandLine(asmbleExe, "translate", path, outPath, "-out", "wast")
	doFirst { println("Output file: $outPath") }
}

val compileWasm = task<Exec>("compileWasm") {
	group = asmble
	dependsOn(unzipAsmble, gcWasm)
	workingDir(projectDir.absolutePath)
	val classRelativePath = listOf("rs", "pest", "vm", "PestUtil.class")
	val outFile = classRelativePath
		.fold(rustTarget.resolve("java"), File::resolve)
		.apply { parentFile.mkdirs() }
		.also { outputs.file(it) }
		.absolutePath
	val wasmGcFile = wasmGcFile.also { inputs.file(it) }.absolutePath
	val cls = classRelativePath.joinToString(separator = ".").removeSuffix(".class")
	commandLine(asmbleExe, "compile", wasmGcFile, cls, "-out", outFile)
	doFirst {
		println("Input file: $wasmGcFile")
		println("Output file: $outFile")
	}
}

fun path(more: Iterable<*>) = more.joinToString(File.separator)

val genParser = task<GenerateParser>("genParser") {
	group = tasks["init"].group!!
	description = "Generate the Parser and PsiElement classes"
	source = "grammar/pest.bnf"
	targetRoot = "gen/"
	val parserRoot = Paths.get("rs", "pest")
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
	dependsOn(genParser, genLexer, compileWasm)
	kotlinOptions {
		jvmTarget = "1.8"
		languageVersion = "1.3"
		apiVersion = "1.3"
		freeCompilerArgs = listOf("-Xjvm-default=enable")
	}
}
