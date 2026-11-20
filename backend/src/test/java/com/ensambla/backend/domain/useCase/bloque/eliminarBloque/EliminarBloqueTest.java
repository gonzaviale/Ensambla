package com.ensambla.backend.domain.useCase.bloque.eliminarBloque;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.infrastructure.config.useCase.BloqueUseCaseConfig;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.BloquesDePrueba.BLOQUE_ID;
import static com.ensambla.backend.support.BloquesDePrueba.OTRO_BLOQUE_ID;
import static com.ensambla.backend.support.BloquesDePrueba.unBloqueDelNegocio;
import static com.ensambla.backend.support.BloquesDePrueba.unaConexionDesde;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.PROYECTO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EliminarBloqueTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final BloqueRepository bloqueRepository = mock(BloqueRepository.class, CALLS_REAL_METHODS);
    private final CampoRepository campoRepository = mock(CampoRepository.class);
    private final EliminarBloque eliminarBloque = new BloqueUseCaseConfig().eliminarBloque(
            negocioRepository, bloqueRepository, campoRepository, new TransaccionDirecta());

    private Throwable rechazo;

    @BeforeEach
    void dadoUnNegocioDelUsuario() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Elimina el bloque del negocio de la sesión filtrando por su negocio")
    void eliminaElBloqueDelNegocioDeLaSesionFiltrandoPorSuNegocio() {
        dadoUnBloqueLlamado(BLOQUE_ID, "Clientes");

        cuandoSeEliminaElBloque();

        entoncesSeBuscaronLasConexionesHaciaElBloque();
        entoncesSeBorraElBloqueDelNegocio();
    }

    @Test
    @DisplayName("Rechaza borrar una tabla que otra tabla usa en una conexión")
    void rechazaBorrarUnaTablaQueOtraTablaUsaEnUnaConexion() {
        dadoUnBloqueLlamado(BLOQUE_ID, "Clientes");
        dadoUnBloqueLlamado(OTRO_BLOQUE_ID, "Pedidos");
        dadoUnaConexionHaciaElBloqueDesdeLaColumnaClienteDePedidos();

        cuandoSeIntentaEliminarElBloque();

        entoncesSeRechazaPorque("La tabla «Clientes» se usa en la columna «Cliente» de «Pedidos»");
        entoncesNoSeBorraNingunBloque();
    }

    @Test
    @DisplayName("Rechaza borrar la única sección de Stock del negocio")
    void rechazaBorrarLaUnicaSeccionDeStockDelNegocio() {
        dadoUnaSeccionDeStockUnicaEnElSistema();

        cuandoSeIntentaEliminarElBloque();

        entoncesSeProtegeLaEstructuraPorque(
                "No se puede quitar la sección «Stock»: Ensambla la necesita para funcionar.");
        entoncesNoSeBorraNingunBloque();
    }

    @Test
    @DisplayName("Borra una sección de Stock si el sistema tiene otra")
    void borraUnaSeccionDeStockSiElSistemaTieneOtra() {
        dadoDosSeccionesDeStockEnElSistema();

        cuandoSeEliminaElBloque();

        entoncesSeBorraElBloqueDelNegocio();
    }

    @Test
    @DisplayName("Un bloque ajeno propaga el 404 y no borra nada")
    void unBloqueAjenoPropagaElNoEncontradoYNoBorraNada() {
        dadoUnBloqueAjeno();

        cuandoSeIntentaEliminarElBloque();

        entoncesSeRechazaPorqueElBloqueNoExiste();
        entoncesNoSeBorraNingunBloque();
    }

    // --- dado ---
    private void dadoUnBloqueLlamado(BloqueId bloqueId, String nombre) {
        when(bloqueRepository.findByIdAndNegocioId(bloqueId, NEGOCIO_ID))
                .thenReturn(Optional.of(unBloqueDelNegocio(bloqueId, nombre)));
    }

    private void dadoUnaConexionHaciaElBloqueDesdeLaColumnaClienteDePedidos() {
        when(campoRepository.findRelacionesHacia(BLOQUE_ID, NEGOCIO_ID))
                .thenReturn(List.of(unaConexionDesde(OTRO_BLOQUE_ID, "Cliente")));
    }

    private void dadoUnaSeccionDeStockUnicaEnElSistema() {
        Bloque stock = unaSeccionDeStock(BLOQUE_ID);
        when(bloqueRepository.findByIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(Optional.of(stock));
        when(bloqueRepository.findByProyectoIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID)).thenReturn(List.of(stock));
    }

    private void dadoDosSeccionesDeStockEnElSistema() {
        Bloque stock = unaSeccionDeStock(BLOQUE_ID);
        when(bloqueRepository.findByIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(Optional.of(stock));
        when(bloqueRepository.findByProyectoIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID))
                .thenReturn(List.of(stock, unaSeccionDeStock(OTRO_BLOQUE_ID)));
    }

    private Bloque unaSeccionDeStock(BloqueId bloqueId) {
        return Bloque.builder().id(bloqueId).negocioId(NEGOCIO_ID).proyectoId(PROYECTO_ID).nombre("Stock")
                .tipo(TipoBloque.INVENTARIO).build();
    }

    private void dadoUnBloqueAjeno() {
        when(bloqueRepository.findByIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    // --- cuando ---
    private void cuandoSeEliminaElBloque() {
        eliminarBloque.execute(new EliminarBloqueRequest(USUARIO_ID, BLOQUE_ID));
    }

    private void cuandoSeIntentaEliminarElBloque() {
        rechazo = catchThrowable(this::cuandoSeEliminaElBloque);
    }

    // --- entonces ---
    private void entoncesSeBuscaronLasConexionesHaciaElBloque() {
        verify(campoRepository).findRelacionesHacia(BLOQUE_ID, NEGOCIO_ID);
    }

    private void entoncesSeBorraElBloqueDelNegocio() {
        verify(bloqueRepository).deleteByIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID);
    }

    private void entoncesSeRechazaPorque(String motivo) {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class).hasMessage(motivo);
    }

    private void entoncesSeProtegeLaEstructuraPorque(String motivo) {
        assertThat(rechazo).isInstanceOf(DuplicateResourceException.class).hasMessage(motivo);
    }

    private void entoncesSeRechazaPorqueElBloqueNoExiste() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }

    private void entoncesNoSeBorraNingunBloque() {
        verify(bloqueRepository, never()).deleteByIdAndNegocioId(any(), any());
    }
}
