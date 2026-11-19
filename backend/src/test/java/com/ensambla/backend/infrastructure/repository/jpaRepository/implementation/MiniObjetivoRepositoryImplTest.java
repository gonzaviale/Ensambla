package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ModoMiniObjetivo;
import com.ensambla.backend.infrastructure.repository.entity.MiniObjetivoEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IMiniObjetivoJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.MiniObjetivoRepositoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniManual;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MiniObjetivoRepositoryImplTest {

    private final IMiniObjetivoJpaRepository jpa = mock(IMiniObjetivoJpaRepository.class);
    private final MiniObjetivoRepositoryMapper mapper = new MiniObjetivoRepositoryMapper();
    private final MiniObjetivoRepositoryImpl repository = new MiniObjetivoRepositoryImpl(jpa, mapper);
    private List<MiniObjetivo> minis;
    private Optional<MiniObjetivo> mini;
    private MiniObjetivo guardado;

    @Test
    @DisplayName("Lista los mini-objetivos del objetivo del negocio, en el orden de la base, con los UUID de los ids")
    void listaLosMiniObjetivosDelObjetivoDelNegocioEnElOrdenDeLaBaseConLosUuidDeLos() {
        dadoLaBaseTieneDosMinisDelObjetivo();

        cuandoSeListanLosMinisDelObjetivo();

        entoncesLosIdsSonEnOrden(MINI_ID, OTRO_MINI_ID);
    }

    @Test
    @DisplayName("Sin mini-objetivos devuelve una lista vacía")
    void sinMiniObjetivosDevuelveUnaListaVacia() {
        dadoLaBaseNoTieneMinisDelObjetivo();

        cuandoSeListanLosMinisDelObjetivo();

        entoncesNoHayMinis();
    }

    @Test
    @DisplayName("Busca por id, objetivo y negocio y mapea el resultado")
    void buscaPorIdObjetivoYNegocioYMapeaElResultado() {
        dadoLaBaseTieneElMini();

        cuandoSeBuscaElMini();

        entoncesSeEncuentraElMini();
    }

    @Test
    @DisplayName("Uno de otro objetivo o de otro negocio no se encuentra")
    void unoDeOtroObjetivoODeOtroNegocioNoSeEncuentra() {
        dadoLaBaseNoTieneElMini();

        cuandoSeBuscaElMini();

        entoncesNoSeEncuentraElMini();
    }

    @Test
    @DisplayName("Guarda escribiendo en el acto y devuelve lo guardado como dominio")
    void guardaEscribiendoEnElActoYDevuelveLoGuardadoComoDominio() {
        dadoLaBaseDevuelveLoQueGuarda();

        cuandoSeGuardaUnMini();

        entoncesSeEscribioEnElActoYSeDevuelveElDominio();
    }

    @Test
    @DisplayName("Lista los manuales pendientes del negocio con el modo manual y el estado en curso")
    void listaLosManualesPendientesDelNegocioConElModoManualYElEstadoEnCurso() {
        dadoLaBaseTieneUnManualPendiente();

        cuandoSeListanLosPendientes();

        entoncesHayUnManualPendiente();
    }

    // --- arrange ---
    private void dadoLaBaseTieneDosMinisDelObjetivo() {
        MiniObjetivo otro = miniObjetivo();
        otro.setId(OTRO_MINI_ID);
        when(jpa.findAllByObjetivoIdAndNegocioIdOrderByCreadoEnAscPosicionAsc(
                OBJETIVO_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(List.of(mapper.toEntity(miniObjetivo()), mapper.toEntity(otro)));
    }

    private void dadoLaBaseNoTieneMinisDelObjetivo() {
        when(jpa.findAllByObjetivoIdAndNegocioIdOrderByCreadoEnAscPosicionAsc(
                OBJETIVO_ID.valor(), NEGOCIO_ID.valor())).thenReturn(List.of());
    }

    private void dadoLaBaseTieneElMini() {
        when(jpa.findByIdAndObjetivoIdAndNegocioId(MINI_ID.valor(), OBJETIVO_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(Optional.of(mapper.toEntity(miniObjetivo())));
    }

    private void dadoLaBaseNoTieneElMini() {
        when(jpa.findByIdAndObjetivoIdAndNegocioId(MINI_ID.valor(), OBJETIVO_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(Optional.empty());
    }

    private void dadoLaBaseDevuelveLoQueGuarda() {
        when(jpa.saveAndFlush(any(MiniObjetivoEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void dadoLaBaseTieneUnManualPendiente() {
        when(jpa.findPendientesDeRespuesta(NEGOCIO_ID.valor(), ModoMiniObjetivo.MANUAL, EstadoMiniObjetivo.EN_CURSO))
                .thenReturn(List.of(mapper.toEntity(miniManual())));
    }

    // --- act ---
    private void cuandoSeListanLosMinisDelObjetivo() {
        minis = repository.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID);
    }

    private void cuandoSeBuscaElMini() {
        mini = repository.findByIdAndObjetivoIdAndNegocioId(MINI_ID, OBJETIVO_ID, NEGOCIO_ID);
    }

    private void cuandoSeGuardaUnMini() {
        guardado = repository.save(miniObjetivo());
    }

    private void cuandoSeListanLosPendientes() {
        minis = repository.findAllPendientesDeRespuestaByNegocioId(NEGOCIO_ID);
    }

    // --- assert ---
    private void entoncesLosIdsSonEnOrden(MiniObjetivoId... ids) {
        assertThat(minis).extracting(MiniObjetivo::getId).containsExactly(ids);
    }

    private void entoncesNoHayMinis() {
        assertThat(minis).isEmpty();
    }

    private void entoncesSeEncuentraElMini() {
        assertThat(mini).hasValueSatisfying(encontrado -> assertThat(encontrado.getId()).isEqualTo(MINI_ID));
    }

    private void entoncesNoSeEncuentraElMini() {
        assertThat(mini).isEmpty();
    }

    private void entoncesSeEscribioEnElActoYSeDevuelveElDominio() {
        ArgumentCaptor<MiniObjetivoEntity> captor = ArgumentCaptor.forClass(MiniObjetivoEntity.class);
        verify(jpa).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(MINI_ID.valor());
        assertThat(guardado.getId()).isEqualTo(MiniObjetivoId.de(MINI_ID.valor()));
    }

    private void entoncesHayUnManualPendiente() {
        assertThat(minis).singleElement().satisfies(manual -> assertThat(manual.esManual()).isTrue());
    }
}
