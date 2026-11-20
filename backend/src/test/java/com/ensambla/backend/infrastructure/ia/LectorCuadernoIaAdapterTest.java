package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.model.lectura.BloqueConocido;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.lectura.PaginaCuaderno;
import com.ensambla.backend.domain.model.lectura.TipoRegistroCuaderno;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LectorCuadernoIaAdapterTest {

    private static final int MAX_PAGINAS = 3;
    private static final PaginaCuaderno PAGINA = new PaginaCuaderno(1, new byte[]{1, 2, 3}, "image/jpeg");
    private static final List<BloqueConocido> SIN_BLOQUES = List.of();

    @Mock
    private ClienteModeloVision cliente;

    private LectorCuadernoIaAdapter adapter;
    private List<PaginaCuaderno> paginas = List.of(PAGINA);
    private LecturaCuaderno lectura;
    private Throwable rechazo;
    private LecturaCuadernoException errorDelCliente;
    private IllegalStateException fallaDelProveedor;

    @BeforeEach
    void setUp() {
        PromptLecturaCuaderno prompt = new PromptLecturaCuaderno(
                texto("instrucciones"), texto("{\"type\":\"object\"}"));
        adapter = new LectorCuadernoIaAdapter(cliente, prompt, JsonMapper.builder().build(), MAX_PAGINAS);
    }

    @Test
    @DisplayName("Lee las páginas, valida la respuesta y devuelve la lectura con su consumo")
    void leeLasPaginasValidaLaRespuestaYDevuelveLaLecturaConSuConsumo() {
        dadoQueElModeloResponde(lecturaDeEjemplo());

        cuandoSeLee();

        entoncesSeDevuelveLaLecturaConSuConsumo();
    }

    @Test
    @DisplayName("Le manda al modelo las instrucciones, el esquema, el contexto y las páginas")
    void leMandaAlModeloLasInstruccionesElEsquemaElContextoYLasPaginas() {
        dadoQueElModeloResponde(lecturaDeEjemplo());

        cuandoSeLee();

        entoncesElModeloRecibeInstruccionesEsquemaContextoYPaginas();
    }

    @Test
    @DisplayName("Rechaza un lote sin páginas sin llamar al modelo")
    void rechazaUnLoteSinPaginasSinLlamarAlModelo() {
        dadoUnLoteSinPaginas();

        cuandoSeIntentaLeer();

        entoncesSeRechazaElLoteSinLlamarAlModelo();
    }

    @Test
    @DisplayName("Rechaza un lote con más páginas que el máximo")
    void rechazaUnLoteConMasPaginasQueElMaximo() {
        dadoUnLoteConUnaPaginaDeMasQueElMaximo();

        cuandoSeIntentaLeer();

        entoncesSeRechazaPorLaCantidadDePaginas();
    }

    @Test
    @DisplayName("Traduce una falla del proveedor a un error de lectura conservando la causa")
    void traduceUnaFallaDelProveedorAUnErrorDeLecturaConservandoLaCausa() {
        dadoUnProveedorQueFallaPorTimeout();

        cuandoSeIntentaLeer();

        entoncesSeTraduceLaFallaConservandoLaCausa();
    }

    @Test
    @DisplayName("Deja pasar un error de lectura que ya viene del cliente")
    void dejaPasarUnErrorDeLecturaQueYaVieneDelCliente() {
        dadoUnClienteQueYaFallaConUnErrorDeLectura();

        cuandoSeIntentaLeer();

        entoncesSePropagaElMismoError();
    }

    @Test
    @DisplayName("Rechaza una respuesta vacía del modelo")
    void rechazaUnaRespuestaVaciaDelModelo() {
        dadoQueElModeloResponde("   ");

        cuandoSeIntentaLeer();

        entoncesSeRechazaElJson();
    }

    @Test
    @DisplayName("Rechaza una respuesta nula del modelo")
    void rechazaUnaRespuestaNulaDelModelo() {
        dadoQueElModeloResponde(null);

        cuandoSeIntentaLeer();

        entoncesSeRechazaElJson();
    }

    @Test
    @DisplayName("Rechaza una respuesta que no es JSON")
    void rechazaUnaRespuestaQueNoEsJson() {
        dadoQueElModeloResponde("Acá tenés la lectura: {");

        cuandoSeIntentaLeer();

        entoncesSeRechazaPorJsonMalformadoConSuCausa();
    }

    @Test
    @DisplayName("Rechaza propiedades repetidas en vez de conservar sólo la última")
    void rechazaPropiedadesRepetidasEnVezDeConservarSoloLaUltima() {
        dadoQueElModeloResponde(lecturaDeEjemplo().replace("\"tipoRegistro\": \"VENTAS\"",
                "\"tipoRegistro\": \"GASTOS\", \"tipoRegistro\": \"VENTAS\""));

        cuandoSeIntentaLeer();

        entoncesSeRechazaElJson();
    }

    @Test
    @DisplayName("Rechaza atributos extra que no pertenecen al contrato")
    void rechazaAtributosExtraQueNoPertenecenAlContrato() {
        dadoQueElModeloResponde(lecturaDeEjemplo().replace("\"tipoRegistro\":", "\"extra\": 1, \"tipoRegistro\":"));

        cuandoSeIntentaLeer();

        entoncesSeRechazaElJson();
    }

    @Test
    @DisplayName("Rechaza campos requeridos ausentes")
    void rechazaCamposRequeridosAusentes() {
        dadoQueElModeloResponde(lecturaDeEjemplo().replace("\"bloqueExistenteId\": null,", ""));

        cuandoSeIntentaLeer();

        entoncesSeRechazaElJson();
    }

    @Test
    @DisplayName("Rechaza contenido JSON adicional después de la respuesta")
    void rechazaContenidoJsonAdicionalDespuesDeLaRespuesta() {
        dadoQueElModeloResponde(lecturaDeEjemplo() + "{}");

        cuandoSeIntentaLeer();

        entoncesSeRechazaElJson();
    }

    // --- dado ---

    private void dadoQueElModeloResponde(String json) {
        when(cliente.completar(any())).thenReturn(new RespuestaModeloVision(json, "proveedor", "modelo-x", 1200, 300));
    }

    private void dadoUnLoteSinPaginas() {
        paginas = List.of();
    }

    private void dadoUnLoteConUnaPaginaDeMasQueElMaximo() {
        paginas = Collections.nCopies(MAX_PAGINAS + 1, PAGINA);
    }

    private void dadoUnProveedorQueFallaPorTimeout() {
        fallaDelProveedor = new IllegalStateException("timeout");
        when(cliente.completar(any())).thenThrow(fallaDelProveedor);
    }

    private void dadoUnClienteQueYaFallaConUnErrorDeLectura() {
        errorDelCliente = new LecturaCuadernoException("Límite diario alcanzado");
        when(cliente.completar(any())).thenThrow(errorDelCliente);
    }

    // --- cuando ---

    private void cuandoSeLee() {
        lectura = adapter.leer(paginas, SIN_BLOQUES, null);
    }

    private void cuandoSeIntentaLeer() {
        rechazo = catchThrowable(this::cuandoSeLee);
    }

    // --- entonces ---

    private void entoncesSeDevuelveLaLecturaConSuConsumo() {
        assertThat(lectura.tipoRegistro()).isEqualTo(TipoRegistroCuaderno.VENTAS);
        assertThat(lectura.filas()).hasSize(3);
        assertThat(lectura.consumo().proveedor()).isEqualTo("proveedor");
        assertThat(lectura.consumo().modelo()).isEqualTo("modelo-x");
        assertThat(lectura.consumo().versionPrompt()).isEqualTo(PromptLecturaCuaderno.VERSION);
        assertThat(lectura.consumo().paginas()).isEqualTo(1);
        assertThat(lectura.consumo().tokensEntrada()).isEqualTo(1200);
        assertThat(lectura.consumo().tokensSalida()).isEqualTo(300);
        assertThat(lectura.consumo().duracionMs()).isNotNegative();
    }

    private void entoncesElModeloRecibeInstruccionesEsquemaContextoYPaginas() {
        ArgumentCaptor<PeticionModeloVision> captor = ArgumentCaptor.forClass(PeticionModeloVision.class);
        verify(cliente).completar(captor.capture());
        PeticionModeloVision peticion = captor.getValue();
        assertThat(peticion.instrucciones()).isEqualTo("instrucciones");
        assertThat(peticion.esquemaJson()).isEqualTo("{\"type\":\"object\"}");
        assertThat(peticion.contexto()).isEqualTo(PromptLecturaCuaderno.SIN_BLOQUES);
        assertThat(peticion.paginas()).containsExactly(PAGINA);
    }

    private void entoncesSeRechazaElLoteSinLlamarAlModelo() {
        assertThat(rechazo).isInstanceOf(LecturaCuadernoException.class)
                .hasMessage("Se pueden leer entre 1 y 3 páginas por vez");
        verify(cliente, never()).completar(any());
    }

    private void entoncesSeRechazaPorLaCantidadDePaginas() {
        assertThat(rechazo).isInstanceOf(LecturaCuadernoException.class).hasMessageContaining("entre 1 y 3");
    }

    private void entoncesSeTraduceLaFallaConservandoLaCausa() {
        assertThat(rechazo).isInstanceOf(LecturaCuadernoException.class)
                .hasMessage(LectorCuadernoIaAdapter.ERROR_PROVEEDOR)
                .hasCause(fallaDelProveedor);
    }

    private void entoncesSePropagaElMismoError() {
        assertThat(rechazo).isSameAs(errorDelCliente);
    }

    private void entoncesSeRechazaElJson() {
        assertThat(rechazo).isInstanceOf(LecturaCuadernoException.class).hasMessage(LectorCuadernoIaAdapter.ERROR_JSON);
    }

    private void entoncesSeRechazaPorJsonMalformadoConSuCausa() {
        assertThat(rechazo).isInstanceOf(LecturaCuadernoException.class)
                .hasMessage(LectorCuadernoIaAdapter.ERROR_JSON)
                .hasCauseInstanceOf(JacksonException.class);
    }

    // --- datos ---

    private static String lecturaDeEjemplo() {
        return RecursoTexto.contenido(new ClassPathResource("ia/lectura-simulada.json"));
    }

    private static ByteArrayResource texto(String contenido) {
        return new ByteArrayResource(contenido.getBytes(StandardCharsets.UTF_8));
    }
}
