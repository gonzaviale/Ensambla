package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.infrastructure.repository.entity.MiniObjetivoEntity;
import com.ensambla.backend.infrastructure.repository.entity.ObjetivoEntity;
import com.ensambla.backend.support.PostgresDatosTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class IMiniObjetivoJpaRepositoryTest extends PostgresDatosTestSupport {

    private static final BigDecimal META = new BigDecimal("400000.00");
    private static final LocalDate SEMANA = LocalDate.of(2026, 10, 5);

    @Autowired
    private IObjetivoJpaRepository objetivos;

    @Autowired
    private IMiniObjetivoJpaRepository minis;

    private UUID negocioA;
    private UUID negocioB;
    private UUID objetivoA;
    private UUID objetivoB;

    @BeforeEach
    void dadoDosNegociosConUnObjetivoCadaUno() {
        negocioA = givenNegocio("a@example.com").getId();
        negocioB = givenNegocio("b@example.com").getId();
        objetivoA = unObjetivo(negocioA);
        objetivoB = unObjetivo(negocioB);
    }

    private UUID producto;
    private UUID reemplazo;
    private UUID idGuardado;
    private UUID nuevo;
    private UUID segundo;
    private UUID primero;
    private UUID deA;
    private MiniObjetivoEntity releido;
    private List<MiniObjetivoEntity> listados;
    private Optional<MiniObjetivoEntity> otroNegocio;
    private Optional<MiniObjetivoEntity> otroObjetivo;
    private Throwable rechazo;

    @Test
    @DisplayName("Guarda y relee el mini-objetivo con todos sus datos")
    void guardaYReleeElMiniObjetivoConTodosSusDatos() {
        dadoUnMiniObjetivoReemplazadoConTodosSusDatos();

        cuandoSeReleeElMiniObjetivo();

        entoncesSeConservanTodosLosDatosDelMiniObjetivo();
    }

    @Test
    @DisplayName("Lista sólo los del objetivo del negocio, del más viejo al más nuevo y, a igualdad, por posición")
    void listaSoloLosDelObjetivoDelNegocioDelMasViejoAlMasNuevoYAIgualdadPorPosicion() {
        dadoTresMiniObjetivosDelObjetivoAYUnoDelB();

        cuandoSeListanLosMiniObjetivosDelObjetivoA();

        entoncesSeListanPrimeroSegundoYNuevoEnOrden();
    }

    @Test
    @DisplayName("No devuelve un mini-objetivo de otro negocio ni de otro objetivo")
    void noDevuelveUnMiniObjetivoDeOtroNegocioNiDeOtroObjetivo() {
        dadoUnMiniObjetivoEnCurso();

        cuandoSeBuscaConOtroNegocioYConOtroObjetivo();

        entoncesNoSeEncuentraEnNingunoDeLosDos();
    }

    @Test
    @DisplayName("Un lugar lo ocupa uno solo en curso: el segundo en la misma posición se rechaza")
    void unLugarLoOcupaUnoSoloEnCursoElSegundoEnLaMismaPosicionSeRechaza() {
        dadoUnMiniObjetivoEnCurso();

        cuandoSeIntentaGuardarOtroEnCursoEnLaMismaPosicion();

        entoncesLaBaseRechazaPorIntegridad();
    }

    @Test
    @DisplayName("Los que ya no están en curso no ocupan lugar: pueden compartir la posición con uno en curso")
    void losQueYaNoEstanEnCursoNoOcupanLugarPuedenCompartirLaPosicionConUnoEnCurso() {
        dadoUnoReemplazadoUnoLogradoYUnoEnCursoEnLaMismaPosicion();

        cuandoSeListanLosMiniObjetivosDelObjetivoA();

        entoncesSeListanTres();
    }

    @Test
    @DisplayName("Otro objetivo puede usar la misma posición")
    void otroObjetivoPuedeUsarLaMismaPosicion() {
        dadoUnMiniObjetivoEnCurso();

        cuandoSeGuardaUnoEnCursoEnLaMismaPosicionDelObjetivoB();

        entoncesHayDosMiniObjetivosEnTotal();
    }

    @Test
    @DisplayName("La base rechaza una posición fuera de 1 a 3")
    void laBaseRechazaUnaPosicionFueraDeUnoATres() {
        cuandoSeIntentaGuardarUnoEnLaPosicionCuatro();

        entoncesLaBaseRechazaPorIntegridad();
    }

    @Test
    @DisplayName("La base rechaza un texto en blanco")
    void laBaseRechazaUnTextoEnBlanco() {
        cuandoSeIntentaGuardarUnoConTituloEnBlanco();

        entoncesLaBaseRechazaPorIntegridad();
    }

    @Test
    @DisplayName("Al borrar el objetivo se borran sus mini-objetivos")
    void alBorrarElObjetivoSeBorranSusMiniObjetivos() {
        dadoUnMiniObjetivoEnCurso();

        cuandoSeBorraElObjetivoA();

        entoncesNoQuedanMiniObjetivosDelObjetivoA();
    }

    // --- dado ---
    private void dadoUnMiniObjetivoReemplazadoConTodosSusDatos() {
        producto = UUID.randomUUID();
        reemplazo = unMiniGuardado(objetivoA, negocioA, 2, EstadoMiniObjetivo.EN_CURSO, AHORA).getId();
        MiniObjetivoEntity entity = unMini(objetivoA, negocioA, 1, EstadoMiniObjetivo.REEMPLAZADO, AHORA);
        entity.setRegistroId(producto);
        entity.setReferencia("Yerba 1 kg");
        entity.setTextoConIa(true);
        entity.setReemplazadoPor(reemplazo);
        idGuardado = minis.saveAndFlush(entity).getId();
    }

    private void dadoTresMiniObjetivosDelObjetivoAYUnoDelB() {
        nuevo = unMiniGuardado(objetivoA, negocioA, 1, EstadoMiniObjetivo.EN_CURSO, AHORA.plusDays(1)).getId();
        segundo = unMiniGuardado(objetivoA, negocioA, 2, EstadoMiniObjetivo.EN_CURSO, AHORA).getId();
        primero = unMiniGuardado(objetivoA, negocioA, 1, EstadoMiniObjetivo.LOGRADO, AHORA).getId();
        unMiniGuardado(objetivoB, negocioB, 1, EstadoMiniObjetivo.EN_CURSO, AHORA);
    }

    private void dadoUnMiniObjetivoEnCurso() {
        deA = unMiniGuardado(objetivoA, negocioA, 1, EstadoMiniObjetivo.EN_CURSO, AHORA).getId();
    }

    private void dadoUnoReemplazadoUnoLogradoYUnoEnCursoEnLaMismaPosicion() {
        unMiniGuardado(objetivoA, negocioA, 1, EstadoMiniObjetivo.REEMPLAZADO, AHORA);
        unMiniGuardado(objetivoA, negocioA, 1, EstadoMiniObjetivo.LOGRADO, AHORA);
        unMiniGuardado(objetivoA, negocioA, 1, EstadoMiniObjetivo.EN_CURSO, AHORA);
    }

    private UUID unObjetivo(UUID negocioId) {
        return objetivos.saveAndFlush(ObjetivoEntity.builder()
                .negocioId(negocioId)
                .texto("Ganar más")
                .meta(META)
                .creadoEn(AHORA)
                .build()).getId();
    }

    private MiniObjetivoEntity unMiniGuardado(
            UUID objetivoId, UUID negocioId, int posicion, EstadoMiniObjetivo estado, OffsetDateTime creadoEn) {
        return minis.saveAndFlush(unMini(objetivoId, negocioId, posicion, estado, creadoEn));
    }

    private static MiniObjetivoEntity unMini(
            UUID objetivoId, UUID negocioId, int posicion, EstadoMiniObjetivo estado, OffsetDateTime creadoEn) {
        return MiniObjetivoEntity.builder()
                .id(UUID.randomUUID())
                .objetivoId(objetivoId)
                .negocioId(negocioId)
                .posicion((short) posicion)
                .tipo(TipoMiniObjetivo.SUBIR_PRECIO)
                .meta(new BigDecimal("14183.00"))
                .valorInicial(new BigDecimal("12600.00"))
                .valorActual(new BigDecimal("12600.00"))
                .aporteEstimado(new BigDecimal("6786.00"))
                .estado(estado)
                .semanaInicio(SEMANA)
                .titulo("Subile $250")
                .justificacion("Porque sí")
                .creadoEn(creadoEn)
                .actualizadoEn(creadoEn)
                .build();
    }

    // --- cuando ---
    private void cuandoSeReleeElMiniObjetivo() {
        releido = minis.findByIdAndObjetivoIdAndNegocioId(idGuardado, objetivoA, negocioA).orElseThrow();
    }

    private void cuandoSeListanLosMiniObjetivosDelObjetivoA() {
        listados = minis.findAllByObjetivoIdAndNegocioIdOrderByCreadoEnAscPosicionAsc(objetivoA, negocioA);
    }

    private void cuandoSeBuscaConOtroNegocioYConOtroObjetivo() {
        otroNegocio = minis.findByIdAndObjetivoIdAndNegocioId(deA, objetivoA, negocioB);
        otroObjetivo = minis.findByIdAndObjetivoIdAndNegocioId(deA, objetivoB, negocioA);
    }

    private void cuandoSeIntentaGuardarOtroEnCursoEnLaMismaPosicion() {
        rechazo = catchThrowable(
                () -> unMiniGuardado(objetivoA, negocioA, 1, EstadoMiniObjetivo.EN_CURSO, AHORA));
    }

    private void cuandoSeGuardaUnoEnCursoEnLaMismaPosicionDelObjetivoB() {
        unMiniGuardado(objetivoB, negocioB, 1, EstadoMiniObjetivo.EN_CURSO, AHORA);
    }

    private void cuandoSeIntentaGuardarUnoEnLaPosicionCuatro() {
        rechazo = catchThrowable(
                () -> unMiniGuardado(objetivoA, negocioA, 4, EstadoMiniObjetivo.EN_CURSO, AHORA));
    }

    private void cuandoSeIntentaGuardarUnoConTituloEnBlanco() {
        MiniObjetivoEntity sinTitulo = unMini(objetivoA, negocioA, 1, EstadoMiniObjetivo.EN_CURSO, AHORA);
        sinTitulo.setTitulo("  ");
        rechazo = catchThrowable(() -> minis.saveAndFlush(sinTitulo));
    }

    private void cuandoSeBorraElObjetivoA() {
        objetivos.deleteByIdAndNegocioId(objetivoA, negocioA);
    }

    // --- entonces ---
    private void entoncesSeConservanTodosLosDatosDelMiniObjetivo() {
        assertThat(releido.getObjetivoId()).isEqualTo(objetivoA);
        assertThat(releido.getNegocioId()).isEqualTo(negocioA);
        assertThat(releido.getPosicion()).isEqualTo((short) 1);
        assertThat(releido.getTipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(releido.getRegistroId()).isEqualTo(producto);
        assertThat(releido.getReferencia()).isEqualTo("Yerba 1 kg");
        assertThat(releido.getMeta()).isEqualByComparingTo("14183.00");
        assertThat(releido.getValorInicial()).isEqualByComparingTo("12600.00");
        assertThat(releido.getValorActual()).isEqualByComparingTo("12600.00");
        assertThat(releido.getAporteEstimado()).isEqualByComparingTo("6786.00");
        assertThat(releido.getEstado()).isEqualTo(EstadoMiniObjetivo.REEMPLAZADO);
        assertThat(releido.getSemanaInicio()).isEqualTo(SEMANA);
        assertThat(releido.getTitulo()).isEqualTo("Subile $250");
        assertThat(releido.getJustificacion()).isEqualTo("Porque sí");
        assertThat(releido.isTextoConIa()).isTrue();
        assertThat(releido.getReemplazadoPor()).isEqualTo(reemplazo);
        assertThat(releido.getCreadoEn()).isEqualTo(AHORA);
        assertThat(releido.getActualizadoEn()).isEqualTo(AHORA);
    }

    private void entoncesSeListanPrimeroSegundoYNuevoEnOrden() {
        assertThat(listados).extracting(MiniObjetivoEntity::getId).containsExactly(primero, segundo, nuevo);
    }

    private void entoncesNoSeEncuentraEnNingunoDeLosDos() {
        assertThat(otroNegocio).isEmpty();
        assertThat(otroObjetivo).isEmpty();
    }

    private void entoncesLaBaseRechazaPorIntegridad() {
        assertThat(rechazo).isInstanceOf(DataIntegrityViolationException.class);
    }

    private void entoncesSeListanTres() {
        assertThat(listados).hasSize(3);
    }

    private void entoncesHayDosMiniObjetivosEnTotal() {
        assertThat(minis.findAll()).hasSize(2);
    }

    private void entoncesNoQuedanMiniObjetivosDelObjetivoA() {
        assertThat(minis.findAllByObjetivoIdAndNegocioIdOrderByCreadoEnAscPosicionAsc(objetivoA, negocioA)).isEmpty();
    }
}
