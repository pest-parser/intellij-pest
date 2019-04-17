package rs.pest

import org.junit.Test
import rs.pest.livePreview.Lib
import rs.pest.livePreview.Rendering
import rs.pest.vm.PestUtil
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InteractionTest {
	@Test
	fun `connectivity -- a + b problem`() {
		val random = Random(System.currentTimeMillis())
		val instance = PestUtil(1919810)
		repeat(10) {
			val a = random.nextInt()
			val b = random.nextInt()
			assertEquals(a + b, instance.connectivity_check_add(a, b))
		}
	}
}

class IntegrationTest {
	@Test
	fun `load Pest VM`() {
		val lib = Lib(PestUtil(1919810))
		val (works, output) = lib.loadVM("""a = { "Hello" }""")
		assertEquals(listOf("a"), output.toList())
		assertTrue(works)
	}

	@Test
	fun `load Pest VM with multiple rules`() {
		val lib = Lib(PestUtil(1919810))
		val (works, output) = lib.loadVM("""
a = { "Hello" }
b = { a }
""")
		assertEquals(listOf("a", "b"), output.toList())
		assertTrue(works)
	}

	@Test
	fun `load Pest VM with invalid rule name`() {
		val lib = Lib(PestUtil(1919810))
		val (parses, output) = lib.loadVM("""type = { "Hello" }""")
		assertFalse(parses)
		assertEquals(listOf("1^1^1^5^type is a rust keyword"), output.toList())
	}

	@Test
	fun `load Pest VM with syntax error`() {
		val lib = Lib(PestUtil(1919810))
		val (parses, output) = lib.loadVM("""bla = { "Hello }""")
		assertFalse(parses)
		assertEquals(listOf("""1^17^1^17^expected `\"`"""), output.toList())
	}

	@Test
	fun `render simple code in Pest VM`() {
		val lib = Lib(PestUtil(1919810))
		val (parses, output) = lib.loadVM("""bla = { "Dio" }""")
		assertTrue(parses)
		assertEquals(listOf("bla"), output.toList())
		val renderCode = lib.renderCode("bla", "Dio") as Rendering.Ok
		assertEquals(listOf("0^3^bla"), renderCode.lexical.toList())
	}

	@Test
	fun `render syntactically incorrect code in Pest VM`() {
		val lib = Lib(PestUtil(1919810))
		val (parses, output) = lib.loadVM("""bla = { "Hello" }""")
		assertTrue(parses)
		assertEquals(listOf("bla"), output.toList())
		val renderCode = lib.renderCode("bla", "The World!") as Rendering.Err
		assertEquals(""" --> 1:1
  |
1 | The World!
  | ^---
  |
  = expected bla""", renderCode.msg)
	}
}
