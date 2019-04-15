package rs.pest

import org.junit.Test
import rs.pest.interop.RustStr
import rs.pest.vm.PestUtil
import kotlin.random.Random
import kotlin.test.assertEquals

class InteractionTest {
	@Test
	fun connectivity() {
		val random = Random(System.currentTimeMillis())
		val instance = PestUtil(114514 * 10)
		repeat(10) {
			val a = random.nextInt()
			val b = random.nextInt()
			assertEquals(a + b, instance.connectivity_check_add(a, b))
		}
	}

	@Test
	fun textLen() {
		val random = Random(System.currentTimeMillis())
		val instance = RustStr(PestUtil(114514 * 10))
		repeat(10) {
			val a = random.nextDouble().toString()
			assertEquals(a.length, instance.stringLength(a))
		}
	}

	@Test
	fun textPrepend() {
		val random = Random(System.currentTimeMillis())
		val instance = RustStr(PestUtil(114514 * 514))
		repeat(3) {
			val a = random.nextDouble().toString()
			assertEquals("From Rust: $a", instance.prependFromRust(a))
		}
	}
}
