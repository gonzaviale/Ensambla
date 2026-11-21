package com.ensambla.backend.domain.useCase.objetivo.listarCandidatos;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.objetivo.CandidatoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.CandidatosDelNegocio;
import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.service.objetivo.PropuestaMiniObjetivosService;
import com.ensambla.backend.domain.useCase.objetivo.listarCandidatos.ListarCandidatosResponse.CandidatoItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidato;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidatoDelNegocio;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ListarCandidatosTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 10);

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ObjetivoRepository objetivoRepository = mock(ObjetivoRepository.class);
    private final MiniObjetivoRepository miniObjetivoRepository = mock(MiniObjetivoRepository.class);
    private final PropuestaMiniObjetivosService propuestaMiniObjetivosService =
            mock(PropuestaMiniObjetivosService.class);
    private ListarCandidatos listarCandidatos;

    private ListarCandidatosResponse respuesta;
    private Throwable rechazo;

    @BeforeEach
    void setUp() {
        listarCandidatos = new ListarCandidatos(
                negocioRepository,
                objetivoRepository,
                miniObjetivoRepository,
                propuestaMiniObjetivosService,
                Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));
        dadoUnMiniObjetivoDeUnObjetivoDelNegocio();
    }

    @Test
    @DisplayName("Ofrece los candidatos de hoy menos el mismo mini-objetivo")
    void ofreceLosCandidatosDeHoyMenosElMismoMiniObjetivo() {
        dadoElMiniObjetivoEnCurso();
        dadoCandidatosDeSubirPrecioYMejorarMargen();

        cuandoSeListan();

        entoncesSeOfreceSoloEl(TipoMiniObjetivo.MEJORAR_MARGEN);
    }

    @Test
    @DisplayName("No ofrece el tipo de los otros mini-objetivos en curso")
    void noOfreceElTipoDeLosOtrosMiniObjetivosEnCurso() {
        dadoOtroMiniObjetivoEnCursoDeMejorarMargen();
        dadoCandidatosDeMejorarMargenYRecortarGasto();

        cuandoSeListan();

        entoncesSeOfreceSoloEl(TipoMiniObjetivo.RECORTAR_GASTO);
    }

    @Test
    @DisplayName("Un mini-objetivo logrado antes no ocupa lugar: sus tipos siguen disponibles")
    void unMiniObjetivoLogradoAntesNoOcupaLugarSusTiposSiguenDisponibles() {
        dadoUnMiniObjetivoLogradoDeMejorarMargen();
        dadoCandidatosDeMejorarMargen();

        cuandoSeListan();

        entoncesSeOfreceSoloEl(TipoMiniObjetivo.MEJORAR_MARGEN);
    }

    @Test
    @DisplayName("Sin candidatos devuelve una lista vacía")
    void sinCandidatosDevuelveUnaListaVacia() {
        dadoElMiniObjetivoEnCurso();
        dadoQueNoHayCandidatos();

        cuandoSeListan();

        entoncesNoSeOfreceNada();
    }

    @Test
    @DisplayName("Un mini-objetivo que ya no está en curso no tiene alternativas y no se calcula nada")
    void unMiniObjetivoQueYaNoEstaEnCursoNoTieneAlternativasYNoSeCalculaNada() {
        dadoUnMiniObjetivoQueYaNoEstaEnCurso();

        cuandoSeIntentaListar();

        entoncesSeRechazaPorReglaDeNegocioSinCalcularCandidatos();
    }

    @Test
    @DisplayName("Un objetivo de otro negocio responde como inexistente")
    void unObjetivoDeOtroNegocioRespondeComoInexistente() {
        dadoUnObjetivoDeOtroNegocio();

        cuandoSeIntentaListar();

        entoncesSeRechazaComoInexistenteSinConsultarNada();
    }

    @Test
    @DisplayName("Un mini-objetivo de otro objetivo responde como inexistente")
    void unMiniObjetivoDeOtroObjetivoRespondeComoInexistente() {
        dadoUnMiniObjetivoDeOtroObjetivo();

        cuandoSeIntentaListar();

        entoncesSeRechazaComoInexistenteSinCalcularCandidatos();
    }

    // --- dado ---

    private void dadoUnMiniObjetivoDeUnObjetivoDelNegocio() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID)).thenReturn(objetivo());
        when(miniObjetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID, MINI_ID)).thenReturn(miniObjetivo());
    }

    private void dadoElMiniObjetivoEnCurso() {
        dadoLosMiniObjetivosEnCurso(miniObjetivo());
    }

    private void dadoOtroMiniObjetivoEnCursoDeMejorarMargen() {
        MiniObjetivo otro = miniObjetivo();
        otro.setId(MiniObjetivoId.nuevo());
        otro.setTipo(TipoMiniObjetivo.MEJORAR_MARGEN);
        otro.setRegistroId(null);
        dadoLosMiniObjetivosEnCurso(miniObjetivo(), otro);
    }

    private void dadoUnMiniObjetivoLogradoDeMejorarMargen() {
        MiniObjetivo logrado = miniObjetivo();
        logrado.setId(MiniObjetivoId.nuevo());
        logrado.setTipo(TipoMiniObjetivo.MEJORAR_MARGEN);
        logrado.setEstado(EstadoMiniObjetivo.LOGRADO);
        dadoLosMiniObjetivosEnCurso(miniObjetivo(), logrado);
    }

    private void dadoUnMiniObjetivoQueYaNoEstaEnCurso() {
        MiniObjetivo logrado = miniObjetivo();
        logrado.setEstado(EstadoMiniObjetivo.LOGRADO);
        when(miniObjetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID, MINI_ID)).thenReturn(logrado);
    }

    private void dadoUnObjetivoDeOtroNegocio() {
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID))
                .thenThrow(new ResourceNotFoundException("Objetivo no encontrado"));
    }

    private void dadoUnMiniObjetivoDeOtroObjetivo() {
        when(miniObjetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID, MINI_ID))
                .thenThrow(new ResourceNotFoundException("Mini-objetivo no encontrado"));
    }

    private void dadoCandidatosDeSubirPrecioYMejorarMargen() {
        dadoLosCandidatos(candidato(), candidatoDelNegocio(TipoMiniObjetivo.MEJORAR_MARGEN, "6180"));
    }

    private void dadoCandidatosDeMejorarMargenYRecortarGasto() {
        dadoLosCandidatos(candidatoDelNegocio(TipoMiniObjetivo.MEJORAR_MARGEN, "6180"),
                candidatoDelNegocio(TipoMiniObjetivo.RECORTAR_GASTO, "4000"));
    }

    private void dadoCandidatosDeMejorarMargen() {
        dadoLosCandidatos(candidatoDelNegocio(TipoMiniObjetivo.MEJORAR_MARGEN, "6180"));
    }

    private void dadoQueNoHayCandidatos() {
        dadoLosCandidatos();
    }

    private void dadoLosMiniObjetivosEnCurso(MiniObjetivo... enCurso) {
        when(miniObjetivoRepository.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID))
                .thenReturn(List.of(enCurso));
    }

    private void dadoLosCandidatos(CandidatoMiniObjetivo... candidatos) {
        when(propuestaMiniObjetivosService.candidatos(NEGOCIO_ID, HOY))
                .thenReturn(new CandidatosDelNegocio(BigDecimal.ZERO, List.of(candidatos)));
    }

    // --- cuando ---

    private void cuandoSeListan() {
        respuesta = listarCandidatos.execute(new ListarCandidatosRequest(USUARIO_ID, OBJETIVO_ID, MINI_ID));
    }

    private void cuandoSeIntentaListar() {
        rechazo = catchThrowable(this::cuandoSeListan);
    }

    // --- entonces ---

    private void entoncesSeOfreceSoloEl(TipoMiniObjetivo tipo) {
        assertThat(respuesta.content()).extracting(CandidatoItem::tipo).containsExactly(tipo);
    }

    private void entoncesNoSeOfreceNada() {
        assertThat(respuesta.content()).isEmpty();
    }

    private void entoncesSeRechazaPorReglaDeNegocioSinCalcularCandidatos() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(propuestaMiniObjetivosService);
    }

    private void entoncesSeRechazaComoInexistenteSinConsultarNada() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(miniObjetivoRepository, propuestaMiniObjetivosService);
    }

    private void entoncesSeRechazaComoInexistenteSinCalcularCandidatos() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(propuestaMiniObjetivosService);
    }
}
