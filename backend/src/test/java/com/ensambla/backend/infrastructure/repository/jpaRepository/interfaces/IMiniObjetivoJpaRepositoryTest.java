package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.HizoManual;
import com.ensambla.backend.domain.model.objetivo.ModoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.QuePaso;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.infrastructure.repository.entity.MiniObjetivoEntity;
import com.ensambla.backend.infrastructure.repository.entity.ObjetivoEntity;
import com.ensambla.backend.infrastructure.repository.entity.RespuestaMiniObjetivoEntity;
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
    private static final int POSICION_FUERA_DE_RANGO = 4;

    @Autowired
    private IObjetivoJpaRepository objetivos;

    @Autowired
    private IMiniObjetivoJpaRepository minis;

    @Autowired
    private IRespuestaMiniObjetivoJpaRepository respuestas;

    private UUID negocioA;
    private UUID negocioB;
    private UUID objetivoA;
    private UUID objetivoB;
    private UUID productoDelMini;
    private UUID reemplazo;
    private UUID idDelMini;
    private UUID idNuevo;
    private UUID idSegundo;
    private UUID idPrimero;
    private UUID idViejo;
    private MiniObjetivoEntity guardado;
    private MiniObjetivoEntity leido;
    private Optional<MiniObjetivoEntity> deOtroNegocio;
    private Optional<MiniObjetivoEntity> deOtroObjetivo;
    private List<MiniObjetivoEntity> listados;
    private List<MiniObjetivoEntity> guardados;
    private Throwable error;

    @BeforeEach
    void setUp() {
        negocioA = givenNegocio("a@example.com").getId();
        negocioB = givenNegocio("b@example.com").getId();
        objetivoA = givenObjetivo(negocioA);
        objetivoB = givenObjetivo(negocioB);
    }

    @Test
    @DisplayName("Guarda y relee el mini-objetivo con todos sus datos")
    void saveShouldRoundTripEveryField() {
        givenUnMiniReemplazadoConProductoYTextoDeIa();

        whenSeLeeElMini();

        thenSeRelenTodosLosDatos();
    }

    @Test
    @DisplayName("Lista sólo los del objetivo del negocio, del más viejo al más nuevo y, a igualdad, por posición")
    void findAllShouldFilterAndOrder() {
        givenTresMinisDeAYUnoDeB();

        whenSeListanLosMinisDeA();

        thenSeListanEnElOrdenEsperado();
    }

    @Test
    @DisplayName("No devuelve un mini-objetivo de otro negocio ni de otro objetivo")
    void findByIdShouldBeScopedToObjetivoAndNegocio() {
        givenUnMiniEnCurso(objetivoA, negocioA, 1);

        whenSeBuscaElMiniEnOtroNegocioYEnOtroObjetivo();

        thenNoSeEncuentraEnNingunLado();
    }

    @Test
    @DisplayName("Un lugar lo ocupa uno solo en curso: el segundo en la misma posición se rechaza")
    void shouldRejectTwoInProgressInTheSamePlace() {
        givenUnMiniEnCurso(objetivoA, negocioA, 1);

        whenSeGuardaConError(objetivoA, negocioA, 1, EstadoMiniObjetivo.EN_CURSO);

        thenLaBaseLoRechaza();
    }

    @Test
    @DisplayName("Los que ya no están en curso no ocupan lugar: pueden compartir la posición con uno en curso")
    void shouldAllowHistoryInTheSamePlaceAsOneInProgress() {
        givenUnoReemplazadoUnoLogradoYUnoEnCursoEnLaMismaPosicion();

        whenSeListanLosMinisDeA();

        thenHayMinis(3);
    }

    @Test
    @DisplayName("Otro objetivo puede usar la misma posición")
    void shouldAllowTheSamePlaceInAnotherObjetivo() {
        givenUnMiniEnCurso(objetivoA, negocioA, 1);

        whenSeGuardaUnMiniEnCursoEn(objetivoB, negocioB, 1);

        thenHayMinisEnLaBase(2);
    }

    @Test
    @DisplayName("La base rechaza una posición fuera de 1 a 3")
    void shouldRejectAPositionOutOfRange() {
        whenSeGuardaConError(objetivoA, negocioA, POSICION_FUERA_DE_RANGO, EstadoMiniObjetivo.EN_CURSO);

        thenLaBaseLoRechaza();
    }

    @Test
    @DisplayName("La base rechaza un texto en blanco")
    void shouldRejectABlankTitle() {
        whenSeGuardaConTituloEnBlanco();

        thenLaBaseLoRechaza();
    }

    @Test
    @DisplayName("Guarda y relee un mini-objetivo manual sin métrica ni números")
    void saveShouldRoundTripAManualMiniObjetivo() {
        whenSeGuardaYSeLeeUnManual();

        thenElManualNoTieneMetricaNiNumeros();
    }

    @Test
    @DisplayName("La base acepta las métricas nuevas sin producto")
    void saveShouldAcceptTheNewMetrics() {
        whenSeGuardanLasMetricasNuevas();

        thenSeGuardanLasMetricasNuevas();
    }

    @Test
    @DisplayName("Un mini-objetivo guardado sin modo es medido")
    void saveShouldDefaultToMedido() {
        whenSeGuardaUnMiniEnCursoEn(objetivoA, negocioA, 1);

        thenElModoEs(ModoMiniObjetivo.MEDIDO);
    }

    @Test
    @DisplayName("La base rechaza un mini-objetivo medido sin meta")
    void saveShouldRejectAMedidoWithoutGoal() {
        whenSeGuardaUnMedidoSinMeta();

        thenLaBaseLoRechaza();
    }

    @Test
    @DisplayName("La base rechaza un mini-objetivo manual con números")
    void saveShouldRejectAManualWithNumbers() {
        whenSeGuardaUnManualConMeta();

        thenLaBaseLoRechaza();
    }

    @Test
    @DisplayName("Los pendientes son los manuales en curso sin respuesta, del más viejo al más nuevo")
    void findPendientesDeRespuestaShouldListManualMinisInProgressWithoutAnswer() {
        givenDosManualesEnCursoYUnMedido();

        whenSeListanLosPendientesDe(negocioA);

        thenLosPendientesSonDelMasViejoAlMasNuevo();
    }

    @Test
    @DisplayName("Un manual ya respondido o que ya no está en curso no es pendiente")
    void findPendientesDeRespuestaShouldSkipAnsweredAndNotInProgress() {
        givenUnManualRespondidoYUnoAbandonado();

        whenSeListanLosPendientesDe(negocioA);

        thenNoHayPendientes();
    }

    @Test
    @DisplayName("Los pendientes nunca son de otro negocio")
    void findPendientesDeRespuestaShouldNeverListOtherBusinessMinis() {
        givenUnManualDeOtroNegocio();

        whenSeListanLosPendientesDe(negocioA);

        thenNoHayPendientes();
    }

    @Test
    @DisplayName("Al borrar el objetivo se borran sus mini-objetivos")
    void deletingObjetivoShouldDeleteItsMiniObjetivos() {
        givenUnMiniEnCurso(objetivoA, negocioA, 1);

        whenSeBorraElObjetivoA();

        thenElObjetivoNoTieneMinis();
    }

    // --- arrange ---
    private void givenUnMiniReemplazadoConProductoYTextoDeIa() {
        productoDelMini = UUID.randomUUID();
        reemplazo = givenMini(objetivoA, negocioA, 2, EstadoMiniObjetivo.EN_CURSO, AHORA).getId();
        MiniObjetivoEntity entity = mini(objetivoA, negocioA, 1, EstadoMiniObjetivo.REEMPLAZADO, AHORA);
        entity.setRegistroId(productoDelMini);
        entity.setReferencia("Yerba 1 kg");
        entity.setTextoConIa(true);
        entity.setReemplazadoPor(reemplazo);
        idDelMini = minis.saveAndFlush(entity).getId();
    }

    private void givenTresMinisDeAYUnoDeB() {
        idNuevo = givenMini(objetivoA, negocioA, 1, EstadoMiniObjetivo.EN_CURSO, AHORA.plusDays(1)).getId();
        idSegundo = givenMini(objetivoA, negocioA, 2, EstadoMiniObjetivo.EN_CURSO, AHORA).getId();
        idPrimero = givenMini(objetivoA, negocioA, 1, EstadoMiniObjetivo.LOGRADO, AHORA).getId();
        givenMini(objetivoB, negocioB, 1, EstadoMiniObjetivo.EN_CURSO, AHORA);
    }

    private void givenUnMiniEnCurso(UUID objetivoId, UUID negocioId, int posicion) {
        guardado = givenMini(objetivoId, negocioId, posicion, EstadoMiniObjetivo.EN_CURSO, AHORA);
        idDelMini = guardado.getId();
    }

    private void givenUnoReemplazadoUnoLogradoYUnoEnCursoEnLaMismaPosicion() {
        givenMini(objetivoA, negocioA, 1, EstadoMiniObjetivo.REEMPLAZADO, AHORA);
        givenMini(objetivoA, negocioA, 1, EstadoMiniObjetivo.LOGRADO, AHORA);
        givenMini(objetivoA, negocioA, 1, EstadoMiniObjetivo.EN_CURSO, AHORA);
    }

    private void givenDosManualesEnCursoYUnMedido() {
        idNuevo = minis.saveAndFlush(manual(objetivoA, negocioA, 2, AHORA.plusDays(1))).getId();
        idViejo = minis.saveAndFlush(manual(objetivoA, negocioA, 3, AHORA)).getId();
        givenMini(objetivoA, negocioA, 1, EstadoMiniObjetivo.EN_CURSO, AHORA);
    }

    private void givenUnManualRespondidoYUnoAbandonado() {
        MiniObjetivoEntity respondido = minis.saveAndFlush(manual(objetivoA, negocioA, 2, AHORA));
        respuestas.saveAndFlush(respuestaDe(respondido));
        MiniObjetivoEntity quitado = manual(objetivoA, negocioA, 3, AHORA);
        quitado.setEstado(EstadoMiniObjetivo.ABANDONADO);
        minis.saveAndFlush(quitado);
    }

    private void givenUnManualDeOtroNegocio() {
        minis.saveAndFlush(manual(objetivoB, negocioB, 1, AHORA));
    }

    // --- act ---
    private void whenSeLeeElMini() {
        leido = minis.findByIdAndObjetivoIdAndNegocioId(idDelMini, objetivoA, negocioA).orElseThrow();
    }

    private void whenSeListanLosMinisDeA() {
        listados = minis.findAllByObjetivoIdAndNegocioIdOrderByCreadoEnAscPosicionAsc(objetivoA, negocioA);
    }

    private void whenSeBuscaElMiniEnOtroNegocioYEnOtroObjetivo() {
        deOtroNegocio = minis.findByIdAndObjetivoIdAndNegocioId(idDelMini, objetivoA, negocioB);
        deOtroObjetivo = minis.findByIdAndObjetivoIdAndNegocioId(idDelMini, objetivoB, negocioA);
    }

    private void whenSeGuardaConError(UUID objetivoId, UUID negocioId, int posicion, EstadoMiniObjetivo estado) {
        error = catchThrowable(() -> givenMini(objetivoId, negocioId, posicion, estado, AHORA));
    }

    private void whenSeGuardaUnMiniEnCursoEn(UUID objetivoId, UUID negocioId, int posicion) {
        guardado = givenMini(objetivoId, negocioId, posicion, EstadoMiniObjetivo.EN_CURSO, AHORA);
    }

    private void whenSeGuardaConTituloEnBlanco() {
        MiniObjetivoEntity sinTitulo = mini(objetivoA, negocioA, 1, EstadoMiniObjetivo.EN_CURSO, AHORA);
        sinTitulo.setTitulo("  ");
        error = catchThrowable(() -> minis.saveAndFlush(sinTitulo));
    }

    private void whenSeGuardaYSeLeeUnManual() {
        idDelMini = minis.saveAndFlush(manual(objetivoA, negocioA, 2, AHORA)).getId();
        whenSeLeeElMini();
    }

    private void whenSeGuardanLasMetricasNuevas() {
        MiniObjetivoEntity capital = mini(objetivoA, negocioA, 1, EstadoMiniObjetivo.EN_CURSO, AHORA);
        capital.setTipo(TipoMiniObjetivo.CAPITAL_PARADO);
        MiniObjetivoEntity variedad = mini(objetivoA, negocioA, 2, EstadoMiniObjetivo.EN_CURSO, AHORA);
        variedad.setTipo(TipoMiniObjetivo.VARIEDAD_VENDIDA);
        MiniObjetivoEntity minimo = mini(objetivoA, negocioA, 3, EstadoMiniObjetivo.EN_CURSO, AHORA);
        minimo.setTipo(TipoMiniObjetivo.PRODUCTOS_BAJO_EL_MINIMO);
        guardados = minis.saveAllAndFlush(List.of(capital, variedad, minimo));
    }

    private void whenSeGuardaUnMedidoSinMeta() {
        MiniObjetivoEntity medidoSinMeta = mini(objetivoA, negocioA, 1, EstadoMiniObjetivo.EN_CURSO, AHORA);
        medidoSinMeta.setMeta(null);
        error = catchThrowable(() -> minis.saveAndFlush(medidoSinMeta));
    }

    private void whenSeGuardaUnManualConMeta() {
        MiniObjetivoEntity manualConMeta = manual(objetivoA, negocioA, 2, AHORA);
        manualConMeta.setMeta(new BigDecimal("10.00"));
        error = catchThrowable(() -> minis.saveAndFlush(manualConMeta));
    }

    private void whenSeListanLosPendientesDe(UUID negocioId) {
        listados = minis.findPendientesDeRespuesta(negocioId, ModoMiniObjetivo.MANUAL, EstadoMiniObjetivo.EN_CURSO);
    }

    private void whenSeBorraElObjetivoA() {
        objetivos.deleteByIdAndNegocioId(objetivoA, negocioA);
    }

    // --- assert ---
    private void thenSeRelenTodosLosDatos() {
        assertThat(leido.getObjetivoId()).isEqualTo(objetivoA);
        assertThat(leido.getNegocioId()).isEqualTo(negocioA);
        assertThat(leido.getPosicion()).isEqualTo((short) 1);
        assertThat(leido.getTipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(leido.getRegistroId()).isEqualTo(productoDelMini);
        assertThat(leido.getReferencia()).isEqualTo("Yerba 1 kg");
        assertThat(leido.getMeta()).isEqualByComparingTo("14183.00");
        assertThat(leido.getValorInicial()).isEqualByComparingTo("12600.00");
        assertThat(leido.getValorActual()).isEqualByComparingTo("12600.00");
        assertThat(leido.getAporteEstimado()).isEqualByComparingTo("6786.00");
        assertThat(leido.getEstado()).isEqualTo(EstadoMiniObjetivo.REEMPLAZADO);
        assertThat(leido.getSemanaInicio()).isEqualTo(SEMANA);
        assertThat(leido.getTitulo()).isEqualTo("Subile $250");
        assertThat(leido.getJustificacion()).isEqualTo("Porque sí");
        assertThat(leido.isTextoConIa()).isTrue();
        assertThat(leido.getReemplazadoPor()).isEqualTo(reemplazo);
        assertThat(leido.getCreadoEn()).isEqualTo(AHORA);
        assertThat(leido.getActualizadoEn()).isEqualTo(AHORA);
    }

    private void thenSeListanEnElOrdenEsperado() {
        assertThat(listados).extracting(MiniObjetivoEntity::getId)
                .containsExactly(idPrimero, idSegundo, idNuevo);
    }

    private void thenNoSeEncuentraEnNingunLado() {
        assertThat(deOtroNegocio).isEmpty();
        assertThat(deOtroObjetivo).isEmpty();
    }

    private void thenLaBaseLoRechaza() {
        assertThat(error).isInstanceOf(DataIntegrityViolationException.class);
    }

    private void thenHayMinis(int cantidad) {
        assertThat(listados).hasSize(cantidad);
    }

    private void thenHayMinisEnLaBase(int cantidad) {
        assertThat(minis.findAll()).hasSize(cantidad);
    }

    private void thenElManualNoTieneMetricaNiNumeros() {
        assertThat(leido.getModo()).isEqualTo(ModoMiniObjetivo.MANUAL);
        assertThat(leido.getTipo()).isNull();
        assertThat(leido.getMeta()).isNull();
        assertThat(leido.getValorInicial()).isNull();
        assertThat(leido.getValorActual()).isNull();
        assertThat(leido.getAporteEstimado()).isNull();
        assertThat(leido.getTitulo()).isEqualTo("Pedile a 3 clientes");
    }

    private void thenSeGuardanLasMetricasNuevas() {
        assertThat(guardados).extracting(MiniObjetivoEntity::getTipo).containsExactly(
                TipoMiniObjetivo.CAPITAL_PARADO, TipoMiniObjetivo.VARIEDAD_VENDIDA,
                TipoMiniObjetivo.PRODUCTOS_BAJO_EL_MINIMO);
    }

    private void thenElModoEs(ModoMiniObjetivo esperado) {
        assertThat(guardado.getModo()).isEqualTo(esperado);
    }

    private void thenLosPendientesSonDelMasViejoAlMasNuevo() {
        assertThat(listados).extracting(MiniObjetivoEntity::getId).containsExactly(idViejo, idNuevo);
    }

    private void thenNoHayPendientes() {
        assertThat(listados).isEmpty();
    }

    private void thenElObjetivoNoTieneMinis() {
        assertThat(minis.findAllByObjetivoIdAndNegocioIdOrderByCreadoEnAscPosicionAsc(objetivoA, negocioA))
                .isEmpty();
    }

    // --- helpers ---
    private UUID givenObjetivo(UUID negocioId) {
        return objetivos.saveAndFlush(ObjetivoEntity.builder()
                .negocioId(negocioId)
                .texto("Ganar más")
                .meta(META)
                .creadoEn(AHORA)
                .build()).getId();
    }

    private MiniObjetivoEntity givenMini(
            UUID objetivoId, UUID negocioId, int posicion, EstadoMiniObjetivo estado, OffsetDateTime creadoEn) {
        return minis.saveAndFlush(mini(objetivoId, negocioId, posicion, estado, creadoEn));
    }

    private RespuestaMiniObjetivoEntity respuestaDe(MiniObjetivoEntity mini) {
        return RespuestaMiniObjetivoEntity.builder()
                .id(UUID.randomUUID())
                .negocioId(negocioA)
                .objetivoId(objetivoA)
                .miniObjetivoId(mini.getId())
                .hizo(HizoManual.SI)
                .quePaso(QuePaso.FUNCIONO)
                .semanaInicio(SEMANA)
                .respondidaEn(AHORA)
                .build();
    }

    private static MiniObjetivoEntity manual(UUID objetivoId, UUID negocioId, int posicion, OffsetDateTime creadoEn) {
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
                .creadoEn(creadoEn)
                .actualizadoEn(creadoEn)
                .build();
    }

    private static MiniObjetivoEntity mini(
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
}
