package com.ensambla.backend.domain.useCase.campo;

import com.ensambla.backend.domain.dto.campo.CampoItem;
import com.ensambla.backend.domain.dto.campo.ListarCamposRequest;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.infrastructure.config.useCase.CampoUseCaseConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ListarCamposUseCaseTest {

    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final ListarCamposRequest REQUEST = new ListarCamposRequest(USUARIO_ID, BLOQUE_ID);

    private final NegocioService negocioService = mock(NegocioService.class);
    private final BloqueService bloqueService = mock(BloqueService.class);
    private final CampoService campoService = mock(CampoService.class);
    private final CampoUseCaseConfig config = new CampoUseCaseConfig();
    private final ListarCamposUseCase listarCamposUseCase = config.listarCamposUseCase(
            negocioService,
            bloqueService,
            campoService,
            config.listarCamposMapper(config.campoItemMapper()));

    @Test
    @DisplayName("Lista los campos del bloque del negocio de la sesión en el orden del repositorio")
    void executeShouldListCamposOfSessionBloque() {
        givenNegocio();
        when(bloqueService.obtener(BLOQUE_ID, NEGOCIO_ID)).thenReturn(Bloque.builder().id(BLOQUE_ID).build());
        when(campoService.listarDeBloque(BLOQUE_ID, NEGOCIO_ID))
                .thenReturn(List.of(givenCampo("Producto"), givenCampo("Precio")));

        List<CampoItem> result = listarCamposUseCase.execute(REQUEST);

        assertThat(result).extracting(CampoItem::nombre).containsExactly("Producto", "Precio");
    }

    @Test
    @DisplayName("Si el bloque es ajeno propaga el 404 sin consultar los campos")
    void executeShouldStopWhenBloqueIsForeign() {
        givenNegocio();
        when(bloqueService.obtener(BLOQUE_ID, NEGOCIO_ID))
                .thenThrow(new ResourceNotFoundException("El bloque no existe"));

        assertThatThrownBy(() -> listarCamposUseCase.execute(REQUEST))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(campoService);
    }

    // --- arrange ---
    private void givenNegocio() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    private Campo givenCampo(String nombre) {
        return Campo.builder()
                .bloqueId(BLOQUE_ID)
                .nombre(nombre)
                .tipo(TipoCampo.TEXTO)
                .opciones(List.of())
                .build();
    }
}
