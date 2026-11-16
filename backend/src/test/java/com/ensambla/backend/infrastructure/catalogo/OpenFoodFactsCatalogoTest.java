package com.ensambla.backend.infrastructure.catalogo;

import com.ensambla.backend.domain.exception.CatalogoNoDisponibleException;
import com.ensambla.backend.domain.model.catalogo.CodigoBarras;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenFoodFactsCatalogoTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T12:00:00Z");
    private static final CodigoBarras CODIGO = new CodigoBarras("77903754");
    private static final String RESPUESTA = """
            {"product":{"code":"77903754","product_name_es":"Alfajor completo","product_name":"Alfajor",
            "brands":"Marca","quantity":"40 g","image_front_url":"https://images.openfoodfacts.org/foto.jpg"}}
            """;
    private MockRestServiceServer servidor;
    private OpenFoodFactsCatalogo catalogo;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("https://catalogo.test");
        servidor = MockRestServiceServer.bindTo(builder).build();
        catalogo = new OpenFoodFactsCatalogo(builder.build(), Duration.ofMinutes(15), 12);
    }

    @Test
    @DisplayName("Consulta sólo campos públicos, sin credenciales, y reutiliza la caché por GTIN")
    void buscarShouldNormalizeAndCachePublicProduct() {
        givenRespuesta(RESPUESTA);

        var result = catalogo.buscar(CODIGO, AHORA).orElseThrow();
        var repetido = catalogo.buscar(new CodigoBarras("00000077903754"), AHORA.plusSeconds(1));

        assertThat(result.codigo()).isEqualTo("77903754");
        assertThat(result.nombre()).isEqualTo("Alfajor completo");
        assertThat(result.marca()).isEqualTo("Marca");
        assertThat(result.presentacion()).isEqualTo("40 g");
        assertThat(result.imagen()).isEqualTo("https://images.openfoodfacts.org/foto.jpg");
        assertThat(repetido).contains(result);
        servidor.verify();
    }

    @Test
    @DisplayName("Usa el nombre alternativo y descarta una imagen de otro sitio")
    void buscarShouldUseFallbackNameAndDropUnsafeImage() {
        givenRespuesta(RESPUESTA.replace("Alfajor completo", " ").replace(
                "https://images.openfoodfacts.org", "https://otro.test"));

        var result = catalogo.buscar(CODIGO, AHORA).orElseThrow();

        assertThat(result.nombre()).isEqualTo("Alfajor");
        assertThat(result.imagen()).isNull();
    }

    @Test
    @DisplayName("El 404 se conserva como ausencia, incluida en la caché")
    void buscarShouldCacheMissingProduct() {
        servidor.expect(requestTo(givenUrl())).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(catalogo.buscar(CODIGO, AHORA)).isEmpty();
        assertThat(catalogo.buscar(CODIGO, AHORA.plusSeconds(2))).isEmpty();
        servidor.verify();
    }

    @Test
    @DisplayName("Un error del proveedor no se cachea y permite reintentar")
    void buscarShouldRetryUnavailableProvider() {
        servidor.expect(requestTo(givenUrl())).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        givenRespuesta(RESPUESTA);

        assertThatThrownBy(() -> catalogo.buscar(CODIGO, AHORA))
                .isInstanceOf(CatalogoNoDisponibleException.class);
        assertThat(catalogo.buscar(CODIGO, AHORA.plusSeconds(1))).isPresent();
    }

    @Test
    @DisplayName("Rechaza JSON inválido y productos que corresponden a otro código")
    void buscarShouldRejectMalformedResponse() {
        for (String json : new String[]{"{", "{}", RESPUESTA.replace("77903754", "77905741")}) {
            givenRespuesta(json);
        }

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> catalogo.buscar(CODIGO, AHORA))
                    .isInstanceOf(CatalogoNoDisponibleException.class);
        }
    }

    @Test
    @DisplayName("La caché vence y el límite de consultas se recupera al minuto siguiente")
    void buscarShouldRespectExpiryAndRateLimit() {
        givenRespuesta(RESPUESTA);
        givenRespuesta(RESPUESTA);
        var builder = RestClient.builder().baseUrl("https://catalogo.test");
        var limitadoServidor = MockRestServiceServer.bindTo(builder).build();
        limitadoServidor.expect(requestTo(givenUrl())).andRespond(withStatus(HttpStatus.NOT_FOUND));
        limitadoServidor.expect(requestTo(givenUrl())).andRespond(withStatus(HttpStatus.NOT_FOUND));
        var limitado = new OpenFoodFactsCatalogo(builder.build(), Duration.ZERO, 1);

        catalogo.buscar(CODIGO, AHORA);
        catalogo.buscar(CODIGO, AHORA.plusSeconds(900));
        limitado.buscar(CODIGO, AHORA);

        assertThatThrownBy(() -> limitado.buscar(CODIGO, AHORA.plusSeconds(1)))
                .isInstanceOf(CatalogoNoDisponibleException.class);
        assertThat(limitado.buscar(CODIGO, AHORA.plusSeconds(60))).isEmpty();
        servidor.verify();
        limitadoServidor.verify();
    }

    @Test
    @DisplayName("La caché limita su tamaño y elimina la entrada más vieja")
    void buscarShouldBoundCacheSize() {
        var builder = RestClient.builder().baseUrl("https://catalogo.test");
        var muchas = MockRestServiceServer.bindTo(builder).build();
        var grande = new OpenFoodFactsCatalogo(builder.build(), Duration.ofDays(1), 2000);
        for (int i = 0; i < 1002; i++) {
            muchas.expect(requestTo(startsWith("https://catalogo.test/")))
                    .andRespond(withStatus(HttpStatus.NOT_FOUND));
        }

        for (int i = 1; i <= 1001; i++) {
            grande.buscar(new CodigoBarras(givenCodigo(i)), AHORA);
        }
        grande.buscar(new CodigoBarras(givenCodigo(1)), AHORA);

        muchas.verify();
    }

    // --- arrange ---
    private void givenRespuesta(String json) {
        servidor.expect(requestTo(givenUrl())).andExpect(headerDoesNotExist("Cookie"))
                .andExpect(headerDoesNotExist("Authorization"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    private String givenUrl() {
        return "https://catalogo.test/api/v3/product/77903754?product_type=all&lc=es&fields="
                + "code,product_name,product_name_es,brands,quantity,image_front_url";
    }

    private String givenCodigo(int numero) {
        String base = "%07d".formatted(numero);
        int suma = 0;
        for (int i = 6, peso = 3; i >= 0; i--, peso = 4 - peso) {
            suma += (base.charAt(i) - '0') * peso;
        }
        return base + ((10 - suma % 10) % 10);
    }
}
