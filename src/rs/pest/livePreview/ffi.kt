package rs.pest.livePreview

import rs.pest.vm.PestUtil
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

class Lib(private var native: PestUtil) {
	/**
	 * @return (true, rule names) or (false, error messages)
	 */
	fun loadVM(pestCode: String): Pair<Boolean, Sequence<String>> {
		val pestCodePtr = ptrFromString(pestCode)
		val returned = native.load_vm(pestCodePtr.offset, pestCodePtr.size)
		val str = nullTermedStringFromOffset(returned)
		val isError = str.startsWith("Err")
		return !isError to str
			.removePrefix("Err")
			.removeSurrounding(prefix = "[", suffix = "]")
			.splitToSequence(',')
			.map { it.trim() }
			.map { it.removeSurrounding(prefix = "\"", suffix = "\"") }
	}

	fun reboot(newMemory: ByteBuffer = native.memory.duplicate()) {
		native = PestUtil(newMemory)
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
		val r = InputStreamReader(object : InputStream() {
			@Throws(IOException::class)
			override fun read() = if (!memory.hasRemaining()) -1 else memory.get().toInt() and 0xFF
		}, StandardCharsets.UTF_8)
		val builder = StringBuilder()
		while (true) {
			val c = r.read()
			if (c <= 0) break
			builder.append(c.toChar())
		}

		native.dealloc(offset, memory.position() - offset)
		return builder.toString()
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
