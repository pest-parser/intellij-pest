package rs.pest

import org.junit.Test
import rs.pest.interop.Lib
import rs.pest.interop.PestCodes
import rs.pest.vm.PestUtil
import kotlin.random.Random
import kotlin.test.assertEquals

class InteractionTest {
	@Test
	fun connectivity() {
		val random = Random(System.currentTimeMillis())
		val instance = PestUtil(1919810)
		repeat(10) {
			val a = random.nextInt()
			val b = random.nextInt()
			assertEquals(a + b, instance.connectivity_check_add(a, b))
		}
	}

	@Test
	fun textLen() {
		val random = Random(System.currentTimeMillis())
		val instance = Lib(PestUtil(1919810))
		repeat(10) {
			val a = random.nextDouble().toString()
			assertEquals(a.length, instance.stringLength(a))
		}
	}

	@Test
	fun textPrepend() {
		val random = Random(System.currentTimeMillis())
		val instance = Lib(PestUtil(114514 * 514))
		repeat(3) {
			val a = random.nextDouble().toString()
			assertEquals("From Rust: $a", instance.prependFromRust(a))
		}
	}
}

class IntegrationTest {
	@Test
	fun loadVM() {
		var lib = Lib(PestUtil(1919810))
		lib.
	}
}
