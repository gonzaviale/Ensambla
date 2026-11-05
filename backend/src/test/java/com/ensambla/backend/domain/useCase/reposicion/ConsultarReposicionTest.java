package com.ensambla.backend.domain.useCase.reposicion;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.reposicion.Faltantes;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.service.reposicion.ReposicionService;
import com.ensambla.backend.domain.useCase.reposicion.consultarReposicion.ConsultarReposicion;
import com.ensambla.backend.domain.useCase.reposicion.consultarReposicion.ConsultarReposicionRequest;
import com.ensambla.backend.domain.useCase.reposicion.consultarReposicion.ConsultarReposicionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.ReposicionDePrueba.NEGOCIO_ID;
import static com.ensambla.backend.support.ReposicionDePrueba.PROYECTO_ID;
import static com.ensambla.backend.support.ReposicionDePrueba.YERBA_ID;
import static com.ensambla.backend.support.ReposicionDePrueba.datosCon;
import static com.ensambla.backend.support.ReposicionDePrueba.inventarioCon;
import static com.ensambla.backend.support.ReposicionDePrueba.producto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultarReposicionTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ReposicionService reposicionService = mock(ReposicionService.class);

    private ConsultarReposicion consultarReposicion;

    @BeforeEach
    void setUp() {
        consultarReposicion = new ConsultarReposicion(negocioRepository, reposicionService);
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Devuelve los productos a reponer agrupados por proveedor")
    void executeShouldReturnTheMissingProductsGroupedBySupplier() {
        givenFaltantesDelSistema();

        ConsultarReposicionResponse result = whenConsulta();

        assertThat(result.grupos()).singleElement().satisfies(grupo -> {
            assertThat(grupo.proveedor()).isEqualTo("Molinos");
            assertThat(grupo.productos()).extracting(producto -> producto.id()).containsExactly(YERBA_ID);
        });
    }

    @Test
    @DisplayName("Un sistema de otro negocio responde como inexistente")
    void executeShouldNotFindAForeignProject() {
        when(reposicionService.faltantesDe(NEGOCIO_ID, PROYECTO_ID))
                .thenThrow(new ResourceNotFoundException("Proyecto no encontrado"));

        assertThatThrownBy(this::whenConsulta).isInstanceOf(ResourceNotFoundException.class);
    }

    // --- arrange ---
    private void givenFaltantesDelSistema() {
        Faltantes faltantes = Faltantes.de(datosCon(inventarioCon(producto(YERBA_ID, "Yerba", 0, 10, "Molinos"))));
        when(reposicionService.faltantesDe(NEGOCIO_ID, PROYECTO_ID)).thenReturn(faltantes);
    }

    // --- act ---
    private ConsultarReposicionResponse whenConsulta() {
        return consultarReposicion.execute(new ConsultarReposicionRequest(USUARIO_ID, PROYECTO_ID));
    }
}
