package rs.pest

import com.intellij.testFramework.ParsingTestCase
import org.rust.lang.core.parser.RustParserDefinition

class ParsingTest : ParsingTestCase("parse", "pest", PestParserDefinition()) {
	override fun getTestDataPath() = "testData"
	fun testNestedComment() = doTest(true)
	fun testSimpleRule() = doTest(true)
	fun testChar() = doTest(true)
	fun testBuiltins() = doTest(true)
	fun testParen() = doTest(true)
}

/// To inspect Rust plugin's parsed output.
class RustAstStructureTest : ParsingTestCase("rust", "rs", RustParserDefinition()) {
	override fun getTestDataPath() = "testData"
	fun testExternal() = doTest(true)
	fun testInline() = doTest(true)
}
