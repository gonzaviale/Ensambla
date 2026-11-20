package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.infrastructure.repository.entity.ObjetivoEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IObjetivoJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.ObjetivoRepositoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.META;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.TEXTO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ObjetivoRepositoryImplTest {

    private static final UUID OTRO_ID = UUID.fromString("00000000-0000-4000-8000-000000000006");

    private final IObjetivoJpaRepository jpa = mock(IObjetivoJpaRepository.class);
    private final ObjetivoRepositoryImpl repository = new ObjetivoRepositoryImpl(jpa, new ObjetivoRepositoryMapper());

    private List<Objetivo> objetivos;
    private List<NegocioId> negocioIds;
    private Optional<Objetivo> encontrado;
    private Objetivo guardado;

    @Test
    @DisplayName("Lista los objetivos del negocio en el orden de la consulta y los mapea al dominio")
    void listaLosObjetivosDelNegocioEnElOrdenDeLaConsultaYLosMapeaAlDominio() {
        dadoQueElNegocioTieneDosObjetivos();

        cuandoSeListanLosObjetivosDelNegocio();

        entoncesSeListanLosDosObjetivosEnOrdenConSuTexto();
    }

    @Test
    @DisplayName("Devuelve una lista vacía cuando el negocio no tiene objetivos")
    void devuelveUnaListaVaciaCuandoElNegocioNoTieneObjetivos() {
        dadoQueElNegocioNoTieneObjetivos();

        cuandoSeListanLosObjetivosDelNegocio();

        entoncesNoSeListaNingunObjetivo();
    }

    @Test
    @DisplayName("Pide los negocios con objetivos por página y los mapea a ids de negocio")
    void pideLosNegociosConObjetivosPorPaginaYLosMapeaAIdsDeNegocio() {
        dadoQueHayUnNegocioConObjetivosEnLaPaginaDos();

        cuandoSeListanLosNegociosConObjetivosDeLaPaginaDos();

        entoncesSeListaElNegocioConObjetivos();
    }

    @Test
    @DisplayName("Busca por id y negocio con los UUID y mapea el resultado")
    void buscaPorIdYNegocioConLosUuidYMapeaElResultado() {
        dadoQueExisteElObjetivo();

        cuandoSeBuscaElObjetivoPorIdYNegocio();

        entoncesSeEncuentraElObjetivo();
    }

    @Test
    @DisplayName("Devuelve vacío cuando el objetivo no es del negocio")
    void devuelveVacioCuandoElObjetivoNoEsDelNegocio() {
        dadoQueElObjetivoNoEsDelNegocio();

        cuandoSeBuscaElObjetivoPorIdYNegocio();

        entoncesNoSeEncuentraNingunObjetivo();
    }

    @Test
    @DisplayName("Guarda la entidad armada desde el dominio y devuelve lo persistido")
    void guardaLaEntidadArmadaDesdeElDominioYDevuelveLoPersistido() {
        dadoQueGuardarDevuelveElObjetivo();

        cuandoSeGuardaUnObjetivoNuevo();

        entoncesSePersistioLaEntidadDelNegocioConSuTextoYSeDevuelveElObjetivo();
    }

    @Test
    @DisplayName("Borra pasando al JPA los UUID del objetivo y del negocio")
    void borraPasandoAlJpaLosUuidDelObjetivoYDelNegocio() {
        cuandoSeBorraElObjetivo();

        entoncesSeDelegoElBorradoConLosUuid();
    }

    // --- dado ---
    private void dadoQueElNegocioTieneDosObjetivos() {
        when(jpa.findAllByNegocioIdOrderByCreadoEnAscIdAsc(NEGOCIO_ID.valor()))
                .thenReturn(List.of(entity(OBJETIVO_ID.valor()), entity(OTRO_ID)));
    }

    private void dadoQueElNegocioNoTieneObjetivos() {
        when(jpa.findAllByNegocioIdOrderByCreadoEnAscIdAsc(NEGOCIO_ID.valor())).thenReturn(List.of());
    }

    private void dadoQueHayUnNegocioConObjetivosEnLaPaginaDos() {
        when(jpa.findNegocioIdsConObjetivos(PageRequest.of(2, 50))).thenReturn(List.of(NEGOCIO_ID.valor()));
    }

    private void dadoQueExisteElObjetivo() {
        when(jpa.findByIdAndNegocioId(OBJETIVO_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(Optional.of(entity(OBJETIVO_ID.valor())));
    }

    private void dadoQueElObjetivoNoEsDelNegocio() {
        when(jpa.findByIdAndNegocioId(OBJETIVO_ID.valor(), NEGOCIO_ID.valor())).thenReturn(Optional.empty());
    }

    private void dadoQueGuardarDevuelveElObjetivo() {
        when(jpa.save(any(ObjetivoEntity.class))).thenReturn(entity(OBJETIVO_ID.valor()));
    }

    private ObjetivoEntity entity(UUID id) {
        return ObjetivoEntity.builder()
                .id(id)
                .negocioId(NEGOCIO_ID.valor())
                .texto(TEXTO)
                .meta(META)
                .creadoEn(AHORA)
                .build();
    }

    // --- cuando ---
    private void cuandoSeListanLosObjetivosDelNegocio() {
        objetivos = repository.findAllByNegocioId(NEGOCIO_ID);
    }

    private void cuandoSeListanLosNegociosConObjetivosDeLaPaginaDos() {
        negocioIds = repository.findNegocioIdsConObjetivos(2, 50);
    }

    private void cuandoSeBuscaElObjetivoPorIdYNegocio() {
        encontrado = repository.findByIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID);
    }

    private void cuandoSeGuardaUnObjetivoNuevo() {
        guardado = repository.save(Objetivo.nuevo(NEGOCIO_ID, TEXTO, META, AHORA));
    }

    private void cuandoSeBorraElObjetivo() {
        repository.deleteByIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID);
    }

    // --- entonces ---
    private void entoncesSeListanLosDosObjetivosEnOrdenConSuTexto() {
        assertThat(objetivos).extracting(Objetivo::getId)
                .extracting(id -> id.valor())
                .containsExactly(OBJETIVO_ID.valor(), OTRO_ID);
        assertThat(objetivos.getFirst().getTexto()).isEqualTo(TEXTO);
    }

    private void entoncesNoSeListaNingunObjetivo() {
        assertThat(objetivos).isEmpty();
    }

    private void entoncesSeListaElNegocioConObjetivos() {
        assertThat(negocioIds).containsExactly(NEGOCIO_ID);
    }

    private void entoncesSeEncuentraElObjetivo() {
        assertThat(encontrado).map(Objetivo::getId).contains(OBJETIVO_ID);
    }

    private void entoncesNoSeEncuentraNingunObjetivo() {
        assertThat(encontrado).isEmpty();
    }

    private void entoncesSePersistioLaEntidadDelNegocioConSuTextoYSeDevuelveElObjetivo() {
        ArgumentCaptor<ObjetivoEntity> captor = ArgumentCaptor.forClass(ObjetivoEntity.class);
        verify(jpa).save(captor.capture());
        assertThat(captor.getValue().getNegocioId()).isEqualTo(NEGOCIO_ID.valor());
        assertThat(captor.getValue().getTexto()).isEqualTo(TEXTO);
        assertThat(guardado.getId()).isEqualTo(OBJETIVO_ID);
    }

    private void entoncesSeDelegoElBorradoConLosUuid() {
        verify(jpa).deleteByIdAndNegocioId(OBJETIVO_ID.valor(), NEGOCIO_ID.valor());
    }
}
