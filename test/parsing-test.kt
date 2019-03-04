package rs.pest

import com.intellij.testFramework.ParsingTestCase

class ParsingTest : ParsingTestCase("parse", "pest", PestParserDefinition()) {
	override fun getTestDataPath() = "testData"
	fun testNestedComment() = doTest(true)
}
