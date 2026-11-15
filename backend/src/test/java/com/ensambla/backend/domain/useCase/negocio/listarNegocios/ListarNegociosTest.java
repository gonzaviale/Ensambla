package com.ensambla.backend.domain.useCase.negocio.listarNegocios;

import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.negocio.ListaNegociosResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;

import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.UsuarioTestData.FECHA;
import static com.ensambla.backend.support.UsuarioTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListarNegociosTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ControlPlanService controlPlanService = mock(ControlPlanService.class);
    private final ListarNegocios listarNegocios = new ListarNegocios(negocioRepository, controlPlanService,
            Clock.fixed(FECHA.toInstant(), ZoneOffset.UTC));

    @Test
    @DisplayName("Lista los negocios ordenados con el activo y el límite vigente de la cuenta")
    void executeShouldBuildAccountList() {
        Negocio negocio = Negocio.builder().id(NEGOCIO_ID).nombre("Mi negocio").creadoEn(FECHA).build();
        when(negocioRepository.findAllByDuenioId(USUARIO_ID)).thenReturn(List.of(negocio));
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(controlPlanService.limiteNegocios(USUARIO_ID, FECHA)).thenReturn(6);

        ListaNegociosResponse response = listarNegocios.execute(new ListarNegociosRequest(USUARIO_ID));

        assertThat(response.activoId()).isEqualTo(NEGOCIO_ID);
        assertThat(response.limite()).isEqualTo(6);
        assertThat(response.negocios()).hasSize(1);
        verify(controlPlanService).limiteNegocios(USUARIO_ID, FECHA);
    }
}
