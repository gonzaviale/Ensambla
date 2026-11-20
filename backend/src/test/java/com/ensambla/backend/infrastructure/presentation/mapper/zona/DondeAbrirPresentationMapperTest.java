package com.ensambla.backend.infrastructure.presentation.mapper.zona;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.zona.DatosZonaDondeAbrir;
import com.ensambla.backend.domain.model.zona.FactorDondeAbrir;
import com.ensambla.backend.domain.model.zona.PublicoDondeAbrir;
import com.ensambla.backend.domain.model.zona.ZonaId;
import com.ensambla.backend.domain.useCase.zona.explorarDondeAbrir.ExplorarDondeAbrirResponse;
import com.ensambla.backend.infrastructure.presentation.dto.zona.ExplorarDondeAbrirWebRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DondeAbrirPresentationMapperTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");
    private static final UUID ZONA_UUID = UUID.fromString("c0000000-0000-4000-8000-000000000001");
    private final DondeAbrirPresentationMapper mapper = new DondeAbrirPresentationMapper();

    @Test
    @DisplayName("Convierte un pedido mínimo y deja vacíos los alquileres opcionales")
    void shouldMapMinimalRequest() {
        var request = new ExplorarDondeAbrirWebRequest(
                RubroNegocio.KIOSCO, PublicoDondeAbrir.OFICINAS, null, null, null, null);

        var resultado = mapper.toRequest(request, USUARIO_ID);

        assertThat(resultado.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(resultado.rubro()).isEqualTo(RubroNegocio.KIOSCO);
        assertThat(resultado.limites()).isNull();
        assertThat(resultado.alquileres()).isEmpty();
        assertThat(resultado.punto()).isNull();
    }

    @Test
    @DisplayName("Convierte el punto elegido en el mapa")
    void shouldMapChosenPoint() {
        var request = new ExplorarDondeAbrirWebRequest(RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS, null, null,
                null, new ExplorarDondeAbrirWebRequest.PuntoWebRequest(new BigDecimal("-34.6"), new BigDecimal("-58.4")));

        var resultado = mapper.toRequest(request, USUARIO_ID);

        assertThat(resultado.punto().latitud()).isEqualByComparingTo("-34.6");
        assertThat(resultado.punto().longitud()).isEqualByComparingTo("-58.4");
    }

    @Test
    @DisplayName("Rechaza dos cotizaciones para la misma zona")
    void shouldRejectDuplicateRentQuotes() {
        var alquiler = new ExplorarDondeAbrirWebRequest.AlquilerWebRequest(ZONA_UUID, BigDecimal.TEN);
        var request = new ExplorarDondeAbrirWebRequest(
                RubroNegocio.ALMACEN, PublicoDondeAbrir.VECINOS, Map.of(), null, List.of(alquiler, alquiler), null);

        assertThatThrownBy(() -> mapper.toRequest(request, USUARIO_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("dos veces");
    }

    @Test
    @DisplayName("Convierte el detalle completo del ranking, sus fuentes, puntos y descartes")
    void shouldMapCompleteResponse() {
        DatosZonaDondeAbrir.Fuente fuente = new DatosZonaDondeAbrir.Fuente(
                "OSM", "OpenStreetMap", "COMUNITARIA", "https://www.openstreetmap.org/", "ODbL");
        DatosZonaDondeAbrir.Punto punto = new DatosZonaDondeAbrir.Punto(
                "node:1", "ESCUELA", null, "Escuela", new BigDecimal("-34.6"), new BigDecimal("-58.4"),
                fuente, LocalDate.parse("2026-11-10"));
        var aporte = new ExplorarDondeAbrirResponse.Aporte(
                FactorDondeAbrir.ESCUELAS, "Escuelas", BigDecimal.TEN, "lugares_500m", new BigDecimal("0.5"),
                BigDecimal.TEN, new BigDecimal("5"), fuente, LocalDate.parse("2026-11-10"));
        ZonaId zonaId = ZonaId.de(ZONA_UUID);
        var zona = new ExplorarDondeAbrirResponse.ZonaEvaluada(
                zonaId, "Almagro", "CABA", "CABA", new BigDecimal("-34.6"), new BigDecimal("-58.4"),
                BigDecimal.ONE, BigDecimal.TEN, "BUENA", List.of(aporte), List.of("Escuelas"), List.of("Validar"),
                List.of(punto));
        var response = new ExplorarDondeAbrirResponse(
                new ExplorarDondeAbrirResponse.NegocioActual(
                        "Negocio", RubroNegocio.ALMACEN, null, null, null, null),
                2, List.of(zona), List.of(new ExplorarDondeAbrirResponse.ZonaDescartada(
                        zonaId, "Boedo", "CABA", "CABA", new BigDecimal("-34.6"),
                        new BigDecimal("-58.4"), BigDecimal.TEN, List.of("Lejos"))), List.of("Aviso"), zonaId);

        var resultado = mapper.toResponse(response);

        assertThat(resultado.negocioActual().zonaId()).isNull();
        assertThat(resultado.puntoElegidoId()).isEqualTo(ZONA_UUID.toString());
        assertThat(resultado.ranking()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(ZONA_UUID.toString());
            assertThat(item.aportes().getFirst().fuente().codigo()).isEqualTo("OSM");
            assertThat(item.puntos().getFirst().nombre()).isEqualTo("Escuela");
        });
        assertThat(resultado.descartadas().getFirst().motivos()).containsExactly("Lejos");
    }
}
