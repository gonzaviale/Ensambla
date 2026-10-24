package com.ensambla.backend.domain.useCase.bloque.eliminarBloque;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.infrastructure.config.useCase.BloqueUseCaseConfig;
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

class EliminarBloqueTest {

    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final BloqueId OTRO_BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    private static final EliminarBloqueRequest REQUEST = new EliminarBloqueRequest(USUARIO_ID, BLOQUE_ID);

    private final NegocioService negocioService = mock(NegocioService.class);
    private final BloqueRepository bloqueRepository = mock(BloqueRepository.class);
    private final CampoRepository campoRepository = mock(CampoRepository.class);
    private final BloqueService bloqueService = new BloqueService(bloqueRepository, new TransaccionDirecta());
    private final EliminarBloque eliminarBloque = new BloqueUseCaseConfig().eliminarBloque(
            negocioService, bloqueService, bloqueRepository, campoRepository, new TransaccionDirecta());

    @BeforeEach
    void givenNegocio() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Elimina el bloque del negocio de la sesión filtrando por su negocio")
    void executeShouldDeleteBloqueOfSessionNegocio() {
        givenBloque(BLOQUE_ID, "Clientes");

        eliminarBloque.execute(REQUEST);

        verify(campoRepository).findRelacionesHacia(BLOQUE_ID, NEGOCIO_ID);
        verify(bloqueRepository).deleteByIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID);
    }

    @Test
    @DisplayName("Rechaza borrar una tabla que otra tabla usa en una conexión")
    void executeShouldRejectWhenOtherBloqueReferencesIt() {
        givenBloque(BLOQUE_ID, "Clientes");
        givenBloque(OTRO_BLOQUE_ID, "Pedidos");
        Campo conexion = Campo.builder().bloqueId(OTRO_BLOQUE_ID).negocioId(NEGOCIO_ID).nombre("Cliente").build();
        when(campoRepository.findRelacionesHacia(BLOQUE_ID, NEGOCIO_ID)).thenReturn(List.of(conexion));

        assertThatThrownBy(() -> eliminarBloque.execute(REQUEST))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La tabla «Clientes» se usa en la columna «Cliente» de «Pedidos»");
        verify(bloqueRepository, never()).deleteByIdAndNegocioId(any(), any());
    }

    @Test
    @DisplayName("Un bloque ajeno propaga el 404 y no borra nada")
    void executeShouldNotDeleteForeignBloque() {
        when(bloqueRepository.findByIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eliminarBloque.execute(REQUEST))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(bloqueRepository, never()).deleteByIdAndNegocioId(any(), any());
    }

    // --- arrange ---
    private void givenBloque(BloqueId bloqueId, String nombre) {
        when(bloqueRepository.findByIdAndNegocioId(bloqueId, NEGOCIO_ID)).thenReturn(Optional.of(
                Bloque.builder().id(bloqueId).negocioId(NEGOCIO_ID).nombre(nombre).build()));
    }
}
