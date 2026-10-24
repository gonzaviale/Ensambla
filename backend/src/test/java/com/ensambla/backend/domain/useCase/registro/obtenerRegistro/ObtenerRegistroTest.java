package com.ensambla.backend.domain.useCase.registro.obtenerRegistro;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObtenerRegistroTest {

    private static final UUID USUARIO_ID = UUID.fromString("b9000000-0000-4000-8000-000000000001");
    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID BLOQUE_ID = UUID.fromString("c0000000-0000-4000-8000-000000000001");
    private static final UUID REGISTRO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");

    @Mock
    private NegocioService negocioService;

    @Mock
    private CampoService campoService;

    @Mock
    private RegistroDatosService registroDatosService;

    private ObtenerRegistro obtenerRegistro;

    @BeforeEach
    void setUp() {
        obtenerRegistro = new ObtenerRegistro(
                negocioService, campoService, registroDatosService);
    }

    @Test
    @DisplayName("Devuelve el registro del negocio de la sesión mapeado")
    void executeShouldReturnMappedRegistroOfTheSessionNegocio() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(registroDatosService.obtener(REGISTRO_ID, NEGOCIO_ID)).thenReturn(
                Registro.builder().id(REGISTRO_ID).bloqueId(BLOQUE_ID).valores(Map.of("campo", "dato")).build());
        when(campoService.listarDeBloque(BLOQUE_ID, NEGOCIO_ID)).thenReturn(List.of());

        ObtenerRegistroResponse response = obtenerRegistro.execute(
                new ObtenerRegistroRequest(USUARIO_ID, REGISTRO_ID));

        assertThat(response.registro().id()).isEqualTo(REGISTRO_ID);
        assertThat(response.registro().valores()).containsEntry("campo", "dato");
    }

    @Test
    @DisplayName("Incluye el valor calculado de los campos fórmula")
    void executeShouldIncludeCalculatedFormulaValues() {
        UUID cantidadId = UUID.fromString("e0000000-0000-4000-8000-000000000001");
        UUID dobleId = UUID.fromString("e0000000-0000-4000-8000-000000000002");
        Campo doble = Campo.builder().id(dobleId).tipo(TipoCampo.FORMULA).formula("{" + cantidadId + "} * 2").build();
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(registroDatosService.obtener(REGISTRO_ID, NEGOCIO_ID)).thenReturn(Registro.builder()
                .id(REGISTRO_ID).bloqueId(BLOQUE_ID).valores(Map.of(cantidadId.toString(), 4)).build());
        when(campoService.listarDeBloque(BLOQUE_ID, NEGOCIO_ID)).thenReturn(List.of(doble));

        ObtenerRegistroResponse response = obtenerRegistro.execute(
                new ObtenerRegistroRequest(USUARIO_ID, REGISTRO_ID));

        assertThat(response.registro().valores()).containsEntry(dobleId.toString(), new BigDecimal("8"));
    }

    @Test
    @DisplayName("Propaga 'no existe' cuando el registro es de otro negocio")
    void executeShouldPropagateNotFound() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(registroDatosService.obtener(REGISTRO_ID, NEGOCIO_ID))
                .thenThrow(new ResourceNotFoundException("El registro no existe"));

        assertThatThrownBy(() -> obtenerRegistro.execute(new ObtenerRegistroRequest(USUARIO_ID, REGISTRO_ID)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
