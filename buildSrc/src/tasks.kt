package rs.pest.build

import asmble.cli.Command
import asmble.cli.Compile
import asmble.util.Logger
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader
import java.io.File

open class CompileWasm : DefaultTask() {
	@field:Input var buildType = "debug"
	@field:Input var classQualifiedName = "WasmClasses"
	@field:InputDirectory var cargoProject = ""
	@field:OutputDirectory var generatedDir = "build/wasm-classes"

	init {
		group = "build setup"
	}

	fun rustWasmFileName() = File(cargoProject)
		.resolve("target")
		.resolve("wasm32-unknown-unknown")
		.resolve(buildType)
		.also { println("Wasm Directory: ${it.absolutePath}") }
		.listFiles { _, name -> name.endsWith(".wasm") }
		.filterNotNull()
		.also { if (it.size != 1) throw GradleException("Expected only one .wasm file, got: $it") }
		.first()
		.absolutePath

	@TaskAction
	fun compile() {
		val logger = Logger.Print(Logger.Level.WARN)
		val command = Compile()
		command.logger = logger
		val outFile = classQualifiedName.split('.').fold(File(generatedDir), File::resolve)
		outFile.parentFile.mkdirs()
		val classFilePath = outFile.absolutePath + ".class"
		val args = listOf(rustWasmFileName(), classQualifiedName, "-out", classFilePath)
		command.runWithArgs(Command.ArgsBuilder.ActualArgBuilder(args))
		println("Generated class: $classFilePath")
	}
}
