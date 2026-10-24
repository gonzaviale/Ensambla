package com.ensambla.backend.domain.model.negocio;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.model.zona.ZonaId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfiguracionNegocioTest {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");
    private static final ZonaId ZONA_ID = ZonaId.de("00000000-0000-4000-8000-000000000001");

    @Test
    @DisplayName("Conserva todos los campos ausentes y la zona al editar sólo el nombre")
    void configurarShouldPreserveMissingFields() {
        Negocio negocio = givenNegocio();

        negocio.configurar("  Nuevo nombre  ", null, null, null, AHORA);

        assertThat(negocio.getNombre()).isEqualTo("Nuevo nombre");
        assertThat(negocio.getRubro()).isEqualTo(RubroNegocio.ALMACEN);
        assertThat(negocio.getDireccion()).isEqualTo("Calle 1");
        assertThat(negocio.getLocalidad()).isEqualTo("San Justo");
        assertThat(negocio.getLatitud()).isEqualByComparingTo("-34");
        assertThat(negocio.getLongitud()).isEqualByComparingTo("-58");
        assertThat(negocio.getComparteDatosZona()).isTrue();
        assertThat(negocio.getZonaId()).isEqualTo(ZONA_ID);
        assertThat(negocio.getActualizadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Actualiza ubicación, rubro y privacidad sin cambiar el nombre")
    void configurarShouldReplaceConfiguration() {
        Negocio negocio = givenNegocio();

        negocio.configurar(null, RubroNegocio.KIOSCO, givenUbicacion("-35", "-59"), false, AHORA);

        assertThat(negocio.getNombre()).isEqualTo("Mi negocio");
        assertThat(negocio.getDireccion()).isEqualTo("Nueva dirección");
        assertThat(negocio.getLocalidad()).isEqualTo("San Justo");
        assertThat(negocio.getLatitud()).isEqualByComparingTo("-35");
        assertThat(negocio.getLongitud()).isEqualByComparingTo("-59");
        assertThat(negocio.getRubro()).isEqualTo(RubroNegocio.KIOSCO);
        assertThat(negocio.getComparteDatosZona()).isFalse();
        assertThat(negocio.getZonaId()).isNull();
    }

    @Test
    @DisplayName("Conserva la zona si se confirma el mismo punto")
    void configurarShouldKeepZoneForSamePoint() {
        Negocio negocio = givenNegocio();

        negocio.configurar(null, null, givenUbicacion("-34", "-58"), null, AHORA);

        assertThat(negocio.getZonaId()).isEqualTo(ZONA_ID);
    }

    @Test
    @DisplayName("Invalida la zona anterior si cambia sólo la longitud")
    void configurarShouldClearZoneForChangedLongitude() {
        Negocio negocio = givenNegocio();

        negocio.configurar(null, null, givenUbicacion("-34", "-59"), null, AHORA);

        assertThat(negocio.getZonaId()).isNull();
    }

    @Test
    @DisplayName("La misma ubicación guardada no cuenta como cambio")
    void cambiaUbicacionShouldBeFalseForSameLocation() {
        assertThat(givenNegocio().cambiaUbicacion(givenUbicacion("-34", "-58"))).isFalse();
    }

    @Test
    @DisplayName("Cambiar la latitud, la longitud o la localidad cuenta como cambio")
    void cambiaUbicacionShouldBeTrueWhenPointOrLocalityDiffers() {
        Negocio negocio = givenNegocio();

        assertThat(negocio.cambiaUbicacion(givenUbicacion("-35", "-58"))).isTrue();
        assertThat(negocio.cambiaUbicacion(givenUbicacion("-34", "-59"))).isTrue();
        assertThat(negocio.cambiaUbicacion(new UbicacionNegocio("Calle 1", "Ramos Mejía",
                new BigDecimal("-34"), new BigDecimal("-58")))).isTrue();
    }

    @Test
    @DisplayName("Necesita zona si tiene punto y no tiene zona")
    void necesitaZonaShouldBeTrueForPointWithoutZone() {
        Negocio negocio = givenNegocio();
        negocio.setZonaId(null);

        assertThat(negocio.necesitaZona()).isTrue();
    }

    @Test
    @DisplayName("No necesita zona si ya tiene una")
    void necesitaZonaShouldBeFalseWhenZoneAssigned() {
        assertThat(givenNegocio().necesitaZona()).isFalse();
    }

    @Test
    @DisplayName("No necesita zona si no tiene punto")
    void necesitaZonaShouldBeFalseWithoutPoint() {
        assertThat(Negocio.builder().build().necesitaZona()).isFalse();
    }

    @Test
    @DisplayName("Asigna el id de la zona resuelta")
    void asignarZonaShouldStoreZoneId() {
        Negocio negocio = givenNegocio();
        UUID otraZona = UUID.fromString("00000000-0000-4000-8000-000000000002");

        negocio.asignarZona(Zona.builder().id(ZonaId.de(otraZona)).build());

        assertThat(negocio.getZonaId()).isEqualTo(ZonaId.de(otraZona));
    }

    @Test
    @DisplayName("Quita la zona si no se resolvió ninguna")
    void asignarZonaShouldClearZoneWhenNoneResolved() {
        Negocio negocio = givenNegocio();

        negocio.asignarZona(null);

        assertThat(negocio.getZonaId()).isNull();
    }

    @Test
    @DisplayName("Rechaza un nombre vacío")
    void configurarShouldRejectBlankName() {
        Negocio negocio = givenNegocio();

        assertThatThrownBy(() -> negocio.configurar("  ", null, null, null, AHORA))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Rechaza un nombre mayor a 120 caracteres")
    void configurarShouldRejectLongName() {
        Negocio negocio = givenNegocio();

        assertThatThrownBy(() -> negocio.configurar("a".repeat(121), null, null, null, AHORA))
                .isInstanceOf(BusinessRuleException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Rechaza dirección o localidad sin contenido")
    void ubicacionShouldRejectMissingText(String texto) {
        assertThatThrownBy(() -> new UbicacionNegocio(texto, "San Justo", BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Rechaza una dirección demasiado larga")
    void ubicacionShouldRejectLongAddress() {
        assertThatThrownBy(() -> new UbicacionNegocio("a".repeat(501), "San Justo",
                BigDecimal.ZERO, BigDecimal.ZERO)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Rechaza una localidad demasiado larga")
    void ubicacionShouldRejectLongLocality() {
        assertThatThrownBy(() -> new UbicacionNegocio("Calle 1", "a".repeat(121),
                BigDecimal.ZERO, BigDecimal.ZERO)).isInstanceOf(BusinessRuleException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"90.00001", "-90.00001"})
    @DisplayName("Rechaza latitudes fuera de rango antes de redondear")
    void ubicacionShouldRejectLatitudeOutsideRange(String latitud) {
        assertThatThrownBy(() -> givenUbicacion(latitud, "0"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Rechaza una coordenada ausente")
    void ubicacionShouldRejectMissingCoordinate() {
        assertThatThrownBy(() -> new UbicacionNegocio("Calle 1", "San Justo", null, BigDecimal.ZERO))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Rechaza longitudes fuera de rango")
    void ubicacionShouldRejectLongitudeOutsideRange() {
        assertThatThrownBy(() -> givenUbicacion("0", "181"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Normaliza los textos y redondea al formato de coordenadas de PostgreSQL")
    void ubicacionShouldNormalizeAndRound() {
        UbicacionNegocio ubicacion = givenUbicacion("-34.1234567", "-58.1234567");

        assertThat(ubicacion.direccion()).isEqualTo("Nueva dirección");
        assertThat(ubicacion.localidad()).isEqualTo("San Justo");
        assertThat(ubicacion.latitud()).isEqualTo(new BigDecimal("-34.123457"));
        assertThat(ubicacion.longitud()).isEqualTo(new BigDecimal("-58.123457"));
    }

    // --- arrange ---
    private Negocio givenNegocio() {
        return Negocio.builder().nombre("Mi negocio").rubro(RubroNegocio.ALMACEN)
                .direccion("Calle 1").localidad("San Justo")
                .latitud(new BigDecimal("-34.000000")).longitud(new BigDecimal("-58.000000"))
                .zonaId(ZONA_ID).build();
    }

    private UbicacionNegocio givenUbicacion(String latitud, String longitud) {
        return new UbicacionNegocio("  Nueva dirección  ", "  San Justo  ",
                new BigDecimal(latitud), new BigDecimal(longitud));
    }
}
