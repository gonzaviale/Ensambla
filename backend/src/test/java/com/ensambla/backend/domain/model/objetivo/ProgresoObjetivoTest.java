package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static org.assertj.core.api.Assertions.assertThat;

class ProgresoObjetivoTest {

    private static final LocalDate MARTES = LocalDate.of(2026, 11, 3);
    private static final LocalDate JUEVES = LocalDate.of(2026, 11, 5);
    private static final LocalDate LUNES_SIGUIENTE = LocalDate.of(2026, 11, 9);
    private static final LocalDate DOMINGO_ANTERIOR = LocalDate.of(2026, 11, 1);

    private ProgresoObjetivo foto;
    private List<ProgresoObjetivo> fotos;
    private List<ProgresoObjetivo> ultimasDeCadaSemana;

    @Test
    @DisplayName("Una foto nueva lleva un id propio y los datos recibidos")
    void unaFotoNuevaLlevaUnIdPropioYLosDatosRecibidos() {
        cuandoSeCreaUnaFotoDeUnMiniObjetivo();

        entoncesLlevaUnIdPropioYLosDatosRecibidos();
    }

    @Test
    @DisplayName("Es la foto del objetivo mayor cuando no tiene mini-objetivo")
    void esLaFotoDelObjetivoMayorCuandoNoTieneMiniObjetivo() {
        dadoUnaFotoSinMiniObjetivo();

        entoncesEsLaFotoDelObjetivo();
    }

    @Test
    @DisplayName("No es la foto del objetivo mayor cuando tiene mini-objetivo")
    void noEsLaFotoDelObjetivoMayorCuandoTieneMiniObjetivo() {
        dadoUnaFotoDeUnMiniObjetivo();

        entoncesNoEsLaFotoDelObjetivo();
    }

    @Test
    @DisplayName("Se queda con la última foto de cada semana, de lunes a domingo, de la más vieja a la más nueva")
    void seQuedaConLaUltimaFotoDeCadaSemanaDeLunesADomingoDeLaMasViejaALaMasNueva() {
        dadoFotosDeTresSemanas();

        cuandoSeTomaLaUltimaDeCadaSemana();

        entoncesQuedaUnaFotoPorSemanaDeLaMasViejaALaMasNueva();
    }

    @Test
    @DisplayName("Sin fotos no hay semanas")
    void sinFotosNoHaySemanas() {
        dadoQueNoHayFotos();

        cuandoSeTomaLaUltimaDeCadaSemana();

        entoncesNoHaySemanas();
    }

    // --- dado ---

    private void dadoUnaFotoSinMiniObjetivo() {
        foto = unaFoto(null, MARTES, "1");
    }

    private void dadoUnaFotoDeUnMiniObjetivo() {
        foto = unaFoto(MINI_ID, MARTES, "1");
    }

    private void dadoFotosDeTresSemanas() {
        fotos = List.of(
                unaFoto(null, LUNES_SIGUIENTE, "30"),
                unaFoto(null, JUEVES, "20"),
                unaFoto(null, MARTES, "10"),
                unaFoto(null, DOMINGO_ANTERIOR, "5"));
    }

    private void dadoQueNoHayFotos() {
        fotos = List.of();
    }

    // --- cuando ---

    private void cuandoSeCreaUnaFotoDeUnMiniObjetivo() {
        foto = ProgresoObjetivo.de(OBJETIVO_ID, MINI_ID, MARTES, BigDecimal.TEN, AHORA);
    }

    private void cuandoSeTomaLaUltimaDeCadaSemana() {
        ultimasDeCadaSemana = ProgresoObjetivo.ultimaDeCadaSemana(fotos);
    }

    // --- entonces ---

    private void entoncesLlevaUnIdPropioYLosDatosRecibidos() {
        assertThat(foto.id()).isNotNull();
        assertThat(foto.objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(foto.miniObjetivoId()).isEqualTo(MINI_ID);
        assertThat(foto.dia()).isEqualTo(MARTES);
        assertThat(foto.valor()).isEqualTo(BigDecimal.TEN);
        assertThat(foto.medidoEn()).isEqualTo(AHORA);
    }

    private void entoncesEsLaFotoDelObjetivo() {
        assertThat(foto.esDelObjetivo()).isTrue();
    }

    private void entoncesNoEsLaFotoDelObjetivo() {
        assertThat(foto.esDelObjetivo()).isFalse();
    }

    private void entoncesQuedaUnaFotoPorSemanaDeLaMasViejaALaMasNueva() {
        assertThat(ultimasDeCadaSemana).extracting(ProgresoObjetivo::valor)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("5"), new BigDecimal("20"), new BigDecimal("30"));
    }

    private void entoncesNoHaySemanas() {
        assertThat(ultimasDeCadaSemana).isEmpty();
    }

    // --- datos ---

    private static ProgresoObjetivo unaFoto(MiniObjetivoId miniObjetivoId, LocalDate dia, String valor) {
        return ProgresoObjetivo.de(OBJETIVO_ID, miniObjetivoId, dia, new BigDecimal(valor), AHORA);
    }
}
