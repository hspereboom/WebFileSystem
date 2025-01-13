/*
 * MIT License
 *
 * Copyright (C) 2024-2025 Harry Shungo Pereboom (github.com/hspereboom)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package WFS.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class Janitor {

	private static final String[] support;

	static {
		// #0 is default
		support = FileSystemProvider.installedProviders()
			.stream()
			.map(fsp -> fsp.getScheme() + ":")
			.toArray(String[]::new);
	}

	private Janitor() {}

	@SuppressWarnings("unchecked")
	public static <P extends Path> P toAbsPath(final String path) {
		// We cannot distinguish between URI-style paths and 'disk' paths
		// using URI.isAbsolute, because URI.create throws an RTE on
		// schemeless paths.
		String scheme = null;

		for (final String tbd : support) {
			if (path.startsWith(tbd)) {
				scheme = tbd;
				break;
			}
		}

		return (P)(scheme == null
			? Path.of(path).toAbsolutePath()
			: Path.of(URI.create(path)));
	}

	public static URI toSubPath(final URI base, final String elem, final boolean file) {
		// URI.resolve without the normalization that will ruin the path.
		final String path = toSubPath(base.getSchemeSpecificPart(), elem, file);

		try {
			return new URI(base.getScheme(), path, null);
		} catch (final URISyntaxException e) {
			throw new InvalidPathException(path, e.getMessage());
		}
	}

	private static String toSubPath(final String base, final String elem, final boolean file) {
		final boolean self = elem.isEmpty() || elem.equals(".");
		final boolean bsep = base.endsWith("/");
		final boolean esep = elem.endsWith("/");
		final StringBuilder path = new StringBuilder(256)
			.append(base)
			.append(bsep ? "" : "/")
			.append(self ? "" : elem)
			.append(self || file || esep ? "" : "/");

		if (file && (self ? bsep : esep)) {
			path.deleteCharAt(path.length() - 1);
		}

		return path.toString();
	}

	public static Pattern toPattern(final String tbd) {
		final int mid = tbd.indexOf(':') + 1;
		String type = tbd.substring(0, mid);
		String expr = tbd.substring(mid);

		switch (type) {
			case "regex:":
				break;
			case "glob:":
				expr = gl2rx(expr);
				break;
			default:
				throw new PatternSyntaxException("syntax:pattern", tbd, 0);
		}

		return Pattern.compile(expr);
	}

	private static char[] prepGL(final char[] source) {
		final char[] target = new char[source.length + 1];
		System.arraycopy(source, 0, target, 0, target.length - 1);
		return target;
	}

	private static StringBuilder prepRX(final String source) {
		final StringBuilder target = new StringBuilder(
			Math.max(64, 32 + source.length()));
		return target;
	}

	private static String gl2rx(final String pattern) {
		final char[] glob = prepGL(pattern.toCharArray());
		final StringBuilder regex = prepRX(pattern).append('^');
		final Deque<Integer> state = new ArrayDeque<>(List.of(0));

		// Quick & Dirty: UTF-16 low surrogate handling may be flaky
		for (int i = 0, jump = 0; i < glob.length; i++, jump = 0) {
			char c = glob[i];

			switch (state.peek()) {
				case 0:
					switch (c) {
						case 0: jump = 9; break;
						case '*': jump = 1; break;
						case '\\': jump = 2; break;
						case '.': case '^': case '$': case '+':
						case '|': case '(': case ')': case '}': jump = -2; break;
						case '{': jump = -3; break;
						case '/': regex.append("[\\\\/]"); break;
						default: regex.append(c); break;
					} break;
				case 1:
					switch (c) {
						case '*': regex.append(".*"); jump = 9; break;
						default: regex.append("[^/]*"); jump = -9; break;
					} break;
				case 2: regex.append("\\").append(c); jump = 9; break;
				case 3:
					switch (c) {
						case '{': regex.append("(?:"); jump = 4; break;
						case ',': regex.append("|"); jump = 4; break;
						case '}': regex.append(")"); jump = 9; break;
						default: jump = 8; break;
					} break;
				case 4:
					switch (c) {
						case '*': regex.append("[^/]*"); break;
						case '\\': jump = 2; break;
						case '.': case '^': case '$': case '+':
						case '|': case '(': case ')': jump = -2; break;
						case '}': case ',': jump = -9; break;
						case '/': jump = -8; break;
						default: regex.append(c); break;
					} break;
				default:
					throw new PatternSyntaxException(regex.toString(), pattern, i);
			}

			// LR(0) backtrack
			if (jump < 0) { i--; jump = Math.abs(jump); }

			switch (jump) {
				case 0: continue;
				case 9: state.pop(); break;
				default: state.push(jump); break;
			}
		}

		// Make URI-safe
		switch (glob[Math.max(2, glob.length) - 2]) {
			case '/': break;
			default: regex.append("[\\\\/]?");
		}

		return regex.append('$').toString();
	}

}
