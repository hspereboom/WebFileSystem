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
package WFS;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

import WFS.util.Globals;

public final class WebFileSystem extends FileSystem implements Globals {

	private final WebFileSystemProvider fact;
	private final WebFileAttributes none;
	private final WebFilePath root;
	private final WebFileClient stub;

	WebFileSystem(
		final WebFileSystemProvider fact,
		final URI root
	) {
		this.fact = fact;
		this.none = new WebFileAttributes(false, -1, FileTime.from(Instant.now()));
		this.root = new WebFilePath(this, null, "", root, none);
		this.stub = new WebFileClient(new WebFileClientConfig.Builder()
			.setServerBaseUrl(root.getRawSchemeSpecificPart().substring(root.isOpaque() ? 0 : 1))
			.toConfig());
	}

	@Override
	public boolean isOpen() {
		return fact.getFileSystem(root.toUri()) == this;
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public void close() throws IOException {
		fact.ridFileSystem(root.toUri());
	}

	@Override
	public String getSeparator() {
		return "/";
	}

	@Override
	public FileSystemProvider provider() {
		return fact;
	}

	@Override
	public Iterable<Path> getRootDirectories() {
		return List.of(root);
	}

	WebFilePath getRoot() {
		return root;
	}

	@SuppressWarnings("unlikely-arg-type")
	@Override
	public Path getPath(final String base, final String... elems) {
		final StringBuilder path = new StringBuilder(256)
			.append(fact.getScheme())
			.append(':')
			.append(base.trim());

		final int junk = path.length() - 1;

		switch (path.charAt(junk)) {
			case ':':
				throw new InvalidPathException(base, "blank", 0);
			case '/':
				path.setLength(junk);
		}

		for (final String elem : elems) {
			path.append('/').append(elem);
		} path.append('/');

		final URI uri = URI.create(path.toString());

		if (root.equals(uri)) {
			return root;
		}

		try (final WebFilePaths ping = list(uri)) {
			return ping.next();
		} catch (final IOException|NoSuchElementException e) {
			return new WebFilePath(this, null, "", uri, none);
		}
	}

	@Override
	public PathMatcher getPathMatcher(final String tbd) {
		final Pattern rex = pattern(tbd);

		return path -> rex.matcher(path.toString()).matches();
	}

	WebFilePaths list(final WebFilePath node) throws IOException {
		return list(node, node.toUri()).skip();
	}

	WebFilePaths list(final URI base) throws IOException {
		return list(null, base);
	}

	private WebFilePaths list(final WebFilePath node, final URI base) throws IOException {
		final WebFilePath sire = node;
		final String path = root.toUri().relativize(base).toString();

		return new WebFilePaths(stub.list(path), line -> {
			final String[] args = line.split("\\t", -1);
			final String part = xl8(args, 0, WebFileSystem::name);
			final FileTime time = xl8(args, 1, WebFileSystem::time);
			final long size = xl8(args, 2, WebFileSystem::size);

			if (part == null || args.length > 3) {
				throw new ArrayStoreException();
			}

			final boolean file = size >= 0;
			final String name;

			if (part.equals(".")) {
				if (sire != null) {
					return sire.setMemoAtts(file, size, time);
				}

				int s = path.lastIndexOf('/') + 1;
				int e = path.length();

				if (s == e) {
					s = path.lastIndexOf('/', e - 2) + 1;
					e = Math.max(e - 1, 0);
				}

				name = path.substring(s, e);
			} else {
				name = part;
			}

			final URI link = resolve(base, part, file);

			return new WebFilePath(this, sire, name, link,
				new WebFileAttributes(file, size, time));
		});
	}

	WebFileAttributes atts(final WebFilePath node) throws IOException {
		final WebFileAttributes wfa = node.getMemoAtts();

		// Satisfy Files.exists(Path) logic
		if (!root.equals(node) && wfa == none) {
			throw new IOException(node + " has no attributes");
		}

		return wfa;
	}

	InputStream file(final WebFilePath node) throws IOException {
		final URI base = node.toUri();
		final String path = root.toUri().relativize(base).toString();

		return stub.file(path);
	}

	//

	private static boolean xl8(final String[] a, final int i) {
		return a.length > i && !(a[i] = a[i].trim()).isEmpty();
	}

	private static <T> T xl8(final String[] a, final int i, final Function<String, T> f) {
		return f.apply(xl8(a, i) ? a[i] : null);
	}

	private static String name(final String s) {
		return s;
	}

	private static FileTime time(final String s) {
		return s == null ? FileTime.fromMillis(0) : FileTime.from(Instant.parse(s));
	}

	private static long size(final String s) {
		return s == null || "-".equals(s) ? -1 : Long.parseUnsignedLong(s);
	}

	//

	@Override
	public Iterable<FileStore> getFileStores() {
		return Collections.emptySet();
	}

	@Override
	public Set<String> supportedFileAttributeViews() {
		return Collections.emptySet();
	}

	@Override
	public UserPrincipalLookupService getUserPrincipalLookupService() {
		throw new UnsupportedOperationException();
	}

	@Override
	public WatchService newWatchService() {
		throw new UnsupportedOperationException();
	}

}
