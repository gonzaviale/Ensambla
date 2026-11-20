package com.ensambla.backend.infrastructure.entorno;

import com.ensambla.backend.domain.port.EntornoDelPuntoPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class EntornoConfig {

    @Bean
    public EntornoDelPuntoPort entornoDelPuntoPort(
            @Value("${ensambla.entorno.url}") String url,
            @Value("${ensambla.entorno.user-agent}") String userAgent,
            @Value("${ensambla.entorno.timeout}") Duration timeout) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);
        RestClient restClient = RestClient.builder()
                .baseUrl(url)
                .defaultHeader("User-Agent", userAgent)
                .requestFactory(requestFactory)
                .build();
        return new OverpassEntornoAdapter(restClient);
    }
}
