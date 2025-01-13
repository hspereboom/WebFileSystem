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
import java.nio.channels.NotYetBoundException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.ReadOnlyFileSystemException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import WFS.util.Globals;

public final class WebFileSystemProvider extends FileSystemProvider implements Globals {

	private final Map<String, WebFileSystem> cache = new HashMap<>();

	@Override
	public String getScheme() {
		return "webfs";
	}

	private WebFileSystem mapFileSystem(final URI uri, final int crud) {
		if (neq(getScheme(), uri.getScheme()))
			throw new ProviderMismatchException();
		if (crud > 1 || (crud == 1 && cache.isEmpty()))
			return null;

		String ssp = uri.getRawSchemeSpecificPart();
		WebFileSystem wfs = cache.get(ssp);

		if (wfs == null) {
			for (Map.Entry<String, WebFileSystem> entry : cache.entrySet()) {
				final String tbd = entry.getKey();

				if (ssp.startsWith(tbd) && ssp.charAt(tbd.length()) == '/') {
					wfs = entry.getValue();
					break;
				}
			}
		}

		if (crud == 1)
			return wfs;
		if (wfs != null)
			throw new FileSystemAlreadyExistsException(ssp);

		cache.put(ssp, wfs = new WebFileSystem(this, uri));
		return wfs;
	}

	synchronized void ridFileSystem(final URI uri) {
		mapFileSystem(uri, 3);
		cache.remove(uri.getRawSchemeSpecificPart());
	}

	@Override
	public synchronized WebFileSystem getFileSystem(final URI uri) {
		return mapFileSystem(uri, 1);
	}

	@Override
	public synchronized WebFileSystem newFileSystem(
		final URI uri,
		final Map<String, ?> env // ignored
	) throws IOException {
		return mapFileSystem(uri, 0);
	}

	@Override
	public synchronized Path getPath(final URI uri) { // directory
		WebFileSystem wfs = getFileSystem(uri);

		if (wfs == null) {
			try {
				wfs = newFileSystem(uri, null);
			} catch (IOException e) {
				throw new FileSystemNotFoundException(e.getMessage());
			}
		}

		return wfs.getPath(uri.getRawSchemeSpecificPart());
	}

	@Override
	public DirectoryStream<Path> newDirectoryStream(
		final Path path,
		final DirectoryStream.Filter<? super Path> filter // ignored
	) throws IOException {
		final WebFilePath wfp = (WebFilePath)path;
		final WebFileSystem wfs = wfp.getFileSystem();

		return wfs.list(wfp);
	}

	@Override
	public InputStream newInputStream(
		final Path path,
		final OpenOption... opts
	) throws IOException {
		final WebFilePath wfp = (WebFilePath)path;
		final WebFileSystem wfs = wfp.getFileSystem();

		return wfs.file(wfp);
	}

	@Override
	public SeekableByteChannel newByteChannel(
		final Path path,
		final Set<? extends OpenOption> opts,
		final FileAttribute<?>... atts
	) throws IOException {
		throw new NotYetBoundException();
	}

	@Override
	public boolean isHidden(final Path path) {
		return false;
	}

	@Override
	public boolean isSameFile(final Path path1, final Path path2) {
		return path1 == null || path2 == null ? false :
			Objects.equals(path1.toAbsolutePath(), path2.toAbsolutePath());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <A extends BasicFileAttributes> A readAttributes(
		final Path path,
		final Class<A> type,
		final LinkOption... opts
	) throws IOException {
		final WebFilePath wfp = (WebFilePath)path;
		final WebFileSystem wfs = wfp.getFileSystem();

		return (A)wfs.atts(wfp);
	}

	@Override
	public Map<String, Object> readAttributes(
		final Path path,
		final String atts,
		final LinkOption... opts
	) throws IOException {
		return readAttributes(path, WebFileAttributes.class).toMap();
	}

	//

	@Override
	public void checkAccess(final Path path, final AccessMode... modes) {
	}

	@Override
	public void setAttribute(final Path path, final String attr, final Object value, final LinkOption... opts) {
		throw new ReadOnlyFileSystemException();
	}

	@Override
	public void createDirectory(final Path path, final FileAttribute<?>... atts) {
		throw new ReadOnlyFileSystemException();
	}

	@Override
	public void delete(final Path path) {
		throw new ReadOnlyFileSystemException();
	}

	@Override
	public void copy(final Path source, final Path target, final CopyOption... opts) {
		throw new ReadOnlyFileSystemException();
	}

	@Override
	public void move(final Path source, final Path target, final CopyOption... opts) {
		throw new ReadOnlyFileSystemException();
	}

	@Override
	public FileStore getFileStore(final Path path) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <V extends FileAttributeView> V getFileAttributeView(final Path path, final Class<V> type, final LinkOption... opts) {
		throw new UnsupportedOperationException();
	}

}
