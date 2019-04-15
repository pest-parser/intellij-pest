package rs.pest.interop

import rs.pest.vm.PestUtil
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class RustStr(private val native: PestUtil) {
	fun stringLength(str: String): Int {
		val strPtr = ptrFromString(str)
		return native.string_len(strPtr.offset, strPtr.size)
	}

	fun prependFromRust(str: String): String {
		val strPtr = ptrFromString(str)
		val nullTermOffset = native.prepend_from_rust(strPtr.offset, strPtr.size)
		return nullTermedStringFromOffset(nullTermOffset)
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
