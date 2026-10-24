package com.ensambla.backend.domain.useCase.campo;

import com.ensambla.backend.domain.dto.campo.CampoItem;
import com.ensambla.backend.domain.dto.campo.EditarCampoRequest;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.infrastructure.config.useCase.CampoUseCaseConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EditarCampoUseCaseTest {

    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final UUID CAMPO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final EditarCampoRequest REQUEST =
            new EditarCampoRequest(USUARIO_ID, CAMPO_ID, "Costo", "NUMERO", false, "0", null, null, null);
    private static final ConfiguracionCampo CONFIGURACION =
            new ConfiguracionCampo("Costo", "NUMERO", false, "0", null, null, null);

    private final NegocioService negocioService = mock(NegocioService.class);
    private final CampoService campoService = mock(CampoService.class);
    private final BloqueService bloqueService = mock(BloqueService.class);
    private final CampoUseCaseConfig config = new CampoUseCaseConfig();
    private final EditarCampoUseCase editarCampoUseCase = config.editarCampoUseCase(
            negocioService,
            campoService,
            bloqueService,
            config.editarCampoMapper(config.campoItemMapper()),
            Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));

    @Test
    @DisplayName("Edita validando contra el bloque dueño del campo y con la hora del reloj")
    void executeShouldEditAgainstOwnerBloque() {
        Campo campo = givenCampo("Precio");
        Bloque bloque = Bloque.builder().id(BLOQUE_ID).negocioId(NEGOCIO_ID).build();
        givenNegocio();
        when(campoService.obtener(CAMPO_ID, NEGOCIO_ID)).thenReturn(campo);
        when(bloqueService.obtener(BLOQUE_ID, NEGOCIO_ID)).thenReturn(bloque);
        when(campoService.editar(bloque, campo, CONFIGURACION, AHORA)).thenReturn(givenCampo("Costo"));

        CampoItem result = editarCampoUseCase.execute(REQUEST);

        assertThat(result.id()).isEqualTo(CAMPO_ID);
        assertThat(result.nombre()).isEqualTo("Costo");
    }

    @Test
    @DisplayName("Un campo ajeno propaga el 404 sin consultar el bloque ni editar")
    void executeShouldStopWhenCampoIsForeign() {
        givenNegocio();
        when(campoService.obtener(CAMPO_ID, NEGOCIO_ID)).thenThrow(new ResourceNotFoundException("El campo no existe"));

        assertThatThrownBy(() -> editarCampoUseCase.execute(REQUEST))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(bloqueService);
        verify(campoService, never()).editar(any(), any(), any(), any());
    }

    // --- arrange ---
    private void givenNegocio() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    private Campo givenCampo(String nombre) {
        return Campo.builder()
                .id(CAMPO_ID)
                .negocioId(NEGOCIO_ID)
                .bloqueId(BLOQUE_ID)
                .nombre(nombre)
                .tipo(TipoCampo.NUMERO)
                .opciones(List.of())
                .posicion((short) 0)
                .build();
    }
}
