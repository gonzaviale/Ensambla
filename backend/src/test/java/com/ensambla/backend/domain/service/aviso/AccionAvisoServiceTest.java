package com.ensambla.backend.domain.service.aviso;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import com.ensambla.backend.support.AvisoTestData;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccionAvisoServiceTest {

    private static final UUID NEGOCIO_ID = AvisoTestData.NEGOCIO_ID;
    private static final UUID REGISTRO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID CAMPO_PRECIO_ID = UUID.fromString("f0000000-0000-4000-8000-000000000009");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");

    @Mock
    private RegistroDatosService registroDatosService;

    @Mock
    private CampoService campoService;

    @Spy
    private TransaccionPort transaccionPort = new TransaccionDirecta();

    @InjectMocks
    private AccionAvisoService accionAvisoService;

    @Test
    @DisplayName("Editar registro cambia los valores del registro usando la clave del campo")
    void ejecutarShouldEditRegistroByCampoClave() {
        Registro registro = AvisoTestData.registro(REGISTRO_ID, "Yerba", 2, 5);
        List<Campo> campos = List.of(AvisoTestData.campo(CAMPO_PRECIO_ID, "price"),
                AvisoTestData.campo(AvisoTestData.CAMPO_NOMBRE_ID, "name"));
        when(registroDatosService.obtener(REGISTRO_ID, NEGOCIO_ID)).thenReturn(registro);
        when(campoService.listarDeBloque(AvisoTestData.INVENTARIO_ID, NEGOCIO_ID)).thenReturn(campos);

        accionAvisoService.ejecutar(givenAviso(Map.of("tipo", "EDITAR_REGISTRO",
                "registroId", REGISTRO_ID.toString(), "valores", Map.of("price", 4200))), AHORA);

        verify(registroDatosService).editar(registro, campos, Map.of(CAMPO_PRECIO_ID.toString(), 4200), AHORA);
    }

    @Test
    @DisplayName("Las acciones que sólo abren pantallas no tocan ningún dato")
    void ejecutarShouldDoNothingForNavigationActions() {
        accionAvisoService.ejecutar(givenAviso(Map.of("tipo", "REGISTRAR", "bloqueId", "x"), "texto suelto"), AHORA);

        verifyNoInteractions(registroDatosService, campoService);
    }

    @Test
    @DisplayName("Un aviso sin acciones no toca ningún dato")
    void ejecutarShouldDoNothingWhenAvisoHasNoActions() {
        accionAvisoService.ejecutar(givenAviso(), AHORA);

        verifyNoInteractions(registroDatosService, campoService);
    }

    @Test
    @DisplayName("Rechaza una acción de edición con un registro que no es un id")
    void ejecutarShouldThrowWhenRegistroIdIsInvalid() {
        Aviso aviso = givenAviso(Map.of("tipo", "EDITAR_REGISTRO", "registroId", "no-es-un-id",
                "valores", Map.of("price", 1)));

        assertThatThrownBy(() -> accionAvisoService.ejecutar(aviso, AHORA))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La acción del aviso no indica qué registro cambiar");
    }

    @Test
    @DisplayName("Rechaza una acción de edición sin valores")
    void ejecutarShouldThrowWhenValoresAreMissing() {
        givenRegistroYCampos();
        Aviso aviso = givenAviso(Map.of("tipo", "EDITAR_REGISTRO", "registroId", REGISTRO_ID.toString()));

        assertThatThrownBy(() -> accionAvisoService.ejecutar(aviso, AHORA))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La acción del aviso no indica qué valores cambiar");
    }

    @Test
    @DisplayName("Rechaza una acción de edición con valores vacíos")
    void ejecutarShouldThrowWhenValoresAreEmpty() {
        givenRegistroYCampos();
        Aviso aviso = givenAviso(Map.of("tipo", "EDITAR_REGISTRO", "registroId", REGISTRO_ID.toString(),
                "valores", Map.of()));

        assertThatThrownBy(() -> accionAvisoService.ejecutar(aviso, AHORA))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Rechaza una acción cuyos campos ya no existen en la tabla")
    void ejecutarShouldThrowWhenCamposNoLongerExist() {
        givenRegistroYCampos();
        Aviso aviso = givenAviso(Map.of("tipo", "EDITAR_REGISTRO", "registroId", REGISTRO_ID.toString(),
                "valores", Map.of("costo-que-no-existe", 1)));

        assertThatThrownBy(() -> accionAvisoService.ejecutar(aviso, AHORA))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La tabla ya no tiene los campos que el aviso quería cambiar");
        verify(registroDatosService, org.mockito.Mockito.never()).editar(any(), any(), any(), any());
    }

    private void givenRegistroYCampos() {
        when(registroDatosService.obtener(REGISTRO_ID, NEGOCIO_ID))
                .thenReturn(AvisoTestData.registro(REGISTRO_ID, "Yerba", 2, 5));
        when(campoService.listarDeBloque(AvisoTestData.INVENTARIO_ID, NEGOCIO_ID))
                .thenReturn(List.of(AvisoTestData.campo(CAMPO_PRECIO_ID, "price")));
    }

    private static Aviso givenAviso(Object... acciones) {
        return Aviso.builder().id(UUID.randomUUID()).negocioId(NEGOCIO_ID).acciones(List.of(acciones)).build();
    }
}
