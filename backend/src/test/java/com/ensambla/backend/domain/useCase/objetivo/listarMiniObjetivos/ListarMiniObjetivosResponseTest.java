package com.ensambla.backend.domain.useCase.objetivo.listarMiniObjetivos;

import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.FaltanDatosMiniObjetivos;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.useCase.objetivo.MiniObjetivoItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;

class ListarMiniObjetivosResponseTest {

    private MiniObjetivo primero;
    private MiniObjetivo segundo;
    private MiniObjetivo tercero;
    private MiniObjetivo reemplazado;
    private MiniObjetivo logrado;
    private ListarMiniObjetivosResponse respuesta;

    @Test
    @DisplayName("Separa los que están en curso, por posición, de la historia, del más nuevo al más viejo")
    void separaLosQueEstanEnCursoPorPosicionDeLaHistoriaDelMasNuevoAlMasViejo() {
        dadoDosEnCursoUnReemplazadoYUnLogradoQueYaTieneReemplazo();

        cuandoSeArmaLaRespuestaDeTodos();

        entoncesLosEnCursoVanPorPosicionYLaHistoriaDelMasNuevoAlMasViejo();
    }

    @Test
    @DisplayName("Un logrado sigue en su lugar hasta el cierre del domingo y cuenta como ocupado")
    void unLogradoSigueEnSuLugarHastaElCierreDelDomingoYCuentaComoOcupado() {
        dadoUnEnCursoUnLogradoYOtroEnCurso();

        cuandoSeArmaLaRespuestaDeTodos();

        entoncesElLogradoSigueEnSuLugarSinHistoriaNiFaltanteDeDatos();
    }

    @Test
    @DisplayName("Un logrado cuyo lugar ya ocupa otro en curso queda en la historia")
    void unLogradoCuyoLugarYaOcupaOtroEnCursoQuedaEnLaHistoria() {
        dadoUnLogradoCuyoLugarOcupaOtroEnCurso();

        cuandoSeArmaLaRespuestaDeTodos();

        entoncesElLogradoPasaALaHistoria();
    }

    @Test
    @DisplayName("Con los 3 en curso no falta ningún dato")
    void conLosTresEnCursoNoFaltaNingunDato() {
        dadoTresEnCurso();

        cuandoSeArmaLaRespuestaDeTodos();

        entoncesNoFaltanDatos();
    }

    @Test
    @DisplayName("Con menos de 3 en curso avisa que faltan datos para proponer más")
    void conMenosDeTresEnCursoAvisaQueFaltanDatosParaProponerMas() {
        dadoUnSoloEnCurso();

        cuandoSeArmaLaRespuestaDeTodos();

        entoncesAvisaQueSonPocosLosDatos();
    }

    @Test
    @DisplayName("Un objetivo alcanzado lo dice, tenga o no mini-objetivos")
    void unObjetivoAlcanzadoLoDiceTengaONoMiniObjetivos() {
        cuandoSeArmaLaRespuestaDeUnObjetivoAlcanzadoSinMiniObjetivos();

        entoncesAvisaQueElObjetivoFueAlcanzado();
    }

    // --- dado ---

    private void dadoDosEnCursoUnReemplazadoYUnLogradoQueYaTieneReemplazo() {
        segundo = mini(2, EstadoMiniObjetivo.EN_CURSO, AHORA);
        primero = mini(1, EstadoMiniObjetivo.EN_CURSO, AHORA);
        reemplazado = mini(3, EstadoMiniObjetivo.REEMPLAZADO, AHORA.minusDays(2));
        logrado = mini(3, EstadoMiniObjetivo.LOGRADO, AHORA.minusDays(1));
        logrado.setReemplazadoPor(MiniObjetivoId.nuevo());
    }

    private void dadoUnEnCursoUnLogradoYOtroEnCurso() {
        primero = mini(1, EstadoMiniObjetivo.EN_CURSO, AHORA);
        logrado = mini(2, EstadoMiniObjetivo.LOGRADO, AHORA.minusDays(1));
        tercero = mini(3, EstadoMiniObjetivo.EN_CURSO, AHORA);
    }

    private void dadoUnLogradoCuyoLugarOcupaOtroEnCurso() {
        logrado = mini(1, EstadoMiniObjetivo.LOGRADO, AHORA.minusDays(1));
        primero = mini(1, EstadoMiniObjetivo.EN_CURSO, AHORA);
    }

    private void dadoTresEnCurso() {
        primero = mini(1, EstadoMiniObjetivo.EN_CURSO, AHORA);
        segundo = mini(2, EstadoMiniObjetivo.EN_CURSO, AHORA);
        tercero = mini(3, EstadoMiniObjetivo.EN_CURSO, AHORA);
    }

    private void dadoUnSoloEnCurso() {
        primero = mini(1, EstadoMiniObjetivo.EN_CURSO, AHORA);
    }

    // --- cuando ---

    private void cuandoSeArmaLaRespuestaDeTodos() {
        List<MiniObjetivo> todos = Stream.of(tercero, segundo, reemplazado, primero, logrado)
                .filter(Objects::nonNull).toList();
        respuesta = ListarMiniObjetivosResponse.de(objetivo(), todos);
    }

    private void cuandoSeArmaLaRespuestaDeUnObjetivoAlcanzadoSinMiniObjetivos() {
        Objetivo alcanzado = objetivo();
        alcanzado.marcarAlcanzado(AHORA);
        respuesta = ListarMiniObjetivosResponse.de(alcanzado, List.of());
    }

    // --- entonces ---

    private void entoncesLosEnCursoVanPorPosicionYLaHistoriaDelMasNuevoAlMasViejo() {
        assertThat(respuesta.minis()).extracting(MiniObjetivoItem::id).containsExactly(primero.getId(), segundo.getId());
        assertThat(respuesta.historial()).extracting(MiniObjetivoItem::id)
                .containsExactly(logrado.getId(), reemplazado.getId());
    }

    private void entoncesElLogradoSigueEnSuLugarSinHistoriaNiFaltanteDeDatos() {
        assertThat(respuesta.minis()).extracting(MiniObjetivoItem::id)
                .containsExactly(primero.getId(), logrado.getId(), tercero.getId());
        assertThat(respuesta.historial()).isEmpty();
        assertThat(respuesta.faltanDatos()).isNull();
    }

    private void entoncesElLogradoPasaALaHistoria() {
        assertThat(respuesta.minis()).extracting(MiniObjetivoItem::id).containsExactly(primero.getId());
        assertThat(respuesta.historial()).extracting(MiniObjetivoItem::id).containsExactly(logrado.getId());
    }

    private void entoncesNoFaltanDatos() {
        assertThat(respuesta.faltanDatos()).isNull();
    }

    private void entoncesAvisaQueSonPocosLosDatos() {
        assertThat(respuesta.faltanDatos()).isEqualTo(FaltanDatosMiniObjetivos.POCOS_DATOS);
    }

    private void entoncesAvisaQueElObjetivoFueAlcanzado() {
        assertThat(respuesta.faltanDatos()).isEqualTo(FaltanDatosMiniObjetivos.OBJETIVO_ALCANZADO);
        assertThat(respuesta.minis()).isEmpty();
        assertThat(respuesta.historial()).isEmpty();
    }

    // --- datos ---

    private static MiniObjetivo mini(int posicion, EstadoMiniObjetivo estado, OffsetDateTime creadoEn) {
        MiniObjetivo mini = miniObjetivo();
        mini.setId(MiniObjetivoId.nuevo());
        mini.setPosicion(posicion);
        mini.setEstado(estado);
        mini.setCreadoEn(creadoEn);
        return mini;
    }
}
