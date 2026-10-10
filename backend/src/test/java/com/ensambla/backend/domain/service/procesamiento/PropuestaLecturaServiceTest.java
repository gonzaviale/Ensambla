package com.ensambla.backend.domain.service.procesamiento;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.domain.model.procesamiento.BloqueLectura;
import com.ensambla.backend.domain.model.procesamiento.CampoConocido;
import com.ensambla.backend.domain.model.procesamiento.PropuestaLectura;
import com.ensambla.backend.support.ProcesamientoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropuestaLecturaServiceTest {

    private final PropuestaLecturaService propuestaLecturaService = new PropuestaLecturaService();

    @Test
    @DisplayName("Propone un bloque nuevo cuando el negocio no tiene bloques")
    void proponerShouldProposeNewBlockWhenEmpty() {
        PropuestaLectura propuesta = propuestaLecturaService.proponer(ProcesamientoTestData.lectura(), List.of(), null);

        assertThat(propuesta.lectura().bloque().bloqueExistenteId()).isNull();
        assertThat(propuesta.mapeoCampos()).isEmpty();
    }

    @Test
    @DisplayName("Reutiliza el mismo bloque y mapea columnas compatibles ignorando acentos")
    void proponerShouldReuseCompatibleBlock() {
        BloqueLectura bloque = givenBlock(" Véntas ", "Mónto", TipoCampo.NUMERO, List.of());

        PropuestaLectura propuesta = propuestaLecturaService.proponer(
                ProcesamientoTestData.lectura(), List.of(bloque), null);

        assertThat(propuesta.lectura().bloque().bloqueExistenteId()).isEqualTo(bloque.id());
        assertThat(propuesta.mapeoCampos()).containsEntry("monto", ProcesamientoTestData.CAMPO_ID);
        assertThat(propuesta.lectura().filas()).isEqualTo(ProcesamientoTestData.lectura().filas());
    }

    @Test
    @DisplayName("Deja una propuesta nueva si el nombre del bloque no coincide")
    void proponerShouldRejectDifferentBlockName() {
        BloqueLectura bloque = givenBlock("Gastos", "Monto", TipoCampo.NUMERO, List.of());

        PropuestaLectura propuesta = propuestaLecturaService.proponer(
                ProcesamientoTestData.lectura(), List.of(bloque), null);

        assertThat(propuesta.mapeoCampos()).isEmpty();
    }

    @Test
    @DisplayName("No reutiliza columnas con nombres distintos")
    void proponerShouldRejectDifferentFieldName() {
        BloqueLectura bloque = givenBlock("Ventas", "Cantidad", TipoCampo.NUMERO, List.of());

        PropuestaLectura propuesta = propuestaLecturaService.proponer(
                ProcesamientoTestData.lectura(), List.of(bloque), null);

        assertThat(propuesta.mapeoCampos()).isEmpty();
    }

    @Test
    @DisplayName("No reutiliza columnas con tipos incompatibles")
    void proponerShouldRejectDifferentFieldType() {
        BloqueLectura bloque = givenBlock("Ventas", "Monto", TipoCampo.TEXTO, List.of());

        PropuestaLectura propuesta = propuestaLecturaService.proponer(
                ProcesamientoTestData.lectura(), List.of(bloque), null);

        assertThat(propuesta.mapeoCampos()).isEmpty();
    }

    @Test
    @DisplayName("No reutiliza columnas cuyas opciones no incluyen las leídas")
    void proponerShouldRejectDifferentOptions() {
        BloqueLectura bloque = givenBlock("Ventas", "Monto", TipoCampo.OPCION, List.of("A"));

        PropuestaLectura propuesta = propuestaLecturaService.proponer(
                ProcesamientoTestData.lectura(TipoCampo.OPCION, "B", List.of("B")), List.of(bloque), null);

        assertThat(propuesta.mapeoCampos()).isEmpty();
    }

    @Test
    @DisplayName("No decide automáticamente entre dos bloques compatibles")
    void proponerShouldKeepAmbiguousProposal() {
        BloqueLectura primero = givenBlock("Ventas", "Monto", TipoCampo.NUMERO, List.of());
        BloqueLectura segundo = givenBlock("Ventas", "Monto", TipoCampo.NUMERO, List.of());

        PropuestaLectura propuesta = propuestaLecturaService.proponer(
                ProcesamientoTestData.lectura(), List.of(primero, segundo), null);

        assertThat(propuesta.lectura().bloque().bloqueExistenteId()).isNull();
    }

    @Test
    @DisplayName("Respeta un destino explícito entre varios compatibles")
    void proponerShouldUseExplicitDestination() {
        BloqueLectura primero = givenBlock("Ventas", "Monto", TipoCampo.NUMERO, List.of());
        BloqueLectura segundo = givenBlock("Ventas", "Monto", TipoCampo.NUMERO, List.of());

        PropuestaLectura propuesta = propuestaLecturaService.proponer(
                ProcesamientoTestData.lectura(), List.of(primero, segundo), segundo.id());

        assertThat(propuesta.lectura().bloque().bloqueExistenteId()).isEqualTo(segundo.id());
    }

    @Test
    @DisplayName("Rechaza un destino inexistente o incompatible")
    void proponerShouldFailWhenDestinationMissing() {
        assertThatThrownBy(() -> propuestaLecturaService.proponer(
                ProcesamientoTestData.lectura(), List.of(), UUID.randomUUID()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("compatible");
    }

    private BloqueLectura givenBlock(String nombre, String campo, TipoCampo tipo, List<String> opciones) {
        return new BloqueLectura(UUID.randomUUID(), nombre,
                List.of(new CampoConocido(ProcesamientoTestData.CAMPO_ID, campo, tipo, opciones)));
    }
}
