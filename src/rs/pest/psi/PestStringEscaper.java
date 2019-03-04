package rs.pest.psi;

import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.LiteralTextEscaper;
import com.intellij.psi.PsiLanguageInjectionHost;
import org.jetbrains.annotations.NotNull;

public class PestStringEscaper<T extends PsiLanguageInjectionHost> extends LiteralTextEscaper<T> {
	private int[] outSourceOffsets;

	public PestStringEscaper(@NotNull T host) {
		super(host);
	}

	@Override
	public boolean decode(@NotNull final TextRange rangeInsideHost, @NotNull StringBuilder outChars) {
		String subText = rangeInsideHost.substring(myHost.getText());

		Ref<int[]> sourceOffsetsRef = new Ref<>();
		boolean result = parseStringCharacters(subText, outChars, sourceOffsetsRef, !isOneLine());
		outSourceOffsets = sourceOffsetsRef.get();
		return result;
	}

	@Override
	public int getOffsetInHost(int offsetInDecoded, @NotNull final TextRange rangeInsideHost) {
		int result = offsetInDecoded < outSourceOffsets.length ? outSourceOffsets[offsetInDecoded] : -1;
		if (result == -1) return -1;
		return (result <= rangeInsideHost.getLength() ? result : rangeInsideHost.getLength()) +
			rangeInsideHost.getStartOffset();
	}

	@Override
	public boolean isOneLine() {
		return true;
	}

	public static boolean parseStringCharacters(
		@NotNull String chars,
		StringBuilder outChars,
		@NotNull Ref<int[]> sourceOffsetsRef,
		boolean escapeBacktick) {
		int[] sourceOffsets = new int[chars.length() + 1];
		sourceOffsetsRef.set(sourceOffsets);

		if (chars.indexOf('\\') < 0) {
			outChars.append(chars);
			for (int i = 0; i < sourceOffsets.length; i++) {
				sourceOffsets[i] = i;
			}
			return true;
		}

		int index = 0;
		final int outOffset = outChars.length();
		while (index < chars.length()) {
			char c = chars.charAt(index++);

			sourceOffsets[outChars.length() - outOffset] = index - 1;
			sourceOffsets[outChars.length() + 1 - outOffset] = index;

			if (c != '\\') {
				outChars.append(c);
				continue;
			}
			if (index == chars.length()) return false;
			c = chars.charAt(index++);
			if (escapeBacktick && c == '`') {
				outChars.append(c);
			} else {
				switch (c) {
					case 'b':
						outChars.append('\b');
						break;

					case 't':
						outChars.append('\t');
						break;

					case 'n':
					case '\n':
						outChars.append('\n');
						break;

					case 'f':
						outChars.append('\f');
						break;

					case 'r':
						outChars.append('\r');
						break;

					case '"':
						outChars.append('"');
						break;

					case '/':
						outChars.append('/');
						break;

					case '\'':
						outChars.append('\'');
						break;

					case '\\':
						outChars.append('\\');
						break;

					case '0':
					case '1':
					case '2':
					case '3':
					case '4':
					case '5':
					case '6':
					case '7':
						index = number(chars, outChars, index, c);
						break;
					case 'x':
						if (index + 2 <= chars.length()) {
							try {
								int v = Integer.parseInt(chars.substring(index, index + 2), 16);
								outChars.append((char) v);
								index += 2;
							} catch (Exception e) {
								return false;
							}
						} else {
							return false;
						}
						break;
					case 'u':
						index = unicode(chars, outChars, index);
						if (index == -1) return false;
						else break;

					default:
						outChars.append(c);
						break;
				}
			}

			sourceOffsets[outChars.length() - outOffset] = index;
		}
		return true;
	}

	private static int number(@NotNull String chars, StringBuilder outChars, int index, char c) {
		char startC = c;
		int v = (int) c - '0';
		if (index < chars.length()) {
			c = chars.charAt(index++);
			if ('0' <= c && c <= '7') {
				v <<= 3;
				v += c - '0';
				if (startC <= '3' && index < chars.length()) {
					c = chars.charAt(index++);
					if ('0' <= c && c <= '7') {
						v <<= 3;
						v += c - '0';
					} else {
						index--;
					}
				}
			} else {
				index--;
			}
		}
		outChars.append((char) v);
		return index;
	}

	public static int unicode(@NotNull String chars, StringBuilder outChars, int index) {
		if (index + 4 > chars.length()) return -1;
		if (chars.charAt(index) != '{') return -1;
		int range;
		if (chars.charAt(index + 3) == '}') range = 2;
		else if (index + 4 < chars.length() && chars.charAt(index + 4) == '}') range = 3;
		else if (index + 5 < chars.length() && chars.charAt(index + 5) == '}') range = 4;
		else if (index + 6 < chars.length() && chars.charAt(index + 6) == '}') range = 5;
		else if (index + 7 < chars.length() && chars.charAt(index + 7) == '}') range = 6;
		else return -1;
		try {
			int v = Integer.parseInt(chars.substring(index + 1, index + 1 + range), 16);
			char c = chars.charAt(index + 1);
			if (c == '+' || c == '-') return -1;
			outChars.append((char) v);
			return index + range + 2;
		} catch (Exception e) {
			return -1;
		}
	}
}
