package com.ensambla.backend.domain.useCase.whatsapp.procesarMensajeEntrante;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.whatsapp.CodigoVinculacion;
import com.ensambla.backend.domain.model.whatsapp.MensajeEntrante;
import com.ensambla.backend.domain.model.whatsapp.MensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TelefonoWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TextosWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TipoMensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.whatsapp.CodigoVinculacionRepository;
import com.ensambla.backend.domain.repository.whatsapp.MensajeWhatsappRepository;
import com.ensambla.backend.domain.repository.whatsapp.VerificacionWhatsappRepository;
import com.ensambla.backend.domain.repository.whatsapp.VinculoWhatsappRepository;
import com.ensambla.backend.domain.service.whatsapp.VinculacionWhatsappService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcesarMensajeEntranteTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    private static final UsuarioId OTRO_USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000002");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final Instant INSTANTE = Instant.parse("2026-10-19T12:00:00Z");
    private static final OffsetDateTime AHORA = OffsetDateTime.ofInstant(INSTANTE, ZoneOffset.UTC);
    private static final String TELEFONO = "5491122334455";
    private static final String CODIGO = "ABC234";

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final CodigoVinculacionRepository codigoRepository = mock(CodigoVinculacionRepository.class);
    private final VinculoWhatsappRepository vinculoRepository = mock(VinculoWhatsappRepository.class,
            CALLS_REAL_METHODS);
    private final VerificacionWhatsappRepository verificacionRepository = mock(VerificacionWhatsappRepository.class);
    private final MensajeWhatsappRepository mensajeRepository = mock(MensajeWhatsappRepository.class);
    private final MensajeriaWhatsappPort mensajeria = mock(MensajeriaWhatsappPort.class);
    private final ProcesarMensajeEntrante procesarMensajeEntrante = new WhatsappUseCaseConfig()
            .procesarMensajeEntrante(negocioRepository, codigoRepository, vinculoRepository, mensajeRepository,
                    new VinculacionWhatsappService(vinculoRepository, verificacionRepository), mensajeria,
                    new TransaccionDirecta(), Clock.fixed(INSTANTE, ZoneOffset.UTC));

    @Test
    @DisplayName("VINCULAR con un código vigente vincula el número al negocio del usuario")
    void vincularConUnCodigoVigenteVinculaElNumero() {
        dadoUnCodigoVigenteDe(USUARIO_ID);
        dadoQueElNumeroEstaLibre();

        cuandoLlegaElTexto("ID1", " vincular abc234 ");

        entoncesSeVinculoElNumeroAlNegocioDe(USUARIO_ID);
    }

    @Test
    @DisplayName("VINCULAR reemplaza el número anterior, marca el código como usado y avisa por el chat")
    void vincularReemplazaElNumeroAnteriorYAvisa() {
        dadoUnCodigoVigenteDe(USUARIO_ID);
        dadoQueElNumeroEstaLibre();

        cuandoLlegaElTexto("ID1", " vincular abc234 ");

        entoncesSeBorroElVinculoAnterior();
        entoncesSeGuardoElCodigoUsado();
        entoncesSeContesto(TextosWhatsapp.VINCULADO);
    }

    @Test
    @DisplayName("VINCULAR permite que el mismo usuario vuelva a vincular su número")
    void vincularPermiteQueElMismoUsuarioVuelvaAVincular() {
        dadoUnCodigoVigenteDe(USUARIO_ID);
        dadoQueElNumeroEsDe(USUARIO_ID);

        cuandoLlegaElTexto("ID1", "VINCULAR ABC234");

        entoncesSeContesto(TextosWhatsapp.VINCULADO);
    }

    @Test
    @DisplayName("VINCULAR con un código inexistente responde el motivo y no vincula")
    void vincularConUnCodigoInexistenteNoVincula() {
        dadoQueNoExisteNingunCodigo();

        cuandoLlegaElTexto("ID1", "VINCULAR ZZZ999");

        entoncesSeContesto("El código de vinculación no existe");
        entoncesNoSeVinculoNada();
    }

    @Test
    @DisplayName("VINCULAR con un código vencido responde el motivo y no vincula")
    void vincularConUnCodigoVencidoNoVincula() {
        dadoUnCodigoVencido();

        cuandoLlegaElTexto("ID1", "VINCULAR ABC234");

        entoncesSeContesto("El código de vinculación venció o ya se usó");
        entoncesNoSeVinculoNada();
    }

    @Test
    @DisplayName("VINCULAR un número que ya es de otra cuenta responde el motivo y no vincula")
    void vincularUnNumeroDeOtraCuentaNoVincula() {
        dadoUnCodigoVigenteDe(USUARIO_ID);
        dadoQueElNumeroEsDe(OTRO_USUARIO_ID);

        cuandoLlegaElTexto("ID1", "VINCULAR ABC234");

        entoncesSeContesto("Ese número de WhatsApp ya está vinculado a otra cuenta");
        entoncesNoSeVinculoNada();
    }

    @Test
    @DisplayName("Un texto de un número vinculado se guarda en el negocio del vínculo sin contestar nada")
    void unTextoDeUnNumeroVinculadoSeGuardaEnSuNegocio() {
        dadoQueElNumeroEsDe(USUARIO_ID);
        dadoQueElMensajeEsNuevo("ID2");

        cuandoLlegaElTexto("ID2", "gasté 5000 en harina");

        entoncesSeEncoloEnElNegocioElTexto("gasté 5000 en harina");
        entoncesNoSeContestoNada();
    }

    @Test
    @DisplayName("Una nota de voz de un número vinculado se guarda sin buscar un código")
    void unaNotaDeVozDeUnNumeroVinculadoSeGuarda() {
        dadoQueElNumeroEsDe(USUARIO_ID);

        cuandoLlega(new MensajeEntrante(TELEFONO, "ID3", TipoMensajeWhatsapp.AUDIO, null, "audio/ogg", 4, AHORA));

        entoncesSeEncoloUn(TipoMensajeWhatsapp.AUDIO);
    }

    @Test
    @DisplayName("Una foto de un número vinculado se guarda con su pie y su tipo")
    void unaFotoDeUnNumeroVinculadoSeGuardaConSuPie() {
        dadoQueElNumeroEsDe(USUARIO_ID);

        cuandoLlega(new MensajeEntrante(TELEFONO, "ID5", TipoMensajeWhatsapp.IMAGEN, "ventas", "image/jpeg", null,
                AHORA));

        entoncesSeEncoloUnaFotoConElPie("ventas", "image/jpeg");
    }

    @Test
    @DisplayName("Un mensaje que el proveedor reintentó no se guarda dos veces")
    void unMensajeReintentadoNoSeGuardaDosVeces() {
        dadoQueElNumeroEsDe(USUARIO_ID);
        dadoQueElMensajeYaLlego("ID2");

        cuandoLlegaElTexto("ID2", "hola");

        entoncesNoSeEncoloNada();
    }

    @Test
    @DisplayName("Un mensaje de un número sin vincular recibe las instrucciones y no se guarda")
    void unMensajeDeUnNumeroSinVincularRecibeLasInstrucciones() {
        dadoQueElNumeroEstaLibre();

        cuandoLlegaElTexto("ID4", "hola");

        entoncesSeContesto(TextosWhatsapp.NO_VINCULADO);
        entoncesNoSeEncoloNada();
    }

    private void dadoUnCodigoVigenteDe(UsuarioId usuarioId) {
        when(codigoRepository.findByCodigo(CODIGO))
                .thenReturn(Optional.of(CodigoVinculacion.nuevo(usuarioId, CODIGO, AHORA)));
        when(negocioRepository.obtenerIdDelUsuario(usuarioId)).thenReturn(NEGOCIO_ID);
    }

    private void dadoUnCodigoVencido() {
        when(codigoRepository.findByCodigo(CODIGO))
                .thenReturn(Optional.of(CodigoVinculacion.nuevo(USUARIO_ID, CODIGO, AHORA.minusHours(1))));
    }

    private void dadoQueNoExisteNingunCodigo() {
        when(codigoRepository.findByCodigo(anyString())).thenReturn(Optional.empty());
    }

    private void dadoQueElNumeroEstaLibre() {
        when(vinculoRepository.findByTelefono(TELEFONO)).thenReturn(Optional.empty());
    }

    private void dadoQueElNumeroEsDe(UsuarioId usuarioId) {
        when(vinculoRepository.findByTelefono(TELEFONO)).thenReturn(Optional.of(
                VinculoWhatsapp.nuevo(usuarioId, NEGOCIO_ID, new TelefonoWhatsapp(TELEFONO), AHORA)));
    }

    private void dadoQueElMensajeEsNuevo(String idExterno) {
        when(mensajeRepository.existsByIdExterno(idExterno)).thenReturn(false);
    }

    private void dadoQueElMensajeYaLlego(String idExterno) {
        when(mensajeRepository.existsByIdExterno(idExterno)).thenReturn(true);
    }

    private void cuandoLlegaElTexto(String idExterno, String texto) {
        cuandoLlega(new MensajeEntrante(TELEFONO, idExterno, TipoMensajeWhatsapp.TEXTO, texto, null, null, AHORA));
    }

    private void cuandoLlega(MensajeEntrante entrante) {
        procesarMensajeEntrante.execute(entrante);
    }

    private VinculoWhatsapp vinculoGuardado() {
        ArgumentCaptor<VinculoWhatsapp> captor = ArgumentCaptor.forClass(VinculoWhatsapp.class);
        verify(vinculoRepository).save(captor.capture());
        return captor.getValue();
    }

    private MensajeWhatsapp mensajeEncolado() {
        ArgumentCaptor<MensajeWhatsapp> captor = ArgumentCaptor.forClass(MensajeWhatsapp.class);
        verify(mensajeRepository).save(captor.capture());
        return captor.getValue();
    }

    private void entoncesSeVinculoElNumeroAlNegocioDe(UsuarioId usuarioId) {
        VinculoWhatsapp vinculo = vinculoGuardado();
        assertThat(vinculo.getUsuarioId()).isEqualTo(usuarioId);
        assertThat(vinculo.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(vinculo.getTelefono()).isEqualTo(TELEFONO);
    }

    private void entoncesSeBorroElVinculoAnterior() {
        verify(vinculoRepository).deleteByUsuarioId(USUARIO_ID);
    }

    private void entoncesSeGuardoElCodigoUsado() {
        verify(codigoRepository).save(any(CodigoVinculacion.class));
    }

    private void entoncesSeContesto(String texto) {
        verify(mensajeria).enviarTexto(TELEFONO, texto);
    }

    private void entoncesNoSeContestoNada() {
        verify(mensajeria, never()).enviarTexto(anyString(), anyString());
    }

    private void entoncesNoSeVinculoNada() {
        verify(vinculoRepository, never()).save(any());
    }

    private void entoncesSeEncoloEnElNegocioElTexto(String texto) {
        MensajeWhatsapp mensaje = mensajeEncolado();
        assertThat(mensaje.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(mensaje.getTexto()).isEqualTo(texto);
    }

    private void entoncesSeEncoloUn(TipoMensajeWhatsapp tipo) {
        assertThat(mensajeEncolado().getTipo()).isEqualTo(tipo);
    }

    private void entoncesSeEncoloUnaFotoConElPie(String pie, String mime) {
        MensajeWhatsapp mensaje = mensajeEncolado();
        assertThat(mensaje.getTipo()).isEqualTo(TipoMensajeWhatsapp.IMAGEN);
        assertThat(mensaje.getTexto()).isEqualTo(pie);
        assertThat(mensaje.getMimeMedia()).isEqualTo(mime);
    }

    private void entoncesNoSeEncoloNada() {
        verify(mensajeRepository, never()).save(any());
    }
}