package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.infrastructure.repository.entity.MiniObjetivoEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.MiniObjetivoTestData.APORTE;
import static com.ensambla.backend.support.MiniObjetivoTestData.JUSTIFICACION;
import static com.ensambla.backend.support.MiniObjetivoTestData.META_MINI;
import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.SEMANA;
import static com.ensambla.backend.support.MiniObjetivoTestData.TITULO;
import static com.ensambla.backend.support.MiniObjetivoTestData.VALOR_INICIAL;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static org.assertj.core.api.Assertions.assertThat;

class MiniObjetivoRepositoryMapperTest {

    private final MiniObjetivoRepositoryMapper mapper = new MiniObjetivoRepositoryMapper();

    @Test
    @DisplayName("Mapea la entidad al dominio con todos sus campos")
    void toDomainShouldMapEveryField() {
        MiniObjetivoEntity entity = MiniObjetivoEntity.builder()
                .id(MINI_ID.valor())
                .objetivoId(OBJETIVO_ID.valor())
                .negocioId(NEGOCIO_ID.valor())
                .posicion((short) 3)
                .tipo(TipoMiniObjetivo.SUBIR_PRECIO)
                .registroId(PRODUCTO_ID.valor())
                .referencia(PRODUCTO)
                .meta(META_MINI)
                .valorInicial(VALOR_INICIAL)
                .valorActual(VALOR_INICIAL.add(APORTE))
                .aporteEstimado(APORTE)
                .estado(EstadoMiniObjetivo.REEMPLAZADO)
                .semanaInicio(SEMANA)
                .titulo(TITULO)
                .justificacion(JUSTIFICACION)
                .textoConIa(true)
                .sinAvanceDesde(SEMANA)
                .reemplazadoPor(OTRO_MINI_ID.valor())
                .creadoEn(AHORA)
                .actualizadoEn(AHORA.plusDays(1))
                .build();

        MiniObjetivo result = mapper.toDomain(entity);

        assertThat(result.getId()).isEqualTo(MINI_ID);
        assertThat(result.getObjetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.getPosicion()).isEqualTo(3);
        assertThat(result.getTipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(result.getRegistroId()).isEqualTo(PRODUCTO_ID);
        assertThat(result.getReferencia()).isEqualTo(PRODUCTO);
        assertThat(result.getMeta()).isEqualTo(META_MINI);
        assertThat(result.getValorInicial()).isEqualTo(VALOR_INICIAL);
        assertThat(result.getValorActual()).isEqualTo(VALOR_INICIAL.add(APORTE));
        assertThat(result.getAporteEstimado()).isEqualTo(APORTE);
        assertThat(result.getEstado()).isEqualTo(EstadoMiniObjetivo.REEMPLAZADO);
        assertThat(result.getSemanaInicio()).isEqualTo(SEMANA);
        assertThat(result.getTitulo()).isEqualTo(TITULO);
        assertThat(result.getJustificacion()).isEqualTo(JUSTIFICACION);
        assertThat(result.isTextoConIa()).isTrue();
        assertThat(result.getSinAvanceDesde()).isEqualTo(SEMANA);
        assertThat(result.getReemplazadoPor()).isEqualTo(OTRO_MINI_ID);
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
        assertThat(result.getActualizadoEn()).isEqualTo(AHORA.plusDays(1));
    }

    @Test
    @DisplayName("Una entidad sin ids se mapea con ids nulos")
    void toDomainShouldKeepMissingIdsNull() {
        MiniObjetivo result = mapper.toDomain(MiniObjetivoEntity.builder().build());

        assertThat(result.getId()).isNull();
        assertThat(result.getObjetivoId()).isNull();
        assertThat(result.getNegocioId()).isNull();
        assertThat(result.getRegistroId()).isNull();
        assertThat(result.getReemplazadoPor()).isNull();
    }

    @Test
    @DisplayName("Mapea el dominio a la entidad con todos sus campos")
    void toEntityShouldMapEveryField() {
        MiniObjetivo mini = miniObjetivo();
        mini.setPosicion(2);
        mini.setTextoConIa(true);
        mini.setReemplazadoPor(OTRO_MINI_ID);
        mini.setSinAvanceDesde(SEMANA);

        MiniObjetivoEntity result = mapper.toEntity(mini);

        assertThat(result.getId()).isEqualTo(MINI_ID.valor());
        assertThat(result.getObjetivoId()).isEqualTo(OBJETIVO_ID.valor());
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID.valor());
        assertThat(result.getPosicion()).isEqualTo((short) 2);
        assertThat(result.getTipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(result.getRegistroId()).isEqualTo(PRODUCTO_ID.valor());
        assertThat(result.getReferencia()).isEqualTo(PRODUCTO);
        assertThat(result.getMeta()).isEqualTo(META_MINI);
        assertThat(result.getValorInicial()).isEqualTo(VALOR_INICIAL);
        assertThat(result.getValorActual()).isEqualTo(VALOR_INICIAL);
        assertThat(result.getAporteEstimado()).isEqualTo(APORTE);
        assertThat(result.getSinAvanceDesde()).isEqualTo(SEMANA);
        assertThat(result.getEstado()).isEqualTo(EstadoMiniObjetivo.EN_CURSO);
        assertThat(result.getSemanaInicio()).isEqualTo(SEMANA);
        assertThat(result.getTitulo()).isEqualTo(TITULO);
        assertThat(result.getJustificacion()).isEqualTo(JUSTIFICACION);
        assertThat(result.isTextoConIa()).isTrue();
        assertThat(result.getReemplazadoPor()).isEqualTo(OTRO_MINI_ID.valor());
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
        assertThat(result.getActualizadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Un mini-objetivo sin producto ni reemplazo se mapea con esos ids nulos")
    void toEntityShouldKeepMissingIdsNull() {
        MiniObjetivo mini = miniObjetivo();
        mini.setRegistroId(null);
        mini.setReemplazadoPor(null);

        MiniObjetivoEntity result = mapper.toEntity(mini);

        assertThat(result.getRegistroId()).isNull();
        assertThat(result.getReemplazadoPor()).isNull();
    }
}
