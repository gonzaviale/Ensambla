package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.objetivo.CierreMini;
import com.ensambla.backend.domain.model.objetivo.ConteoDecisiones;
import com.ensambla.backend.domain.model.objetivo.ReajusteMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.ReajusteMiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ResumenSemanal;
import com.ensambla.backend.domain.model.objetivo.ResumenSemanalId;
import com.ensambla.backend.domain.model.objetivo.TipoReajuste;
import com.ensambla.backend.infrastructure.repository.entity.ReajusteMiniObjetivoEntity;
import com.ensambla.backend.infrastructure.repository.entity.ResumenSemanalEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IReajusteMiniObjetivoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IResumenSemanalJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.ResumenSemanalRepositoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.SEMANA;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResumenSemanalRepositoryImplTest {

    private final IResumenSemanalJpaRepository resumenes = mock(IResumenSemanalJpaRepository.class);
    private final IReajusteMiniObjetivoJpaRepository reajustes = mock(IReajusteMiniObjetivoJpaRepository.class);
    private final ResumenSemanalRepositoryMapper mapper = new ResumenSemanalRepositoryMapper();
    private final ResumenSemanalRepositoryImpl repository =
            new ResumenSemanalRepositoryImpl(resumenes, reajustes, mapper);

    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<List<ReajusteMiniObjetivoEntity>> reajustesGuardados =
            ArgumentCaptor.forClass(List.class);

    private static final UUID RESUMEN_NUEVO_ID = UUID.fromString("00000000-0000-4000-8000-0000000000f3");
    private static final UUID RESUMEN_VIEJO_ID = UUID.fromString("00000000-0000-4000-8000-0000000000f4");

    private ResumenSemanal resumenDado;
    private ResumenSemanal guardado;
    private List<ResumenSemanal> semanas;
    private Throwable rechazo;
    private DataIntegrityViolationException otroError;

    @Test
    @DisplayName("Guarda el resumen y después sus reajustes colgados de él")
    void guardaElResumenYDespuesSusReajustesColgadosDeEl() {
        dadoUnResumenConUnReajuste();

        cuandoSeGuardaElResumen();

        entoncesSeGuardaElResumenYSusReajustesColgadosDeEl();
    }

    @Test
    @DisplayName("Una semana ya cerrada se informa como duplicada y no guarda reajustes")
    void unaSemanaYaCerradaSeInformaComoDuplicadaYNoGuardaReajustes() {
        dadoQueLaSemanaYaEstaCerrada();

        cuandoSeIntentaGuardarElResumen();

        entoncesSeRechazaComoSemanaDuplicadaSinGuardarReajustes();
    }

    @Test
    @DisplayName("Otro error de integridad se deja pasar tal cual")
    void otroErrorDeIntegridadSeDejaPasarTalCual() {
        dadoQueGuardarFallaPorUnaClaveForanea();

        cuandoSeIntentaGuardarElResumen();

        entoncesSePropagaElMismoError();
    }

    @Test
    @DisplayName("Un objetivo sin semanas cerradas devuelve una lista vacía sin buscar reajustes")
    void unObjetivoSinSemanasCerradasDevuelveUnaListaVaciaSinBuscarReajustes() {
        dadoQueElObjetivoNoTieneSemanasCerradas();

        cuandoSeListanLasSemanasDelObjetivo();

        entoncesNoHaySemanasNiSeBuscaronReajustes();
    }

    @Test
    @DisplayName("Lista las semanas en el orden de la consulta, cada una con sus propios reajustes")
    void listaLasSemanasEnElOrdenDeLaConsultaCadaUnaConSusPropiosReajustes() {
        dadoQueElObjetivoTieneUnaSemanaNuevaConReajusteYOtraViejaSin();

        cuandoSeListanLasSemanasDelObjetivo();

        entoncesCadaSemanaTraeSusPropiosReajustes();
    }

    // --- dado ---
    private void dadoUnResumenConUnReajuste() {
        resumenDado = resumen(UUID.randomUUID(), reajuste());
    }

    private void dadoQueLaSemanaYaEstaCerrada() {
        resumenDado = resumen(UUID.randomUUID());
        when(resumenes.saveAndFlush(any(ResumenSemanalEntity.class))).thenThrow(new DataIntegrityViolationException(
                "error", new RuntimeException("duplicate key value violates unique constraint \"ux_resumen_semana\"")));
    }

    private void dadoQueGuardarFallaPorUnaClaveForanea() {
        resumenDado = resumen(UUID.randomUUID());
        otroError = new DataIntegrityViolationException(
                "error", new RuntimeException("violates foreign key constraint"));
        when(resumenes.saveAndFlush(any(ResumenSemanalEntity.class))).thenThrow(otroError);
    }

    private void dadoQueElObjetivoNoTieneSemanasCerradas() {
        when(resumenes.findAllByObjetivoIdOrderBySemanaInicioDesc(OBJETIVO_ID.valor())).thenReturn(List.of());
    }

    private void dadoQueElObjetivoTieneUnaSemanaNuevaConReajusteYOtraViejaSin() {
        when(resumenes.findAllByObjetivoIdOrderBySemanaInicioDesc(OBJETIVO_ID.valor())).thenReturn(List.of(
                mapper.toEntity(resumen(RESUMEN_NUEVO_ID)), mapper.toEntity(resumen(RESUMEN_VIEJO_ID))));
        when(reajustes.findAllByResumenIdInOrderByCreadoEnAscIdAsc(List.of(RESUMEN_NUEVO_ID, RESUMEN_VIEJO_ID)))
                .thenReturn(List.of(mapper.toEntity(ResumenSemanalId.de(RESUMEN_NUEVO_ID), reajuste())));
    }

    private static ResumenSemanal resumen(UUID id, ReajusteMiniObjetivo... reajustesDelResumen) {
        return new ResumenSemanal(ResumenSemanalId.de(id), OBJETIVO_ID, SEMANA, BigDecimal.TEN, BigDecimal.ONE,
                new ConteoDecisiones(0, 0, 0, 0), List.of(new CierreMini(MINI_ID, BigDecimal.ONE)), "Bien", false,
                AHORA, List.of(reajustesDelResumen));
    }

    private static ReajusteMiniObjetivo reajuste() {
        return new ReajusteMiniObjetivo(ReajusteMiniObjetivoId.de("00000000-0000-4000-8000-0000000000f2"), MINI_ID,
                OTRO_MINI_ID, TipoReajuste.REEMPLAZAR, "Cambio", "Antes", BigDecimal.ONE, "Después", BigDecimal.TEN,
                AHORA);
    }

    // --- cuando ---
    private void cuandoSeGuardaElResumen() {
        guardado = repository.save(resumenDado);
    }

    private void cuandoSeIntentaGuardarElResumen() {
        rechazo = catchThrowable(this::cuandoSeGuardaElResumen);
    }

    private void cuandoSeListanLasSemanasDelObjetivo() {
        semanas = repository.findAllByObjetivoId(OBJETIVO_ID);
    }

    // --- entonces ---
    private void entoncesSeGuardaElResumenYSusReajustesColgadosDeEl() {
        assertThat(guardado).isSameAs(resumenDado);
        ArgumentCaptor<ResumenSemanalEntity> guardadoEntidad = ArgumentCaptor.forClass(ResumenSemanalEntity.class);
        verify(resumenes).saveAndFlush(guardadoEntidad.capture());
        assertThat(guardadoEntidad.getValue().getId()).isEqualTo(resumenDado.id().valor());
        verify(reajustes).saveAllAndFlush(reajustesGuardados.capture());
        assertThat(reajustesGuardados.getValue()).singleElement().satisfies(reajuste -> {
            assertThat(reajuste.getResumenId()).isEqualTo(resumenDado.id().valor());
            assertThat(reajuste.getMiniNuevoId()).isEqualTo(OTRO_MINI_ID.valor());
        });
    }

    private void entoncesSeRechazaComoSemanaDuplicadaSinGuardarReajustes() {
        assertThat(rechazo).isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Esa semana del objetivo ya está cerrada");
        verify(reajustes, never()).saveAllAndFlush(anyList());
    }

    private void entoncesSePropagaElMismoError() {
        assertThat(rechazo).isSameAs(otroError);
    }

    private void entoncesNoHaySemanasNiSeBuscaronReajustes() {
        assertThat(semanas).isEmpty();
        verify(reajustes, never()).findAllByResumenIdInOrderByCreadoEnAscIdAsc(anyList());
    }

    private void entoncesCadaSemanaTraeSusPropiosReajustes() {
        assertThat(semanas).extracting(resumen -> resumen.id().valor())
                .containsExactly(RESUMEN_NUEVO_ID, RESUMEN_VIEJO_ID);
        assertThat(semanas.get(0).reajustes()).containsExactly(reajuste());
        assertThat(semanas.get(1).reajustes()).isEmpty();
    }
}
