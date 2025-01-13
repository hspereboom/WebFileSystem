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
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Objects;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public final class WebFileClientConfig {

	public final Proxy proxy;
	public final int connTO; // default 1000
	public final int readTO; // default 0
	public final URL server;
	public final SSLSocketFactory unsafe;

	private WebFileClientConfig(
		String proxyHostPort,
		String connTimeoutMS,
		String readTimeoutMS,
		String serverBaseUrl
	) {
		this.proxy = asProxy(proxyHostPort);
		this.connTO = asAbort(connTimeoutMS, "1000");
		this.readTO = asAbort(readTimeoutMS, "0");
		this.server = asURL(serverBaseUrl);
		this.unsafe = asBlind(serverBaseUrl);
	}

	private static URL asURL(final String serverBaseUrl) {
		try {
			return new URL(serverBaseUrl);
		} catch (final IOException e) {
			throw new IllegalArgumentException(serverBaseUrl);
		}
	}

	private static Proxy asProxy(final String proxyHostPort) {
		if (proxyHostPort == null || proxyHostPort.isEmpty()) {
			return null;
		}

		final int colon = proxyHostPort.indexOf(':');

		return new Proxy(Proxy.Type.HTTP, colon < 0
			? new InetSocketAddress(proxyHostPort, 80)
			: new InetSocketAddress(
				proxyHostPort.substring(0, colon),
				Integer.parseInt(proxyHostPort.substring(colon + 1))));
	}

	private static int asAbort(final String timeoutMillis, final String defaultMillis) {
		return Math.max(0, Integer.parseInt(Objects.toString(timeoutMillis, defaultMillis)));
	}

	private static SSLSocketFactory asBlind(final String serverBaseUrl) {
		if (!serverBaseUrl.startsWith("https")) {
			return null;
		}

		final TrustManager[] trusts = { new X509TrustManager() {
			@Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
			@Override public void checkServerTrusted(final X509Certificate[] certs, final String authType) {}
			@Override public void checkClientTrusted(final X509Certificate[] certs, final String authType) {}
		}};

		try {
			final SSLContext ctx = SSLContext.getInstance("TLS");
			ctx.init(null, trusts, new SecureRandom());

			return ctx.getSocketFactory();
		} catch (final GeneralSecurityException e) {
			return null;
		}
	}

	public static final class Builder {

		private String proxyHostPort = null;
		private String connTimeoutMS = null;
		private String readTimeoutMS = null;
		private String serverBaseUrl = null;

		public Builder setProxyHostPort(final String proxyHostPort) {
			this.proxyHostPort = proxyHostPort;
			return this;
		}

		public Builder setTimeoutMillis(final String connTimeoutMS, final String readTimeoutMS) {
			this.connTimeoutMS = connTimeoutMS;
			this.readTimeoutMS = readTimeoutMS;
			return this;
		}

		public Builder setServerBaseUrl(final String serverBaseUrl) {
			this.serverBaseUrl = serverBaseUrl;
			return this;
		}

		public WebFileClientConfig toConfig() {
			return new WebFileClientConfig(
				proxyHostPort, connTimeoutMS, readTimeoutMS, serverBaseUrl);
		}

	}

}
