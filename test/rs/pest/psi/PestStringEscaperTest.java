package rs.pest.psi;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class PestStringEscaperTest {

	@Test
	public void unicode() {
		assertEquals(PestStringEscaper.unicode("\\u{0}", new StringBuilder(), 2), -1);
		assertNotEquals(PestStringEscaper.unicode("\\u{01}", new StringBuilder(), 2), -1);
		assertNotEquals(PestStringEscaper.unicode("\\u{022}", new StringBuilder(), 2), -1);
		assertNotEquals(PestStringEscaper.unicode("\\u{0223}", new StringBuilder(), 2), -1);
		assertNotEquals(PestStringEscaper.unicode("\\u{02234}", new StringBuilder(), 2), -1);
		assertNotEquals(PestStringEscaper.unicode("\\u{022345}", new StringBuilder(), 2), -1);
		assertEquals(PestStringEscaper.unicode("\\u{022345699}", new StringBuilder(), 2), -1);

		StringBuilder builder = new StringBuilder();
		String inString = "\\u{1234}";
		int outIndex = PestStringEscaper.unicode(inString, builder, 2);
		assertEquals("\u1234", builder.toString());
		assertEquals(inString.length(), outIndex);
	}
}