package com.ensambla.backend.domain.useCase.negocio.eliminarNegocio;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.port.NegocioActivoPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.UsuarioTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EliminarNegocioTest {

    private static final NegocioId PRIMERO = NegocioId.de("10000000-0000-4000-8000-000000000001");
    private static final NegocioId SEGUNDO = NegocioId.de("10000000-0000-4000-8000-000000000002");
    private static final NegocioId AJENO = NegocioId.de("10000000-0000-4000-8000-000000000003");

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final NegocioActivoPort negocioActivoPort = mock(NegocioActivoPort.class);
    private final EliminarNegocio eliminar = new EliminarNegocio(
            negocioRepository, negocioActivoPort, new TransaccionDirecta());

    @Test
    @DisplayName("Eliminar el activo selecciona el negocio más antiguo que queda")
    void executeShouldSelectOldestRemainingWhenDeletingActive() {
        givenDosNegocios();
        when(negocioActivoPort.obtener(USUARIO_ID)).thenReturn(Optional.of(PRIMERO));

        eliminar.execute(new EliminarNegocioRequest(USUARIO_ID, PRIMERO));

        verify(negocioRepository).deleteById(PRIMERO);
        verify(negocioActivoPort).activar(USUARIO_ID, SEGUNDO);
    }

    @Test
    @DisplayName("Eliminar uno que no está activo conserva la selección de la sesión")
    void executeShouldKeepActiveWhenDeletingAnotherBusiness() {
        givenDosNegocios();
        when(negocioActivoPort.obtener(USUARIO_ID)).thenReturn(Optional.of(PRIMERO));

        eliminar.execute(new EliminarNegocioRequest(USUARIO_ID, SEGUNDO));

        verify(negocioRepository).deleteById(SEGUNDO);
        verify(negocioActivoPort, never()).activar(USUARIO_ID, PRIMERO);
    }

    @Test
    @DisplayName("Una selección ausente usa el negocio más antiguo como activo")
    void executeShouldFallBackToOldestWhenSessionHasNoSelection() {
        givenDosNegocios();
        when(negocioActivoPort.obtener(USUARIO_ID)).thenReturn(Optional.empty());

        eliminar.execute(new EliminarNegocioRequest(USUARIO_ID, PRIMERO));

        verify(negocioActivoPort).activar(USUARIO_ID, SEGUNDO);
    }

    @Test
    @DisplayName("Una selección que ya no pertenece a la cuenta también usa el más antiguo")
    void executeShouldFallBackWhenSessionSelectionIsStale() {
        givenDosNegocios();
        when(negocioActivoPort.obtener(USUARIO_ID)).thenReturn(Optional.of(AJENO));

        eliminar.execute(new EliminarNegocioRequest(USUARIO_ID, PRIMERO));

        verify(negocioActivoPort).activar(USUARIO_ID, SEGUNDO);
    }

    @Test
    @DisplayName("No permite eliminar el último negocio de la cuenta")
    void executeShouldRejectLastBusiness() {
        when(negocioRepository.findAllByDuenioIdParaActualizar(USUARIO_ID))
                .thenReturn(List.of(negocio(PRIMERO)));

        assertThatThrownBy(() -> eliminar.execute(new EliminarNegocioRequest(USUARIO_ID, PRIMERO)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("No podés eliminar el único negocio de tu cuenta.");

        verifyNoInteractions(negocioActivoPort);
        verify(negocioRepository, never()).deleteById(PRIMERO);
    }

    @Test
    @DisplayName("Un negocio ajeno responde como no encontrado")
    void executeShouldHideForeignBusiness() {
        givenDosNegocios();

        assertThatThrownBy(() -> eliminar.execute(new EliminarNegocioRequest(USUARIO_ID, AJENO)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Negocio no encontrado");

        verifyNoInteractions(negocioActivoPort);
        verify(negocioRepository, never()).deleteById(AJENO);
    }

    private void givenDosNegocios() {
        when(negocioRepository.findAllByDuenioIdParaActualizar(USUARIO_ID))
                .thenReturn(List.of(negocio(PRIMERO), negocio(SEGUNDO)));
    }

    private Negocio negocio(NegocioId id) {
        return Negocio.builder().id(id).build();
    }
}
