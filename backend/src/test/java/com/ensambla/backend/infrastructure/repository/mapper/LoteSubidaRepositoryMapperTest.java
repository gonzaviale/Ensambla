package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.lote.EstadoLote;
import com.ensambla.backend.domain.model.lote.FotoCuaderno;
import com.ensambla.backend.domain.model.lote.LoteSubida;
import com.ensambla.backend.infrastructure.repository.entity.FotoCuadernoEntity;
import com.ensambla.backend.infrastructure.repository.entity.LoteSubidaEntity;
import com.ensambla.backend.support.LoteTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.LoteTestData.AHORA;
import static com.ensambla.backend.support.LoteTestData.BLOQUE_ID;
import static com.ensambla.backend.support.LoteTestData.CLAVE;
import static com.ensambla.backend.support.LoteTestData.FOTO_ID;
import static com.ensambla.backend.support.LoteTestData.JPEG;
import static com.ensambla.backend.support.LoteTestData.LOTE_ID;
import static com.ensambla.backend.support.LoteTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;

class LoteSubidaRepositoryMapperTest {

    private final LoteSubidaRepositoryMapper mapper = new LoteSubidaRepositoryMapper();

    @Test
    @DisplayName("Convierte el lote a entidad con todos sus campos")
    void unLoteSePasaALaEntidadConTodosSusDatos() {
        LoteSubida lote = dadoUnLoteSinFotos();

        LoteSubidaEntity entidad = cuandoSePasaALaEntidad(lote);

        entoncesLaEntidadTieneTodosLosDatosDelLote(entidad);
    }

    @Test
    @DisplayName("Convierte la entidad del lote al dominio con todos sus campos")
    void unLoteGuardadoSePasaAlDominioConTodosSusDatos() {
        LoteSubidaEntity entidad = dadoUnaEntidadDeLoteSinFotos();

        LoteSubida lote = cuandoSePasaAlDominio(entidad);

        entoncesElLoteTieneTodosLosDatosDeLaEntidadYNingunaFoto(lote);
    }

    @Test
    @DisplayName("Convierte la foto a entidad con todos sus campos")
    void unaFotoSePasaALaEntidadConTodosSusDatos() {
        FotoCuaderno foto = dadoUnaFotoDelCuaderno();

        FotoCuadernoEntity entidad = cuandoSePasaALaEntidad(foto);

        entoncesLaEntidadTieneTodosLosDatosDeLaFoto(entidad);
    }

    @Test
    @DisplayName("Convierte la entidad de la foto al dominio con todos sus campos")
    void unaFotoGuardadaSePasaAlDominioConTodosSusDatos() {
        FotoCuadernoEntity entidad = dadoUnaEntidadDeFotoDelCuaderno();

        FotoCuaderno foto = cuandoSePasaAlDominio(entidad);

        entoncesLaFotoTieneTodosLosDatosDeLaEntidadYNingunContenido(foto);
    }

    // --- dado ---
    private LoteSubida dadoUnLoteSinFotos() {
        return LoteTestData.lote(List.of());
    }

    private LoteSubidaEntity dadoUnaEntidadDeLoteSinFotos() {
        return mapper.toEntity(dadoUnLoteSinFotos());
    }

    private FotoCuaderno dadoUnaFotoDelCuaderno() {
        return LoteTestData.foto();
    }

    private FotoCuadernoEntity dadoUnaEntidadDeFotoDelCuaderno() {
        return mapper.toEntity(dadoUnaFotoDelCuaderno());
    }

    // --- cuando ---
    private LoteSubidaEntity cuandoSePasaALaEntidad(LoteSubida lote) {
        return mapper.toEntity(lote);
    }

    private LoteSubida cuandoSePasaAlDominio(LoteSubidaEntity entidad) {
        return mapper.toDomain(entidad);
    }

    private FotoCuadernoEntity cuandoSePasaALaEntidad(FotoCuaderno foto) {
        return mapper.toEntity(foto);
    }

    private FotoCuaderno cuandoSePasaAlDominio(FotoCuadernoEntity entidad) {
        return mapper.toDomain(entidad);
    }

    // --- entonces ---
    private void entoncesLaEntidadTieneTodosLosDatosDelLote(LoteSubidaEntity entidad) {
        assertThat(entidad.getId()).isEqualTo(LOTE_ID.valor());
        assertThat(entidad.getNegocioId()).isEqualTo(NEGOCIO_ID.valor());
        assertThat(entidad.getBloqueDestinoId()).isEqualTo(BLOQUE_ID.valor());
        assertThat(entidad.getEstado()).isEqualTo(EstadoLote.PENDIENTE);
        assertThat(entidad.getIntentos()).isZero();
        assertThat(entidad.getCreadoEn()).isEqualTo(AHORA);
        assertThat(entidad.getActualizadoEn()).isEqualTo(AHORA);
    }

    private void entoncesElLoteTieneTodosLosDatosDeLaEntidadYNingunaFoto(LoteSubida lote) {
        assertThat(lote.getId()).isEqualTo(LOTE_ID);
        assertThat(lote.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(lote.getBloqueDestinoId()).isEqualTo(BLOQUE_ID);
        assertThat(lote.getEstado()).isEqualTo(EstadoLote.PENDIENTE);
        assertThat(lote.getIntentos()).isZero();
        assertThat(lote.getCreadoEn()).isEqualTo(AHORA);
        assertThat(lote.getActualizadoEn()).isEqualTo(AHORA);
        assertThat(lote.getFotos()).isEmpty();
    }

    private void entoncesLaEntidadTieneTodosLosDatosDeLaFoto(FotoCuadernoEntity entidad) {
        assertThat(entidad.getId()).isEqualTo(FOTO_ID.valor());
        assertThat(entidad.getNegocioId()).isEqualTo(NEGOCIO_ID.valor());
        assertThat(entidad.getLoteId()).isEqualTo(LOTE_ID.valor());
        assertThat(entidad.getClaveAlmacenamiento()).isEqualTo(CLAVE);
        assertThat(entidad.getTipoContenido()).isEqualTo("image/jpeg");
        assertThat(entidad.getTamanioBytes()).isEqualTo(JPEG.length);
        assertThat(entidad.getRotacion()).isZero();
        assertThat(entidad.getPosicion()).isZero();
        assertThat(entidad.getCreadoEn()).isEqualTo(AHORA);
        assertThat(entidad.getActualizadoEn()).isEqualTo(AHORA);
    }

    private void entoncesLaFotoTieneTodosLosDatosDeLaEntidadYNingunContenido(FotoCuaderno foto) {
        assertThat(foto.getId()).isEqualTo(FOTO_ID);
        assertThat(foto.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(foto.getLoteId()).isEqualTo(LOTE_ID);
        assertThat(foto.getClaveAlmacenamiento()).isEqualTo(CLAVE);
        assertThat(foto.getTipoContenido()).isEqualTo("image/jpeg");
        assertThat(foto.getTamanioBytes()).isEqualTo(JPEG.length);
        assertThat(foto.getRotacion()).isZero();
        assertThat(foto.getPosicion()).isZero();
        assertThat(foto.getCreadoEn()).isEqualTo(AHORA);
        assertThat(foto.getActualizadoEn()).isEqualTo(AHORA);
        assertThat(foto.getContenido()).isNull();
    }
}
