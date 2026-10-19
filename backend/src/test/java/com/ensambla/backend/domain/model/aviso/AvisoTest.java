package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.enums.EstadoAviso;
import com.ensambla.backend.domain.model.enums.GrupoAviso;
import com.ensambla.backend.domain.model.enums.TipoAviso;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvisoTest {

    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID PROYECTO_ID = UUID.fromString("c0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");
    private static final OffsetDateTime DESPUES = OffsetDateTime.parse("2026-10-19T13:00:00Z");

    @Test
    @DisplayName("Un aviso nuevo queda pendiente con el grupo de su tipo y los datos de la regla")
    void nuevoShouldBePendingWithGrupoOfTipo() {
        ResultadoRegla resultado = givenResultado((short) 3, "Te quedaste sin yerba");

        Aviso aviso = Aviso.nuevo(NEGOCIO_ID, TipoAviso.STOCK, resultado, AHORA);

        assertThat(aviso.getEstado()).isEqualTo(EstadoAviso.PENDIENTE);
        assertThat(aviso.getGrupo()).isEqualTo(GrupoAviso.STOCK);
        assertThat(aviso.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(aviso.getProyectoId()).isEqualTo(PROYECTO_ID);
        assertThat(aviso.getTitulo()).isEqualTo("Te quedaste sin yerba");
        assertThat(aviso.getHuella()).isEqualTo(resultado.huellaPara(TipoAviso.STOCK));
        assertThat(aviso.getCreadoEn()).isEqualTo(AHORA);
        assertThat(aviso.getActualizadoEn()).isEqualTo(AHORA);
        assertThat(aviso.estaPendiente()).isTrue();
    }

    @Test
    @DisplayName("Es urgente desde la prioridad 2")
    void esUrgenteShouldDependOnPrioridad() {
        assertThat(givenAviso((short) 3).esUrgente()).isTrue();
        assertThat(givenAviso((short) 2).esUrgente()).isTrue();
        assertThat(givenAviso((short) 1).esUrgente()).isFalse();
    }

    @Test
    @DisplayName("Aplicar un aviso pendiente guarda cuándo se resolvió")
    void aplicarShouldResolvePendingAviso() {
        Aviso aviso = givenAviso((short) 2);

        aviso.aplicar(DESPUES);

        assertThat(aviso.getEstado()).isEqualTo(EstadoAviso.APLICADO);
        assertThat(aviso.getResueltoEn()).isEqualTo(DESPUES);
        assertThat(aviso.getActualizadoEn()).isEqualTo(DESPUES);
    }

    @Test
    @DisplayName("Descartar un aviso pendiente guarda cuándo se resolvió")
    void descartarShouldResolvePendingAviso() {
        Aviso aviso = givenAviso((short) 2);

        aviso.descartar(DESPUES);

        assertThat(aviso.getEstado()).isEqualTo(EstadoAviso.DESCARTADO);
        assertThat(aviso.getResueltoEn()).isEqualTo(DESPUES);
    }

    @Test
    @DisplayName("No se puede aplicar ni descartar un aviso que ya fue resuelto")
    void aplicarAndDescartarShouldThrowWhenAvisoWasAlreadyResolved() {
        Aviso aviso = givenAviso((short) 2);
        aviso.descartar(DESPUES);

        assertThatThrownBy(() -> aviso.aplicar(DESPUES))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El aviso ya fue resuelto");
        assertThatThrownBy(() -> aviso.descartar(DESPUES)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Un aviso que el motor ya no detecta vence sin fecha de resolución")
    void vencerShouldExpirePendingAviso() {
        Aviso aviso = givenAviso((short) 2);

        aviso.vencer(DESPUES);

        assertThat(aviso.getEstado()).isEqualTo(EstadoAviso.VENCIDO);
        assertThat(aviso.getResueltoEn()).isNull();
        assertThat(aviso.getActualizadoEn()).isEqualTo(DESPUES);
    }

    @Test
    @DisplayName("Un aviso vencido no vuelve a vencer")
    void vencerShouldThrowWhenAvisoIsNotPending() {
        Aviso aviso = givenAviso((short) 2);
        aviso.vencer(DESPUES);

        assertThatThrownBy(() -> aviso.vencer(DESPUES)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Actualizar un aviso pendiente cambia sus datos y mantiene el estado")
    void actualizarConShouldRefreshPendingAviso() {
        Aviso aviso = givenAviso((short) 2);

        aviso.actualizarCon(givenResultado((short) 3, "Te quedaste sin yerba"), DESPUES);

        assertThat(aviso.getPrioridad()).isEqualTo((short) 3);
        assertThat(aviso.getTitulo()).isEqualTo("Te quedaste sin yerba");
        assertThat(aviso.getEstado()).isEqualTo(EstadoAviso.PENDIENTE);
        assertThat(aviso.getActualizadoEn()).isEqualTo(DESPUES);
    }

    @Test
    @DisplayName("Actualizar un aviso vencido lo deja pendiente otra vez")
    void actualizarConShouldReactivateExpiredAviso() {
        Aviso aviso = givenAviso((short) 2);
        aviso.vencer(AHORA);

        aviso.actualizarCon(givenResultado((short) 2, "Yerba por debajo del mínimo"), DESPUES);

        assertThat(aviso.getEstado()).isEqualTo(EstadoAviso.PENDIENTE);
        assertThat(aviso.getResueltoEn()).isNull();
    }

    @Test
    @DisplayName("No se puede actualizar un aviso aplicado ni descartado")
    void actualizarConShouldThrowWhenAvisoWasResolved() {
        Aviso aplicado = givenAviso((short) 2);
        aplicado.aplicar(AHORA);
        Aviso descartado = givenAviso((short) 2);
        descartado.descartar(AHORA);
        ResultadoRegla resultado = givenResultado((short) 2, "Yerba");

        assertThatThrownBy(() -> aplicado.actualizarCon(resultado, DESPUES))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> descartado.actualizarCon(resultado, DESPUES))
                .isInstanceOf(BusinessRuleException.class);
    }

    private static Aviso givenAviso(short prioridad) {
        return Aviso.nuevo(NEGOCIO_ID, TipoAviso.STOCK, givenResultado(prioridad, "Yerba por debajo del mínimo"),
                AHORA);
    }

    private static ResultadoRegla givenResultado(short prioridad, String titulo) {
        return new ResultadoRegla(PROYECTO_ID, "registro:1", prioridad, titulo, "Te quedan 2",
                Map.of("cantidad", 2), List.of("motivo"), List.of());
    }
}
