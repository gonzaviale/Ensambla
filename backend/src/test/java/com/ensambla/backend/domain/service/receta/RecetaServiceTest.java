package com.ensambla.backend.domain.service.receta;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.receta.Receta;
import com.ensambla.backend.domain.model.receta.RecetaCosteada;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.RecetasDePrueba.BLOQUE_INSUMOS_ID;
import static com.ensambla.backend.support.RecetasDePrueba.HARINA_ID;
import static com.ensambla.backend.support.RecetasDePrueba.NEGOCIO_ID;
import static com.ensambla.backend.support.RecetasDePrueba.PRECIO_ID;
import static com.ensambla.backend.support.RecetasDePrueba.unInsumoConPrecio;
import static com.ensambla.backend.support.RecetasDePrueba.unaColumnaDelPrecioDeTipo;
import static com.ensambla.backend.support.RecetasDePrueba.unaRecetaDelProductoCon;
import static com.ensambla.backend.support.RecetasDePrueba.unaRecetaVacia;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecetaServiceTest {

    @Mock
    private CampoRepository campoRepository;

    @Mock
    private RegistroRepository registroRepository;

    @InjectMocks
    private RecetaService recetaService;

    private RecetaCosteada costeada;
    private Throwable rechazo;

    @Test
    @DisplayName("Costear calcula el costo con los insumos y los campos de su tabla")
    void costearCalculaElCostoConLosInsumosYLosCamposDeSuTabla() {
        dadoUnaColumnaNumericaDelPrecio();
        dadoUnInsumoDeHarinaConPrecioDiez();
        dadoQueLaTablaDeInsumosNoTieneOtrosCampos();

        cuandoSeCosteaUnaRecetaDeHarina();

        entoncesElCostoEsElDeLaHarinaYSeIndicaLaTablaDeInsumos();
    }

    @Test
    @DisplayName("Costear una receta vacía no consulta nada y cuesta cero")
    void costearUnaRecetaVaciaNoConsultaNadaYCuestaCero() {
        cuandoSeCosteaUnaRecetaVacia();

        entoncesLaRecetaNoTieneLineasYCuestaCero();
    }

    @Test
    @DisplayName("Costear responde que no existe si la columna del precio ya no está")
    void costearRespondeQueNoExisteSiLaColumnaDelPrecioYaNoEsta() {
        dadoQueLaColumnaDelPrecioYaNoExiste();

        cuandoSeIntentaCostearUnaRecetaDeHarina();

        entoncesSeRechazaPorqueLaColumnaDelPrecioNoExiste();
    }

    // --- dado ---
    private void dadoUnaColumnaNumericaDelPrecio() {
        when(campoRepository.findByIdAndNegocioId(PRECIO_ID, NEGOCIO_ID))
                .thenReturn(Optional.of(unaColumnaDelPrecioDeTipo(TipoCampo.NUMERO)));
    }

    private void dadoUnInsumoDeHarinaConPrecioDiez() {
        when(registroRepository.findAllByIdsAndBloque(List.of(HARINA_ID), BLOQUE_INSUMOS_ID, NEGOCIO_ID))
                .thenReturn(List.of(unInsumoConPrecio(HARINA_ID, "Harina", 10)));
    }

    private void dadoQueLaTablaDeInsumosNoTieneOtrosCampos() {
        when(campoRepository.findByBloqueIdAndNegocioId(BLOQUE_INSUMOS_ID, NEGOCIO_ID)).thenReturn(List.of());
    }

    private void dadoQueLaColumnaDelPrecioYaNoExiste() {
        when(campoRepository.findByIdAndNegocioId(PRECIO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    // --- cuando ---
    private void cuandoSeCosteaUnaRecetaDeHarina() {
        costeada = recetaService.costear(unaRecetaDelProductoCon(HARINA_ID), NEGOCIO_ID);
    }

    private void cuandoSeCosteaUnaRecetaVacia() {
        costeada = recetaService.costear(unaRecetaVacia(), NEGOCIO_ID);
    }

    private void cuandoSeIntentaCostearUnaRecetaDeHarina() {
        Receta receta = unaRecetaDelProductoCon(HARINA_ID);
        rechazo = catchThrowable(() -> recetaService.costear(receta, NEGOCIO_ID));
    }

    // --- entonces ---
    private void entoncesElCostoEsElDeLaHarinaYSeIndicaLaTablaDeInsumos() {
        assertThat(costeada.costo()).isEqualByComparingTo(new BigDecimal("20"));
        assertThat(costeada.bloqueInsumosId()).isEqualTo(BLOQUE_INSUMOS_ID);
    }

    private void entoncesLaRecetaNoTieneLineasYCuestaCero() {
        assertThat(costeada.lineas()).isEmpty();
        assertThat(costeada.costo()).isEqualByComparingTo("0");
    }

    private void entoncesSeRechazaPorqueLaColumnaDelPrecioNoExiste() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }
}
