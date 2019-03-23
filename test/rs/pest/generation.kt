package rs.pest

import org.junit.Test

class CodeGeneration {
	@Test
	fun generateLexer() {
		BUILTIN_RULES.forEach {
			println("$it { return ${it}_TOKEN; }")
		}
	}

	@Test
	fun generateParser() {
		BUILTIN_RULES.forEach {
			println(" | ${it}_TOKEN")
		}
	}
}