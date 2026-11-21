package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.aviso.AccionAbrirRegistro;
import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.GrupoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.infrastructure.repository.entity.AvisoEntity;
import com.ensambla.backend.support.IdsDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AvisoRepositoryMapperTest {

    private static final AvisoId ID = AvisoId.de("e0000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final ProyectoId PROYECTO_ID = ProyectoId.de("c0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");

    private final AvisoRepositoryMapper mapper = new AvisoRepositoryMapper();

    @Test
    @DisplayName("Pasa el aviso de la entidad al dominio sin perder datos")
    void unAvisoGuardadoSePasaAlDominioSinPerderDatos() {
        AvisoEntity entidad = dadoUnaEntidadDeAviso();

        Aviso aviso = cuandoSePasaAlDominio(entidad);

        entoncesTieneTodosLosDatosDeUnAvisoDeStock(aviso);
    }

    @Test
    @DisplayName("Pasa el aviso del dominio a la entidad sin perder datos")
    void unAvisoDelDominioSePasaALaEntidadSinPerderDatos() {
        Aviso aviso = dadoUnAvisoDeStock();

        AvisoEntity entidad = cuandoSePasaALaEntidad(aviso);

        entoncesTieneTodosLosDatosDeLaEntidadDeAviso(entidad);
    }

    // --- dado ---
    private AvisoEntity dadoUnaEntidadDeAviso() {
        return AvisoEntity.builder()
                .id(ID.valor())
                .negocioId(NEGOCIO_ID.valor())
                .proyectoId(PROYECTO_ID.valor())
                .tipo(TipoAviso.STOCK)
                .grupo(GrupoAviso.STOCK)
                .prioridad((short) 3)
                .entidad("registro:1")
                .huella("STOCK:registro:1:ab")
                .datos(Map.of("cantidad", 0))
                .titulo("Te quedaste sin yerba")
                .detalle("No te quedan unidades")
                .motivos(List.of("motivo"))
                .acciones(List.of(Map.of("tipo", "ABRIR", "texto", "Ver producto")))
                .estado(EstadoAviso.DESCARTADO)
                .resueltoEn(AHORA)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build();
    }

    private Aviso dadoUnAvisoDeStock() {
        return Aviso.builder()
                .id(ID)
                .negocioId(NEGOCIO_ID)
                .proyectoId(PROYECTO_ID)
                .tipo(TipoAviso.STOCK)
                .grupo(GrupoAviso.STOCK)
                .prioridad((short) 3)
                .entidad("registro:1")
                .huella("STOCK:registro:1:ab")
                .datos(Map.of("cantidad", 0))
                .titulo("Te quedaste sin yerba")
                .detalle("No te quedan unidades")
                .motivos(List.of("motivo"))
                .acciones(List.of(new AccionAbrirRegistro("Ver producto", null, null, null)))
                .estado(EstadoAviso.DESCARTADO)
                .resueltoEn(AHORA)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build();
    }

    // --- cuando ---
    private Aviso cuandoSePasaAlDominio(AvisoEntity entidad) {
        return mapper.toDomain(entidad);
    }

    private AvisoEntity cuandoSePasaALaEntidad(Aviso aviso) {
        return mapper.toEntity(aviso);
    }

    // --- entonces ---
    private void entoncesTieneTodosLosDatosDeUnAvisoDeStock(Aviso aviso) {
        assertThat(aviso).usingRecursiveComparison()
                .withEqualsForFieldsMatchingRegexes(IdsDePrueba::mismoValor, IdsDePrueba.CAMPOS_DE_ID)
                .isEqualTo(dadoUnAvisoDeStock());
    }

    private void entoncesTieneTodosLosDatosDeLaEntidadDeAviso(AvisoEntity entidad) {
        assertThat(entidad).usingRecursiveComparison()
                .withEqualsForFieldsMatchingRegexes(IdsDePrueba::mismoValor, IdsDePrueba.CAMPOS_DE_ID)
                .isEqualTo(dadoUnaEntidadDeAviso());
    }
}
