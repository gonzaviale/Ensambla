package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.model.enums.TipoRegistroCuaderno;
import com.ensambla.backend.domain.model.lectura.BloqueConocido;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.lectura.PaginaCuaderno;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @BeforeEach
    void setUp() {
        PromptLecturaCuaderno prompt = new PromptLecturaCuaderno(texto("instrucciones"), texto("{\"type\":\"object\"}"));
        adapter = new LectorCuadernoIaAdapter(cliente, prompt, JsonMapper.builder().build(), MAX_PAGINAS);
    }

    @Test
    @DisplayName("Lee las páginas, valida la respuesta y devuelve la lectura con su consumo")
    void leerShouldReturnLecturaWithConsumoWhenModelAnswersValidJson() {
        givenModelAnswers(lecturaDeEjemplo());

        LecturaCuaderno result = adapter.leer(List.of(PAGINA), SIN_BLOQUES);

        thenLecturaHasConsumo(result);
    }

    @Test
    @DisplayName("Le manda al modelo las instrucciones, el esquema, el contexto y las páginas")
    void leerShouldSendPromptSchemaAndPagesToModel() {
        givenModelAnswers(lecturaDeEjemplo());

        adapter.leer(List.of(PAGINA), SIN_BLOQUES);

        thenModelReceived();
    }

    @Test
    @DisplayName("Rechaza un lote sin páginas sin llamar al modelo")
    void leerShouldRejectEmptyLoteWithoutCallingModel() {
        assertThatThrownBy(() -> adapter.leer(List.of(), SIN_BLOQUES))
                .isInstanceOf(LecturaCuadernoException.class)
                .hasMessage("Se pueden leer entre 1 y 3 páginas por vez");
        verify(cliente, never()).completar(any());
    }

    @Test
    @DisplayName("Rechaza un lote con más páginas que el máximo")
    void leerShouldRejectLoteAboveMaxPaginas() {
        List<PaginaCuaderno> paginas = Collections.nCopies(MAX_PAGINAS + 1, PAGINA);

        assertThatThrownBy(() -> adapter.leer(paginas, SIN_BLOQUES))
                .isInstanceOf(LecturaCuadernoException.class)
                .hasMessageContaining("entre 1 y 3");
    }

    @Test
    @DisplayName("Traduce una falla del proveedor a un error de lectura conservando la causa")
    void leerShouldWrapProviderFailure() {
        IllegalStateException falla = new IllegalStateException("timeout");
        when(cliente.completar(any())).thenThrow(falla);

        assertThatThrownBy(() -> adapter.leer(List.of(PAGINA), SIN_BLOQUES))
                .isInstanceOf(LecturaCuadernoException.class)
                .hasMessage(LectorCuadernoIaAdapter.ERROR_PROVEEDOR)
                .hasCause(falla);
    }

    @Test
    @DisplayName("Deja pasar un error de lectura que ya viene del cliente")
    void leerShouldRethrowLecturaCuadernoExceptionFromClient() {
        LecturaCuadernoException error = new LecturaCuadernoException("Límite diario alcanzado");
        when(cliente.completar(any())).thenThrow(error);

        assertThatThrownBy(() -> adapter.leer(List.of(PAGINA), SIN_BLOQUES)).isSameAs(error);
    }

    @Test
    @DisplayName("Rechaza una respuesta vacía del modelo")
    void leerShouldRejectBlankJson() {
        givenModelAnswers("   ");

        thenRejectsJson();
    }

    @Test
    @DisplayName("Rechaza una respuesta nula del modelo")
    void leerShouldRejectNullJson() {
        givenModelAnswers(null);

        thenRejectsJson();
    }

    @Test
    @DisplayName("Rechaza una respuesta que no es JSON")
    void leerShouldRejectMalformedJson() {
        givenModelAnswers("Acá tenés la lectura: {");

        assertThatThrownBy(() -> adapter.leer(List.of(PAGINA), SIN_BLOQUES))
                .isInstanceOf(LecturaCuadernoException.class)
                .hasMessage(LectorCuadernoIaAdapter.ERROR_JSON)
                .hasCauseInstanceOf(tools.jackson.core.JacksonException.class);
    }

    // --- arrange ---
    private void givenModelAnswers(String json) {
        when(cliente.completar(any())).thenReturn(new RespuestaModeloVision(json, "proveedor", "modelo-x", 1200, 300));
    }

    private static String lecturaDeEjemplo() {
        return RecursoTexto.contenido(new ClassPathResource("ia/lectura-simulada.json"));
    }

    private static ByteArrayResource texto(String contenido) {
        return new ByteArrayResource(contenido.getBytes(StandardCharsets.UTF_8));
    }

    // --- assert ---
    private void thenLecturaHasConsumo(LecturaCuaderno result) {
        assertThat(result.tipoRegistro()).isEqualTo(TipoRegistroCuaderno.VENTAS);
        assertThat(result.filas()).hasSize(3);
        assertThat(result.consumo().proveedor()).isEqualTo("proveedor");
        assertThat(result.consumo().modelo()).isEqualTo("modelo-x");
        assertThat(result.consumo().versionPrompt()).isEqualTo(PromptLecturaCuaderno.VERSION);
        assertThat(result.consumo().paginas()).isEqualTo(1);
        assertThat(result.consumo().tokensEntrada()).isEqualTo(1200);
        assertThat(result.consumo().tokensSalida()).isEqualTo(300);
        assertThat(result.consumo().duracionMs()).isNotNegative();
    }

    private void thenModelReceived() {
        ArgumentCaptor<PeticionModeloVision> captor = ArgumentCaptor.forClass(PeticionModeloVision.class);
        verify(cliente).completar(captor.capture());
        PeticionModeloVision peticion = captor.getValue();
        assertThat(peticion.instrucciones()).isEqualTo("instrucciones");
        assertThat(peticion.esquemaJson()).isEqualTo("{\"type\":\"object\"}");
        assertThat(peticion.contexto()).isEqualTo(PromptLecturaCuaderno.SIN_BLOQUES);
        assertThat(peticion.paginas()).containsExactly(PAGINA);
    }

    private void thenRejectsJson() {
        assertThatThrownBy(() -> adapter.leer(List.of(PAGINA), SIN_BLOQUES))
                .isInstanceOf(LecturaCuadernoException.class)
                .hasMessage(LectorCuadernoIaAdapter.ERROR_JSON);
    }
}
