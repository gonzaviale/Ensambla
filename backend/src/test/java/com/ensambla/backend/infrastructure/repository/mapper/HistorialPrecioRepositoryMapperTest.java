package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.precio.CambioPrecio;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.infrastructure.repository.entity.HistorialPrecioEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class HistorialPrecioRepositoryMapperTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final RegistroId REGISTRO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");

    private final HistorialPrecioRepositoryMapper mapper = new HistorialPrecioRepositoryMapper();

    @Test
    @DisplayName("Pasa el cambio de precio de la entidad al dominio sin perder datos")
    void unCambioDePrecioGuardadoSePasaAlDominioSinPerderDatos() {
        HistorialPrecioEntity entidad = dadoUnaEntidadDeCambioDePrecio();

        CambioPrecio cambio = cuandoSePasaAlDominio(entidad);

        entoncesEsElCambioDeMilAMilDoscientos(cambio);
    }

    @Test
    @DisplayName("Pasa el cambio de precio del dominio a la entidad sin perder datos")
    void unCambioDePrecioSinPrecioAnteriorSePasaALaEntidadSinPerderDatos() {
        CambioPrecio cambio = dadoUnPrimerPrecioDeMilDoscientos();

        HistorialPrecioEntity entidad = cuandoSePasaALaEntidad(cambio);

        entoncesTieneTodosLosDatosDelPrimerPrecio(entidad);
    }

    // --- dado ---
    private HistorialPrecioEntity dadoUnaEntidadDeCambioDePrecio() {
        return HistorialPrecioEntity.builder()
                .negocioId(NEGOCIO_ID.valor())
                .registroId(REGISTRO_ID.valor())
                .precioAnterior(new BigDecimal("1000"))
                .precioNuevo(new BigDecimal("1200"))
                .cambiadoEn(AHORA)
                .build();
    }

    private CambioPrecio dadoUnPrimerPrecioDeMilDoscientos() {
        return new CambioPrecio(NEGOCIO_ID, REGISTRO_ID, null, new BigDecimal("1200"), AHORA);
    }

    // --- cuando ---
    private CambioPrecio cuandoSePasaAlDominio(HistorialPrecioEntity entidad) {
        return mapper.toDomain(entidad);
    }

    private HistorialPrecioEntity cuandoSePasaALaEntidad(CambioPrecio cambio) {
        return mapper.toEntity(cambio);
    }

    // --- entonces ---
    private void entoncesEsElCambioDeMilAMilDoscientos(CambioPrecio cambio) {
        assertThat(cambio).isEqualTo(
                new CambioPrecio(NEGOCIO_ID, REGISTRO_ID, new BigDecimal("1000"), new BigDecimal("1200"), AHORA));
    }

    private void entoncesTieneTodosLosDatosDelPrimerPrecio(HistorialPrecioEntity entidad) {
        assertThat(entidad.getId()).isNull();
        assertThat(entidad.getNegocioId()).isEqualTo(NEGOCIO_ID.valor());
        assertThat(entidad.getRegistroId()).isEqualTo(REGISTRO_ID.valor());
        assertThat(entidad.getPrecioAnterior()).isNull();
        assertThat(entidad.getPrecioNuevo()).isEqualByComparingTo("1200");
        assertThat(entidad.getCambiadoEn()).isEqualTo(AHORA);
    }
}
