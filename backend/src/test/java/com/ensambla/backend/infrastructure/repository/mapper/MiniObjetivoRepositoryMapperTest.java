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
    void unMiniObjetivoGuardadoSePasaAlDominioConTodosSusDatos() {
        MiniObjetivoEntity entidad = dadoUnaEntidadDeMiniObjetivoReemplazado();

        MiniObjetivo mini = cuandoSePasaAlDominio(entidad);

        entoncesElMiniObjetivoTieneTodosLosDatosDeLaEntidadReemplazada(mini);
    }

    @Test
    @DisplayName("Una entidad sin ids se mapea con ids nulos")
    void unMiniObjetivoGuardadoSinIdsSePasaAlDominioConIdsNulos() {
        MiniObjetivoEntity entidad = dadoUnaEntidadDeMiniObjetivoSinIds();

        MiniObjetivo mini = cuandoSePasaAlDominio(entidad);

        entoncesElMiniObjetivoNoTieneIds(mini);
    }

    @Test
    @DisplayName("Mapea el dominio a la entidad con todos sus campos")
    void unMiniObjetivoDelDominioSePasaALaEntidadConTodosSusDatos() {
        MiniObjetivo mini = dadoUnMiniObjetivoReemplazadoConTextoDeIa();

        MiniObjetivoEntity entidad = cuandoSePasaALaEntidad(mini);

        entoncesLaEntidadTieneTodosLosDatosDelMiniObjetivoReemplazado(entidad);
    }

    @Test
    @DisplayName("Un mini-objetivo sin producto ni reemplazo se mapea con esos ids nulos")
    void unMiniObjetivoSinProductoNiReemplazoSePasaALaEntidadConEsosIdsNulos() {
        MiniObjetivo mini = dadoUnMiniObjetivoSinProductoNiReemplazo();

        MiniObjetivoEntity entidad = cuandoSePasaALaEntidad(mini);

        entoncesLaEntidadNoTieneProductoNiReemplazo(entidad);
    }

    // --- dado ---
    private MiniObjetivoEntity dadoUnaEntidadDeMiniObjetivoReemplazado() {
        return MiniObjetivoEntity.builder()
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
    }

    private MiniObjetivoEntity dadoUnaEntidadDeMiniObjetivoSinIds() {
        return MiniObjetivoEntity.builder().build();
    }

    private MiniObjetivo dadoUnMiniObjetivoReemplazadoConTextoDeIa() {
        MiniObjetivo mini = miniObjetivo();
        mini.setPosicion(2);
        mini.setTextoConIa(true);
        mini.setReemplazadoPor(OTRO_MINI_ID);
        mini.setSinAvanceDesde(SEMANA);
        return mini;
    }

    private MiniObjetivo dadoUnMiniObjetivoSinProductoNiReemplazo() {
        MiniObjetivo mini = miniObjetivo();
        mini.setRegistroId(null);
        mini.setReemplazadoPor(null);
        return mini;
    }

    // --- cuando ---
    private MiniObjetivo cuandoSePasaAlDominio(MiniObjetivoEntity entidad) {
        return mapper.toDomain(entidad);
    }

    private MiniObjetivoEntity cuandoSePasaALaEntidad(MiniObjetivo mini) {
        return mapper.toEntity(mini);
    }

    // --- entonces ---
    private void entoncesElMiniObjetivoTieneTodosLosDatosDeLaEntidadReemplazada(MiniObjetivo mini) {
        assertThat(mini.getId()).isEqualTo(MINI_ID);
        assertThat(mini.getObjetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(mini.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(mini.getPosicion()).isEqualTo(3);
        assertThat(mini.getTipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(mini.getRegistroId()).isEqualTo(PRODUCTO_ID);
        assertThat(mini.getReferencia()).isEqualTo(PRODUCTO);
        assertThat(mini.getMeta()).isEqualTo(META_MINI);
        assertThat(mini.getValorInicial()).isEqualTo(VALOR_INICIAL);
        assertThat(mini.getValorActual()).isEqualTo(VALOR_INICIAL.add(APORTE));
        assertThat(mini.getAporteEstimado()).isEqualTo(APORTE);
        assertThat(mini.getEstado()).isEqualTo(EstadoMiniObjetivo.REEMPLAZADO);
        assertThat(mini.getSemanaInicio()).isEqualTo(SEMANA);
        assertThat(mini.getTitulo()).isEqualTo(TITULO);
        assertThat(mini.getJustificacion()).isEqualTo(JUSTIFICACION);
        assertThat(mini.isTextoConIa()).isTrue();
        assertThat(mini.getSinAvanceDesde()).isEqualTo(SEMANA);
        assertThat(mini.getReemplazadoPor()).isEqualTo(OTRO_MINI_ID);
        assertThat(mini.getCreadoEn()).isEqualTo(AHORA);
        assertThat(mini.getActualizadoEn()).isEqualTo(AHORA.plusDays(1));
    }

    private void entoncesElMiniObjetivoNoTieneIds(MiniObjetivo mini) {
        assertThat(mini.getId()).isNull();
        assertThat(mini.getObjetivoId()).isNull();
        assertThat(mini.getNegocioId()).isNull();
        assertThat(mini.getRegistroId()).isNull();
        assertThat(mini.getReemplazadoPor()).isNull();
    }

    private void entoncesLaEntidadTieneTodosLosDatosDelMiniObjetivoReemplazado(MiniObjetivoEntity entidad) {
        assertThat(entidad.getId()).isEqualTo(MINI_ID.valor());
        assertThat(entidad.getObjetivoId()).isEqualTo(OBJETIVO_ID.valor());
        assertThat(entidad.getNegocioId()).isEqualTo(NEGOCIO_ID.valor());
        assertThat(entidad.getPosicion()).isEqualTo((short) 2);
        assertThat(entidad.getTipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(entidad.getRegistroId()).isEqualTo(PRODUCTO_ID.valor());
        assertThat(entidad.getReferencia()).isEqualTo(PRODUCTO);
        assertThat(entidad.getMeta()).isEqualTo(META_MINI);
        assertThat(entidad.getValorInicial()).isEqualTo(VALOR_INICIAL);
        assertThat(entidad.getValorActual()).isEqualTo(VALOR_INICIAL);
        assertThat(entidad.getAporteEstimado()).isEqualTo(APORTE);
        assertThat(entidad.getSinAvanceDesde()).isEqualTo(SEMANA);
        assertThat(entidad.getEstado()).isEqualTo(EstadoMiniObjetivo.EN_CURSO);
        assertThat(entidad.getSemanaInicio()).isEqualTo(SEMANA);
        assertThat(entidad.getTitulo()).isEqualTo(TITULO);
        assertThat(entidad.getJustificacion()).isEqualTo(JUSTIFICACION);
        assertThat(entidad.isTextoConIa()).isTrue();
        assertThat(entidad.getReemplazadoPor()).isEqualTo(OTRO_MINI_ID.valor());
        assertThat(entidad.getCreadoEn()).isEqualTo(AHORA);
        assertThat(entidad.getActualizadoEn()).isEqualTo(AHORA);
    }

    private void entoncesLaEntidadNoTieneProductoNiReemplazo(MiniObjetivoEntity entidad) {
        assertThat(entidad.getRegistroId()).isNull();
        assertThat(entidad.getReemplazadoPor()).isNull();
    }
}
