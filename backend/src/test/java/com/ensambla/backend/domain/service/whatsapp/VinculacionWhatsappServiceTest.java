package com.ensambla.backend.domain.service.whatsapp;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.whatsapp.TelefonoWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.repository.whatsapp.VerificacionWhatsappRepository;
import com.ensambla.backend.domain.repository.whatsapp.VinculoWhatsappRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VinculacionWhatsappServiceTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    private static final UsuarioId OTRO_USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000002");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-27T12:00:00Z");
    private static final TelefonoWhatsapp TELEFONO = new TelefonoWhatsapp("5491122334455");

    private final VinculoWhatsappRepository vinculoRepository = mock(VinculoWhatsappRepository.class,
            CALLS_REAL_METHODS);
    private final VerificacionWhatsappRepository verificacionRepository = mock(VerificacionWhatsappRepository.class);
    private final VinculacionWhatsappService service =
            new VinculacionWhatsappService(vinculoRepository, verificacionRepository);
    private VinculoWhatsapp vinculo;
    private Throwable error;

    @Test
    @DisplayName("Vincular guarda el número nuevo en el negocio del usuario")
    void vincularGuardaElNumeroNuevo() {
        dadoQueGuardarDevuelveLoGuardado();

        cuandoSeVincula();

        entoncesQuedoVinculadoA("5491122334455");
    }

    @Test
    @DisplayName("Vincular borra el número anterior y la verificación en curso antes de guardar el nuevo")
    void vincularReemplazaElNumeroAnterior() {
        dadoQueGuardarDevuelveLoGuardado();

        cuandoSeVincula();

        entoncesSeBorroLoAnteriorAntesDeGuardar();
    }

    @Test
    @DisplayName("Un número que ya es de otra cuenta no se vincula ni toca el vínculo actual")
    void unNumeroDeOtraCuentaNoSeVincula() {
        dadoQueElNumeroEsDeOtraCuenta();

        cuandoSeIntentaVincular();

        entoncesSeRechazaPorque("Ese número de WhatsApp ya está vinculado a otra cuenta");
        entoncesNoSeTocoElVinculoActual();
    }

    private void dadoQueGuardarDevuelveLoGuardado() {
        when(vinculoRepository.save(any(VinculoWhatsapp.class))).thenAnswer(guardado -> guardado.getArgument(0));
    }

    private void dadoQueElNumeroEsDeOtraCuenta() {
        when(vinculoRepository.findByTelefono("5491122334455")).thenReturn(Optional.of(
                VinculoWhatsapp.nuevo(OTRO_USUARIO_ID, NEGOCIO_ID, TELEFONO, AHORA)));
    }

    private void cuandoSeVincula() {
        vinculo = service.vincular(USUARIO_ID, NEGOCIO_ID, TELEFONO, AHORA);
    }

    private void cuandoSeIntentaVincular() {
        error = catchThrowable(this::cuandoSeVincula);
    }

    private void entoncesQuedoVinculadoA(String telefono) {
        assertThat(vinculo.getTelefono()).isEqualTo(telefono);
        assertThat(vinculo.getNegocioId()).isEqualTo(NEGOCIO_ID);
    }

    private void entoncesSeBorroLoAnteriorAntesDeGuardar() {
        InOrder orden = inOrder(vinculoRepository, verificacionRepository);
        orden.verify(vinculoRepository).deleteByUsuarioId(USUARIO_ID);
        orden.verify(verificacionRepository).deleteByUsuarioId(USUARIO_ID);
        orden.verify(vinculoRepository).save(any(VinculoWhatsapp.class));
    }

    private void entoncesSeRechazaPorque(String mensaje) {
        assertThat(error).isInstanceOf(DuplicateResourceException.class).hasMessage(mensaje);
    }

    private void entoncesNoSeTocoElVinculoActual() {
        verify(vinculoRepository, never()).deleteByUsuarioId(any());
        verify(vinculoRepository, never()).save(any());
    }
}