package com.ensambla.backend.infrastructure.catalogo;

import com.ensambla.backend.domain.exception.CatalogoNoDisponibleException;
import com.ensambla.backend.domain.model.catalogo.CodigoBarras;
import com.ensambla.backend.domain.model.catalogo.ProductoCatalogo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.Matchers.startsWith;
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
    private static final String URL_DEL_PRODUCTO = "https://catalogo.test/api/v3/product/77903754"
            + "?product_type=all&lc=es&fields=code,product_name,product_name_es,brands,quantity,image_front_url";

    private MockRestServiceServer servidor;
    private OpenFoodFactsCatalogo catalogo;
    private MockRestServiceServer servidorLimitado;
    private OpenFoodFactsCatalogo catalogoLimitado;
    private MockRestServiceServer servidorDeMuchos;
    private OpenFoodFactsCatalogo catalogoGrande;
    private Optional<ProductoCatalogo> encontrado;
    private Optional<ProductoCatalogo> repetido;
    private Throwable rechazo;

    static Stream<String> respuestasMalformadas() {
        return Stream.of("{", "{}", RESPUESTA.replace("77903754", "77905741"));
    }

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("https://catalogo.test");
        servidor = MockRestServiceServer.bindTo(builder).build();
        catalogo = new OpenFoodFactsCatalogo(builder.build(), Duration.ofMinutes(15), 12);
    }

    @Test
    @DisplayName("Consulta sólo campos públicos, sin credenciales, y reutiliza la caché por GTIN")
    void consultaSoloCamposPublicosSinCredencialesYReutilizaLaCachePorGtin() {
        dadoQueElProveedorResponde(RESPUESTA);

        cuandoSeBuscaElCodigoYSuEquivalenteConCeros();

        entoncesSeNormalizaElProductoPublico();
        entoncesLaSegundaBusquedaSaleDeLaCache();
    }

    @Test
    @DisplayName("Usa el nombre alternativo y descarta una imagen de otro sitio")
    void usaElNombreAlternativoYDescartaUnaImagenDeOtroSitio() {
        dadoQueElProveedorResponde(RESPUESTA.replace("Alfajor completo", " ")
                .replace("https://images.openfoodfacts.org", "https://otro.test"));

        cuandoSeBuscaElCodigo();

        entoncesElNombreEsElAlternativoYNoHayImagen();
    }

    @Test
    @DisplayName("El 404 se conserva como ausencia, incluida en la caché")
    void elNoEncontradoSeConservaComoAusenciaIncluidaEnLaCache() {
        dadoQueElProveedorNoEncuentraElProducto();

        cuandoSeBuscaElCodigoDosVeces();

        entoncesAmbasBusquedasVienenVacias();
    }

    @Test
    @DisplayName("Un error del proveedor no se cachea y permite reintentar")
    void unErrorDelProveedorNoSeCacheaYPermiteReintentar() {
        dadoQueElProveedorFallaUnaVezYDespuesResponde();

        cuandoSeBuscaConUnErrorYDespuesSeReintenta();

        entoncesElPrimerIntentoFallaYElReintentoEncuentraElProducto();
    }

    @ParameterizedTest
    @MethodSource("respuestasMalformadas")
    @DisplayName("Rechaza JSON inválido y productos que corresponden a otro código")
    void rechazaJsonInvalidoYProductosQueCorrespondenAOtroCodigo(String json) {
        dadoQueElProveedorResponde(json);

        cuandoSeIntentaBuscarElCodigo();

        entoncesElCatalogoNoEstaDisponible();
    }

    @Test
    @DisplayName("La caché vence a los quince minutos y se vuelve a consultar al proveedor")
    void laCacheVenceALosQuinceMinutosYSeVuelveAConsultarAlProveedor() {
        dadoQueElProveedorRespondeDosVeces();

        cuandoSeBuscaAntesYDespuesDeQueVenzaLaCache();

        entoncesSeConsultoDosVecesAlProveedor();
    }

    @Test
    @DisplayName("El límite de consultas se recupera al minuto siguiente")
    void elLimiteDeConsultasSeRecuperaAlMinutoSiguiente() {
        dadoUnCatalogoLimitadoAUnaConsultaPorMinuto();

        cuandoSeSuperaElLimiteYPasaUnMinuto();

        entoncesElLimiteRechazaYDespuesSeRecupera();
    }

    @Test
    @DisplayName("La caché limita su tamaño y elimina la entrada más vieja")
    void laCacheLimitaSuTamanoYEliminaLaEntradaMasVieja() {
        dadoUnCatalogoConCacheLimitadaYMilConsultas();

        cuandoSeBuscanMilUnProductosYSeRepiteElPrimero();

        entoncesElPrimeroSeVolvioAConsultar();
    }

    // --- dado ---

    private void dadoQueElProveedorResponde(String json) {
        servidor.expect(requestTo(URL_DEL_PRODUCTO)).andExpect(headerDoesNotExist("Cookie"))
                .andExpect(headerDoesNotExist("Authorization"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    private void dadoQueElProveedorNoEncuentraElProducto() {
        servidor.expect(requestTo(URL_DEL_PRODUCTO)).andRespond(withStatus(HttpStatus.NOT_FOUND));
    }

    private void dadoQueElProveedorFallaUnaVezYDespuesResponde() {
        servidor.expect(requestTo(URL_DEL_PRODUCTO)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        dadoQueElProveedorResponde(RESPUESTA);
    }

    private void dadoQueElProveedorRespondeDosVeces() {
        dadoQueElProveedorResponde(RESPUESTA);
        dadoQueElProveedorResponde(RESPUESTA);
    }

    private void dadoUnCatalogoLimitadoAUnaConsultaPorMinuto() {
        var builder = RestClient.builder().baseUrl("https://catalogo.test");
        servidorLimitado = MockRestServiceServer.bindTo(builder).build();
        servidorLimitado.expect(requestTo(URL_DEL_PRODUCTO)).andRespond(withStatus(HttpStatus.NOT_FOUND));
        servidorLimitado.expect(requestTo(URL_DEL_PRODUCTO)).andRespond(withStatus(HttpStatus.NOT_FOUND));
        catalogoLimitado = new OpenFoodFactsCatalogo(builder.build(), Duration.ZERO, 1);
    }

    private void dadoUnCatalogoConCacheLimitadaYMilConsultas() {
        var builder = RestClient.builder().baseUrl("https://catalogo.test");
        servidorDeMuchos = MockRestServiceServer.bindTo(builder).build();
        catalogoGrande = new OpenFoodFactsCatalogo(builder.build(), Duration.ofDays(1), 2000);
        for (int i = 0; i < 1002; i++) {
            servidorDeMuchos.expect(requestTo(startsWith("https://catalogo.test/")))
                    .andRespond(withStatus(HttpStatus.NOT_FOUND));
        }
    }

    // --- cuando ---

    private void cuandoSeBuscaElCodigo() {
        encontrado = catalogo.buscar(CODIGO, AHORA);
    }

    private void cuandoSeIntentaBuscarElCodigo() {
        rechazo = catchThrowable(this::cuandoSeBuscaElCodigo);
    }

    private void cuandoSeBuscaElCodigoYSuEquivalenteConCeros() {
        encontrado = catalogo.buscar(CODIGO, AHORA);
        repetido = catalogo.buscar(new CodigoBarras("00000077903754"), AHORA.plusSeconds(1));
    }

    private void cuandoSeBuscaElCodigoDosVeces() {
        encontrado = catalogo.buscar(CODIGO, AHORA);
        repetido = catalogo.buscar(CODIGO, AHORA.plusSeconds(2));
    }

    private void cuandoSeBuscaConUnErrorYDespuesSeReintenta() {
        rechazo = catchThrowable(() -> catalogo.buscar(CODIGO, AHORA));
        encontrado = catalogo.buscar(CODIGO, AHORA.plusSeconds(1));
    }

    private void cuandoSeBuscaAntesYDespuesDeQueVenzaLaCache() {
        catalogo.buscar(CODIGO, AHORA);
        catalogo.buscar(CODIGO, AHORA.plusSeconds(900));
    }

    private void cuandoSeSuperaElLimiteYPasaUnMinuto() {
        catalogoLimitado.buscar(CODIGO, AHORA);
        rechazo = catchThrowable(() -> catalogoLimitado.buscar(CODIGO, AHORA.plusSeconds(1)));
        encontrado = catalogoLimitado.buscar(CODIGO, AHORA.plusSeconds(60));
    }

    private void cuandoSeBuscanMilUnProductosYSeRepiteElPrimero() {
        for (int i = 1; i <= 1001; i++) {
            catalogoGrande.buscar(new CodigoBarras(unCodigoValido(i)), AHORA);
        }
        catalogoGrande.buscar(new CodigoBarras(unCodigoValido(1)), AHORA);
    }

    // --- entonces ---

    private void entoncesSeNormalizaElProductoPublico() {
        ProductoCatalogo producto = encontrado.orElseThrow();
        assertThat(producto.codigo()).isEqualTo("77903754");
        assertThat(producto.nombre()).isEqualTo("Alfajor completo");
        assertThat(producto.marca()).isEqualTo("Marca");
        assertThat(producto.presentacion()).isEqualTo("40 g");
        assertThat(producto.imagen()).isEqualTo("https://images.openfoodfacts.org/foto.jpg");
    }

    private void entoncesLaSegundaBusquedaSaleDeLaCache() {
        assertThat(repetido).isEqualTo(encontrado);
        servidor.verify();
    }

    private void entoncesElNombreEsElAlternativoYNoHayImagen() {
        assertThat(encontrado.orElseThrow().nombre()).isEqualTo("Alfajor");
        assertThat(encontrado.orElseThrow().imagen()).isNull();
    }

    private void entoncesAmbasBusquedasVienenVacias() {
        assertThat(encontrado).isEmpty();
        assertThat(repetido).isEmpty();
        servidor.verify();
    }

    private void entoncesElPrimerIntentoFallaYElReintentoEncuentraElProducto() {
        assertThat(rechazo).isInstanceOf(CatalogoNoDisponibleException.class);
        assertThat(encontrado).isPresent();
    }

    private void entoncesElCatalogoNoEstaDisponible() {
        assertThat(rechazo).isInstanceOf(CatalogoNoDisponibleException.class);
    }

    private void entoncesSeConsultoDosVecesAlProveedor() {
        servidor.verify();
    }

    private void entoncesElLimiteRechazaYDespuesSeRecupera() {
        assertThat(rechazo).isInstanceOf(CatalogoNoDisponibleException.class);
        assertThat(encontrado).isEmpty();
        servidorLimitado.verify();
    }

    private void entoncesElPrimeroSeVolvioAConsultar() {
        servidorDeMuchos.verify();
    }

    // --- datos ---

    private String unCodigoValido(int numero) {
        String base = "%07d".formatted(numero);
        int suma = 0;
        for (int i = 6, peso = 3; i >= 0; i--, peso = 4 - peso) {
            suma += (base.charAt(i) - '0') * peso;
        }
        return base + ((10 - suma % 10) % 10);
    }
}
