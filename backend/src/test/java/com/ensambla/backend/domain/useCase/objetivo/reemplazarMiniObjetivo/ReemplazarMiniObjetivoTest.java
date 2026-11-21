package com.ensambla.backend.domain.useCase.objetivo.reemplazarMiniObjetivo;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.objetivo.CandidatoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.CandidatosDelNegocio;
import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.service.objetivo.PropuestaMiniObjetivosService;
import com.ensambla.backend.domain.useCase.objetivo.MiniObjetivoItem;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.SEMANA;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidatoDelNegocio;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ReemplazarMiniObjetivoTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 10);
    private static final RegistroId OTRO_PRODUCTO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000002");

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ObjetivoRepository objetivoRepository = mock(ObjetivoRepository.class);
    private final MiniObjetivoRepository miniObjetivoRepository = mock(MiniObjetivoRepository.class);
    private final PropuestaMiniObjetivosService propuestaMiniObjetivosService =
            mock(PropuestaMiniObjetivosService.class);
    private ReemplazarMiniObjetivo reemplazarMiniObjetivo;

    private MiniObjetivoItem elegido;
    private Throwable rechazo;

    @BeforeEach
    void setUp() {
        reemplazarMiniObjetivo = new ReemplazarMiniObjetivo(
                negocioRepository,
                objetivoRepository,
                miniObjetivoRepository,
                propuestaMiniObjetivosService,
                new TransaccionDirecta(),
                Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));
        dadoUnSubirPrecioEnCursoEnLaPosicionDos();
    }

    @Test
    @DisplayName("El elegido ocupa el mismo lugar y arranca en curso con sus números de hoy")
    void elElegidoOcupaElMismoLugarYArrancaEnCursoConSusNumerosDeHoy() {
        dadoUnCandidatoDeMejorarMargen();

        cuandoSeReemplazaPorMejorarMargen();

        entoncesElElegidoOcupaElMismoLugarYArrancaEnCurso();
    }

    @Test
    @DisplayName("El viejo queda reemplazado y apuntando al nuevo; se guarda primero el viejo y después el nuevo")
    void elViejoQuedaReemplazadoYApuntandoAlNuevoSeGuardaPrimeroElViejoYDespuesElNuevo() {
        dadoUnCandidatoDeMejorarMargen();

        cuandoSeReemplazaPorMejorarMargen();

        entoncesSeGuardaPrimeroElViejoReemplazadoYDespuesElNuevo();
    }

    @Test
    @DisplayName("Se puede elegir uno del mismo tipo que el actual con otro producto")
    void sePuedeElegirUnoDelMismoTipoQueElActualConOtroProducto() {
        dadoUnCandidatoDeSubirPrecioDelAceite();

        cuandoSeReemplazaPorSubirPrecioDelAceite();

        entoncesElElegidoEsDelAceite();
    }

    @Test
    @DisplayName("Si el elegido ya no es una alternativa con los datos de hoy responde que no está disponible")
    void siElElegidoYaNoEsUnaAlternativaConLosDatosDeHoyRespondeQueNoEstaDisponible() {
        dadoUnCandidatoDeMejorarMargen();

        cuandoSeIntentaReemplazarPorRecortarGasto();

        entoncesSeRechazaPorqueYaNoEstaDisponible();
    }

    @Test
    @DisplayName("No se puede elegir el mismo mini-objetivo que ya está en curso")
    void noSePuedeElegirElMismoMiniObjetivoQueYaEstaEnCurso() {
        dadoCandidatosDeMejorarMargenYDelMismoSubirPrecioDeLaYerba();

        cuandoSeIntentaReemplazarPorElMismoSubirPrecio();

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("No se puede elegir un tipo que ya tiene otro mini-objetivo en curso")
    void noSePuedeElegirUnTipoQueYaTieneOtroMiniObjetivoEnCurso() {
        dadoOtroMiniObjetivoEnCursoDeMejorarMargen();
        dadoUnCandidatoDeMejorarMargen();

        cuandoSeIntentaReemplazarPorMejorarMargen();

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("Un mini-objetivo que ya no está en curso no se reemplaza y no se calcula nada")
    void unMiniObjetivoQueYaNoEstaEnCursoNoSeReemplazaYNoSeCalculaNada() {
        dadoUnMiniObjetivoLogrado();

        cuandoSeIntentaReemplazarPorMejorarMargen();

        entoncesSeRechazaPorqueYaNoEstaEnCursoSinCalcularNiGuardar();
    }

    @Test
    @DisplayName("Un objetivo de otro negocio responde como inexistente")
    void unObjetivoDeOtroNegocioRespondeComoInexistente() {
        dadoUnObjetivoDeOtroNegocio();

        cuandoSeIntentaReemplazarPorMejorarMargen();

        entoncesSeRechazaComoInexistenteSinCalcularCandidatos();
    }

    @Test
    @DisplayName("Un mini-objetivo de otro objetivo responde como inexistente")
    void unMiniObjetivoDeOtroObjetivoRespondeComoInexistente() {
        dadoUnMiniObjetivoDeOtroObjetivo();

        cuandoSeIntentaReemplazarPorMejorarMargen();

        entoncesSeRechazaComoInexistenteSinCalcularCandidatos();
    }

    // --- dado ---

    private void dadoUnSubirPrecioEnCursoEnLaPosicionDos() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID)).thenReturn(objetivo());
        when(miniObjetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID, MINI_ID)).thenReturn(actual());
        when(miniObjetivoRepository.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID))
                .thenReturn(List.of(actual()));
        when(miniObjetivoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void dadoUnCandidatoDeMejorarMargen() {
        dadoLosCandidatos(candidatoDelNegocio(TipoMiniObjetivo.MEJORAR_MARGEN, "6180"));
    }

    private void dadoUnCandidatoDeSubirPrecioDelAceite() {
        dadoLosCandidatos(new CandidatoMiniObjetivo(TipoMiniObjetivo.SUBIR_PRECIO, OTRO_PRODUCTO_ID,
                "Aceite 900 ml", BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN, "Subile", "Razón", false));
    }

    private void dadoCandidatosDeMejorarMargenYDelMismoSubirPrecioDeLaYerba() {
        dadoLosCandidatos(candidatoDelNegocio(TipoMiniObjetivo.MEJORAR_MARGEN, "6180"),
                new CandidatoMiniObjetivo(TipoMiniObjetivo.SUBIR_PRECIO, PRODUCTO_ID, PRODUCTO, BigDecimal.ONE,
                        BigDecimal.TEN, BigDecimal.TEN, "Subile", "Razón", false));
    }

    private void dadoOtroMiniObjetivoEnCursoDeMejorarMargen() {
        MiniObjetivo otro = actual();
        otro.setId(OTRO_MINI_ID);
        otro.setPosicion(3);
        otro.setTipo(TipoMiniObjetivo.MEJORAR_MARGEN);
        otro.setRegistroId(null);
        when(miniObjetivoRepository.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID))
                .thenReturn(List.of(actual(), otro));
    }

    private void dadoUnMiniObjetivoLogrado() {
        MiniObjetivo logrado = actual();
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

    private void dadoLosCandidatos(CandidatoMiniObjetivo... candidatos) {
        when(propuestaMiniObjetivosService.candidatos(NEGOCIO_ID, HOY))
                .thenReturn(new CandidatosDelNegocio(BigDecimal.ZERO, List.of(candidatos)));
    }

    // --- cuando ---

    private void cuandoSeReemplazaPorMejorarMargen() {
        cuandoSeReemplazaPor(TipoMiniObjetivo.MEJORAR_MARGEN, null, null);
    }

    private void cuandoSeReemplazaPorSubirPrecioDelAceite() {
        cuandoSeReemplazaPor(TipoMiniObjetivo.SUBIR_PRECIO, OTRO_PRODUCTO_ID, "Aceite 900 ml");
    }

    private void cuandoSeReemplazaPor(TipoMiniObjetivo tipo, RegistroId registroId, String referencia) {
        elegido = reemplazarMiniObjetivo.execute(new ReemplazarMiniObjetivoRequest(
                USUARIO_ID, OBJETIVO_ID, MINI_ID, tipo, registroId, referencia));
    }

    private void cuandoSeIntentaReemplazarPorMejorarMargen() {
        rechazo = catchThrowable(this::cuandoSeReemplazaPorMejorarMargen);
    }

    private void cuandoSeIntentaReemplazarPorRecortarGasto() {
        rechazo = catchThrowable(() -> cuandoSeReemplazaPor(TipoMiniObjetivo.RECORTAR_GASTO, null, "Servicios"));
    }

    private void cuandoSeIntentaReemplazarPorElMismoSubirPrecio() {
        rechazo = catchThrowable(() -> cuandoSeReemplazaPor(TipoMiniObjetivo.SUBIR_PRECIO, PRODUCTO_ID, PRODUCTO));
    }

    // --- entonces ---

    private void entoncesElElegidoOcupaElMismoLugarYArrancaEnCurso() {
        assertThat(elegido.id()).isNotEqualTo(MINI_ID);
        assertThat(elegido.posicion()).isEqualTo(2);
        assertThat(elegido.tipo()).isEqualTo(TipoMiniObjetivo.MEJORAR_MARGEN);
        assertThat(elegido.estado()).isEqualTo(EstadoMiniObjetivo.EN_CURSO);
        assertThat(elegido.semanaInicio()).isEqualTo(SEMANA);
        assertThat(elegido.valorActual()).isEqualByComparingTo("35.71");
    }

    private void entoncesSeGuardaPrimeroElViejoReemplazadoYDespuesElNuevo() {
        ArgumentCaptor<MiniObjetivo> captor = ArgumentCaptor.forClass(MiniObjetivo.class);
        verify(miniObjetivoRepository, times(2)).save(captor.capture());
        MiniObjetivo viejo = captor.getAllValues().get(0);
        MiniObjetivo nuevo = captor.getAllValues().get(1);
        assertThat(viejo.getId()).isEqualTo(MINI_ID);
        assertThat(viejo.getEstado()).isEqualTo(EstadoMiniObjetivo.REEMPLAZADO);
        assertThat(viejo.getReemplazadoPor()).isEqualTo(nuevo.getId());
        assertThat(viejo.getActualizadoEn()).isEqualTo(AHORA);
        assertThat(nuevo.getId()).isEqualTo(elegido.id());
        assertThat(nuevo.getObjetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(nuevo.getNegocioId()).isEqualTo(NEGOCIO_ID);
    }

    private void entoncesElElegidoEsDelAceite() {
        assertThat(elegido.registroId()).isEqualTo(OTRO_PRODUCTO_ID);
    }

    private void entoncesSeRechazaPorqueYaNoEstaDisponible() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class)
                .hasMessage("Ese mini-objetivo ya no está disponible: elegí otro de la lista");
        verify(miniObjetivoRepository, times(0)).save(any());
    }

    private void entoncesSeRechazaPorReglaDeNegocio() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class);
    }

    private void entoncesSeRechazaPorqueYaNoEstaEnCursoSinCalcularNiGuardar() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class)
                .hasMessage("El mini-objetivo ya no está en curso");
        verifyNoInteractions(propuestaMiniObjetivosService);
        verify(miniObjetivoRepository, times(0)).save(any());
    }

    private void entoncesSeRechazaComoInexistenteSinCalcularCandidatos() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
        verifyNoMoreInteractions(propuestaMiniObjetivosService);
    }

    // --- datos ---

    /** Un «subir precio» de la yerba en la posición 2. */
    private static MiniObjetivo actual() {
        MiniObjetivo mini = miniObjetivo();
        mini.setPosicion(2);
        return mini;
    }
}
