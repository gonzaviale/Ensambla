package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.lote.EstadoLote;
import com.ensambla.backend.domain.model.lote.FotoCuaderno;
import com.ensambla.backend.domain.model.lote.LoteSubida;
import com.ensambla.backend.infrastructure.repository.entity.FotoCuadernoEntity;
import com.ensambla.backend.infrastructure.repository.entity.LoteSubidaEntity;
import com.ensambla.backend.support.LoteTestData;
import org.junit.jupiter.api.BeforeEach;
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

    private LoteSubidaRepositoryMapper loteSubidaRepositoryMapper;

    @BeforeEach
    void setUp() {
        loteSubidaRepositoryMapper = new LoteSubidaRepositoryMapper();
    }

    @Test
    @DisplayName("Convierte el lote a entidad con todos sus campos")
    void toEntityShouldMapLote() {
        LoteSubidaEntity result = loteSubidaRepositoryMapper.toEntity(LoteTestData.lote(List.of()));

        assertThat(result.getId()).isEqualTo(LOTE_ID.valor());
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID.valor());
        assertThat(result.getBloqueDestinoId()).isEqualTo(BLOQUE_ID.valor());
        assertThat(result.getEstado()).isEqualTo(EstadoLote.PENDIENTE);
        assertThat(result.getIntentos()).isZero();
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
        assertThat(result.getActualizadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Convierte la entidad del lote al dominio con todos sus campos")
    void toDomainShouldMapLote() {
        LoteSubidaEntity entity = loteSubidaRepositoryMapper.toEntity(LoteTestData.lote(List.of()));

        LoteSubida result = loteSubidaRepositoryMapper.toDomain(entity);

        assertThat(result.getId()).isEqualTo(LOTE_ID);
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.getBloqueDestinoId()).isEqualTo(BLOQUE_ID);
        assertThat(result.getEstado()).isEqualTo(EstadoLote.PENDIENTE);
        assertThat(result.getIntentos()).isZero();
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
        assertThat(result.getActualizadoEn()).isEqualTo(AHORA);
        assertThat(result.getFotos()).isEmpty();
    }

    @Test
    @DisplayName("Convierte la foto a entidad con todos sus campos")
    void toEntityShouldMapFoto() {
        FotoCuadernoEntity result = loteSubidaRepositoryMapper.toEntity(LoteTestData.foto());

        assertThat(result.getId()).isEqualTo(FOTO_ID.valor());
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID.valor());
        assertThat(result.getLoteId()).isEqualTo(LOTE_ID.valor());
        assertThat(result.getClaveAlmacenamiento()).isEqualTo(CLAVE);
        assertThat(result.getTipoContenido()).isEqualTo("image/jpeg");
        assertThat(result.getTamanioBytes()).isEqualTo(JPEG.length);
        assertThat(result.getRotacion()).isZero();
        assertThat(result.getPosicion()).isZero();
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
        assertThat(result.getActualizadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Convierte la entidad de la foto al dominio con todos sus campos")
    void toDomainShouldMapFoto() {
        FotoCuadernoEntity entity = loteSubidaRepositoryMapper.toEntity(LoteTestData.foto());

        FotoCuaderno result = loteSubidaRepositoryMapper.toDomain(entity);

        assertThat(result.getId()).isEqualTo(FOTO_ID);
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.getLoteId()).isEqualTo(LOTE_ID);
        assertThat(result.getClaveAlmacenamiento()).isEqualTo(CLAVE);
        assertThat(result.getTipoContenido()).isEqualTo("image/jpeg");
        assertThat(result.getTamanioBytes()).isEqualTo(JPEG.length);
        assertThat(result.getRotacion()).isZero();
        assertThat(result.getPosicion()).isZero();
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
        assertThat(result.getActualizadoEn()).isEqualTo(AHORA);
        assertThat(result.getContenido()).isNull();
    }
}
