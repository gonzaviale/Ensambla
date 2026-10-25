package com.ensambla.backend.domain.useCase.campo.listarCampos;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.useCase.campo.CampoItem;
import com.ensambla.backend.infrastructure.config.useCase.CampoUseCaseConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ListarCamposTest {

    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final ListarCamposRequest REQUEST = new ListarCamposRequest(USUARIO_ID, BLOQUE_ID);

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final BloqueRepository bloqueRepository = mock(BloqueRepository.class);
    private final CampoRepository campoRepository = mock(CampoRepository.class);
    private final CampoUseCaseConfig config = new CampoUseCaseConfig();
    private final ListarCampos listarCampos = config.listarCampos(
            negocioRepository,
            bloqueRepository,
            campoRepository);

    @Test
    @DisplayName("Lista los campos del bloque del negocio de la sesión en el orden del repositorio")
    void executeShouldListCamposOfSessionBloque() {
        givenNegocio();
        when(bloqueRepository.obtener(BLOQUE_ID, NEGOCIO_ID)).thenReturn(Bloque.builder().id(BLOQUE_ID).build());
        when(campoRepository.findByBloqueIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID))
                .thenReturn(List.of(givenCampo("Producto"), givenCampo("Precio")));

        List<CampoItem> result = listarCampos.execute(REQUEST);

        assertThat(result).extracting(CampoItem::nombre).containsExactly("Producto", "Precio");
    }

    @Test
    @DisplayName("Si el bloque es ajeno propaga el 404 sin consultar los campos")
    void executeShouldStopWhenBloqueIsForeign() {
        givenNegocio();
        when(bloqueRepository.obtener(BLOQUE_ID, NEGOCIO_ID))
                .thenThrow(new ResourceNotFoundException("El bloque no existe"));

        assertThatThrownBy(() -> listarCampos.execute(REQUEST))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(campoRepository);
    }

    // --- arrange ---
    private void givenNegocio() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
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
