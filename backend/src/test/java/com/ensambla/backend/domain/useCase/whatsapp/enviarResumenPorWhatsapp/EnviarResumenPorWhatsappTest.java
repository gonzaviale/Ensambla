package com.ensambla.backend.domain.useCase.whatsapp.enviarResumenPorWhatsapp;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.ResumenMatinal;
import com.ensambla.backend.domain.model.aviso.SeccionAgotados;
import com.ensambla.backend.domain.model.aviso.SeccionAvisosDePrecio;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.preferencia.PreferenciaUsuario;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.preferencia.PreferenciaRepository;
import com.ensambla.backend.domain.repository.whatsapp.VinculoWhatsappRepository;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EnviarResumenPorWhatsappTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");
    private static final UsuarioId OTRO_USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000002");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final String TELEFONO = "5491155550101";
    private static final int HORA_DEL_RESUMEN = 9;
    /** 10:00 en Argentina (13:00 UTC) del 21 de octubre. */
    private static final Instant A_LAS_DIEZ = Instant.parse("2026-10-21T13:00:00Z");
    /** 08:30 en Argentina (11:30 UTC): todavía no es la hora. */
    private static final Instant A_LAS_OCHO_Y_MEDIA = Instant.parse("2026-10-21T11:30:00Z");
    private static final LocalDate HOY = LocalDate.of(2026, 10, 21);

    private final PreferenciaRepository preferenciaRepository = mock(PreferenciaRepository.class);
    private final VinculoWhatsappRepository vinculoWhatsappRepository = mock(VinculoWhatsappRepository.class);
    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final AvisoRepository avisoRepository = mock(AvisoRepository.class);
    private final MensajeriaWhatsappPort mensajeria = mock(MensajeriaWhatsappPort.class);

    @BeforeEach
    void setUp() {
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID))
                .thenReturn(Negocio.builder().id(NEGOCIO_ID).nombre("Almacén de Ana").build());
        when(vinculoWhatsappRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(vinculo()));
        when(mensajeria.enviarTexto(anyString(), anyString())).thenReturn(true);
        when(preferenciaRepository.save(any(PreferenciaUsuario.class))).thenAnswer(guardada -> guardada.getArgument(0));
    }

    @Test
    @DisplayName("Le manda el resumen a quien lo pidió y deja anotado que ya lo recibió hoy")
    void executeShouldSendTheSummaryAndRememberIt() {
        PreferenciaUsuario preferencia = givenAlguienQuePidioElResumen();
        givenAvisosPendientes(aviso(TipoAviso.STOCK, 0), aviso(TipoAviso.PRECIO_ZONA, 0));

        int result = whenSeEjecutaA(A_LAS_DIEZ);

        assertThat(result).isEqualTo(1);
        thenSeEnvioA(TELEFONO, "Buen día, Ana. Sami revisó Almacén de Ana y encontró:\n"
                + "- 1 producto agotado\n- 1 aviso de precio\nAbrí Ensambla para ver los detalles.");
        assertThat(preferencia.yaRecibioElResumenDe(HOY)).isTrue();
        verify(preferenciaRepository).save(preferencia);
    }

    @Test
    @DisplayName("Antes de la hora del resumen no hace nada")
    void executeShouldDoNothingBeforeTheConfiguredHour() {
        givenAlguienQuePidioElResumen();

        int result = whenSeEjecutaA(A_LAS_OCHO_Y_MEDIA);

        assertThat(result).isZero();
        verifyNoInteractions(preferenciaRepository, mensajeria);
    }

    @Test
    @DisplayName("No se lo vuelve a mandar a quien ya lo recibió hoy")
    void executeShouldNotSendItTwiceInTheSameDay() {
        PreferenciaUsuario preferencia = givenAlguienQuePidioElResumen();
        preferencia.registrarResumenEnviado(HOY, OffsetDateTime.ofInstant(A_LAS_DIEZ, ZoneOffset.UTC));
        givenAvisosPendientes(aviso(TipoAviso.STOCK, 0));

        int result = whenSeEjecutaA(A_LAS_DIEZ);

        assertThat(result).isZero();
        verifyNoInteractions(mensajeria);
    }

    @Test
    @DisplayName("Sí se lo manda al día siguiente")
    void executeShouldSendItAgainTheNextDay() {
        PreferenciaUsuario preferencia = givenAlguienQuePidioElResumen();
        preferencia.registrarResumenEnviado(HOY.minusDays(1), OffsetDateTime.ofInstant(A_LAS_DIEZ, ZoneOffset.UTC));
        givenAvisosPendientes(aviso(TipoAviso.STOCK, 0));

        int result = whenSeEjecutaA(A_LAS_DIEZ);

        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("Sin nada que contar no manda nada y no lo da por enviado")
    void executeShouldNotSendWhenThereIsNothingToTell() {
        PreferenciaUsuario preferencia = givenAlguienQuePidioElResumen();
        givenAvisosPendientes();

        int result = whenSeEjecutaA(A_LAS_DIEZ);

        assertThat(result).isZero();
        verifyNoInteractions(mensajeria);
        assertThat(preferencia.yaRecibioElResumenDe(HOY)).isFalse();
    }

    @Test
    @DisplayName("Un grupo de avisos que la persona apagó no entra en el resumen")
    void executeShouldSkipTheGroupsThePersonTurnedOff() {
        PreferenciaUsuario preferencia = givenAlguienQuePidioElResumen();
        preferencia.actualizar(List.of(), Map.of("STOCK", false), true, "Ana", aLasDiezEnUtc());
        givenAvisosPendientes(aviso(TipoAviso.STOCK, 0), aviso(TipoAviso.PRECIO_ZONA, 0));

        whenSeEjecutaA(A_LAS_DIEZ);

        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(mensajeria).enviarTexto(anyString(), texto.capture());
        assertThat(texto.getValue()).contains("1 aviso de precio").doesNotContain("agotado");
    }

    @Test
    @DisplayName("Si no se pudo entregar no lo da por enviado y lo intenta en la próxima vuelta")
    void executeShouldRetryLaterWhenTheDeliveryFails() {
        PreferenciaUsuario preferencia = givenAlguienQuePidioElResumen();
        givenAvisosPendientes(aviso(TipoAviso.STOCK, 0));
        when(mensajeria.enviarTexto(anyString(), anyString())).thenReturn(false);

        int result = whenSeEjecutaA(A_LAS_DIEZ);

        assertThat(result).isZero();
        assertThat(preferencia.yaRecibioElResumenDe(HOY)).isFalse();
        verify(preferenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sin número vinculado no manda nada")
    void executeShouldSkipWhoHasNoLinkedNumber() {
        givenAlguienQuePidioElResumen();
        when(vinculoWhatsappRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.empty());

        int result = whenSeEjecutaA(A_LAS_DIEZ);

        assertThat(result).isZero();
        verifyNoInteractions(mensajeria);
    }

    @Test
    @DisplayName("Un problema con una persona no frena a las demás")
    void executeShouldKeepGoingWhenOnePersonFails() {
        PreferenciaUsuario conProblema = preferenciaDe(OTRO_USUARIO_ID);
        PreferenciaUsuario sana = preferenciaDe(USUARIO_ID);
        when(preferenciaRepository.findAllConAvisosPorWhatsapp()).thenReturn(List.of(conProblema, sana));
        when(vinculoWhatsappRepository.findByUsuarioId(OTRO_USUARIO_ID)).thenReturn(Optional.of(vinculo()));
        when(negocioRepository.obtenerNegocioDelUsuario(OTRO_USUARIO_ID))
                .thenThrow(new ResourceNotFoundException("El negocio no existe"));
        givenAvisosPendientes(aviso(TipoAviso.STOCK, 0));

        int result = whenSeEjecutaA(A_LAS_DIEZ);

        assertThat(result).isEqualTo(1);
        assertThat(sana.yaRecibioElResumenDe(HOY)).isTrue();
        assertThat(conProblema.yaRecibioElResumenDe(HOY)).isFalse();
    }

    // --- arrange ---
    private PreferenciaUsuario givenAlguienQuePidioElResumen() {
        PreferenciaUsuario preferencia = preferenciaDe(USUARIO_ID);
        when(preferenciaRepository.findAllConAvisosPorWhatsapp()).thenReturn(List.of(preferencia));
        return preferencia;
    }

    private void givenAvisosPendientes(Aviso... avisos) {
        when(avisoRepository.findByNegocioIdAndEstado(NEGOCIO_ID, EstadoAviso.PENDIENTE)).thenReturn(List.of(avisos));
    }

    private PreferenciaUsuario preferenciaDe(UsuarioId usuarioId) {
        PreferenciaUsuario preferencia = PreferenciaUsuario.inicial(usuarioId, aLasDiezEnUtc().minusDays(30));
        preferencia.actualizar(List.of(), Map.of(), true, "Ana", aLasDiezEnUtc().minusDays(30));
        preferencia.configurarAvisosPorWhatsapp(true, aLasDiezEnUtc().minusDays(30));
        return preferencia;
    }

    private VinculoWhatsapp vinculo() {
        return VinculoWhatsapp.builder().usuarioId(USUARIO_ID).negocioId(NEGOCIO_ID).telefono(TELEFONO).build();
    }

    private Aviso aviso(TipoAviso tipo, Number cantidad) {
        return Aviso.builder().tipo(tipo).grupo(tipo.grupo()).estado(EstadoAviso.PENDIENTE)
                .datos(Map.of("cantidad", cantidad)).build();
    }

    private static OffsetDateTime aLasDiezEnUtc() {
        return OffsetDateTime.ofInstant(A_LAS_DIEZ, ZoneOffset.UTC);
    }

    // --- act ---
    private int whenSeEjecutaA(Instant instante) {
        ResumenMatinal resumen = new ResumenMatinal(List.of(new SeccionAgotados(), new SeccionAvisosDePrecio()));
        EnviarResumenPorWhatsapp enviarResumen = new EnviarResumenPorWhatsapp(
                preferenciaRepository, vinculoWhatsappRepository, negocioRepository, avisoRepository, resumen,
                mensajeria, new TransaccionDirecta(), Clock.fixed(instante, ZoneOffset.UTC), HORA_DEL_RESUMEN);
        return enviarResumen.execute();
    }

    // --- assert ---
    private void thenSeEnvioA(String telefono, String texto) {
        verify(mensajeria).enviarTexto(telefono, texto);
    }
}
