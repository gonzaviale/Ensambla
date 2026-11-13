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

    @Test
    @DisplayName("Una foto nueva lleva un id propio y los datos recibidos")
    void deShouldCreateAPhotoWithItsOwnId() {
        ProgresoObjetivo result = ProgresoObjetivo.de(OBJETIVO_ID, MINI_ID, MARTES, BigDecimal.TEN, AHORA);

        assertThat(result.id()).isNotNull();
        assertThat(result.objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(result.miniObjetivoId()).isEqualTo(MINI_ID);
        assertThat(result.dia()).isEqualTo(MARTES);
        assertThat(result.valor()).isEqualTo(BigDecimal.TEN);
        assertThat(result.medidoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Es la foto del objetivo mayor cuando no tiene mini-objetivo")
    void esDelObjetivoShouldBeTrueOnlyWithoutMiniObjetivo() {
        assertThat(foto(null, MARTES, "1").esDelObjetivo()).isTrue();
        assertThat(foto(MINI_ID, MARTES, "1").esDelObjetivo()).isFalse();
    }

    @Test
    @DisplayName("Se queda con la última foto de cada semana, de lunes a domingo, de la más vieja a la más nueva")
    void ultimaDeCadaSemanaShouldKeepTheLastPhotoOfEachWeek() {
        List<ProgresoObjetivo> fotos = List.of(
                foto(null, LUNES_SIGUIENTE, "30"),
                foto(null, JUEVES, "20"),
                foto(null, MARTES, "10"),
                foto(null, DOMINGO_ANTERIOR, "5"));

        List<ProgresoObjetivo> result = ProgresoObjetivo.ultimaDeCadaSemana(fotos);

        assertThat(result).extracting(ProgresoObjetivo::valor)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("5"), new BigDecimal("20"), new BigDecimal("30"));
    }

    @Test
    @DisplayName("Sin fotos no hay semanas")
    void ultimaDeCadaSemanaShouldBeEmptyWithoutPhotos() {
        assertThat(ProgresoObjetivo.ultimaDeCadaSemana(List.of())).isEmpty();
    }

    // --- helpers ---
    private static ProgresoObjetivo foto(MiniObjetivoId miniObjetivoId, LocalDate dia, String valor) {
        return ProgresoObjetivo.de(OBJETIVO_ID, miniObjetivoId, dia, new BigDecimal(valor), AHORA);
    }
}
