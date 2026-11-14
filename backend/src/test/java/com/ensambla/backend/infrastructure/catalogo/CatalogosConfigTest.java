package com.ensambla.backend.infrastructure.catalogo;

import com.ensambla.backend.domain.model.catalogo.CodigoBarras;
import com.ensambla.backend.domain.model.catalogo.FuenteCatalogo;
import com.ensambla.backend.infrastructure.config.useCase.CatalogoUseCaseConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CatalogosConfigTest {

    @Test
    @DisplayName("Configura el catálogo con el recurso del JAR, plazos y proveedor centralizados")
    void catalogoProductosPortShouldBuildConfiguredAdapter() {
        var port = new CatalogosConfig().catalogoProductosPort(new ClassPathResource("catalogos/sepa.json.gz"),
                "https://world.openfoodfacts.org", "Ensambla-test", Duration.ofSeconds(5),
                Duration.ofMinutes(15), 12);
        var caso = new CatalogoUseCaseConfig().buscarProducto(port, Clock.systemUTC());

        assertThat(port.buscar(FuenteCatalogo.SEPA, new CodigoBarras("7790895000997"), Instant.now())).isPresent();
        assertThat(caso).isNotNull();
    }

    @Test
    @DisplayName("Selecciona el adaptador remoto cuando la fuente es Open Food Facts")
    void buscarShouldDelegateToExternalCatalog() {
        var builder = RestClient.builder().baseUrl("https://catalogo.test");
        var servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo(startsWith("https://catalogo.test/")))
                .andRespond(withSuccess("{\"product\":{\"code\":\"77903754\"}}", MediaType.APPLICATION_JSON));
        var adapter = new CatalogoProductosAdapter(new IndiceSepa(new ClassPathResource("catalogos/sepa.json.gz")),
                new OpenFoodFactsCatalogo(builder.build(), Duration.ofMinutes(15), 12));

        var result = adapter.buscar(FuenteCatalogo.OPEN_FOOD_FACTS, new CodigoBarras("77903754"), Instant.now());

        assertThat(result).isPresent();
        servidor.verify();
    }
}
