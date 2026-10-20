package com.ensambla.backend.domain.service.whatsapp;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.whatsapp.CodigoVinculacion;
import com.ensambla.backend.domain.model.whatsapp.TelefonoWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.repository.whatsapp.CodigoVinculacionRepository;
import com.ensambla.backend.domain.repository.whatsapp.VinculoWhatsappRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VinculacionWhatsappServiceTest {

    private static final UUID USUARIO_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID OTRO_USUARIO_ID = UUID.fromString("00000000-0000-4000-8000-000000000002");
    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");
    private static final TelefonoWhatsapp TELEFONO = new TelefonoWhatsapp("5491122334455");

    private final CodigoVinculacionRepository codigoRepository = mock(CodigoVinculacionRepository.class);
    private final VinculoWhatsappRepository vinculoRepository = mock(VinculoWhatsappRepository.class);
    private final NegocioService negocioService = mock(NegocioService.class);
    private final VinculacionWhatsappService service =
            new VinculacionWhatsappService(codigoRepository, vinculoRepository, negocioService);

    @Test
    @DisplayName("Generar un código descarta los pendientes y guarda uno de 6 caracteres sin letras confusas")
    void generarCodigoShouldReplacePendingAndSaveSixCharCode() {
        givenCodigoGuardadoDevuelveElMismo();

        CodigoVinculacion result = service.generarCodigo(USUARIO_ID, AHORA);

        assertThat(result.getCodigo()).matches("[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{6}");
        assertThat(result.getExpiraEn()).isEqualTo(AHORA.plusMinutes(10));
        var orden = inOrder(codigoRepository);
        orden.verify(codigoRepository).deleteSinUsarByUsuarioId(USUARIO_ID);
        orden.verify(codigoRepository).save(any(CodigoVinculacion.class));
    }

    @Test
    @DisplayName("Si el código sorteado ya existe, sortea otro")
    void generarCodigoShouldRetryWhenCodeAlreadyExists() {
        givenCodigoGuardadoDevuelveElMismo();
        when(codigoRepository.existsByCodigo(anyString())).thenReturn(true, false);

        service.generarCodigo(USUARIO_ID, AHORA);

        verify(codigoRepository, org.mockito.Mockito.times(2)).existsByCodigo(anyString());
    }

    @Test
    @DisplayName("Canjear un código vigente vincula el teléfono con el negocio del usuario que lo pidió")
    void canjearShouldLinkPhoneToUsersNegocio() {
        CodigoVinculacion codigo = givenCodigo(USUARIO_ID);
        when(codigoRepository.findByCodigo("ABC234")).thenReturn(Optional.of(codigo));
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(vinculoRepository.save(any(VinculoWhatsapp.class))).thenAnswer(call -> call.getArgument(0));

        VinculoWhatsapp result = service.canjear("  abc234 ", TELEFONO, AHORA.plusMinutes(1));

        assertThat(result.getUsuarioId()).isEqualTo(USUARIO_ID);
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.getTelefono()).isEqualTo("5491122334455");
        assertThat(codigo.getUsadoEn()).isEqualTo(AHORA.plusMinutes(1));
        verify(codigoRepository).save(codigo);
        var orden = inOrder(vinculoRepository);
        orden.verify(vinculoRepository).deleteByUsuarioId(USUARIO_ID);
        orden.verify(vinculoRepository).save(any(VinculoWhatsapp.class));
    }

    @Test
    @DisplayName("Un usuario que vuelve a vincular el mismo teléfono lo reemplaza sin error")
    void canjearShouldAllowRelinkingOwnPhone() {
        when(codigoRepository.findByCodigo("ABC234")).thenReturn(Optional.of(givenCodigo(USUARIO_ID)));
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(vinculoRepository.findByTelefono("5491122334455")).thenReturn(Optional.of(givenVinculo(USUARIO_ID)));
        when(vinculoRepository.save(any(VinculoWhatsapp.class))).thenAnswer(call -> call.getArgument(0));

        VinculoWhatsapp result = service.canjear("ABC234", TELEFONO, AHORA);

        assertThat(result.getUsuarioId()).isEqualTo(USUARIO_ID);
    }

    @Test
    @DisplayName("Un código que no existe se rechaza")
    void canjearShouldRejectUnknownCode() {
        when(codigoRepository.findByCodigo("NOEXISTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.canjear("noexiste", TELEFONO, AHORA))
                .isInstanceOf(BusinessRuleException.class);
        verify(vinculoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un código vencido se rechaza y no vincula nada")
    void canjearShouldRejectExpiredCode() {
        when(codigoRepository.findByCodigo("ABC234")).thenReturn(Optional.of(givenCodigo(USUARIO_ID)));

        assertThatThrownBy(() -> service.canjear("ABC234", TELEFONO, AHORA.plusMinutes(11)))
                .isInstanceOf(BusinessRuleException.class);
        verify(vinculoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un teléfono ya vinculado a otra cuenta se rechaza")
    void canjearShouldRejectPhoneLinkedToAnotherUser() {
        when(codigoRepository.findByCodigo("ABC234")).thenReturn(Optional.of(givenCodigo(USUARIO_ID)));
        when(vinculoRepository.findByTelefono("5491122334455")).thenReturn(Optional.of(givenVinculo(OTRO_USUARIO_ID)));

        assertThatThrownBy(() -> service.canjear("ABC234", TELEFONO, AHORA))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("otra cuenta");
        verify(vinculoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Obtener el vínculo de un usuario sin número vinculado responde que no existe")
    void obtenerShouldThrowWhenUserHasNoLink() {
        when(vinculoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(USUARIO_ID)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Obtener el vínculo de un usuario devuelve su número")
    void obtenerShouldReturnUsersLink() {
        VinculoWhatsapp vinculo = givenVinculo(USUARIO_ID);
        when(vinculoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(vinculo));

        assertThat(service.obtener(USUARIO_ID)).isSameAs(vinculo);
    }

    @Test
    @DisplayName("Busca el vínculo por el teléfono normalizado")
    void buscarPorTelefonoShouldDelegateWithDigits() {
        VinculoWhatsapp vinculo = givenVinculo(USUARIO_ID);
        when(vinculoRepository.findByTelefono("5491122334455")).thenReturn(Optional.of(vinculo));

        assertThat(service.buscarPorTelefono(TELEFONO)).containsSame(vinculo);
    }

    @Test
    @DisplayName("Desvincular borra el vínculo del usuario")
    void desvincularShouldDeleteUsersLink() {
        when(vinculoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(givenVinculo(USUARIO_ID)));

        service.desvincular(USUARIO_ID);

        verify(vinculoRepository).deleteByUsuarioId(USUARIO_ID);
    }

    @Test
    @DisplayName("Desvincular a un usuario sin número responde que no existe")
    void desvincularShouldThrowWhenUserHasNoLink() {
        when(vinculoRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.desvincular(USUARIO_ID)).isInstanceOf(ResourceNotFoundException.class);
        verify(vinculoRepository, never()).deleteByUsuarioId(any());
    }

    private void givenCodigoGuardadoDevuelveElMismo() {
        ArgumentCaptor<CodigoVinculacion> captor = ArgumentCaptor.forClass(CodigoVinculacion.class);
        when(codigoRepository.save(captor.capture())).thenAnswer(call -> call.getArgument(0));
    }

    private static CodigoVinculacion givenCodigo(UUID usuarioId) {
        return CodigoVinculacion.nuevo(usuarioId, "ABC234", AHORA);
    }

    private static VinculoWhatsapp givenVinculo(UUID usuarioId) {
        return VinculoWhatsapp.nuevo(usuarioId, NEGOCIO_ID, TELEFONO, AHORA);
    }
}
