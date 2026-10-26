package com.ensambla.backend.domain.model.registro;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.zona.ZonaId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenciaTest {

    private static final RegistroId REGISTRO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final CampoId PRECIO_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final CampoId NOMBRE_ID = CampoId.de("c0000000-0000-4000-8000-000000000002");
    private static final ZonaId ZONA_ID = ZonaId.de("c0000000-0000-4000-8000-000000000003");
    private static final CampoId ALIAS_ID = CampoId.de("c0000000-0000-4000-8000-000000000004");

    @Test
    @DisplayName("La referencia lleva el id del registro y el valor del primer campo de texto")
    void deShouldUseFirstTextoCampo() {
        List<Campo> campos = List.of(campo(PRECIO_ID.valor(), TipoCampo.NUMERO, 0),
                campo(NOMBRE_ID.valor(), TipoCampo.TEXTO, 1));

        Referencia result = Referencia.de(registro(Map.of(PRECIO_ID.toString(), 10, NOMBRE_ID.toString(), "Ana")),
                campos);

        assertThat(result.id()).isEqualTo(REGISTRO_ID);
        assertThat(result.etiqueta()).isEqualTo("Ana");
    }

    @Test
    @DisplayName("Un campo de opción también sirve de etiqueta")
    void deShouldUseOpcionCampo() {
        List<Campo> campos = List.of(campo(PRECIO_ID.valor(), TipoCampo.NUMERO, 0),
                campo(ZONA_ID.valor(), TipoCampo.OPCION, 1));

        Referencia result = Referencia.de(registro(Map.of(ZONA_ID.toString(), "Norte")), campos);

        assertThat(result.etiqueta()).isEqualTo("Norte");
    }

    @Test
    @DisplayName("Elige el campo descriptivo de menor posición aunque la lista venga desordenada")
    void deShouldOrderCamposByPosicion() {
        List<Campo> campos = List.of(campo(ZONA_ID.valor(), TipoCampo.OPCION, 3),
                campo(NOMBRE_ID.valor(), TipoCampo.TEXTO, 1));

        Referencia result = Referencia.de(
                registro(Map.of(ZONA_ID.toString(), "Norte", NOMBRE_ID.toString(), "Ana")), campos);

        assertThat(result.etiqueta()).isEqualTo("Ana");
    }

    @Test
    @DisplayName("Con la misma posición desempata el id del campo")
    void deShouldBreakTiesById() {
        List<Campo> campos = List.of(campo(ALIAS_ID.valor(), TipoCampo.TEXTO, 0),
                campo(NOMBRE_ID.valor(), TipoCampo.TEXTO, 0));

        Referencia result = Referencia.de(
                registro(Map.of(ALIAS_ID.toString(), "Anita", NOMBRE_ID.toString(), "Ana")), campos);

        assertThat(result.etiqueta()).isEqualTo("Ana");
    }

    @Test
    @DisplayName("Sin campos de texto ni de opción usa el primer campo, convertido a texto")
    void deShouldFallBackToFirstCampo() {
        List<Campo> campos = List.of(campo(ZONA_ID.valor(), TipoCampo.SI_NO, 1),
                campo(PRECIO_ID.valor(), TipoCampo.NUMERO, 0));

        Referencia result = Referencia.de(registro(Map.of(PRECIO_ID.toString(), 12.5)), campos);

        assertThat(result.etiqueta()).isEqualTo("12.5");
    }

    @Test
    @DisplayName("Si la fila no cargó el dato de la etiqueta queda \"Sin nombre\"")
    void deShouldUseSinNombreWhenValueIsMissing() {
        Referencia result = Referencia.de(registro(Map.of()), List.of(campo(NOMBRE_ID.valor(), TipoCampo.TEXTO, 0)));

        assertThat(result.etiqueta()).isEqualTo(Referencia.SIN_NOMBRE);
    }

    @Test
    @DisplayName("Un dato en blanco cuenta como faltante")
    void deShouldUseSinNombreWhenValueIsBlank() {
        Referencia result = Referencia.de(
                registro(Map.of(NOMBRE_ID.toString(), "  ")), List.of(campo(NOMBRE_ID.valor(), TipoCampo.TEXTO, 0)));

        assertThat(result.etiqueta()).isEqualTo(Referencia.SIN_NOMBRE);
    }

    @Test
    @DisplayName("Un bloque sin campos da \"Sin nombre\"")
    void deShouldUseSinNombreWhenBloqueHasNoCampos() {
        Referencia result = Referencia.de(registro(Map.of(NOMBRE_ID.toString(), "Ana")), List.of());

        assertThat(result.etiqueta()).isEqualTo("Sin nombre");
    }

    @Test
    @DisplayName("El etiquetador elige el campo una vez y lo aplica a cada registro")
    void etiquetadorShouldApplySameCampoToEveryRegistro() {
        Function<Registro, Referencia> etiquetador = Referencia.etiquetador(
                List.of(campo(NOMBRE_ID.valor(), TipoCampo.TEXTO, 0)));

        List<String> result = List.of(
                        registro(Map.of(NOMBRE_ID.toString(), "Ana")),
                        registro(Map.of(NOMBRE_ID.toString(), "Beto")))
                .stream()
                .map(etiquetador)
                .map(Referencia::etiqueta)
                .toList();

        assertThat(result).containsExactly("Ana", "Beto");
    }

    // --- helpers ---
    private Campo campo(UUID id, TipoCampo tipo, int posicion) {
        return Campo.builder()
                .id(CampoId.de(id))
                .tipo(tipo)
                .posicion((short) posicion)
                .build();
    }

    private Registro registro(Map<String, Object> valores) {
        return Registro.builder()
                .id(REGISTRO_ID)
                .valores(valores)
                .build();
    }
}
