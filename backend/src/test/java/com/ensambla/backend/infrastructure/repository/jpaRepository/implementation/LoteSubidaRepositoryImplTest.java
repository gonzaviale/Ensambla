package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.lote.EstadoLote;
import com.ensambla.backend.domain.model.lote.FotoCuaderno;
import com.ensambla.backend.domain.model.lote.LoteSubida;
import com.ensambla.backend.infrastructure.repository.entity.FotoCuadernoEntity;
import com.ensambla.backend.infrastructure.repository.entity.LoteSubidaEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IFotoCuadernoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ILoteSubidaJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.LoteSubidaRepositoryMapper;
import com.ensambla.backend.support.LoteTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import static com.ensambla.backend.support.LoteTestData.AHORA;
import static com.ensambla.backend.support.LoteTestData.FOTO_ID;
import static com.ensambla.backend.support.LoteTestData.LOTE_ID;
import static com.ensambla.backend.support.LoteTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LoteSubidaRepositoryImplTest {

    private static final BloqueId BLOQUE_ID = BloqueId.de("00000000-0000-4000-8000-000000000015");

    private final ILoteSubidaJpaRepository loteSubidaJpaRepository = mock(ILoteSubidaJpaRepository.class);
    private final IFotoCuadernoJpaRepository fotoCuadernoJpaRepository = mock(IFotoCuadernoJpaRepository.class);
    private final LoteSubidaRepositoryMapper loteSubidaRepositoryMapper = new LoteSubidaRepositoryMapper();
    private final LoteSubidaRepositoryImpl loteSubidaRepositoryImpl = new LoteSubidaRepositoryImpl(
            loteSubidaJpaRepository, fotoCuadernoJpaRepository, loteSubidaRepositoryMapper);

    private LoteSubida guardado;
    private boolean confirmado;
    private Optional<FotoCuaderno> foto;
    private Optional<LoteSubida> encontrado;

    @Test
    @DisplayName("Guarda el lote y después sus fotos apuntando al id que le dio la base")
    void guardaElLoteYDespuesSusFotosApuntandoAlIdQueLeDioLaBase() {
        dadoQueSeGuardanElLoteYSusFotos();

        cuandoSeGuardaElLoteConUnaFotoSinLote();

        entoncesLasFotosQuedanVinculadasAlLoteGuardado();
    }

    @Test
    @DisplayName("Confirma el lote cuando la sentencia condicional cambió una fila")
    void confirmaElLoteCuandoLaSentenciaCondicionalCambioUnaFila() {
        dadoQueLaSentenciaCambiaFilas(1);

        cuandoSeConfirmaElLote();

        entoncesElLoteQuedaConfirmadoConLaSentenciaCondicional();
    }

    @Test
    @DisplayName("No confirma cuando el lote ya no estaba listo o es de otro negocio")
    void noConfirmaCuandoElLoteYaNoEstabaListoOEsDeOtroNegocio() {
        dadoQueLaSentenciaCambiaFilas(0);

        cuandoSeConfirmaElLote();

        entoncesElLoteNoQuedaConfirmado();
    }

    @Test
    @DisplayName("Trae una foto del negocio por su id")
    void traeUnaFotoDelNegocioPorSuId() {
        when(fotoCuadernoJpaRepository.findByIdAndNegocioId(FOTO_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(Optional.of(loteSubidaRepositoryMapper.toEntity(LoteTestData.foto())));

        cuandoSeBuscaLaFotoPorIdYNegocio();

        entoncesSeEncuentraLaFoto();
    }

    @Test
    @DisplayName("Una foto de otro negocio no se encuentra")
    void unaFotoDeOtroNegocioNoSeEncuentra() {
        when(fotoCuadernoJpaRepository.findByIdAndNegocioId(FOTO_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(Optional.empty());

        cuandoSeBuscaLaFotoPorIdYNegocio();

        entoncesNoSeEncuentraNingunaFoto();
    }

    @Test
    @DisplayName("Trae el lote del negocio con sus fotos ordenadas")
    void traeElLoteDelNegocioConSusFotosOrdenadas() {
        dadoQueExisteElLoteConUnaFoto();

        cuandoSeBuscaElLotePorIdYNegocio();

        entoncesSeEncuentraElLoteConSuFoto();
    }

    @Test
    @DisplayName("No busca fotos cuando el lote no es del negocio")
    void noBuscaFotosCuandoElLoteNoEsDelNegocio() {
        dadoQueElLoteNoExiste();

        cuandoSeBuscaElLotePorIdYNegocio();

        entoncesNoSeEncuentraElLoteNiSeBuscanFotos();
    }

    // --- dado ---
    private void dadoQueSeGuardanElLoteYSusFotos() {
        when(loteSubidaJpaRepository.saveAndFlush(any())).thenReturn(loteEntity());
        when(fotoCuadernoJpaRepository.saveAllAndFlush(anyList()))
                .thenReturn(List.of(loteSubidaRepositoryMapper.toEntity(LoteTestData.foto())));
    }

    private void dadoQueLaSentenciaCambiaFilas(int filas) {
        when(loteSubidaJpaRepository.cambiarEstado(any(), any(), any(), any(), any(), any())).thenReturn(filas);
    }

    private void dadoQueExisteElLoteConUnaFoto() {
        when(loteSubidaJpaRepository.findByIdAndNegocioId(LOTE_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(Optional.of(loteEntity()));
        when(fotoCuadernoJpaRepository
                .findAllByLoteIdAndNegocioIdOrderByPosicionAsc(LOTE_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(List.of(loteSubidaRepositoryMapper.toEntity(LoteTestData.foto())));
    }

    private void dadoQueElLoteNoExiste() {
        when(loteSubidaJpaRepository.findByIdAndNegocioId(LOTE_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(Optional.empty());
    }

    private LoteSubidaEntity loteEntity() {
        return loteSubidaRepositoryMapper.toEntity(LoteTestData.lote(List.of()));
    }

    private FotoCuaderno fotoSinLote() {
        FotoCuaderno unaFoto = LoteTestData.foto();
        unaFoto.setId(null);
        unaFoto.setLoteId(null);
        return unaFoto;
    }

    // --- cuando ---
    private void cuandoSeGuardaElLoteConUnaFotoSinLote() {
        guardado = loteSubidaRepositoryImpl.saveConFotos(LoteTestData.lote(List.of(fotoSinLote())));
    }

    private void cuandoSeConfirmaElLote() {
        confirmado = loteSubidaRepositoryImpl.confirmar(LOTE_ID, NEGOCIO_ID, BLOQUE_ID, AHORA);
    }

    private void cuandoSeBuscaLaFotoPorIdYNegocio() {
        foto = loteSubidaRepositoryImpl.findFotoByIdAndNegocioId(FOTO_ID, NEGOCIO_ID);
    }

    private void cuandoSeBuscaElLotePorIdYNegocio() {
        encontrado = loteSubidaRepositoryImpl.findByIdAndNegocioId(LOTE_ID, NEGOCIO_ID);
    }

    // --- entonces ---
    @SuppressWarnings("unchecked")
    private List<FotoCuadernoEntity> fotosGuardadas() {
        ArgumentCaptor<List<FotoCuadernoEntity>> fotos = ArgumentCaptor.forClass(List.class);
        verify(fotoCuadernoJpaRepository).saveAllAndFlush(fotos.capture());
        return fotos.getValue();
    }

    private void entoncesLasFotosQuedanVinculadasAlLoteGuardado() {
        assertThat(guardado.getId()).isEqualTo(LOTE_ID);
        assertThat(guardado.getFotos()).extracting("id").containsExactly(FOTO_ID);
        assertThat(fotosGuardadas()).extracting(FotoCuadernoEntity::getLoteId).containsExactly(LOTE_ID.valor());
    }

    private void entoncesElLoteQuedaConfirmadoConLaSentenciaCondicional() {
        assertThat(confirmado).isTrue();
        verify(loteSubidaJpaRepository)
                .cambiarEstado(LOTE_ID.valor(), NEGOCIO_ID.valor(), EstadoLote.LISTO, EstadoLote.CONFIRMADO,
                        BLOQUE_ID.valor(), AHORA);
    }

    private void entoncesElLoteNoQuedaConfirmado() {
        assertThat(confirmado).isFalse();
    }

    private void entoncesSeEncuentraLaFoto() {
        assertThat(foto).map(FotoCuaderno::getId).contains(FOTO_ID);
    }

    private void entoncesNoSeEncuentraNingunaFoto() {
        assertThat(foto).isEmpty();
    }

    private void entoncesSeEncuentraElLoteConSuFoto() {
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getFotos()).extracting("id").containsExactly(FOTO_ID);
    }

    private void entoncesNoSeEncuentraElLoteNiSeBuscanFotos() {
        assertThat(encontrado).isEmpty();
        verifyNoInteractions(fotoCuadernoJpaRepository);
    }
}
