package com.ensambla.backend.domain.useCase.campo.eliminarCampo;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.infrastructure.config.useCase.CampoUseCaseConfig;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EliminarCampoTest {

    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final CampoId CAMPO_ID = CampoId.de("d0000000-0000-4000-8000-000000000001");
    private static final CampoId FORMULA_ID = CampoId.de("d0000000-0000-4000-8000-000000000002");
    private static final EliminarCampoRequest REQUEST = new EliminarCampoRequest(USUARIO_ID, CAMPO_ID);

    private final NegocioService negocioService = mock(NegocioService.class);
    private final CampoRepository campoRepository = mock(CampoRepository.class);
    private final EliminarCampo eliminarCampo = new CampoUseCaseConfig().eliminarCampo(negocioService,
            new CampoService(campoRepository, mock(BloqueRepository.class), new TransaccionDirecta()),
            campoRepository, new TransaccionDirecta());

    private final Campo cantidad = Campo.builder().id(CAMPO_ID).negocioId(NEGOCIO_ID).bloqueId(BLOQUE_ID)
            .nombre("Cantidad").tipo(TipoCampo.NUMERO).build();

    @BeforeEach
    void givenNegocio() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Elimina el campo del negocio de la sesión filtrando por su negocio")
    void executeShouldDeleteCampoOfSessionNegocio() {
        givenCampo();
        givenCamposDelBloque(cantidad);

        eliminarCampo.execute(REQUEST);

        verify(campoRepository).deleteByIdAndNegocioId(CAMPO_ID, NEGOCIO_ID);
    }

    @Test
    @DisplayName("Rechaza borrar un campo que usa una fórmula")
    void executeShouldRejectFieldUsedByFormula() {
        givenCampo();
        givenCamposDelBloque(cantidad, Campo.builder().id(FORMULA_ID).negocioId(NEGOCIO_ID).bloqueId(BLOQUE_ID)
                .nombre("Doble").tipo(TipoCampo.FORMULA).formula("{" + CAMPO_ID + "} * 2").build());

        assertThatThrownBy(() -> eliminarCampo.execute(REQUEST))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("No se puede eliminar «Cantidad»: la fórmula «Doble» lo usa");
        verify(campoRepository, never()).deleteByIdAndNegocioId(any(), any());
    }

    @Test
    @DisplayName("Un campo ajeno propaga el 404 y no borra nada")
    void executeShouldNotDeleteWhenCampoIsForeign() {
        when(campoRepository.findByIdAndNegocioId(CAMPO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eliminarCampo.execute(REQUEST))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(campoRepository, never()).deleteByIdAndNegocioId(any(), any());
    }

    // --- arrange ---
    private void givenCampo() {
        when(campoRepository.findByIdAndNegocioId(CAMPO_ID, NEGOCIO_ID)).thenReturn(Optional.of(cantidad));
    }

    private void givenCamposDelBloque(Campo... campos) {
        when(campoRepository.findByBloqueIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(List.of(campos));
    }
}
