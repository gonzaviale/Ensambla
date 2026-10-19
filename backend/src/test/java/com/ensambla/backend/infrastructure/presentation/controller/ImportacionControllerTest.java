package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.dto.lectura.LeerImagenItem;
import com.ensambla.backend.domain.dto.lectura.LeerImagenRequest;
import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.DatoLeido;
import com.ensambla.backend.domain.useCase.lectura.LeerImagenUseCase;
import com.ensambla.backend.infrastructure.ia.IaProperties;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.importacion.ImportacionPresentationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;
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

class ImportacionControllerTest {

    private static final String PEDIDO = """
            {"nombre":"Ventas","destinoExistente":true,"contexto":"registros","tipo":"sales",
             "campos":[{"key":"monto","label":"Monto","type":"currency","required":true},
                       {"key":"cliente","label":"Cliente","type":"text","required":false}]}""";

    private final LeerImagenUseCase leerImagenUseCase = mock(LeerImagenUseCase.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ImportacionController controller = new ImportacionController(leerImagenUseCase,
                new ImportacionPresentationMapper(new IaProperties("simulado", "", "", Duration.ofSeconds(60), 10, 0.8)));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST devuelve los registros, las columnas nuevas y las dudas con la forma de la pantalla")
    void leerShouldReturnReadingInTheScreenShape() throws Exception {
        when(leerImagenUseCase.execute(any())).thenReturn(new LeerImagenItem(
                List.of(Map.of("monto", new DatoLeido("1500", 0.95), "cliente", new DatoLeido("Ana", 0.3),
                        "direccion", new DatoLeido("Belgrano 1", 0.9))),
                List.of(new CampoPropuesto("direccion", "Dirección", TipoCampo.TEXTO, List.of())), List.of()));

        ResultActions result = leer(foto("image/jpeg"), pedido(PEDIDO));

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

    @Test
    @DisplayName("POST manda al use case la imagen, su tipo y el bloque de destino")
    void leerShouldSendTheImageAndTheBlockToTheUseCase() throws Exception {
        when(leerImagenUseCase.execute(any())).thenReturn(new LeerImagenItem(List.of(), List.of(), List.of()));

        leer(foto("image/png"), pedido(PEDIDO)).andExpect(status().isOk());

        ArgumentCaptor<LeerImagenRequest> captor = ArgumentCaptor.forClass(LeerImagenRequest.class);
        verify(leerImagenUseCase).execute(captor.capture());
        assertThat(captor.getValue().contenido()).containsExactly(7, 8, 9);
        assertThat(captor.getValue().tipoContenido()).isEqualTo("image/png");
        assertThat(captor.getValue().nombreBloque()).isEqualTo("Ventas");
        assertThat(captor.getValue().destinoExistente()).isTrue();
        assertThat(captor.getValue().campos()).extracting("clave", "tipo")
                .containsExactly(tuple("monto", TipoCampo.NUMERO),
                        tuple("cliente", TipoCampo.TEXTO));
    }

    @Test
    @DisplayName("POST responde 400 con el mensaje cuando la foto no es del bloque o no es válida")
    void leerShouldReturnBadRequestWhenTheBusinessRuleFails() throws Exception {
        when(leerImagenUseCase.execute(any()))
                .thenThrow(new BusinessRuleException("La estructura leída no es compatible con el bloque de destino"));

        leer(foto("image/jpeg"), pedido(PEDIDO)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La estructura leída no es compatible con el bloque de destino"));
    }

    @Test
    @DisplayName("POST responde 502 con el mensaje cuando la IA no puede leer la foto")
    void leerShouldReturnBadGatewayWhenTheReaderFails() throws Exception {
        when(leerImagenUseCase.execute(any())).thenThrow(new LecturaCuadernoException("No pudimos leer el cuaderno"));

        leer(foto("image/jpeg"), pedido(PEDIDO)).andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("No pudimos leer el cuaderno"));
    }

    @Test
    @DisplayName("POST responde 400 sin llamar a la IA cuando el bloque no trae datos")
    void leerShouldRejectABlockWithoutFields() throws Exception {
        leer(foto("image/jpeg"), pedido("{\"nombre\":\"Ventas\",\"destinoExistente\":false,\"campos\":[]}")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['campos']").exists());

        verifyNoInteractions(leerImagenUseCase);
    }

    @Test
    @DisplayName("POST responde 400 cuando falta la imagen")
    void leerShouldRejectAMissingImage() throws Exception {
        mockMvc.perform(multipart("/api/importaciones/lectura").file(pedido(PEDIDO)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(leerImagenUseCase);
    }

    private ResultActions leer(MockMultipartFile imagen, MockMultipartFile pedido) throws Exception {
        return mockMvc.perform(multipart("/api/importaciones/lectura").file(imagen).file(pedido));
    }

    private MockMultipartFile foto(String tipo) {
        return new MockMultipartFile("imagen", "foto", tipo, new byte[]{7, 8, 9});
    }

    private MockMultipartFile pedido(String json) {
        return new MockMultipartFile("pedido", "pedido.json", "application/json", json.getBytes());
    }
}
