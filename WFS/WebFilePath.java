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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import WFS.util.Globals;

public final class WebFilePath implements Path, Globals {

	private final WebFileSystem fact;
	private final WebFilePath sire;
	private final String name;
	private final URI link;
	private final WebFileAttributes atts;

	private final int tell;
	private final WebFilePath self;

	WebFilePath(
		final WebFileSystem fact,
		final WebFilePath sire,
		final String name,
		final URI link,
		final WebFileAttributes atts
	) {
		this.fact = fact;
		this.sire = sire;
		this.name = name;
		this.link = link;
		this.atts = atts;

		if (link != null) {
			this.tell = 1 + (sire == null ? 0 : sire.tell);
			this.self = new WebFilePath(fact, sire, name, null, atts);
		} else {
			this.tell = 1;
			this.self = this;
		}
	}


	@Override
	public WebFileSystem getFileSystem() {
		return fact;
	}

	@Override
	public boolean isAbsolute() {
		return true;
	}

	@Override
	public WebFilePath getRoot() {
		return fact.getRoot();
	}

	@Override
	public WebFilePath getParent() {
		return sire;
	}

	@Override
	public int getNameCount() {
		return tell;
	}

	@Override
	public WebFilePath getName(final int index) {
		if (index < 0 || index >= tell) {
			 throw new IndexOutOfBoundsException(index);
		}

		WebFilePath entry = this;
		for (int skips = tell - index; --skips > 0; entry = entry.sire);
		return entry.self;
	}

	@Override
	public WebFilePath getFileName() {
		return self;
	}

	@SuppressWarnings("unchecked")
	<A extends BasicFileAttributes> A getMemoAtts() {
		return (A)atts;
	}

	WebFilePath setMemoAtts(final boolean file, final long size, final FileTime time) {
		atts.flash(file, size, time);
		return this;
	}

	@Override
	public WebFilePath subpath(final int from, final int till) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean startsWith(final Path tbd) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean startsWith(final String tbd) {
		return startsWith(fact.getPath(tbd));
	}

	@Override
	public boolean endsWith(final Path tbd) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean endsWith(final String tbd) {
		return endsWith(fact.getPath(tbd));
	}

	@Override
	public WebFilePath normalize() {
		return this;
	}

	@Override
	public WebFilePath resolve(final Path path) {
		if (atts.isDirectory()) {
			final WebFilePath wfp = (WebFilePath)path;

			return wfp.toUri().isAbsolute() ? wfp : new WebFilePath(
				wfp.fact, wfp.sire, wfp.name,
				resolve(link, wfp.toString(), wfp.atts.isRegularFile()),
				wfp.getMemoAtts());
		}

		throw new UnsupportedOperationException();
	}

	@Override
	public WebFilePath resolve(final String path) {
		return resolve(fact.getPath(path));
	}

	@Override
	public Path resolveSibling(final Path path) {
		return sire == null ? path : sire.resolve(path);
	}

	@Override
	public Path resolveSibling(final String path) {
		return resolveSibling(fact.getPath(path));
	}

	@Override
	public WebFilePath relativize(final Path path) {
		if (atts.isDirectory()) {
			final WebFilePath wfp = (WebFilePath)path;

			return new WebFilePath(wfp.fact, wfp.sire, wfp.name,
				link.relativize(wfp.toUri()), wfp.getMemoAtts());
		}

		throw new UnsupportedOperationException();
	}

	@Override
	public URI toUri() {
		return link;
	}

	@Override
	public WebFilePath toAbsolutePath() {
		return this;
	}

	@Override
	public WebFilePath toRealPath(LinkOption... options) throws IOException {
		fact.provider().checkAccess(this);
		return this;
	}

	@Override
	public Iterator<Path> iterator() {
		final Deque<Path> deck = new ArrayDeque<>();

		WebFilePath entry = this;
		while (deck.offerFirst(entry) && (entry = entry.sire) != null);

		return deck.iterator();
	}

	@Override
	public int compareTo(final Path path) {
		return compareTo(path.toUri());
	}

	private int compareTo(final URI link) {
		final URI dis = this.link;
		final URI dat = link;

		return dis == null ? dat == null ? 0 : -1 : dat == null ? +1 :
			dis.relativize(dat).toString().isEmpty() ? 0 : dis.compareTo(dat);
	}

	private boolean compareEx(final URI link) {
		return compareTo(link) == 0;
	}

	private boolean compareEx(final WebFilePath path) {
		return fact == path.fact ? compareTo(path) == 0 : false;
	}

	@Override
	public boolean equals(Object that) {
		return
			this == that ? true : that != null &&
			that instanceof URI ? compareEx((URI)that) :
			that instanceof WebFilePath ? compareEx((WebFilePath)that) :
			false;
	}

	@Override
	public int hashCode() {
		return link == null ? name.hashCode() : link.hashCode();
	}

	@Override
	public String toString() {
		return link == null ? name : link.toString();
	}

	//

	@Override
	public File toFile() {
		throw new UnsupportedOperationException();
	}

	@Override
	public WatchKey register(final WatchService watch, final WatchEvent.Kind<?>... evts) {
		throw new UnsupportedOperationException();
	}

	@Override
	public WatchKey register(final WatchService watch, final WatchEvent.Kind<?>[] evts, final WatchEvent.Modifier... mods) {
		throw new UnsupportedOperationException();
	}

}
