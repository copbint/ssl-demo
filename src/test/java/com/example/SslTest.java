package com.example;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.google.common.collect.ImmutableList;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import javax.net.ssl.SSLHandshakeException;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
public class SslTest {
    @LocalServerPort
    int port;

    @Test
    public void should_handshake_fail() throws Exception {
        SslContext sslContext = SslContextBuilder
            .forClient()
            .trustManager(InsecureTrustManagerFactory.INSTANCE)
            .protocols("TLSv1.3")
            .ciphers(ImmutableList.of("TLS_AES_128_GCM_SHA256"))
            .build();

        HttpClient httpClient = HttpClient.create()
            .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext));

        WebClient webClient = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();

        StepVerifier.create(
                webClient.get()
                    .uri(String.format("https://localhost:%s/hello", port))
                    .retrieve()
                    .bodyToMono(String.class)
            )
            .expectErrorMatches(
                e -> e.getCause() instanceof SSLHandshakeException
            )
            .verify();
    }
}
