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
        negocioA = dadoNegocio("a@example.com").getId();
        negocioB = dadoNegocio("b@example.com").getId();
        objetivoA = dadoObjetivo(negocioA);
        objetivoB = dadoObjetivo(negocioB);
    }

    @Test
    @DisplayName("Guarda y relee el mini-objetivo con todos sus datos")
    void guardaYReleeElMiniObjetivoConTodosSusDatos() {
        dadoUnMiniReemplazadoConProductoYTextoDeIa();

        cuandoSeLeeElMini();

        entoncesSeRelenTodosLosDatos();
    }

    @Test
    @DisplayName("Lista sólo los del objetivo del negocio, del más viejo al más nuevo y, a igualdad, por posición")
    void listaSoloLosDelObjetivoDelNegocioDelMasViejoAlMasNuevoYAIgualdadPorPosicion() {
        dadoTresMinisDeAYUnoDeB();

        cuandoSeListanLosMinisDeA();

        entoncesSeListanEnElOrdenEsperado();
    }

    @Test
    @DisplayName("No devuelve un mini-objetivo de otro negocio ni de otro objetivo")
    void noDevuelveUnMiniObjetivoDeOtroNegocioNiDeOtroObjetivo() {
        dadoUnMiniEnCurso(objetivoA, negocioA, 1);

        cuandoSeBuscaElMiniEnOtroNegocioYEnOtroObjetivo();

        entoncesNoSeEncuentraEnNingunLado();
    }

    @Test
    @DisplayName("Un lugar lo ocupa uno solo en curso: el segundo en la misma posición se rechaza")
    void shouldRejectTwoInProgressInTheSamePlace() {
        dadoUnMiniEnCurso(objetivoA, negocioA, 1);

        cuandoSeGuardaConError(objetivoA, negocioA, 1, EstadoMiniObjetivo.EN_CURSO);

        entoncesLaBaseLoRechaza();
    }

    @Test
    @DisplayName("Los que ya no están en curso no ocupan lugar: pueden compartir la posición con uno en curso")
    void shouldAllowHistoryInTheSamePlaceAsOneInProgress() {
        dadoUnoReemplazadoUnoLogradoYUnoEnCursoEnLaMismaPosicion();

        cuandoSeListanLosMinisDeA();

        entoncesHayMinis(3);
    }

    @Test
    @DisplayName("Otro objetivo puede usar la misma posición")
    void shouldAllowTheSamePlaceInAnotherObjetivo() {
        dadoUnMiniEnCurso(objetivoA, negocioA, 1);

        cuandoSeGuardaUnMiniEnCursoEn(objetivoB, negocioB, 1);

        entoncesHayMinisEnLaBase(2);
    }

    @Test
    @DisplayName("La base rechaza una posición fuera de 1 a 3")
    void shouldRejectAPositionOutOfRange() {
        cuandoSeGuardaConError(objetivoA, negocioA, POSICION_FUERA_DE_RANGO, EstadoMiniObjetivo.EN_CURSO);

        entoncesLaBaseLoRechaza();
    }

    @Test
    @DisplayName("La base rechaza un texto en blanco")
    void shouldRejectABlankTitle() {
        cuandoSeGuardaConTituloEnBlanco();

        entoncesLaBaseLoRechaza();
    }

    @Test
    @DisplayName("Guarda y relee un mini-objetivo manual sin métrica ni números")
    void guardaYReleeUnMiniObjetivoManualSinMetricaNiNumeros() {
        cuandoSeGuardaYSeLeeUnManual();

        entoncesElManualNoTieneMetricaNiNumeros();
    }

    @Test
    @DisplayName("La base acepta las métricas nuevas sin producto")
    void laBaseAceptaLasMetricasNuevasSinProducto() {
        cuandoSeGuardanLasMetricasNuevas();

        entoncesSeGuardanLasMetricasNuevas();
    }

    @Test
    @DisplayName("Un mini-objetivo guardado sin modo es medido")
    void unMiniObjetivoGuardadoSinModoEsMedido() {
        cuandoSeGuardaUnMiniEnCursoEn(objetivoA, negocioA, 1);

        entoncesElModoEs(ModoMiniObjetivo.MEDIDO);
    }

    @Test
    @DisplayName("La base rechaza un mini-objetivo medido sin meta")
    void laBaseRechazaUnMiniObjetivoMedidoSinMeta() {
        cuandoSeGuardaUnMedidoSinMeta();

        entoncesLaBaseLoRechaza();
    }

    @Test
    @DisplayName("La base rechaza un mini-objetivo manual con números")
    void laBaseRechazaUnMiniObjetivoManualConNumeros() {
        cuandoSeGuardaUnManualConMeta();

        entoncesLaBaseLoRechaza();
    }

    @Test
    @DisplayName("Los pendientes son los manuales en curso sin respuesta, del más viejo al más nuevo")
    void losPendientesSonLosManualesEnCursoSinRespuestaDelMasViejoAlMasNuevo() {
        dadoDosManualesEnCursoYUnMedido();

        cuandoSeListanLosPendientesDe(negocioA);

        entoncesLosPendientesSonDelMasViejoAlMasNuevo();
    }

    @Test
    @DisplayName("Un manual ya respondido o que ya no está en curso no es pendiente")
    void unManualYaRespondidoOQueYaNoEstaEnCursoNoEsPendiente() {
        dadoUnManualRespondidoYUnoAbandonado();

        cuandoSeListanLosPendientesDe(negocioA);

        entoncesNoHayPendientes();
    }

    @Test
    @DisplayName("Los pendientes nunca son de otro negocio")
    void losPendientesNuncaSonDeOtroNegocio() {
        dadoUnManualDeOtroNegocio();

        cuandoSeListanLosPendientesDe(negocioA);

        entoncesNoHayPendientes();
    }

    @Test
    @DisplayName("Al borrar el objetivo se borran sus mini-objetivos")
    void alBorrarElObjetivoSeBorranSusMiniObjetivos() {
        dadoUnMiniEnCurso(objetivoA, negocioA, 1);

        cuandoSeBorraElObjetivoA();

        entoncesElObjetivoNoTieneMinis();
    }

    // --- arrange ---
    private void dadoUnMiniReemplazadoConProductoYTextoDeIa() {
        productoDelMini = UUID.randomUUID();
        reemplazo = dadoMini(objetivoA, negocioA, 2, EstadoMiniObjetivo.EN_CURSO, AHORA).getId();
        MiniObjetivoEntity entity = mini(objetivoA, negocioA, 1, EstadoMiniObjetivo.REEMPLAZADO, AHORA);
        entity.setRegistroId(productoDelMini);
        entity.setReferencia("Yerba 1 kg");
        entity.setTextoConIa(true);
        entity.setReemplazadoPor(reemplazo);
        idDelMini = minis.saveAndFlush(entity).getId();
    }

    private void dadoTresMinisDeAYUnoDeB() {
        idNuevo = dadoMini(objetivoA, negocioA, 1, EstadoMiniObjetivo.EN_CURSO, AHORA.plusDays(1)).getId();
        idSegundo = dadoMini(objetivoA, negocioA, 2, EstadoMiniObjetivo.EN_CURSO, AHORA).getId();
        idPrimero = dadoMini(objetivoA, negocioA, 1, EstadoMiniObjetivo.LOGRADO, AHORA).getId();
        dadoMini(objetivoB, negocioB, 1, EstadoMiniObjetivo.EN_CURSO, AHORA);
    }

    private void dadoUnMiniEnCurso(UUID objetivoId, UUID negocioId, int posicion) {
        guardado = dadoMini(objetivoId, negocioId, posicion, EstadoMiniObjetivo.EN_CURSO, AHORA);
        idDelMini = guardado.getId();
    }

    private void dadoUnoReemplazadoUnoLogradoYUnoEnCursoEnLaMismaPosicion() {
        dadoMini(objetivoA, negocioA, 1, EstadoMiniObjetivo.REEMPLAZADO, AHORA);
        dadoMini(objetivoA, negocioA, 1, EstadoMiniObjetivo.LOGRADO, AHORA);
        dadoMini(objetivoA, negocioA, 1, EstadoMiniObjetivo.EN_CURSO, AHORA);
    }

    private void dadoDosManualesEnCursoYUnMedido() {
        idNuevo = minis.saveAndFlush(manual(objetivoA, negocioA, 2, AHORA.plusDays(1))).getId();
        idViejo = minis.saveAndFlush(manual(objetivoA, negocioA, 3, AHORA)).getId();
        dadoMini(objetivoA, negocioA, 1, EstadoMiniObjetivo.EN_CURSO, AHORA);
    }

    private void dadoUnManualRespondidoYUnoAbandonado() {
        MiniObjetivoEntity respondido = minis.saveAndFlush(manual(objetivoA, negocioA, 2, AHORA));
        respuestas.saveAndFlush(respuestaDe(respondido));
        MiniObjetivoEntity quitado = manual(objetivoA, negocioA, 3, AHORA);
        quitado.setEstado(EstadoMiniObjetivo.ABANDONADO);
        minis.saveAndFlush(quitado);
    }

    private void dadoUnManualDeOtroNegocio() {
        minis.saveAndFlush(manual(objetivoB, negocioB, 1, AHORA));
    }

    // --- act ---
    private void cuandoSeLeeElMini() {
        leido = minis.findByIdAndObjetivoIdAndNegocioId(idDelMini, objetivoA, negocioA).orElseThrow();
    }

    private void cuandoSeListanLosMinisDeA() {
        listados = minis.findAllByObjetivoIdAndNegocioIdOrderByCreadoEnAscPosicionAsc(objetivoA, negocioA);
    }

    private void cuandoSeBuscaElMiniEnOtroNegocioYEnOtroObjetivo() {
        deOtroNegocio = minis.findByIdAndObjetivoIdAndNegocioId(idDelMini, objetivoA, negocioB);
        deOtroObjetivo = minis.findByIdAndObjetivoIdAndNegocioId(idDelMini, objetivoB, negocioA);
    }

    private void cuandoSeGuardaConError(UUID objetivoId, UUID negocioId, int posicion, EstadoMiniObjetivo estado) {
        error = catchThrowable(() -> dadoMini(objetivoId, negocioId, posicion, estado, AHORA));
    }

    private void cuandoSeGuardaUnMiniEnCursoEn(UUID objetivoId, UUID negocioId, int posicion) {
        guardado = dadoMini(objetivoId, negocioId, posicion, EstadoMiniObjetivo.EN_CURSO, AHORA);
    }

    private void cuandoSeGuardaConTituloEnBlanco() {
        MiniObjetivoEntity sinTitulo = mini(objetivoA, negocioA, 1, EstadoMiniObjetivo.EN_CURSO, AHORA);
        sinTitulo.setTitulo("  ");
        error = catchThrowable(() -> minis.saveAndFlush(sinTitulo));
    }

    private void cuandoSeGuardaYSeLeeUnManual() {
        idDelMini = minis.saveAndFlush(manual(objetivoA, negocioA, 2, AHORA)).getId();
        cuandoSeLeeElMini();
    }

    private void cuandoSeGuardanLasMetricasNuevas() {
        MiniObjetivoEntity capital = mini(objetivoA, negocioA, 1, EstadoMiniObjetivo.EN_CURSO, AHORA);
        capital.setTipo(TipoMiniObjetivo.CAPITAL_PARADO);
        MiniObjetivoEntity variedad = mini(objetivoA, negocioA, 2, EstadoMiniObjetivo.EN_CURSO, AHORA);
        variedad.setTipo(TipoMiniObjetivo.VARIEDAD_VENDIDA);
        MiniObjetivoEntity minimo = mini(objetivoA, negocioA, 3, EstadoMiniObjetivo.EN_CURSO, AHORA);
        minimo.setTipo(TipoMiniObjetivo.PRODUCTOS_BAJO_EL_MINIMO);
        guardados = minis.saveAllAndFlush(List.of(capital, variedad, minimo));
    }

    private void cuandoSeGuardaUnMedidoSinMeta() {
        MiniObjetivoEntity medidoSinMeta = mini(objetivoA, negocioA, 1, EstadoMiniObjetivo.EN_CURSO, AHORA);
        medidoSinMeta.setMeta(null);
        error = catchThrowable(() -> minis.saveAndFlush(medidoSinMeta));
    }

    private void cuandoSeGuardaUnManualConMeta() {
        MiniObjetivoEntity manualConMeta = manual(objetivoA, negocioA, 2, AHORA);
        manualConMeta.setMeta(new BigDecimal("10.00"));
        error = catchThrowable(() -> minis.saveAndFlush(manualConMeta));
    }

    private void cuandoSeListanLosPendientesDe(UUID negocioId) {
        listados = minis.findPendientesDeRespuesta(negocioId, ModoMiniObjetivo.MANUAL, EstadoMiniObjetivo.EN_CURSO);
    }

    private void cuandoSeBorraElObjetivoA() {
        objetivos.deleteByIdAndNegocioId(objetivoA, negocioA);
    }

    // --- assert ---
    private void entoncesSeRelenTodosLosDatos() {
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

    private void entoncesSeListanEnElOrdenEsperado() {
        assertThat(listados).extracting(MiniObjetivoEntity::getId)
                .containsExactly(idPrimero, idSegundo, idNuevo);
    }

    private void entoncesNoSeEncuentraEnNingunLado() {
        assertThat(deOtroNegocio).isEmpty();
        assertThat(deOtroObjetivo).isEmpty();
    }

    private void entoncesLaBaseLoRechaza() {
        assertThat(error).isInstanceOf(DataIntegrityViolationException.class);
    }

    private void entoncesHayMinis(int cantidad) {
        assertThat(listados).hasSize(cantidad);
    }

    private void entoncesHayMinisEnLaBase(int cantidad) {
        assertThat(minis.findAll()).hasSize(cantidad);
    }

    private void entoncesElManualNoTieneMetricaNiNumeros() {
        assertThat(leido.getModo()).isEqualTo(ModoMiniObjetivo.MANUAL);
        assertThat(leido.getTipo()).isNull();
        assertThat(leido.getMeta()).isNull();
        assertThat(leido.getValorInicial()).isNull();
        assertThat(leido.getValorActual()).isNull();
        assertThat(leido.getAporteEstimado()).isNull();
        assertThat(leido.getTitulo()).isEqualTo("Pedile a 3 clientes");
    }

    private void entoncesSeGuardanLasMetricasNuevas() {
        assertThat(guardados).extracting(MiniObjetivoEntity::getTipo).containsExactly(
                TipoMiniObjetivo.CAPITAL_PARADO, TipoMiniObjetivo.VARIEDAD_VENDIDA,
                TipoMiniObjetivo.PRODUCTOS_BAJO_EL_MINIMO);
    }

    private void entoncesElModoEs(ModoMiniObjetivo esperado) {
        assertThat(guardado.getModo()).isEqualTo(esperado);
    }

    private void entoncesLosPendientesSonDelMasViejoAlMasNuevo() {
        assertThat(listados).extracting(MiniObjetivoEntity::getId).containsExactly(idViejo, idNuevo);
    }

    private void entoncesNoHayPendientes() {
        assertThat(listados).isEmpty();
    }

    private void entoncesElObjetivoNoTieneMinis() {
        assertThat(minis.findAllByObjetivoIdAndNegocioIdOrderByCreadoEnAscPosicionAsc(objetivoA, negocioA))
                .isEmpty();
    }

    // --- helpers ---
    private UUID dadoObjetivo(UUID negocioId) {
        return objetivos.saveAndFlush(ObjetivoEntity.builder()
                .negocioId(negocioId)
                .texto("Ganar más")
                .meta(META)
                .creadoEn(AHORA)
                .build()).getId();
    }

    private MiniObjetivoEntity dadoMini(
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
