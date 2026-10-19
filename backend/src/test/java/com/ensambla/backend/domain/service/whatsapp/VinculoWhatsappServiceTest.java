package com.ensambla.backend.domain.service.whatsapp;

import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.port.GeneradorCodigoPort;
import com.ensambla.backend.domain.repository.whatsapp.VinculoWhatsappRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ensambla.backend.support.WhatsappTestData.AHORA;
import static com.ensambla.backend.support.WhatsappTestData.CODIGO;
import static com.ensambla.backend.support.WhatsappTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.WhatsappTestData.OTRO_NEGOCIO_ID;
import static com.ensambla.backend.support.WhatsappTestData.TELEFONO;
import static com.ensambla.backend.support.WhatsappTestData.vinculado;
import static com.ensambla.backend.support.WhatsappTestData.vinculoConCodigo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VinculoWhatsappServiceTest {

    private static final String OTRO_CODIGO = "105377";
    private static final String OTRO_TELEFONO = "5491199887766";

    @Mock
    private VinculoWhatsappRepository vinculoWhatsappRepository;

    @Mock
    private GeneradorCodigoPort generadorCodigoPort;

    @InjectMocks
    private VinculoWhatsappService vinculoWhatsappService;

    @Test
    @DisplayName("La primera vez crea el vínculo del negocio con un código que vence en 10 minutos")
    void generarCodigoShouldCreateVinculoWhenMissing() {
        givenNoVinculoDelNegocio();
        givenCodigosGenerados(CODIGO);
        givenCodigoLibre(CODIGO);
        givenSaveDevuelveLoGuardado();

        VinculoWhatsapp result = vinculoWhatsappService.generarCodigo(NEGOCIO_ID, AHORA);

        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.getCodigo()).isEqualTo(CODIGO);
        assertThat(result.getCodigoVenceEn()).isEqualTo(AHORA.plusMinutes(10));
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Si el negocio ya tenía vínculo le reemplaza el código y conserva el número")
    void generarCodigoShouldReplaceCodigoOfExistingVinculo() {
        givenVinculoDelNegocio(vinculado(NEGOCIO_ID, TELEFONO));
        givenCodigosGenerados(OTRO_CODIGO);
        givenCodigoLibre(OTRO_CODIGO);
        givenSaveDevuelveLoGuardado();

        VinculoWhatsapp result = vinculoWhatsappService.generarCodigo(NEGOCIO_ID, AHORA.plusDays(1));

        assertThat(result.getCodigo()).isEqualTo(OTRO_CODIGO);
        assertThat(result.getTelefono()).isEqualTo(TELEFONO);
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Si el código generado ya lo tiene otro negocio genera otro")
    void generarCodigoShouldRetryWhenCodigoIsTaken() {
        givenNoVinculoDelNegocio();
        givenCodigosGenerados(CODIGO, OTRO_CODIGO);
        when(vinculoWhatsappRepository.existsByCodigo(CODIGO)).thenReturn(true);
        givenCodigoLibre(OTRO_CODIGO);
        givenSaveDevuelveLoGuardado();

        VinculoWhatsapp result = vinculoWhatsappService.generarCodigo(NEGOCIO_ID, AHORA);

        assertThat(result.getCodigo()).isEqualTo(OTRO_CODIGO);
        verify(generadorCodigoPort, times(2)).generar();
    }

    @Test
    @DisplayName("Un código vigente vincula el número con el negocio del código")
    void vincularShouldLinkTelefonoWithCodigoOwner() {
        givenVinculoConCodigo(vinculoConCodigo(NEGOCIO_ID));
        givenTelefonoSinVincular();

        boolean result = vinculoWhatsappService.vincular(CODIGO, TELEFONO, AHORA.plusMinutes(1));

        assertThat(result).isTrue();
        VinculoWhatsapp guardado = thenSaved().getFirst();
        assertThat(guardado.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(guardado.getTelefono()).isEqualTo(TELEFONO);
        assertThat(guardado.getCodigo()).isNull();
    }

    @Test
    @DisplayName("Un código vencido no vincula nada")
    void vincularShouldRejectExpiredCodigo() {
        givenVinculoConCodigo(vinculoConCodigo(NEGOCIO_ID));

        boolean result = vinculoWhatsappService.vincular(CODIGO, TELEFONO, AHORA.plusMinutes(10));

        assertThat(result).isFalse();
        verify(vinculoWhatsappRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un código que no existe no vincula nada")
    void vincularShouldRejectUnknownCodigo() {
        when(vinculoWhatsappRepository.findByCodigo(CODIGO)).thenReturn(Optional.empty());

        boolean result = vinculoWhatsappService.vincular(CODIGO, TELEFONO, AHORA);

        assertThat(result).isFalse();
        verify(vinculoWhatsappRepository, never()).save(any());
    }

    @Test
    @DisplayName("Si el número estaba en otro negocio primero lo desvincula de ahí y después lo vincula al nuevo")
    void vincularShouldMoveTelefonoFromOtherNegocio() {
        VinculoWhatsapp anterior = vinculado(OTRO_NEGOCIO_ID, TELEFONO);
        givenVinculoConCodigo(vinculoConCodigo(NEGOCIO_ID));
        when(vinculoWhatsappRepository.findByTelefono(TELEFONO)).thenReturn(Optional.of(anterior));

        boolean result = vinculoWhatsappService.vincular(CODIGO, TELEFONO, AHORA.plusMinutes(1));

        assertThat(result).isTrue();
        List<VinculoWhatsapp> guardados = thenSaved();
        assertThat(guardados.get(0).getNegocioId()).isEqualTo(OTRO_NEGOCIO_ID);
        assertThat(guardados.get(0).getTelefono()).isNull();
        assertThat(guardados.get(1).getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(guardados.get(1).getTelefono()).isEqualTo(TELEFONO);
    }

    @Test
    @DisplayName("Si el número ya era del mismo negocio no lo desvincula")
    void vincularShouldNotUnlinkTheSameNegocio() {
        VinculoWhatsapp mismo = vinculoConCodigo(NEGOCIO_ID);
        mismo.setTelefono(TELEFONO);
        mismo.setVinculadoEn(AHORA);
        givenVinculoConCodigo(mismo);
        when(vinculoWhatsappRepository.findByTelefono(TELEFONO))
                .thenReturn(Optional.of(vinculado(NEGOCIO_ID, TELEFONO)));

        boolean result = vinculoWhatsappService.vincular(CODIGO, TELEFONO, AHORA.plusMinutes(1));

        assertThat(result).isTrue();
        assertThat(thenSaved()).singleElement().extracting(VinculoWhatsapp::getTelefono).isEqualTo(TELEFONO);
    }

    @Test
    @DisplayName("Otro número que manda el código queda vinculado en lugar del anterior")
    void vincularShouldReplaceTheTelefonoOfTheSameNegocio() {
        VinculoWhatsapp vinculo = vinculoConCodigo(NEGOCIO_ID);
        vinculo.setTelefono(TELEFONO);
        vinculo.setVinculadoEn(AHORA);
        givenVinculoConCodigo(vinculo);
        when(vinculoWhatsappRepository.findByTelefono(OTRO_TELEFONO)).thenReturn(Optional.empty());

        boolean result = vinculoWhatsappService.vincular(CODIGO, OTRO_TELEFONO, AHORA.plusMinutes(1));

        assertThat(result).isTrue();
        assertThat(thenSaved()).singleElement().extracting(VinculoWhatsapp::getTelefono).isEqualTo(OTRO_TELEFONO);
    }

    @Test
    @DisplayName("Un número vinculado resuelve su negocio")
    void negocioDelTelefonoShouldReturnTheNegocio() {
        when(vinculoWhatsappRepository.findByTelefono(TELEFONO))
                .thenReturn(Optional.of(vinculado(NEGOCIO_ID, TELEFONO)));

        Optional<UUID> result = vinculoWhatsappService.negocioDelTelefono(TELEFONO);

        assertThat(result).contains(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Un número sin vincular no tiene negocio")
    void negocioDelTelefonoShouldBeEmptyWhenNotLinked() {
        givenTelefonoSinVincular();

        Optional<UUID> result = vinculoWhatsappService.negocioDelTelefono(TELEFONO);

        assertThat(result).isEmpty();
    }

    // --- arrange ---
    private void givenNoVinculoDelNegocio() {
        when(vinculoWhatsappRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    private void givenVinculoDelNegocio(VinculoWhatsapp vinculo) {
        when(vinculoWhatsappRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(vinculo));
    }

    private void givenVinculoConCodigo(VinculoWhatsapp vinculo) {
        when(vinculoWhatsappRepository.findByCodigo(CODIGO)).thenReturn(Optional.of(vinculo));
    }

    private void givenTelefonoSinVincular() {
        when(vinculoWhatsappRepository.findByTelefono(TELEFONO)).thenReturn(Optional.empty());
    }

    private void givenCodigosGenerados(String primero, String... siguientes) {
        when(generadorCodigoPort.generar()).thenReturn(primero, siguientes);
    }

    private void givenCodigoLibre(String codigo) {
        when(vinculoWhatsappRepository.existsByCodigo(codigo)).thenReturn(false);
    }

    private void givenSaveDevuelveLoGuardado() {
        when(vinculoWhatsappRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // --- assert ---
    // Lo que se guardó en cada llamada, en el orden en que se guardó.
    private List<VinculoWhatsapp> thenSaved() {
        ArgumentCaptor<VinculoWhatsapp> captor = ArgumentCaptor.forClass(VinculoWhatsapp.class);
        verify(vinculoWhatsappRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues();
    }
}
