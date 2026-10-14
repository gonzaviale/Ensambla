package com.ensambla.backend.domain.service.procesamiento;

import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.domain.model.lectura.BloquePropuesto;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.DatoLeido;
import com.ensambla.backend.domain.model.lectura.FilaLeida;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.support.ProcesamientoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidacionLecturaServiceTest {

    private final ValidacionLecturaService validacionLecturaService = new ValidacionLecturaService();

    @ParameterizedTest
    @CsvSource({"NUMERO,10.25", "FECHA,2026-10-10", "SI_NO,true", "SI_NO,false", "OPCION,A", "TEXTO,algo"})
    @DisplayName("Acepta valores con el formato del tipo declarado")
    void validarShouldAcceptTypedValues(TipoCampo tipo, String valor) {
        LecturaCuaderno lectura = ProcesamientoTestData.lectura(tipo, valor, List.of("A"));

        assertThatCode(() -> validacionLecturaService.validar(lectura)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({"NUMERO,no", "FECHA,2026-02-30", "SI_NO,si", "OPCION,B"})
    @DisplayName("Rechaza valores incompatibles con el tipo")
    void validarShouldRejectInvalidTypedValues(TipoCampo tipo, String valor) {
        LecturaCuaderno lectura = ProcesamientoTestData.lectura(tipo, valor, List.of("A"));

        assertThatThrownBy(() -> validacionLecturaService.validar(lectura))
                .isInstanceOf(LecturaCuadernoException.class).hasMessageContaining("no coincide");
    }

    @Test
    @DisplayName("Acepta el texto leído en un campo de conexión: después se resuelve a una fila")
    void validarShouldAcceptTextInRelacionCampo() {
        LecturaCuaderno lectura = ProcesamientoTestData.lectura(TipoCampo.RELACION, "Juan Pérez", List.of());

        assertThatCode(() -> validacionLecturaService.validar(lectura)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Conserva un valor ilegible nulo")
    void validarShouldAcceptNullValue() {
        LecturaCuaderno lectura = ProcesamientoTestData.lectura(TipoCampo.NUMERO, null, List.of());

        assertThatCode(() -> validacionLecturaService.validar(lectura)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Rechaza dos filas con la misma página y renglón")
    void validarShouldRejectDuplicateRows() {
        LecturaCuaderno base = ProcesamientoTestData.lectura();
        LecturaCuaderno lectura = givenLectura(
                base.bloque(), List.of(base.filas().getFirst(), base.filas().getFirst()));

        assertThatThrownBy(() -> validacionLecturaService.validar(lectura)).hasMessageContaining("repetido");
    }

    @Test
    @DisplayName("Rechaza una confianza no finita")
    void validarShouldRejectNonFiniteConfidence() {
        LecturaCuaderno lectura = givenLectura(ProcesamientoTestData.lectura().bloque(),
                List.of(new FilaLeida(1, 1, Map.of("monto", new DatoLeido("1", Double.NaN)))));

        assertThatThrownBy(() -> validacionLecturaService.validar(lectura)).hasMessageContaining("finito");
    }

    @Test
    @DisplayName("Rechaza nombres mayores al límite del esquema")
    void validarShouldRejectLongNames() {
        BloquePropuesto bloque = new BloquePropuesto(
                "a".repeat(121), null, ProcesamientoTestData.lectura().bloque().campos());

        assertThatThrownBy(() -> validacionLecturaService.validar(givenLectura(bloque, List.of())))
                .hasMessageContaining("120");
    }

    @Test
    @DisplayName("Rechaza claves inválidas")
    void validarShouldRejectInvalidKeys() {
        BloquePropuesto bloque = new BloquePropuesto("Ventas", null,
                List.of(new CampoPropuesto("No-valida", "Monto", TipoCampo.NUMERO, List.of())));

        assertThatThrownBy(() -> validacionLecturaService.validar(givenLectura(bloque, List.of())))
                .hasMessageContaining("clave");
    }

    @Test
    @DisplayName("Rechaza opciones vacías")
    void validarShouldRejectBlankOptions() {
        LecturaCuaderno lectura = ProcesamientoTestData.lectura(TipoCampo.OPCION, "A", List.of("A", " "));

        assertThatThrownBy(() -> validacionLecturaService.validar(lectura)).hasMessageContaining("opciones");
    }

    @Test
    @DisplayName("Rechaza opciones duplicadas")
    void validarShouldRejectDuplicateOptions() {
        LecturaCuaderno lectura = ProcesamientoTestData.lectura(TipoCampo.OPCION, "A", List.of("A", "A"));

        assertThatThrownBy(() -> validacionLecturaService.validar(lectura)).hasMessageContaining("opciones");
    }

    private LecturaCuaderno givenLectura(BloquePropuesto bloque, List<FilaLeida> filas) {
        LecturaCuaderno base = ProcesamientoTestData.lectura();
        return new LecturaCuaderno(base.tipoRegistro(), bloque, filas, List.of(), base.consumo());
    }

    @Test
    @DisplayName("Rechaza un valor en un campo fórmula: la lectura de la IA no propone fórmulas")
    void validarShouldRejectFormulaValues() {
        LecturaCuaderno lectura = ProcesamientoTestData.lectura(TipoCampo.FORMULA, "10", List.of());

        assertThatThrownBy(() -> validacionLecturaService.validar(lectura))
                .isInstanceOf(LecturaCuadernoException.class).hasMessageContaining("no coincide");
    }
}
