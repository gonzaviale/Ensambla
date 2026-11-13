package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.infrastructure.repository.entity.ObjetivoEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.META;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.TEXTO;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;

class ObjetivoRepositoryMapperTest {

    private final ObjetivoRepositoryMapper mapper = new ObjetivoRepositoryMapper();

    @Test
    @DisplayName("Mapea la entidad al dominio con todos sus campos")
    void toDomainShouldMapEveryField() {
        ObjetivoEntity entity = ObjetivoEntity.builder()
                .id(OBJETIVO_ID.valor())
                .negocioId(NEGOCIO_ID.valor())
                .texto(TEXTO)
                .meta(META)
                .creadoEn(AHORA)
                .alcanzadoEn(AHORA.plusDays(3))
                .build();

        Objetivo result = mapper.toDomain(entity);

        assertThat(result.getId()).isEqualTo(OBJETIVO_ID);
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.getTexto()).isEqualTo(TEXTO);
        assertThat(result.getMeta()).isEqualTo(META);
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
        assertThat(result.getAlcanzadoEn()).isEqualTo(AHORA.plusDays(3));
    }

    @Test
    @DisplayName("Una entidad sin ids se mapea con ids nulos")
    void toDomainShouldKeepMissingIdsNull() {
        Objetivo result = mapper.toDomain(ObjetivoEntity.builder().build());

        assertThat(result.getId()).isNull();
        assertThat(result.getNegocioId()).isNull();
    }

    @Test
    @DisplayName("Mapea el dominio a la entidad con todos sus campos")
    void toEntityShouldMapEveryField() {
        Objetivo alcanzado = objetivo();
        alcanzado.marcarAlcanzado(AHORA.plusDays(3));

        ObjetivoEntity result = mapper.toEntity(alcanzado);

        assertThat(result.getAlcanzadoEn()).isEqualTo(AHORA.plusDays(3));
        assertThat(result.getId()).isEqualTo(OBJETIVO_ID.valor());
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID.valor());
        assertThat(result.getTexto()).isEqualTo(TEXTO);
        assertThat(result.getMeta()).isEqualTo(META);
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Un objetivo nuevo, sin id, se mapea con id nulo para que la base lo asigne")
    void toEntityShouldKeepMissingIdNull() {
        Objetivo nuevo = Objetivo.nuevo(NEGOCIO_ID, TEXTO, META, AHORA);

        ObjetivoEntity result = mapper.toEntity(nuevo);

        assertThat(result.getId()).isNull();
    }
}
