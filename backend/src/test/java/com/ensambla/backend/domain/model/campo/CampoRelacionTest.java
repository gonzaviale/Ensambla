package com.ensambla.backend.domain.model.campo;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.ensambla.backend.support.CamposDePrueba.unaConexion;
import static org.assertj.core.api.Assertions.assertThat;

class CampoRelacionTest extends CampoDeCualquierTipoTest {

    private static final String FILA_ID = "d0000000-0000-4000-8000-000000000001";
    private static final BloqueId TABLA_RELACIONADA = BloqueId.de("b0000000-0000-4000-8000-000000000002");

    @Override
    protected TipoCampo tipoEsperado() {
        return TipoCampo.RELACION;
    }

    @Override
    protected List<Object> valoresQueAcepta() {
        return List.of(FILA_ID);
    }

    @Override
    protected List<Object> valoresQueRechaza() {
        return List.of("Ana", "1-1-1-1-1", 12);
    }

    @Override
    protected String motivoDelRechazo() {
        return "Elegí una fila de la tabla relacionada";
    }

    @Test
    @DisplayName("Es una conexión y sabe con qué tabla se conecta")
    void esUnaConexionYSabeConQueTablaSeConecta() {
        Campo cliente = unaConexion("Cliente", TABLA_RELACIONADA);

        entoncesEsUnaConexionConLaTabla(cliente, TABLA_RELACIONADA);
    }

    @Test
    @DisplayName("En el reporte va el nombre de la fila a la que apunta")
    void enElReporteVaElNombreDeLaFilaALaQueApunta() {
        cuandoSePideLaCeldaDeReporte(FILA_ID, unaFilaLlamada("Ana"));

        entoncesElResultadoEs("Ana");
    }

    @Test
    @DisplayName("En el reporte se avisa si la fila a la que apuntaba ya no existe")
    void enElReporteSeAvisaSiLaFilaALaQueApuntabaYaNoExiste() {
        cuandoSePideLaCeldaDeReporte(FILA_ID, unaFilaInexistente());

        entoncesElResultadoEs("Fila eliminada");
    }

    private Function<RegistroId, String> unaFilaLlamada(String nombre) {
        Map<RegistroId, String> etiquetas = Map.of(RegistroId.de(FILA_ID), nombre);
        return etiquetas::get;
    }

    private Function<RegistroId, String> unaFilaInexistente() {
        return id -> null;
    }

    private void entoncesEsUnaConexionConLaTabla(Campo campo, BloqueId tabla) {
        assertThat(campo.esRelacion()).isTrue();
        assertThat(campo.getBloqueRelacionadoId()).isEqualTo(tabla);
    }
}
