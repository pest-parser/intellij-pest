import de.undercouch.gradle.tasks.download.Download
import org.jetbrains.grammarkit.tasks.GenerateLexer
import org.jetbrains.grammarkit.tasks.GenerateParser
import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.*
import java.nio.file.Paths

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

val pluginComingVersion = "0.2.7"
val pluginVersion = if (isCI) "$pluginComingVersion-$commitHash" else pluginComingVersion
val packageName = "rs.pest"
val asmble = "asmble"
val rustTarget = projectDir.resolve("rust").resolve("target")

group = packageName
version = pluginVersion

plugins {
	java
	id("org.jetbrains.intellij") version "0.4.10"
	id("org.jetbrains.grammarkit") version "2019.2"
	id("de.undercouch.download") version "3.4.3"
	kotlin("jvm") version "1.3.50"
}

allprojects { apply { plugin("org.jetbrains.grammarkit") } }

fun fromToolbox(root: String, ide: String) = file(root)
	.resolve(ide)
	.takeIf { it.exists() }
	?.resolve("ch-0")
	?.listFiles()
	.orEmpty()
	.filterNotNull()
	.filter { it.isDirectory }
	.maxBy {
		val (major, minor, patch) = it.name.split('.')
		String.format("%5s%5s%5s", major, minor, patch)
	}
	?.also { println("Picked: $it") }

intellij {
	updateSinceUntilBuild = false
	instrumentCode = true
	val user = System.getProperty("user.name")
	val os = System.getProperty("os.name")
	val root = when {
		os.startsWith("Windows") -> "C:\\Users\\$user\\AppData\\Local\\JetBrains\\Toolbox\\apps"
		os == "Linux" -> "/home/$user/.local/share/JetBrains/Toolbox/apps"
		else -> return@intellij
	}
	val intellijPath = sequenceOf("IDEA-C-JDK11", "IDEA-C", "IDEA-JDK11", "IDEA-U")
		.mapNotNull { fromToolbox(root, it) }.firstOrNull()
	intellijPath?.absolutePath?.let { localPath = it }
	val pycharmPath = sequenceOf("PyCharm-C", "IDEA-C-JDK11", "IDEA-C", "IDEA-JDK11", "IDEA-U")
		.mapNotNull { fromToolbox(root, it) }.firstOrNull()
	pycharmPath?.absolutePath?.let { alternativeIdePath = it }

	if (!isCI) setPlugins("PsiViewer:192-SNAPSHOT")
	setPlugins("org.rust.lang:0.2.105.2133-192", "java")
}

java {
	sourceCompatibility = JavaVersion.VERSION_1_8
	targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<PatchPluginXmlTask> {
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

repositories { mavenCentral() }

dependencies {
	compile(kotlin("stdlib-jdk8"))
	compile("org.eclipse.mylyn.github", "org.eclipse.egit.github.core", "2.1.5") {
		exclude(module = "gson")
	}
	compile(files("$projectDir/rust/target/java"))
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
