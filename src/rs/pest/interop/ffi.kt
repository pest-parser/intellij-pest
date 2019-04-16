package rs.pest.interop

import org.jetbrains.annotations.TestOnly
import rs.pest.vm.PestUtil
import java.nio.charset.StandardCharsets

class Lib(private val native: PestUtil) {
	@TestOnly
	fun stringLength(str: String): Int {
		val strPtr = ptrFromString(str)
		return native.string_len(strPtr.offset, strPtr.size)
	}

	@TestOnly
	fun prependFromRust(str: String): String {
		val strPtr = ptrFromString(str)
		val nullTermOffset = native.prepend_from_rust(strPtr.offset, strPtr.size)
		return nullTermedStringFromOffset(nullTermOffset)
	}

	fun loadVM(pestCode: String): String {
		val pestCodePtr = ptrFromString(pestCode)
		val returned = native.load_vm(pestCodePtr.offset, pestCodePtr.size)
		return nullTermedStringFromOffset(returned)
	}

	fun renderCode(ruleName: String, userCode: String): String {
		val ruleNamePtr = ptrFromString(ruleName)
		val userCodePtr = ptrFromString(userCode)
		val returned = native.render_code(
			ruleNamePtr.offset, ruleNamePtr.size,
			userCodePtr.offset, userCodePtr.size)
		return nullTermedStringFromOffset(returned)
	}

	private fun ptrFromString(str: String): Ptr {
		val bytes = str.toByteArray(StandardCharsets.UTF_8)
		val ptr = Ptr(bytes.size)
		ptr.put(bytes)
		return ptr
	}

	private fun nullTermedStringFromOffset(offset: Int): String {
		val memory = native.memory
		memory.position(offset)
		// We're going to turn the mem into an input stream. This is the
		//  reasonable way to stream a UTF8 read using standard Java libs
		//  that I could find.
		val str = buildString {
			while (memory.hasRemaining()) append(memory.get().toInt() and 0xFF)
		}

		native.dealloc(offset, memory.position() - offset)
		return str
	}

	internal inner class Ptr(val offset: Int, val size: Int) {
		constructor(size: Int) : this(native.alloc(size), size)

		fun put(bytes: ByteArray) {
			// Yeah, yeah, not thread safe
			val memory = native.memory
			memory.position(offset)
			memory.put(bytes)
		}

		@Throws(Throwable::class)
		protected fun finalize() {
			native.dealloc(offset, size)
		}
	}
}
