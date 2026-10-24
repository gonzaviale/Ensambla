package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.procesamiento.confirmarLectura.ConfirmarLectura;
import com.ensambla.backend.domain.useCase.procesamiento.consultarConsumo.ConsultarConsumo;
import com.ensambla.backend.domain.useCase.procesamiento.leerImagen.LeerImagen;
import com.ensambla.backend.domain.useCase.procesamiento.leerImagen.LeerImagenItem.CampoItem;
import com.ensambla.backend.domain.useCase.procesamiento.leerImagen.LeerImagenItem.DatoItem;
import com.ensambla.backend.domain.useCase.procesamiento.leerImagen.LeerImagenItem;
import com.ensambla.backend.domain.useCase.procesamiento.leerImagen.LeerImagenRequest;
import com.ensambla.backend.domain.useCase.procesamiento.obtenerLectura.ObtenerLectura;
import com.ensambla.backend.domain.useCase.procesamiento.reintentarLectura.ReintentarLectura;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.procesamiento.LecturaPresentationMapper;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LecturaControllerTest {

    private static final String PEDIDO = """
            {"nombre":"Ventas","destinoExistente":true,"contexto":"registros","tipo":"sales",
             "campos":[{"key":"monto","label":"Monto","type":"currency","required":true},
                       {"key":"cliente","label":"Cliente","type":"text","required":false}]}""";

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");

    private final LeerImagen leerImagen = mock(LeerImagen.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LecturaController controller = new LecturaController(
                mock(ObtenerLectura.class), mock(ReintentarLectura.class),
                mock(ConfirmarLectura.class), mock(ConsultarConsumo.class), leerImagen,
                new LecturaPresentationMapper());
        mockMvc = MockMvcDePrueba.standalone(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .defaultRequest(multipart("/api/importaciones/lectura").principal(
                        new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null, List.of())))
                .build();
    }

    @Test
    @DisplayName("POST devuelve los registros, las columnas nuevas y las dudas con la forma de la pantalla")
    void leerImagenShouldReturnReadingInTheScreenShape() throws Exception {
        givenLectura();

        ResultActions result = leer(foto(), pedido(PEDIDO));

        thenRespondeConLaLecturaDeLaPantalla(result);
    }

    @Test
    @DisplayName("POST manda al use case la imagen y el bloque de destino")
    void leerImagenShouldSendTheImageAndTheBlockToTheUseCase() throws Exception {
        when(leerImagen.execute(any())).thenReturn(new LeerImagenItem(List.of(), List.of(), List.of()));

        leer(foto(), pedido(PEDIDO)).andExpect(status().isOk());

        thenElUseCaseRecibioElBloqueVentas();
    }

    @Test
    @DisplayName("POST responde 400 con el mensaje cuando la foto no es del bloque o no es válida")
    void leerImagenShouldReturnBadRequestWhenTheBusinessRuleFails() throws Exception {
        when(leerImagen.execute(any()))
                .thenThrow(new BusinessRuleException("La estructura leída no es compatible con el bloque de destino"));

        leer(foto(), pedido(PEDIDO)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La estructura leída no es compatible con el bloque de destino"));
    }

    @Test
    @DisplayName("POST responde 502 con el mensaje cuando la IA no puede leer la foto")
    void leerImagenShouldReturnBadGatewayWhenTheReaderFails() throws Exception {
        when(leerImagen.execute(any())).thenThrow(new LecturaCuadernoException("No pudimos leer el cuaderno"));

        leer(foto(), pedido(PEDIDO)).andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("No pudimos leer el cuaderno"));
    }

    @Test
    @DisplayName("POST responde 400 sin llamar a la IA cuando el bloque no trae datos")
    void leerImagenShouldRejectABlockWithoutFields() throws Exception {
        String sinCampos = "{\"nombre\":\"Ventas\",\"destinoExistente\":false,\"campos\":[]}";

        leer(foto(), pedido(sinCampos)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['campos']").exists());

        verifyNoInteractions(leerImagen);
    }

    @Test
    @DisplayName("POST responde 400 cuando falta la imagen")
    void leerImagenShouldRejectAMissingImage() throws Exception {
        mockMvc.perform(multipart("/api/importaciones/lectura").file(pedido(PEDIDO)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(leerImagen);
    }

    // --- arrange ---
    private void givenLectura() {
        Map<String, DatoItem> fila = Map.of(
                "monto", new DatoItem("1500", false), "cliente", new DatoItem("Ana", true),
                "direccion", new DatoItem("Belgrano 1", false));
        when(leerImagen.execute(any())).thenReturn(new LeerImagenItem(List.of(fila),
                List.of(new CampoItem("direccion", "Dirección", "TEXTO", List.of())), List.of()));
    }

    private MockMultipartFile foto() {
        return new MockMultipartFile("imagen", "foto", "image/jpeg", new byte[]{7, 8, 9});
    }

    private MockMultipartFile pedido(String json) {
        return new MockMultipartFile("pedido", "pedido.json", "application/json", json.getBytes());
    }

    // --- act ---
    private ResultActions leer(MockMultipartFile imagen, MockMultipartFile pedido) throws Exception {
        return mockMvc.perform(multipart("/api/importaciones/lectura").file(imagen).file(pedido));
    }

    // --- assert ---
    private void thenRespondeConLaLecturaDeLaPantalla(ResultActions result) throws Exception {
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.filas[0].monto").value(1500.0))
                .andExpect(jsonPath("$.filas[0].cliente").value("Ana"))
                .andExpect(jsonPath("$.filas[0].direccion").value("Belgrano 1"))
                .andExpect(jsonPath("$.camposNuevos[0].key").value("direccion"))
                .andExpect(jsonPath("$.camposNuevos[0].label").value("Dirección"))
                .andExpect(jsonPath("$.camposNuevos[0].type").value("text"))
                .andExpect(jsonPath("$.camposNuevos[0].required").value(false))
                .andExpect(jsonPath("$.camposNuevos[0].options").doesNotExist())
                .andExpect(jsonPath("$.dudas[0].fila").value(0))
                .andExpect(jsonPath("$.dudas[0].campo").value("cliente"))
                .andExpect(jsonPath("$.descripcion").value(""));
    }

    private void thenElUseCaseRecibioElBloqueVentas() {
        ArgumentCaptor<LeerImagenRequest> captor = ArgumentCaptor.forClass(LeerImagenRequest.class);
        verify(leerImagen).execute(captor.capture());
        assertThat(captor.getValue().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(captor.getValue().contenido()).containsExactly(7, 8, 9);
        assertThat(captor.getValue().nombreBloque()).isEqualTo("Ventas");
        assertThat(captor.getValue().destinoExistente()).isTrue();
        assertThat(captor.getValue().campos()).extracting("clave", "tipo")
                .containsExactly(tuple("monto", "NUMERO"), tuple("cliente", "TEXTO"));
    }
}
