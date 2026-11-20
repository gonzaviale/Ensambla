package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.HizoManual;
import com.ensambla.backend.domain.model.objetivo.ModoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.QuePaso;
import com.ensambla.backend.domain.model.objetivo.TipoObjetivo;
import com.ensambla.backend.infrastructure.repository.entity.MiniObjetivoEntity;
import com.ensambla.backend.infrastructure.repository.entity.ObjetivoEntity;
import com.ensambla.backend.infrastructure.repository.entity.RespuestaMiniObjetivoEntity;
import com.ensambla.backend.support.PostgresDatosTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class IRespuestaMiniObjetivoJpaRepositoryTest extends PostgresDatosTestSupport {

    private static final LocalDate SEMANA = LocalDate.of(2026, 11, 9);
    private static final int LARGO_MAXIMO_DE_COMENTARIO = 300;

    @Autowired
    private IRespuestaMiniObjetivoJpaRepository respuestas;

    @Autowired
    private IMiniObjetivoJpaRepository minis;

    @Autowired
    private IObjetivoJpaRepository objetivos;

    private UUID negocioA;
    private UUID negocioB;
    private UUID objetivoA;
    private UUID manualA;
    private UUID idRespuesta;
    private RespuestaMiniObjetivoEntity leida;
    private boolean respondido;
    private Throwable error;

    @BeforeEach
    void setUp() {
        negocioA = givenNegocio("a@example.com").getId();
        objetivoA = objetivos.saveAndFlush(ObjetivoEntity.builder()
                .negocioId(negocioA)
                .texto("Que me conozcan")
                .tipo(TipoObjetivo.SIN_DATOS)
                .armadoConSami(true)
                .creadoEn(AHORA)
                .build()).getId();
        manualA = minis.saveAndFlush(manual(objetivoA, negocioA, 1)).getId();
    }

    @Test
    @DisplayName("Guarda y relee la respuesta con todos sus datos")
    void guardaYReleeLaRespuestaConTodosSusDatos() {
        dadoUnaRespuestaGuardada(HizoManual.NO, QuePaso.OTRA, "Llovió");

        cuandoSeLeeLaRespuesta();

        entoncesSeRelenTodosLosDatos();
    }

    @Test
    @DisplayName("Un mini-objetivo sin respuesta no figura como respondido")
    void unMiniObjetivoSinRespuestaNoFiguraComoRespondido() {
        cuandoSePreguntaSiElManualFueRespondido();

        entoncesNoFueRespondido();
    }

    @Test
    @DisplayName("Un mini-objetivo con respuesta figura como respondido")
    void unMiniObjetivoConRespuestaFiguraComoRespondido() {
        dadoUnaRespuestaGuardada(HizoManual.SI, QuePaso.FUNCIONO, null);

        cuandoSePreguntaSiElManualFueRespondido();

        entoncesFueRespondido();
    }

    @Test
    @DisplayName("La base no admite una segunda respuesta al mismo mini-objetivo")
    void laBaseNoAdmiteUnaSegundaRespuestaAlMismoMiniObjetivo() {
        dadoUnaRespuestaGuardada(HizoManual.SI, QuePaso.FUNCIONO, null);

        cuandoSeGuardaConError(HizoManual.NO, QuePaso.OTRA, null);

        entoncesLaBaseLaRechaza();
    }

    @Test
    @DisplayName("La base rechaza «lo hice» con «no tuve tiempo»")
    void laBaseRechazaLoHiceConNoTuveTiempo() {
        cuandoSeGuardaConError(HizoManual.SI, QuePaso.NO_TUVE_TIEMPO, null);

        entoncesLaBaseLaRechaza();
    }

    @Test
    @DisplayName("La base rechaza un comentario de más de 300 caracteres")
    void laBaseRechazaUnComentarioDeMasDe300Caracteres() {
        cuandoSeGuardaConError(HizoManual.NO, QuePaso.OTRA, "a".repeat(LARGO_MAXIMO_DE_COMENTARIO + 1));

        entoncesLaBaseLaRechaza();
    }

    @Test
    @DisplayName("Al borrar el mini-objetivo se borra su respuesta")
    void alBorrarElMiniObjetivoSeBorraSuRespuesta() {
        dadoUnaRespuestaGuardada(HizoManual.SI, QuePaso.FUNCIONO, null);

        cuandoSeBorraElMini();

        entoncesLaRespuestaYaNoExiste();
    }

    @Test
    @DisplayName("Lista las respuestas del objetivo del negocio y nunca las de otro negocio")
    void listaLasRespuestasDelObjetivoDelNegocioYNuncaLasDeOtroNegocio() {
        dadoUnaRespuestaGuardada(HizoManual.SI, QuePaso.FUNCIONO, null);
        negocioB = givenNegocio("b@example.com").getId();

        entoncesSoloElNegocioDuenoVeLaRespuesta();
    }

    // --- arrange ---
    private void dadoUnaRespuestaGuardada(HizoManual hizo, QuePaso quePaso, String comentario) {
        idRespuesta = respuestas.saveAndFlush(
                respuesta(UUID.randomUUID(), manualA, hizo, quePaso, comentario)).getId();
    }

    // --- act ---
    private void cuandoSeLeeLaRespuesta() {
        leida = respuestas.findById(idRespuesta).orElseThrow();
    }

    private void cuandoSePreguntaSiElManualFueRespondido() {
        respondido = respuestas.existsByMiniObjetivoId(manualA);
    }

    private void cuandoSeGuardaConError(HizoManual hizo, QuePaso quePaso, String comentario) {
        RespuestaMiniObjetivoEntity respuesta = respuesta(UUID.randomUUID(), manualA, hizo, quePaso, comentario);
        error = catchThrowable(() -> respuestas.saveAndFlush(respuesta));
    }

    private void cuandoSeBorraElMini() {
        minis.deleteById(manualA);
        minis.flush();
        respuestas.flush();
    }

    // --- assert ---
    private void entoncesSeRelenTodosLosDatos() {
        assertThat(leida.getNegocioId()).isEqualTo(negocioA);
        assertThat(leida.getObjetivoId()).isEqualTo(objetivoA);
        assertThat(leida.getMiniObjetivoId()).isEqualTo(manualA);
        assertThat(leida.getHizo()).isEqualTo(HizoManual.NO);
        assertThat(leida.getQuePaso()).isEqualTo(QuePaso.OTRA);
        assertThat(leida.getComentario()).isEqualTo("Llovió");
        assertThat(leida.getSemanaInicio()).isEqualTo(SEMANA);
        assertThat(leida.getRespondidaEn()).isEqualTo(AHORA);
    }

    private void entoncesFueRespondido() {
        assertThat(respondido).isTrue();
    }

    private void entoncesNoFueRespondido() {
        assertThat(respondido).isFalse();
    }

    private void entoncesLaBaseLaRechaza() {
        assertThat(error).isInstanceOf(DataIntegrityViolationException.class);
    }

    private void entoncesLaRespuestaYaNoExiste() {
        assertThat(respuestas.existsById(idRespuesta)).isFalse();
    }

    private void entoncesSoloElNegocioDuenoVeLaRespuesta() {
        List<RespuestaMiniObjetivoEntity> delDueno = respuestas.findAllByObjetivoIdAndNegocioId(objetivoA, negocioA);
        assertThat(delDueno).extracting(RespuestaMiniObjetivoEntity::getId).containsExactly(idRespuesta);
        assertThat(respuestas.findAllByObjetivoIdAndNegocioId(objetivoA, negocioB)).isEmpty();
    }

    // --- helpers ---
    private RespuestaMiniObjetivoEntity respuesta(
            UUID id, UUID miniObjetivoId, HizoManual hizo, QuePaso quePaso, String comentario) {
        return RespuestaMiniObjetivoEntity.builder()
                .id(id)
                .negocioId(negocioA)
                .objetivoId(objetivoA)
                .miniObjetivoId(miniObjetivoId)
                .hizo(hizo)
                .quePaso(quePaso)
                .comentario(comentario)
                .semanaInicio(SEMANA)
                .respondidaEn(AHORA)
                .build();
    }

    private static MiniObjetivoEntity manual(UUID objetivoId, UUID negocioId, int posicion) {
        return MiniObjetivoEntity.builder()
                .id(UUID.randomUUID())
                .objetivoId(objetivoId)
                .negocioId(negocioId)
                .posicion((short) posicion)
                .modo(ModoMiniObjetivo.MANUAL)
                .estado(EstadoMiniObjetivo.EN_CURSO)
                .semanaInicio(SEMANA)
                .titulo("Pedile a 3 clientes")
                .justificacion("Porque sí")
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build();
    }
}
