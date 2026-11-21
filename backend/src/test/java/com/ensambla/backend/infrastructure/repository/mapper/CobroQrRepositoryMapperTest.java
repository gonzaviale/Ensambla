package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.caja.CobroQr;
import com.ensambla.backend.domain.model.caja.ConfirmacionDeCobro;
import com.ensambla.backend.domain.model.caja.EstadoCobroQr;
import com.ensambla.backend.infrastructure.repository.entity.CobroQrEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.CobrosQrDePrueba.AHORA;
import static com.ensambla.backend.support.CobrosQrDePrueba.COBRO_QR_ID;
import static com.ensambla.backend.support.CobrosQrDePrueba.IMPORTE;
import static com.ensambla.backend.support.CobrosQrDePrueba.unCobroQrCobrado;
import static com.ensambla.backend.support.CobrosQrDePrueba.unCobroQrPendiente;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;

class CobroQrRepositoryMapperTest {

    private final CobroQrRepositoryMapper cobroQrRepositoryMapper = new CobroQrRepositoryMapper();

    private CobroQrEntity entidad;
    private CobroQr cobro;

    @Test
    @DisplayName("Un cobro pendiente se guarda con todos sus datos y sin confirmación")
    void unCobroPendienteSeGuardaConTodosSusDatosYSinConfirmacion() {
        cuandoSeConvierteEnEntidad(unCobroQrPendiente());

        entoncesLaEntidadTieneLosDatosDelCobroPendiente();
        entoncesLaEntidadNoTieneConfirmacion();
    }

    @Test
    @DisplayName("Un cobro cobrado se guarda con su fecha de confirmación y su marca manual")
    void unCobroCobradoSeGuardaConSuFechaDeConfirmacionYSuMarcaManual() {
        cuandoSeConvierteEnEntidad(unCobroQrCobrado());

        entoncesLaEntidadQuedaCobradaPorConfirmacionManual();
    }

    @Test
    @DisplayName("Una entidad se lee como cobro con todos sus datos")
    void unaEntidadSeLeeComoCobroConTodosSusDatos() {
        cuandoSeLeeComoCobro(cobroQrRepositoryMapper.toEntity(unCobroQrCobrado()));

        entoncesElCobroEsIgualAlOriginal();
    }

    @Test
    @DisplayName("Una entidad pendiente se lee sin confirmación")
    void unaEntidadPendienteSeLeeSinConfirmacion() {
        cuandoSeLeeComoCobro(cobroQrRepositoryMapper.toEntity(unCobroQrPendiente()));

        entoncesElCobroPendienteEsIgualAlOriginal();
    }

    // --- cuando ---
    private void cuandoSeConvierteEnEntidad(CobroQr paraGuardar) {
        entidad = cobroQrRepositoryMapper.toEntity(paraGuardar);
    }

    private void cuandoSeLeeComoCobro(CobroQrEntity guardada) {
        cobro = cobroQrRepositoryMapper.toDomain(guardada);
    }

    // --- entonces ---
    private void entoncesLaEntidadTieneLosDatosDelCobroPendiente() {
        assertThat(entidad.getId()).isEqualTo(COBRO_QR_ID.valor());
        assertThat(entidad.getNegocioId()).isEqualTo(NEGOCIO_ID.valor());
        assertThat(entidad.getAlias()).isEqualTo("mi.alias.mp");
        assertThat(entidad.getTitular()).isEqualTo("Ana Pérez");
        assertThat(entidad.getImporte()).isEqualByComparingTo(IMPORTE);
        assertThat(entidad.getEstado()).isEqualTo(EstadoCobroQr.PENDIENTE);
        assertThat(entidad.getCreadoEn()).isEqualTo(AHORA);
        assertThat(entidad.getVenceEn()).isEqualTo(AHORA.plusMinutes(15));
        assertThat(entidad.getActualizadoEn()).isEqualTo(AHORA);
    }

    private void entoncesLaEntidadNoTieneConfirmacion() {
        assertThat(entidad.getConfirmadoEn()).isNull();
        assertThat(entidad.getConfirmacion()).isNull();
    }

    private void entoncesLaEntidadQuedaCobradaPorConfirmacionManual() {
        assertThat(entidad.getEstado()).isEqualTo(EstadoCobroQr.COBRADO);
        assertThat(entidad.getConfirmacion()).isEqualTo(ConfirmacionDeCobro.MANUAL);
        assertThat(entidad.getConfirmadoEn()).isEqualTo(AHORA.plusMinutes(5));
        assertThat(entidad.getActualizadoEn()).isEqualTo(AHORA.plusMinutes(5));
    }

    private void entoncesElCobroEsIgualAlOriginal() {
        assertThat(cobro).isEqualTo(unCobroQrCobrado());
    }

    private void entoncesElCobroPendienteEsIgualAlOriginal() {
        assertThat(cobro).isEqualTo(unCobroQrPendiente());
    }
}
