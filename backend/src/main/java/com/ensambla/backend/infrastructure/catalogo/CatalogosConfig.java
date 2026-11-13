package com.ensambla.backend.infrastructure.catalogo;

import com.ensambla.backend.domain.port.CatalogoProductosPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class CatalogosConfig {

    @Bean
    public CatalogoProductosPort catalogoProductosPort(
            @Value("${ensambla.catalogos.sepa.recurso}") Resource recurso,
            @Value("${ensambla.catalogos.openfoodfacts.url}") String url,
            @Value("${ensambla.catalogos.openfoodfacts.user-agent}") String userAgent,
            @Value("${ensambla.catalogos.openfoodfacts.timeout}") Duration timeout,
            @Value("${ensambla.catalogos.openfoodfacts.cache}") Duration cache,
            @Value("${ensambla.catalogos.openfoodfacts.limite-por-minuto}") int limite) {
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(timeout).build());
        fabrica.setReadTimeout(timeout);
        RestClient cliente = RestClient.builder().baseUrl(url).requestFactory(fabrica)
                .defaultHeader("User-Agent", userAgent).build();
        return new CatalogoProductosAdapter(new IndiceSepa(recurso), new OpenFoodFactsCatalogo(cliente, cache, limite));
    }
}
