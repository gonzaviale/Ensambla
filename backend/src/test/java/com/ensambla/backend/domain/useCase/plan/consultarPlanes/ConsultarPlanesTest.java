package com.ensambla.backend.domain.useCase.plan.consultarPlanes;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ConsultaPlanesService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.ensambla.backend.support.ConsultaPlanesTestData.AHORA;
import static com.ensambla.backend.support.ConsultaPlanesTestData.consulta;
import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConsultarPlanesTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();

    private final NegocioService negocioService = mock(NegocioService.class);
    private final ConsultaPlanesService consultaPlanesService = mock(ConsultaPlanesService.class);
    private final ConsultarPlanes consultarPlanes = new ConsultarPlanes(
            negocioService, consultaPlanesService,
            Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));

    @Test
    @DisplayName("Resuelve el negocio del usuario y consulta con la hora del servidor")
    void executeShouldResolveBusinessAndPassServerInstant() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(consultaPlanesService.consultar(NEGOCIO_ID, AHORA)).thenReturn(consulta(false));

        var result = consultarPlanes.execute(USUARIO_ID);

        assertThat(result.consultadoEn()).isEqualTo(AHORA);
        assertThat(result.planes()).hasSize(2);
        verify(consultaPlanesService).consultar(NEGOCIO_ID, AHORA);
    }

    @Test
    @DisplayName("Un usuario sin negocio no consulta planes de otro usuario")
    void executeShouldPropagateMissingBusiness() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID))
                .thenThrow(new ResourceNotFoundException("Negocio no encontrado"));

        assertThatThrownBy(() -> consultarPlanes.execute(USUARIO_ID))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Negocio no encontrado");

        verifyNoInteractions(consultaPlanesService);
    }
}
