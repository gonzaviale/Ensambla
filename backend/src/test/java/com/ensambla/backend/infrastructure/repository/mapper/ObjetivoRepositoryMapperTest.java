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

    private static final int DIAS_HASTA_ALCANZARLO = 3;

    private final ObjetivoRepositoryMapper mapper = new ObjetivoRepositoryMapper();

    @Test
    @DisplayName("Mapea la entidad al dominio con todos sus campos")
    void unaEntidadSeLeeComoObjetivoConTodosSusDatos() {
        ObjetivoEntity entidad = dadoUnaEntidadDeObjetivoAlcanzado();

        Objetivo objetivo = cuandoSeConvierteEnObjetivo(entidad);

        entoncesElObjetivoTieneTodosLosDatosDeLaEntidad(objetivo);
    }

    @Test
    @DisplayName("Una entidad sin ids se mapea con ids nulos")
    void unaEntidadSinIdsSeLeeConIdsNulos() {
        ObjetivoEntity entidad = dadoUnaEntidadSinIds();

        Objetivo objetivo = cuandoSeConvierteEnObjetivo(entidad);

        entoncesElObjetivoNoTieneIds(objetivo);
    }

    @Test
    @DisplayName("Mapea el dominio a la entidad con todos sus campos")
    void unObjetivoAlcanzadoSeGuardaConTodosSusDatos() {
        Objetivo alcanzado = dadoUnObjetivoAlcanzado();

        ObjetivoEntity entidad = cuandoSeConvierteEnEntidad(alcanzado);

        entoncesLaEntidadTieneTodosLosDatosDelObjetivoAlcanzado(entidad);
    }

    @Test
    @DisplayName("Un objetivo nuevo, sin id, se mapea con id nulo para que la base lo asigne")
    void unObjetivoNuevoSeGuardaSinIdParaQueLaBaseLoAsigne() {
        Objetivo nuevo = dadoUnObjetivoNuevo();

        ObjetivoEntity entidad = cuandoSeConvierteEnEntidad(nuevo);

        entoncesLaEntidadNoTieneId(entidad);
    }

    // --- dado ---
    private ObjetivoEntity dadoUnaEntidadDeObjetivoAlcanzado() {
        return ObjetivoEntity.builder()
                .id(OBJETIVO_ID.valor())
                .negocioId(NEGOCIO_ID.valor())
                .texto(TEXTO)
                .meta(META)
                .creadoEn(AHORA)
                .alcanzadoEn(AHORA.plusDays(DIAS_HASTA_ALCANZARLO))
                .build();
    }

    private ObjetivoEntity dadoUnaEntidadSinIds() {
        return ObjetivoEntity.builder().build();
    }

    private Objetivo dadoUnObjetivoAlcanzado() {
        Objetivo alcanzado = objetivo();
        alcanzado.marcarAlcanzado(AHORA.plusDays(DIAS_HASTA_ALCANZARLO));
        return alcanzado;
    }

    private Objetivo dadoUnObjetivoNuevo() {
        return Objetivo.nuevo(NEGOCIO_ID, TEXTO, META, AHORA);
    }

    // --- cuando ---
    private Objetivo cuandoSeConvierteEnObjetivo(ObjetivoEntity entidad) {
        return mapper.toDomain(entidad);
    }

    private ObjetivoEntity cuandoSeConvierteEnEntidad(Objetivo objetivo) {
        return mapper.toEntity(objetivo);
    }

    // --- entonces ---
    private void entoncesElObjetivoTieneTodosLosDatosDeLaEntidad(Objetivo objetivo) {
        assertThat(objetivo.getId()).isEqualTo(OBJETIVO_ID);
        assertThat(objetivo.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(objetivo.getTexto()).isEqualTo(TEXTO);
        assertThat(objetivo.getMeta()).isEqualTo(META);
        assertThat(objetivo.getCreadoEn()).isEqualTo(AHORA);
        assertThat(objetivo.getAlcanzadoEn()).isEqualTo(AHORA.plusDays(DIAS_HASTA_ALCANZARLO));
    }

    private void entoncesElObjetivoNoTieneIds(Objetivo objetivo) {
        assertThat(objetivo.getId()).isNull();
        assertThat(objetivo.getNegocioId()).isNull();
    }

    private void entoncesLaEntidadTieneTodosLosDatosDelObjetivoAlcanzado(ObjetivoEntity entidad) {
        assertThat(entidad.getAlcanzadoEn()).isEqualTo(AHORA.plusDays(DIAS_HASTA_ALCANZARLO));
        assertThat(entidad.getId()).isEqualTo(OBJETIVO_ID.valor());
        assertThat(entidad.getNegocioId()).isEqualTo(NEGOCIO_ID.valor());
        assertThat(entidad.getTexto()).isEqualTo(TEXTO);
        assertThat(entidad.getMeta()).isEqualTo(META);
        assertThat(entidad.getCreadoEn()).isEqualTo(AHORA);
    }

    private void entoncesLaEntidadNoTieneId(ObjetivoEntity entidad) {
        assertThat(entidad.getId()).isNull();
    }
}
