package com.ensambla.backend.domain.useCase.whatsapp.solicitarVerificacionWhatsapp;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.whatsapp.TelefonoWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VerificacionWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.whatsapp.VerificacionWhatsappRepository;
import com.ensambla.backend.domain.repository.whatsapp.VinculoWhatsappRepository;
import com.ensambla.backend.infrastructure.config.useCase.WhatsappUseCaseConfig;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SolicitarVerificacionWhatsappTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    private static final UsuarioId OTRO_USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000002");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final Instant INSTANTE = Instant.parse("2026-10-27T12:00:00Z");
    private static final OffsetDateTime AHORA = OffsetDateTime.ofInstant(INSTANTE, ZoneOffset.UTC);
    private static final String TELEFONO = "5491122334455";

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final VinculoWhatsappRepository vinculoRepository = mock(VinculoWhatsappRepository.class,
            CALLS_REAL_METHODS);
    private final VerificacionWhatsappRepository verificacionRepository =
            mock(VerificacionWhatsappRepository.class);
    private final MensajeriaWhatsappPort mensajeria = mock(MensajeriaWhatsappPort.class);
    private final SolicitarVerificacionWhatsapp solicitar = new WhatsappUseCaseConfig()
            .solicitarVerificacionWhatsapp(negocioRepository, vinculoRepository, verificacionRepository, mensajeria,
                    new TransaccionDirecta(), Clock.fixed(INSTANTE, ZoneOffset.UTC));
    private VerificacionWhatsapp previa;
    private SolicitarVerificacionWhatsappResponse solicitud;
    private Throwable error;

    @Test
    @DisplayName("Manda por WhatsApp un código de 6 dígitos al número normalizado")
    void mandaUnCodigoDeSeisDigitosAlNumeroNormalizado() {
        dadoUnUsuarioConNegocio();
        dadoQueWhatsappEntregaLosMensajes();

        cuandoSePideElCodigoPara("+54 11 2233 4455");

        entoncesSeMandoUnCodigoDeSeisDigitosA(TELEFONO);
    }

    @Test
    @DisplayName("Guarda sólo la huella del código y no lo devuelve")
    void guardaSoloLaHuellaDelCodigo() {
        dadoUnUsuarioConNegocio();
        dadoQueWhatsappEntregaLosMensajes();

        cuandoSePideElCodigoPara("+54 11 2233 4455");

        entoncesSeGuardoSoloLaHuella();
        entoncesLaSolicitudEsPara(TELEFONO, AHORA.plusMinutes(10), AHORA.plusSeconds(60));
    }

    @Test
    @DisplayName("Si ya había una verificación en curso la reemplaza respetando la espera entre envíos")
    void siYaHabiaUnaVerificacionLaReemplaza() {
        dadoUnUsuarioConNegocio();
        dadoQueWhatsappEntregaLosMensajes();
        dadoUnaVerificacionEnCursoDesde(AHORA.minusMinutes(2));

        cuandoSePideElCodigoPara("5491100000000");

        entoncesLaVerificacionPreviaPasoA("5491100000000", 2);
    }

    @Test
    @DisplayName("Pedir otro código antes de que pase la espera se rechaza y no manda nada")
    void pedirOtroCodigoAntesDeLaEsperaSeRechaza() {
        dadoUnUsuarioConNegocio();
        dadoUnaVerificacionEnCursoDesde(AHORA.minusSeconds(10));

        cuandoSeIntentaPedirElCodigoPara(TELEFONO);

        entoncesSeRechazaPorque("Esperá un momento antes de pedir otro código");
        entoncesNoSeMandoNada();
    }

    @Test
    @DisplayName("Un número que ya es de otra cuenta se rechaza con un conflicto")
    void unNumeroDeOtraCuentaSeRechazaConUnConflicto() {
        dadoUnUsuarioConNegocio();
        dadoQueElNumeroEsDe(OTRO_USUARIO_ID);

        cuandoSeIntentaPedirElCodigoPara(TELEFONO);

        entoncesHayUnConflicto();
        entoncesNoSeMandoNada();
    }

    @Test
    @DisplayName("Pedir el código del número que ya tiene vinculado se rechaza")
    void pedirElCodigoDelNumeroPropioSeRechaza() {
        dadoUnUsuarioConNegocio();
        dadoQueElUsuarioYaVinculo(TELEFONO);

        cuandoSeIntentaPedirElCodigoPara("+54 9 11 2233 4455");

        entoncesSeRechazaPorque("Ese número ya está vinculado a tu cuenta");
    }

    @Test
    @DisplayName("A un mismo número no se le piden más de 3 códigos por hora entre todas las cuentas")
    void aUnMismoNumeroNoSeLePidenMasDeTresCodigosPorHora() {
        dadoUnUsuarioConNegocio();
        dadoQueAlNumeroYaSeLeMandaronCodigos(3);

        cuandoSeIntentaPedirElCodigoPara(TELEFONO);

        entoncesSeRechazaPorque("Ya mandamos varios códigos a ese número. Probá en una hora");
        entoncesNoSeMandoNada();
    }

    @Test
    @DisplayName("Si el código no se pudo entregar se descarta la verificación y se pide revisar el número")
    void siElCodigoNoSeEntregaSeDescartaLaVerificacion() {
        dadoUnUsuarioConNegocio();
        dadoQueWhatsappNoEntregaLosMensajes();

        cuandoSeIntentaPedirElCodigoPara(TELEFONO);

        entoncesElErrorDice("No pudimos mandarte el código por WhatsApp");
        entoncesSeDescartoLaVerificacion();
    }

    @Test
    @DisplayName("Un teléfono con letras no es un número válido")
    void unTelefonoConLetrasNoEsValido() {
        dadoUnUsuarioConNegocio();

        cuandoSeIntentaPedirElCodigoPara("no es un número");

        entoncesSeRechaza();
    }

    private void dadoUnUsuarioConNegocio() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(verificacionRepository.save(any(VerificacionWhatsapp.class))).thenAnswer(guardada -> guardada.getArgument(0));
    }

    private void dadoQueWhatsappEntregaLosMensajes() {
        when(mensajeria.enviarTexto(anyString(), anyString())).thenReturn(true);
    }

    private void dadoQueWhatsappNoEntregaLosMensajes() {
        when(mensajeria.enviarTexto(anyString(), anyString())).thenReturn(false);
    }

    private void dadoUnaVerificacionEnCursoDesde(OffsetDateTime enviada) {
        previa = VerificacionWhatsapp.nueva(USUARIO_ID, new TelefonoWhatsapp(TELEFONO), "111111", enviada);
        when(verificacionRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(previa));
    }

    private void dadoQueElNumeroEsDe(UsuarioId usuarioId) {
        when(vinculoRepository.findByTelefono(TELEFONO)).thenReturn(Optional.of(
                VinculoWhatsapp.nuevo(usuarioId, NEGOCIO_ID, new TelefonoWhatsapp(TELEFONO), AHORA)));
    }

    private void dadoQueElUsuarioYaVinculo(String telefono) {
        when(vinculoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(
                VinculoWhatsapp.nuevo(USUARIO_ID, NEGOCIO_ID, new TelefonoWhatsapp(telefono), AHORA)));
    }

    private void dadoQueAlNumeroYaSeLeMandaronCodigos(long cantidad) {
        when(verificacionRepository.contarEnviosRecientesA(TELEFONO, AHORA.minusHours(1))).thenReturn(cantidad);
    }

    private void cuandoSePideElCodigoPara(String telefono) {
        solicitud = solicitar.execute(new SolicitarVerificacionWhatsappRequest(USUARIO_ID, telefono));
    }

    private void cuandoSeIntentaPedirElCodigoPara(String telefono) {
        error = catchThrowable(() -> cuandoSePideElCodigoPara(telefono));
    }

    private void entoncesSeMandoUnCodigoDeSeisDigitosA(String telefono) {
        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(mensajeria).enviarTexto(eq(telefono), texto.capture());
        assertThat(texto.getValue()).containsPattern("es [0-9]{6}[.]");
    }

    private void entoncesSeGuardoSoloLaHuella() {
        ArgumentCaptor<VerificacionWhatsapp> guardada = ArgumentCaptor.forClass(VerificacionWhatsapp.class);
        verify(verificacionRepository).save(guardada.capture());
        assertThat(guardada.getValue().getCodigoHash()).hasSize(64);
    }

    private void entoncesLaSolicitudEsPara(String telefono, OffsetDateTime vence, OffsetDateTime reenviarDesde) {
        assertThat(solicitud.telefono()).isEqualTo(telefono);
        assertThat(solicitud.expiraEn()).isEqualTo(vence);
        assertThat(solicitud.reenviarDesde()).isEqualTo(reenviarDesde);
    }

    private void entoncesLaVerificacionPreviaPasoA(String telefono, int envios) {
        assertThat(previa.getTelefono()).isEqualTo(telefono);
        assertThat(previa.getEnviosVentana()).isEqualTo(envios);
    }

    private void entoncesSeRechazaPorque(String mensaje) {
        assertThat(error).hasMessage(mensaje);
    }

    private void entoncesSeRechaza() {
        assertThat(error).isInstanceOf(BusinessRuleException.class);
    }

    private void entoncesHayUnConflicto() {
        assertThat(error).isInstanceOf(DuplicateResourceException.class);
    }

    private void entoncesElErrorDice(String parte) {
        assertThat(error).hasMessageContaining(parte);
    }

    private void entoncesNoSeMandoNada() {
        verify(mensajeria, never()).enviarTexto(anyString(), anyString());
    }

    private void entoncesSeDescartoLaVerificacion() {
        verify(verificacionRepository).deleteByUsuarioId(USUARIO_ID);
    }
}