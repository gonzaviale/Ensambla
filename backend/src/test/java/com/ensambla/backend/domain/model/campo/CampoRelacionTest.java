package com.ensambla.backend.domain.model.campo;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CampoRelacionTest extends CampoPorTipoTest {

    private static final String FILA_ID = "d0000000-0000-4000-8000-000000000001";
    private static final BloqueId TABLA_RELACIONADA = BloqueId.de("b0000000-0000-4000-8000-000000000002");

    @Override
    protected TipoCampo tipoEsperado() {
        return TipoCampo.RELACION;
    }

    @Override
    protected List<Object> valoresValidos() {
        return List.of(FILA_ID);
    }

    @Override
    protected List<Object> valoresInvalidos() {
        return List.of("Ana", "1-1-1-1-1", 12);
    }

    @Override
    protected String mensajeDeError() {
        return "Elegí una fila de la tabla relacionada";
    }

    @Test
    @DisplayName("Es una conexión y sabe con qué tabla se conecta")
    void esRelacionShouldBeTrueAndKnowItsTable() {
        Campo campo = Campo.builder().tipo(TipoCampo.RELACION).bloqueRelacionadoId(TABLA_RELACIONADA).build();

        assertThat(campo.esRelacion()).isTrue();
        assertThat(campo.getBloqueRelacionadoId()).isEqualTo(TABLA_RELACIONADA);
    }

    @Test
    @DisplayName("En el reporte va el nombre de la fila a la que apunta")
    void celdaDeReporteShouldBeTheLabelOfTheRow() {
        Map<RegistroId, String> etiquetas = Map.of(RegistroId.de(FILA_ID), "Ana");

        assertThat(campo().celdaDeReporte(FILA_ID, etiquetas::get)).isEqualTo("Ana");
    }

    @Test
    @DisplayName("En el reporte avisa si la fila a la que apuntaba ya no existe")
    void celdaDeReporteShouldSayTheRowWasDeleted() {
        assertThat(campo().celdaDeReporte(FILA_ID, id -> null)).isEqualTo("Fila eliminada");
    }
}
