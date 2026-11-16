package com.ensambla.backend.domain.useCase.cartel;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.cartel.ComposicionDeCartel;
import com.ensambla.backend.domain.model.cartel.FormatoDeCartel;
import com.ensambla.backend.domain.model.cartel.SalidaDeCartel;
import com.ensambla.backend.domain.model.cartel.TipoDeCartel;
import com.ensambla.backend.domain.model.cartel.VarianteDeComposicion;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.ExportadorDeCartelPort;
import com.ensambla.backend.domain.repository.marca.KitDeMarcaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.useCase.cartel.exportarCartel.ExportarCartel;
import com.ensambla.backend.domain.useCase.cartel.exportarCartel.ExportarCartelRequest;
import com.ensambla.backend.domain.useCase.cartel.exportarCartel.ExportarCartelResponse;
import com.ensambla.backend.infrastructure.config.useCase.CartelUseCaseConfig;
import com.ensambla.backend.support.CartelesDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExportarCartelTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-15T10:00:00Z"), ZoneOffset.UTC);

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class, CALLS_REAL_METHODS);
    private final KitDeMarcaRepository kitDeMarcaRepository = mock(KitDeMarcaRepository.class, CALLS_REAL_METHODS);
    private final ExportadorDeCartelPort exportadorPdf = mock(ExportadorDeCartelPort.class);
    private final ExportadorDeCartelPort exportadorPng = mock(ExportadorDeCartelPort.class);
    private final ExportarCartel exportar = new CartelUseCaseConfig().exportarCartel(
            negocioRepository, kitDeMarcaRepository, List.of(exportadorPdf, exportadorPng), CLOCK);

    private ExportarCartelResponse cartel;

    @Test
    @DisplayName("Una hoja A4 se baja como PDF con el exportador de PDF y un nombre que dice qué es")
    void executeShouldExportA4AsPdf() {
        givenUnNegocioConExportadores();
        when(exportadorPdf.exportar(any(ComposicionDeCartel.class))).thenReturn(new byte[] {1, 2});

        whenSeExporta(FormatoDeCartel.A4, List.of());

        assertThat(cartel.contenido()).containsExactly(1, 2);
        assertThat(cartel.tipoMime()).isEqualTo("application/pdf");
        assertThat(cartel.nombreDeArchivo()).isEqualTo("cartel-promo-precio-a4.pdf");
    }

    @Test
    @DisplayName("El formato cuadrado se baja como PNG con el exportador de PNG")
    void executeShouldExportSquareAsPng() {
        givenUnNegocioConExportadores();
        when(exportadorPng.exportar(any(ComposicionDeCartel.class))).thenReturn(new byte[] {9});

        whenSeExporta(FormatoDeCartel.CUADRADO, List.of());

        assertThat(cartel.contenido()).containsExactly(9);
        assertThat(cartel.nombreDeArchivo()).isEqualTo("cartel-promo-precio-cuadrado.png");
    }

    @Test
    @DisplayName("Rechaza más líneas de las que admite el tipo de cartel")
    void executeShouldRejectTooManyLines() {
        givenUnNegocioConExportadores();

        assertThatThrownBy(() -> whenSeExporta(FormatoDeCartel.A4, List.of("uno", "dos")))
                .isInstanceOf(BusinessRuleException.class);
    }

    private void givenUnNegocioConExportadores() {
        Negocio negocio = Negocio.builder().id(CartelesDePrueba.NEGOCIO_ID).nombre("Don Rufino").build();
        when(negocioRepository.findByDuenioId(USUARIO_ID)).thenReturn(Optional.of(negocio));
        when(kitDeMarcaRepository.findByNegocioId(CartelesDePrueba.NEGOCIO_ID)).thenReturn(Optional.empty());
        when(exportadorPdf.salida()).thenReturn(SalidaDeCartel.PDF);
        when(exportadorPng.salida()).thenReturn(SalidaDeCartel.PNG);
    }

    private void whenSeExporta(FormatoDeCartel formato, List<String> lineas) {
        cartel = exportar.execute(new ExportarCartelRequest(USUARIO_ID, VarianteDeComposicion.BLOQUE,
                TipoDeCartel.PROMO_PRECIO, formato, "Aceite a precio de vecino", "20 % menos", "$2.000", "Antes $2.500",
                lineas));
    }
}
