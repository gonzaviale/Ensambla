package com.ensambla.backend.domain.useCase.negocio.cambiarNegocioActivo;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.port.NegocioActivoPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.UsuarioTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CambiarNegocioActivoTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final NegocioActivoPort negocioActivoPort = mock(NegocioActivoPort.class);
    private final CambiarNegocioActivo cambiar = new CambiarNegocioActivo(negocioRepository, negocioActivoPort);

    @Test
    @DisplayName("Activa un negocio propio")
    void executeShouldActivateOwnedBusiness() {
        Negocio negocio = Negocio.builder().id(NEGOCIO_ID).build();
        when(negocioRepository.findByIdAndDuenioId(NEGOCIO_ID, USUARIO_ID)).thenReturn(Optional.of(negocio));

        cambiar.execute(new CambiarNegocioActivoRequest(USUARIO_ID, NEGOCIO_ID));

        verify(negocioActivoPort).activar(USUARIO_ID, NEGOCIO_ID);
    }

    @Test
    @DisplayName("Un negocio ajeno responde como no encontrado y no cambia la sesión")
    void executeShouldHideForeignBusiness() {
        when(negocioRepository.findByIdAndDuenioId(NEGOCIO_ID, USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cambiar.execute(new CambiarNegocioActivoRequest(USUARIO_ID, NEGOCIO_ID)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Negocio no encontrado");

        verifyNoInteractions(negocioActivoPort);
    }
}
