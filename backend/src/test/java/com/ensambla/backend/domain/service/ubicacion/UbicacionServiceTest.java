package com.ensambla.backend.domain.service.ubicacion;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ServicioUbicacionException;
import com.ensambla.backend.domain.port.GeocodificadorPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UbicacionServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-19T12:00:00Z");
    private static final String CONSULTA = "Florencio Varela 1903, San Justo";

    @Mock
    private GeocodificadorPort geocodificadorPort;

    @InjectMocks
    private UbicacionService ubicacionService;

    @Test
    @DisplayName("Normaliza espacios y conserva la localidad antes de consultar")
    void buscarShouldNormalizeAddressBeforeCallingProvider() {
        when(geocodificadorPort.buscar(CONSULTA, AHORA)).thenReturn(List.of());

        var resultados = ubicacionService.buscar("  Florencio   Varela 1903, San Justo  ", AHORA);

        assertThat(resultados).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "    ", "abcd"})
    @DisplayName("No consulta al proveedor con una dirección demasiado corta")
    void buscarShouldRejectShortAddress(String consulta) {
        assertThatThrownBy(() -> ubicacionService.buscar(consulta, AHORA))
                .isInstanceOf(BusinessRuleException.class);

        verifyNoInteractions(geocodificadorPort);
    }

    @Test
    @DisplayName("No consulta al proveedor con una dirección de más de 200 caracteres")
    void buscarShouldRejectLongAddress() {
        assertThatThrownBy(() -> ubicacionService.buscar("a".repeat(201), AHORA))
                .isInstanceOf(BusinessRuleException.class);

        verifyNoInteractions(geocodificadorPort);
    }

    @Test
    @DisplayName("Propaga la indisponibilidad del proveedor sin inventar resultados")
    void buscarShouldPropagateProviderFailure() {
        when(geocodificadorPort.buscar(CONSULTA, AHORA)).thenThrow(new ServicioUbicacionException("Sin servicio"));

        assertThatThrownBy(() -> ubicacionService.buscar(CONSULTA, AHORA))
                .isInstanceOf(ServicioUbicacionException.class).hasMessage("Sin servicio");
    }
}
