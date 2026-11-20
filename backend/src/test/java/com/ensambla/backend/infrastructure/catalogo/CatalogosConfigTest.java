package com.ensambla.backend.infrastructure.catalogo;

import com.ensambla.backend.domain.model.catalogo.CodigoBarras;
import com.ensambla.backend.domain.model.catalogo.FuenteCatalogo;
import com.ensambla.backend.domain.model.catalogo.ProductoCatalogo;
import com.ensambla.backend.domain.port.CatalogoProductosPort;
import com.ensambla.backend.domain.useCase.catalogo.buscarProducto.BuscarProducto;
import com.ensambla.backend.infrastructure.config.useCase.CatalogoUseCaseConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CatalogosConfigTest {

    private CatalogoProductosPort puerto;
    private BuscarProducto casoDeUso;
    private CatalogoProductosAdapter adaptador;
    private MockRestServiceServer servidor;
    private Optional<ProductoCatalogo> encontrado;

    @Test
    @DisplayName("Configura el catálogo con el recurso del JAR, plazos y proveedor centralizados")
    void configuraElCatalogoConElRecursoDelJarPlazosYProveedorCentralizados() {
        cuandoSeConfiguraElCatalogo();

        entoncesElCatalogoBuscaEnSepaYTieneSuCasoDeUso();
    }

    @Test
    @DisplayName("Selecciona el adaptador remoto cuando la fuente es Open Food Facts")
    void seleccionaElAdaptadorRemotoCuandoLaFuenteEsOpenFoodFacts() {
        dadoUnAdaptadorConUnProveedorRemotoQueResponde();

        cuandoSeBuscaEnOpenFoodFacts();

        entoncesSeConsultoAlProveedorRemoto();
    }

    // --- dado ---

    private void dadoUnAdaptadorConUnProveedorRemotoQueResponde() {
        var builder = RestClient.builder().baseUrl("https://catalogo.test");
        servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo(startsWith("https://catalogo.test/")))
                .andRespond(withSuccess("{\"product\":{\"code\":\"77903754\"}}", MediaType.APPLICATION_JSON));
        adaptador = new CatalogoProductosAdapter(new IndiceSepa(new ClassPathResource("catalogos/sepa.json.gz")),
                new OpenFoodFactsCatalogo(builder.build(), Duration.ofMinutes(15), 12));
    }

    // --- cuando ---

    private void cuandoSeConfiguraElCatalogo() {
        puerto = new CatalogosConfig().catalogoProductosPort(new ClassPathResource("catalogos/sepa.json.gz"),
                "https://world.openfoodfacts.org", "Ensambla-test", Duration.ofSeconds(5),
                Duration.ofMinutes(15), 12);
        casoDeUso = new CatalogoUseCaseConfig().buscarProducto(puerto, Clock.systemUTC());
    }

    private void cuandoSeBuscaEnOpenFoodFacts() {
        encontrado = adaptador.buscar(FuenteCatalogo.OPEN_FOOD_FACTS, new CodigoBarras("77903754"), Instant.now());
    }

    // --- entonces ---

    private void entoncesElCatalogoBuscaEnSepaYTieneSuCasoDeUso() {
        assertThat(puerto.buscar(FuenteCatalogo.SEPA, new CodigoBarras("7790895000997"), Instant.now()))
                .isPresent();
        assertThat(casoDeUso).isNotNull();
    }

    private void entoncesSeConsultoAlProveedorRemoto() {
        assertThat(encontrado).isPresent();
        servidor.verify();
    }
}
