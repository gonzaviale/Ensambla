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

    private static final UUID RESUMEN_NUEVO = UUID.fromString("00000000-0000-4000-8000-0000000000f3");
    private static final UUID RESUMEN_VIEJO = UUID.fromString("00000000-0000-4000-8000-0000000000f4");

    private final IResumenSemanalJpaRepository resumenes = mock(IResumenSemanalJpaRepository.class);
    private final IReajusteMiniObjetivoJpaRepository reajustes = mock(IReajusteMiniObjetivoJpaRepository.class);
    private final ResumenSemanalRepositoryMapper mapper = new ResumenSemanalRepositoryMapper();
    private final ResumenSemanalRepositoryImpl repository =
            new ResumenSemanalRepositoryImpl(resumenes, reajustes, mapper);

    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<List<ReajusteMiniObjetivoEntity>> reajustesGuardados =
            ArgumentCaptor.forClass(List.class);

    private ResumenSemanal resumenAGuardar;
    private ResumenSemanal resumenDevuelto;
    private List<ResumenSemanal> semanas;
    private DataIntegrityViolationException errorDeIntegridad;
    private Throwable error;

    @Test
    @DisplayName("Guarda el resumen y después sus reajustes colgados de él")
    void saveShouldSaveTheResumenAndItsReajustes() {
        givenUnResumenConUnReajuste();

        whenSeGuardaElResumen();

        thenSeDevuelveElMismoResumen();
        thenSeGuardaElResumen();
        thenSeGuardanSusReajustesColgadosDeEl();
    }

    @Test
    @DisplayName("Una semana ya cerrada se informa como duplicada y no guarda reajustes")
    void saveShouldThrowDuplicateWhenTheWeekIsAlreadyClosed() {
        givenUnaSemanaYaCerrada();

        whenSeGuardaElResumenConError();

        thenSeInformaQueLaSemanaYaEstaCerrada();
        thenNoSeGuardanReajustes();
    }

    @Test
    @DisplayName("Otro error de integridad se deja pasar tal cual")
    void saveShouldRethrowOtherIntegrityErrors() {
        givenOtroErrorDeIntegridad();

        whenSeGuardaElResumenConError();

        thenSeDejaPasarElMismoError();
    }

    @Test
    @DisplayName("Un objetivo sin semanas cerradas devuelve una lista vacía sin buscar reajustes")
    void findAllByObjetivoIdShouldReturnEmptyWithoutLookingForReajustes() {
        givenUnObjetivoSinSemanasCerradas();

        whenSeListanLasSemanas();

        thenNoHaySemanas();
        thenNoSeBuscanReajustes();
    }

    @Test
    @DisplayName("Lista las semanas en el orden de la consulta, cada una con sus propios reajustes")
    void findAllByObjetivoIdShouldGroupTheReajustesByResumen() {
        givenDosSemanasConUnReajusteEnLaNueva();

        whenSeListanLasSemanas();

        thenLasSemanasVienenEnOrdenConSusReajustes();
    }

    // --- arrange ---
    private void givenUnResumenConUnReajuste() {
        resumenAGuardar = resumen(UUID.randomUUID(), reajuste());
    }

    private void givenUnaSemanaYaCerrada() {
        resumenAGuardar = resumen(UUID.randomUUID());
        when(resumenes.saveAndFlush(any(ResumenSemanalEntity.class))).thenThrow(new DataIntegrityViolationException(
                "error", new RuntimeException("duplicate key value violates unique constraint \"ux_resumen_semana\"")));
    }

    private void givenOtroErrorDeIntegridad() {
        resumenAGuardar = resumen(UUID.randomUUID());
        errorDeIntegridad = new DataIntegrityViolationException(
                "error", new RuntimeException("violates foreign key constraint"));
        when(resumenes.saveAndFlush(any(ResumenSemanalEntity.class))).thenThrow(errorDeIntegridad);
    }

    private void givenUnObjetivoSinSemanasCerradas() {
        when(resumenes.findAllByObjetivoIdOrderBySemanaInicioDesc(OBJETIVO_ID.valor())).thenReturn(List.of());
    }

    private void givenDosSemanasConUnReajusteEnLaNueva() {
        when(resumenes.findAllByObjetivoIdOrderBySemanaInicioDesc(OBJETIVO_ID.valor())).thenReturn(List.of(
                mapper.toEntity(resumen(RESUMEN_NUEVO)), mapper.toEntity(resumen(RESUMEN_VIEJO))));
        when(reajustes.findAllByResumenIdInOrderByCreadoEnAscIdAsc(List.of(RESUMEN_NUEVO, RESUMEN_VIEJO)))
                .thenReturn(List.of(mapper.toEntity(ResumenSemanalId.de(RESUMEN_NUEVO), reajuste())));
    }

    // --- act ---
    private void whenSeGuardaElResumen() {
        resumenDevuelto = repository.save(resumenAGuardar);
    }

    private void whenSeGuardaElResumenConError() {
        error = catchThrowable(() -> repository.save(resumenAGuardar));
    }

    private void whenSeListanLasSemanas() {
        semanas = repository.findAllByObjetivoId(OBJETIVO_ID);
    }

    // --- assert ---
    private void thenSeDevuelveElMismoResumen() {
        assertThat(resumenDevuelto).isSameAs(resumenAGuardar);
    }

    private void thenSeGuardaElResumen() {
        ArgumentCaptor<ResumenSemanalEntity> guardado = ArgumentCaptor.forClass(ResumenSemanalEntity.class);
        verify(resumenes).saveAndFlush(guardado.capture());
        assertThat(guardado.getValue().getId()).isEqualTo(resumenAGuardar.id().valor());
    }

    private void thenSeGuardanSusReajustesColgadosDeEl() {
        verify(reajustes).saveAllAndFlush(reajustesGuardados.capture());
        assertThat(reajustesGuardados.getValue()).singleElement().satisfies(reajuste -> {
            assertThat(reajuste.getResumenId()).isEqualTo(resumenAGuardar.id().valor());
            assertThat(reajuste.getMiniNuevoId()).isEqualTo(OTRO_MINI_ID.valor());
        });
    }

    private void thenSeInformaQueLaSemanaYaEstaCerrada() {
        assertThat(error).isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Esa semana del objetivo ya está cerrada");
    }

    private void thenNoSeGuardanReajustes() {
        verify(reajustes, never()).saveAllAndFlush(anyList());
    }

    private void thenSeDejaPasarElMismoError() {
        assertThat(error).isSameAs(errorDeIntegridad);
    }

    private void thenNoHaySemanas() {
        assertThat(semanas).isEmpty();
    }

    private void thenNoSeBuscanReajustes() {
        verify(reajustes, never()).findAllByResumenIdInOrderByCreadoEnAscIdAsc(anyList());
    }

    private void thenLasSemanasVienenEnOrdenConSusReajustes() {
        assertThat(semanas).extracting(resumen -> resumen.id().valor())
                .containsExactly(RESUMEN_NUEVO, RESUMEN_VIEJO);
        assertThat(semanas.get(0).reajustes()).containsExactly(reajuste());
        assertThat(semanas.get(1).reajustes()).isEmpty();
    }

    // --- helpers ---
    private static ResumenSemanal resumen(UUID id, ReajusteMiniObjetivo... reajustesDelResumen) {
        return new ResumenSemanal(ResumenSemanalId.de(id), OBJETIVO_ID, SEMANA, BigDecimal.TEN, BigDecimal.ONE,
                new ConteoDecisiones(0, 0, 0, 0), List.of(new CierreMini(MINI_ID, BigDecimal.ONE)), "Bien", false,
                AHORA, List.of(reajustesDelResumen), List.of());
    }

    private static ReajusteMiniObjetivo reajuste() {
        return new ReajusteMiniObjetivo(ReajusteMiniObjetivoId.de("00000000-0000-4000-8000-0000000000f2"), MINI_ID,
                OTRO_MINI_ID, TipoReajuste.REEMPLAZAR, "Cambio", "Antes", BigDecimal.ONE, "Después", BigDecimal.TEN,
                AHORA);
    }
}
