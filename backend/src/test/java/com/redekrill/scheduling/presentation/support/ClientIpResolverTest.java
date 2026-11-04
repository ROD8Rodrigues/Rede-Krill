package com.redekrill.scheduling.presentation.support;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    @Test
    void usesConnectionAddressWhenForwardedHeadersAreNotTrusted() {
        MockHttpServletRequest request = request("203.0.113.9", "10.0.0.5");

        assertThat(ClientIpResolver.resolve(request, false)).isEqualTo("10.0.0.5");
    }

    @Test
    void takesTheFirstForwardedAddressWhenProxyIsTrusted() {
        MockHttpServletRequest request = request("203.0.113.9, 70.41.3.18, 150.172.238.4", "10.0.0.5");

        assertThat(ClientIpResolver.resolve(request, true)).isEqualTo("203.0.113.9");
    }

    @Test
    void acceptsIpv6ForwardedAddress() {
        MockHttpServletRequest request = request("2001:db8:0:0:0:0:0:1", "10.0.0.5");

        assertThat(ClientIpResolver.resolve(request, true)).isEqualTo("2001:db8:0:0:0:0:0:1");
    }

    @Test
    void fallsBackToConnectionWhenForwardedValueIsNotAnAddress() {
        MockHttpServletRequest garbage = request("javascript:alert(1)", "10.0.0.5");
        MockHttpServletRequest oversized = request("a".repeat(80) + ".b", "10.0.0.5");
        MockHttpServletRequest empty = request("   ", "10.0.0.5");
        MockHttpServletRequest missing = request(null, "10.0.0.5");

        assertThat(ClientIpResolver.resolve(garbage, true)).isEqualTo("10.0.0.5");
        assertThat(ClientIpResolver.resolve(oversized, true)).isEqualTo("10.0.0.5");
        assertThat(ClientIpResolver.resolve(empty, true)).isEqualTo("10.0.0.5");
        assertThat(ClientIpResolver.resolve(missing, true)).isEqualTo("10.0.0.5");
    }

    @Test
    void ignoresNonNumericForwardedGroups() {
        MockHttpServletRequest request = request("999.999.999.999", "10.0.0.5");

        // o formato ainda é de endereço; a validação semântica do octeto não é papel do resolver
        assertThat(ClientIpResolver.resolve(request, true)).isEqualTo("999.999.999.999");
    }

    private MockHttpServletRequest request(String forwardedFor, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        return request;
    }
}
