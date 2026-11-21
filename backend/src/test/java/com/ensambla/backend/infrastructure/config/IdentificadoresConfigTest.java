package com.ensambla.backend.infrastructure.config;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.common.Identificador;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.format.support.DefaultFormattingConversionService;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class IdentificadoresConfigTest {

    private static final String TEXTO = "b0000000-0000-4000-8000-000000000001";
    private static final Fila FILA = new Fila(BloqueId.de(TEXTO), Map.of("monto", CampoId.de(TEXTO)));

    private final IdentificadoresConfig config = new IdentificadoresConfig();
    private final JsonMapper json = JsonMapper.builder().addModule(config.identificadoresModule()).build();
    private Throwable error;

    /** Un id sin {@code de(String)}: no lo puede leer ni el JSON ni una ruta. */
    record SinFabrica(UUID valor) implements Identificador {
    }

    /** Un id cuyo {@code de(String)} falla por algo que no es el texto. */
    record FabricaRota(UUID valor) implements Identificador {

        public static FabricaRota de(String valor) {
            throw new IllegalStateException("rota");
        }
    }

    record Fila(BloqueId bloque, Map<String, CampoId> campos) {
    }

    @Test
    @DisplayName("En el JSON un id se escribe como el texto de su UUID")
    void enElJsonUnIdSeEscribeComoElTextoDeSuUuid() {
        entoncesSeEscribeComo(FILA, "{\"bloque\":\"" + TEXTO + "\",\"campos\":{\"monto\":\"" + TEXTO + "\"}}");
    }

    @Test
    @DisplayName("En el JSON un id se vuelve a leer igual")
    void enElJsonUnIdSeVuelveALeerIgual() {
        entoncesSeLeeIgual(FILA);
    }

    @Test
    @DisplayName("Un texto que no es UUID no se lee como id")
    void unTextoQueNoEsUuidNoSeLeeComoId() {
        cuandoSeIntenta(() -> json.readValue("{\"bloque\":\"no-es-uuid\"}", Fila.class));

        entoncesFallaPor(DatabindException.class);
    }

    @Test
    @DisplayName("Las rutas y parámetros se convierten al id de su tipo")
    void lasRutasSeConviertenAlIdDeSuTipo() {
        entoncesLaRutaSeConvierteEn(" " + TEXTO + " ", BloqueId.de(TEXTO));
    }

    @Test
    @DisplayName("Un texto que no es UUID se rechaza como argumento inválido")
    void unTextoQueNoEsUuidSeRechazaComoArgumentoInvalido() {
        cuandoSeIntenta(() -> IdentificadoresConfig.crear(BloqueId.class, "no-es-uuid"));

        entoncesFallaPor(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Un id sin de(String) es un error de programación")
    void unIdSinFabricaEsUnErrorDeProgramacion() {
        cuandoSeIntenta(() -> IdentificadoresConfig.crear(SinFabrica.class, TEXTO));

        entoncesFallaPorque(IllegalStateException.class, "SinFabrica no tiene de(String)");
    }

    @Test
    @DisplayName("Si de(String) falla por otra razón no se confunde con un texto inválido")
    void siLaFabricaFallaPorOtraRazonNoSeConfunde() {
        cuandoSeIntenta(() -> IdentificadoresConfig.crear(FabricaRota.class, TEXTO));

        entoncesFallaPorque(IllegalStateException.class, "No se pudo leer el id FabricaRota");
        entoncesLaCausaEs("rota");
    }

    @Test
    @DisplayName("Los tipos que no son ids siguen con su lectura de siempre")
    void losTiposQueNoSonIdsSiguenConSuLectura() {
        entoncesElMapaSeLeeComoSiempre("{\"monto\":3}", "monto", 3);
    }

    private void cuandoSeIntenta(ThrowingCallable accion) {
        error = catchThrowable(accion);
    }

    private void entoncesSeEscribeComo(Fila fila, String esperado) {
        assertThat(json.writeValueAsString(fila)).isEqualTo(esperado);
    }

    private void entoncesSeLeeIgual(Fila fila) {
        assertThat(json.readValue(json.writeValueAsString(fila), Fila.class)).isEqualTo(fila);
    }

    private void entoncesLaRutaSeConvierteEn(String ruta, BloqueId esperado) {
        DefaultFormattingConversionService conversiones = new DefaultFormattingConversionService();
        config.addFormatters(conversiones);
        assertThat(conversiones.convert(ruta, BloqueId.class)).isEqualTo(esperado);
    }

    private void entoncesFallaPor(Class<? extends Throwable> tipo) {
        assertThat(error).isInstanceOf(tipo);
    }

    private void entoncesFallaPorque(Class<? extends Throwable> tipo, String mensaje) {
        assertThat(error).isInstanceOf(tipo).hasMessage(mensaje);
    }

    private void entoncesLaCausaEs(String mensaje) {
        assertThat(error).cause().hasMessage(mensaje);
    }

    private void entoncesElMapaSeLeeComoSiempre(String texto, String clave, Object valor) {
        assertThat(json.readValue(texto, Map.class)).containsEntry(clave, valor);
    }
}