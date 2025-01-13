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

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Map;

public final class WebFileAttributes implements BasicFileAttributes {

	private boolean file;
	private long size;
	private FileTime time;

	public WebFileAttributes(
		final boolean file,
		final long size,
		final FileTime time
	) {
		flash(file, size, time);
	}

	@Override public boolean isRegularFile() { return file; }
	@Override public boolean isDirectory() { return !file; }
	@Override public boolean isSymbolicLink() { return false; }
	@Override public boolean isOther() { return false; }

	@Override public long size() { return size; }

	@Override public FileTime lastModifiedTime() { return time; }
	@Override public FileTime lastAccessTime() { return null; }
	@Override public FileTime creationTime() { return null; }

	@Override public Object fileKey() { return null; }

	//

	void flash(final boolean file, final long size, final FileTime time) {
		this.file = file;
		this.size = size;
		this.time = time;
	}

	Map<String, Object> toMap() {
		return Map.of(
			"isRegularFile",    isRegularFile(),
			"isDirectory",      isDirectory(),
			"isSymbolicLink",   isSymbolicLink(),
			"isOther",          isOther(),
			"size",             size(),
			"lastModifiedTime", lastModifiedTime());
	}

}
