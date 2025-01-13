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
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public final class WebFileClient {

	private final WebFileClientConfig config;

	public WebFileClient(
		final WebFileClientConfig config
	) {
		this.config = config;
	}

	public InputStream file(
		final String path
	) throws IOException {
		return distill(connect(path));
	}

	public WebFileLines list(
		final String path
	) throws IOException {
		return new WebFileLines(file(path));
	}

	private HttpURLConnection connect(
		final String path,
		final String... args
	) throws IOException {
		final URL url = new URL(config.server, path);
		final HttpURLConnection hnd = config.proxy == null
			? (HttpURLConnection)url.openConnection()
			: (HttpURLConnection)url.openConnection(config.proxy);

		if (config.unsafe != null) {
			HttpsURLConnection sec = (HttpsURLConnection)hnd;
			sec.setSSLSocketFactory(config.unsafe);
		}

		hnd.setConnectTimeout(config.connTO);
		hnd.setReadTimeout(config.readTO);
		hnd.setRequestMethod("GET");

		for (int i = 1; i < args.length; i += 2) {
			hnd.setRequestProperty(args[i-1], args[i]);
		}

		hnd.setDoInput(true);
		hnd.setDoOutput(true);
		return hnd;
	}

	private InputStream distill(
		final HttpURLConnection broker
	) throws IOException {
		if (broker.getResponseCode() != HttpURLConnection.HTTP_OK) {
			throw new IOException(broker.getResponseMessage());
		}

		return broker.getInputStream();
	}

}
