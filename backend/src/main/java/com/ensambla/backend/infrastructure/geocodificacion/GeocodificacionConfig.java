package com.ensambla.backend.infrastructure.geocodificacion;

import com.ensambla.backend.domain.port.GeocodificadorPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class GeocodificacionConfig {

    @Bean
    public GeocodificadorPort geocodificadorPort(
            @Value("${ensambla.geocodificacion.url}") String url,
            @Value("${ensambla.geocodificacion.user-agent}") String userAgent,
            @Value("${ensambla.geocodificacion.timeout}") Duration timeout) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);
        RestClient restClient = RestClient.builder()
                .baseUrl(url)
                .defaultHeader("User-Agent", userAgent)
                .requestFactory(requestFactory)
                .build();
        return new NominatimGeocodificadorAdapter(restClient, System::nanoTime);
    }
}
