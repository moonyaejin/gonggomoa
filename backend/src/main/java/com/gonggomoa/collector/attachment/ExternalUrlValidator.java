package com.gonggomoa.collector.attachment;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;

/**
 * SSRF 방어 공통 검증기 (docs/07-security.md §2). https/http만 허용하고, 호스트가
 * 화이트리스트에 있는지, DNS 해석 결과가 사설/예약 대역(루프백·링크로컬·사설망·IPv6
 * unique-local)을 가리키지 않는지 확인한다. 리다이렉트는 여기서 막지 않는다 — 호출
 * 측에서 {@code HttpClient.Redirect.NEVER}로 따로 강제한다.
 */
public final class ExternalUrlValidator {

	private ExternalUrlValidator() {
	}

	public static boolean isSafe(URI uri, Set<String> allowedHosts) {
		if (uri == null || uri.getHost() == null) {
			return false;
		}
		String scheme = uri.getScheme();
		if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
			return false;
		}
		String host = uri.getHost().toLowerCase(Locale.ROOT);
		if (!allowedHosts.contains(host)) {
			return false;
		}
		try {
			for (InetAddress address : InetAddress.getAllByName(host)) {
				if (isPrivateOrReserved(address)) {
					return false;
				}
			}
		} catch (UnknownHostException e) {
			return false;
		}
		return true;
	}

	private static boolean isPrivateOrReserved(InetAddress address) {
		if (address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress()
				|| address.isAnyLocalAddress() || address.isMulticastAddress()) {
			return true;
		}
		byte[] bytes = address.getAddress();
		// IPv6 unique local address (fc00::/7) — 위 isSiteLocalAddress()는 IPv4 사설망만 잡는다.
		return bytes.length == 16 && (bytes[0] & 0xFE) == 0xFC;
	}
}
