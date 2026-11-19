package com.ensambla.backend.domain.useCase.campo.eliminarCampo;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.infrastructure.config.useCase.CampoUseCaseConfig;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.CamposDePrueba.unCampoNumerico;
import static com.ensambla.backend.support.CamposDePrueba.unaFormulaQueUsa;
import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EliminarCampoTest {

    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final CampoRepository campoRepository = mock(CampoRepository.class, CALLS_REAL_METHODS);
    private final EliminarCampo eliminarCampo = new CampoUseCaseConfig().eliminarCampo(negocioRepository,
            campoRepository, new TransaccionDirecta());

    private Campo cantidad;
    private Throwable rechazo;

    @Test
    @DisplayName("Un campo se elimina del negocio de la sesión")
    void unCampoSeEliminaDelNegocioDeLaSesion() {
        dadoUnCampoQueNingunaFormulaUsa();

        cuandoSeEliminaElCampo();

        entoncesSeEliminaElCampo();
    }

    @Test
    @DisplayName("Un campo usado por una fórmula no se puede eliminar")
    void unCampoUsadoPorUnaFormulaNoSePuedeEliminar() {
        dadoUnCampoUsadoPorUnaFormula();

        cuandoSeIntentaEliminarElCampo();

        entoncesSeRechazaPorque("No se puede eliminar «Cantidad»: la fórmula «Doble» lo usa");
        entoncesNoSeEliminaNingunCampo();
    }

    @Test
    @DisplayName("Un campo ajeno se informa como no encontrado y no borra nada")
    void unCampoAjenoSePropagaComoNoEncontradoYNoBorraNada() {
        dadoUnCampoAjeno();

        cuandoSeIntentaEliminarElCampo();

        entoncesSeRechazaPorqueElCampoNoExiste();
        entoncesNoSeEliminaNingunCampo();
    }

    private void dadoUnCampoQueNingunaFormulaUsa() {
        dadoUnCampoPropioConLosCampos(unaCantidadDelBloque());
    }

    private void dadoUnCampoUsadoPorUnaFormula() {
        Campo cantidadDelBloque = unaCantidadDelBloque();
        dadoUnCampoPropioConLosCampos(cantidadDelBloque, unaFormulaQueUsa(cantidadDelBloque, "Doble"));
    }

    private void dadoUnCampoAjeno() {
        cantidad = unaCantidadDelBloque();
        dadoUnNegocio();
        when(campoRepository.findByIdAndNegocioId(cantidad.getId(), NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    private void dadoUnCampoPropioConLosCampos(Campo propio, Campo... otros) {
        cantidad = propio;
        dadoUnNegocio();
        when(campoRepository.findByIdAndNegocioId(cantidad.getId(), NEGOCIO_ID)).thenReturn(Optional.of(cantidad));
        when(campoRepository.findByBloqueIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(camposDelBloque(otros));
    }

    private void dadoUnNegocio() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    private List<Campo> camposDelBloque(Campo... otros) {
        List<Campo> campos = new ArrayList<>(List.of(cantidad));
        campos.addAll(List.of(otros));
        return campos;
    }

    private Campo unaCantidadDelBloque() {
        return unCampoNumerico("Cantidad").ubicadoEn(NEGOCIO_ID, BLOQUE_ID, (short) 0, AHORA);
    }

    private void cuandoSeEliminaElCampo() {
        eliminarCampo.execute(new EliminarCampoRequest(USUARIO_ID, cantidad.getId()));
    }

    private void cuandoSeIntentaEliminarElCampo() {
        rechazo = catchThrowable(this::cuandoSeEliminaElCampo);
    }

    private void entoncesSeEliminaElCampo() {
        verify(campoRepository).deleteByIdAndNegocioId(cantidad.getId(), NEGOCIO_ID);
    }

    private void entoncesNoSeEliminaNingunCampo() {
        verify(campoRepository, never()).deleteByIdAndNegocioId(any(), any());
    }

    private void entoncesSeRechazaPorque(String motivo) {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class).hasMessage(motivo);
    }

    private void entoncesSeRechazaPorqueElCampoNoExiste() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }
}
