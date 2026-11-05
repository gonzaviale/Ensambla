package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.promocion.redactarPromocion.RedactarPromocion;
import com.ensambla.backend.domain.useCase.promocion.redactarPromocion.RedactarPromocionRequest;
import com.ensambla.backend.domain.useCase.promocion.redactarPromocion.RedactarPromocionRequest.ProductoARedactar;
import com.ensambla.backend.domain.useCase.promocion.redactarPromocion.RedactarPromocionResponse;
import com.ensambla.backend.domain.useCase.promocion.sugerirPromociones.CanalItem;
import com.ensambla.backend.domain.useCase.promocion.sugerirPromociones.ProductoPromocionadoItem;
import com.ensambla.backend.domain.useCase.promocion.sugerirPromociones.PromocionItem;
import com.ensambla.backend.domain.useCase.promocion.sugerirPromociones.SugerirPromociones;
import com.ensambla.backend.domain.useCase.promocion.sugerirPromociones.SugerirPromocionesRequest;
import com.ensambla.backend.domain.useCase.promocion.sugerirPromociones.SugerirPromocionesResponse;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.promocion.PromocionPresentationMapper;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

import static com.ensambla.backend.support.PromocionDePrueba.PROYECTO_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PromocionControllerTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final String RUTA_SUGERENCIAS = "/api/proyectos/{id}/promociones";
    private static final String RUTA_TEXTO = "/api/promociones/texto";
    private static final String CUERPO_VALIDO = "{\"canal\":\"estado\",\"clave\":\"estrella\",\"titulo\":\"Tu producto estrella\","
            + "\"gancho\":\"Lo más vendido\",\"productos\":[{\"nombre\":\"Yerba\",\"precio\":3600}]}";

    private final Principal sesion = new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);

    private SugerirPromociones sugerirPromociones;
    private RedactarPromocion redactarPromocion;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        sugerirPromociones = mock(SugerirPromociones.class);
        redactarPromocion = mock(RedactarPromocion.class);
        mockMvc = MockMvcDePrueba.standalone(
                        new PromocionController(sugerirPromociones, redactarPromocion, new PromocionPresentationMapper()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET devuelve las promociones sugeridas y los canales")
    void sugerirShouldReturnThePromotionsAndTheChannels() throws Exception {
        givenUnaPromocionYUnCanal();

        ResultActions result = mockMvc.perform(get(RUTA_SUGERENCIAS, PROYECTO_ID).principal(sesion));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.promociones[0].clave").value("estrella"))
                .andExpect(jsonPath("$.promociones[0].titulo").value("Tu producto estrella"))
                .andExpect(jsonPath("$.promociones[0].productos[0].nombre").value("Yerba"))
                .andExpect(jsonPath("$.promociones[0].productos[0].precio").value(3600))
                .andExpect(jsonPath("$.canales[0].clave").value("instagram"));
        verify(sugerirPromociones).execute(new SugerirPromocionesRequest(USUARIO_ID, PROYECTO_ID));
    }

    @Test
    @DisplayName("GET de un sistema de otro negocio responde 404")
    void sugerirShouldReturn404ForAForeignProject() throws Exception {
        when(sugerirPromociones.execute(any(SugerirPromocionesRequest.class)))
                .thenThrow(new ResourceNotFoundException("Proyecto no encontrado"));

        ResultActions result = mockMvc.perform(get(RUTA_SUGERENCIAS, PROYECTO_ID).principal(sesion));

        result.andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET responde 400 cuando el id del sistema no es un UUID")
    void sugerirShouldReturn400WhenTheIdIsNotAUuid() throws Exception {
        ResultActions result = mockMvc.perform(get(RUTA_SUGERENCIAS, "id-invalido").principal(sesion));

        result.andExpect(status().isBadRequest());
        verifyNoInteractions(sugerirPromociones);
    }

    @Test
    @DisplayName("POST devuelve el texto escrito y pasa el canal y la promoción al caso de uso")
    void redactarShouldReturnTheWrittenText() throws Exception {
        when(redactarPromocion.execute(any(RedactarPromocionRequest.class)))
                .thenReturn(new RedactarPromocionResponse("*Tu producto estrella*"));

        ResultActions result = whenRedacta(CUERPO_VALIDO);

        result.andExpect(status().isOk()).andExpect(jsonPath("$.texto").value("*Tu producto estrella*"));
        verify(redactarPromocion).execute(new RedactarPromocionRequest(USUARIO_ID, "estado", "estrella",
                "Tu producto estrella", "Lo más vendido",
                List.of(new ProductoARedactar("Yerba", new BigDecimal("3600")))));
    }

    @Test
    @DisplayName("POST responde 400 cuando no hay productos")
    void redactarShouldReturn400WhenThereAreNoProducts() throws Exception {
        ResultActions result = whenRedacta("{\"canal\":\"estado\",\"clave\":\"estrella\",\"titulo\":\"T\","
                + "\"gancho\":\"G\",\"productos\":[]}");

        result.andExpect(status().isBadRequest());
        verifyNoInteractions(redactarPromocion);
    }

    @Test
    @DisplayName("POST responde 400 cuando un precio no es positivo")
    void redactarShouldReturn400WhenAPriceIsNotPositive() throws Exception {
        ResultActions result = whenRedacta("{\"canal\":\"estado\",\"clave\":\"estrella\",\"titulo\":\"T\","
                + "\"gancho\":\"G\",\"productos\":[{\"nombre\":\"Yerba\",\"precio\":0}]}");

        result.andExpect(status().isBadRequest());
        verifyNoInteractions(redactarPromocion);
    }

    @Test
    @DisplayName("POST responde 400 cuando el caso de uso rechaza el canal")
    void redactarShouldReturn400WhenTheUseCaseRejectsTheChannel() throws Exception {
        when(redactarPromocion.execute(any(RedactarPromocionRequest.class)))
                .thenThrow(new BusinessRuleException("No conocemos ese canal para compartir"));

        ResultActions result = whenRedacta(CUERPO_VALIDO);

        result.andExpect(status().isBadRequest());
    }

    // --- arrange ---
    private void givenUnaPromocionYUnCanal() {
        PromocionItem estrella = new PromocionItem("estrella", "Tu producto estrella", "Lo más vendido",
                List.of(new ProductoPromocionadoItem("Yerba", new BigDecimal("3600"))));
        when(sugerirPromociones.execute(any(SugerirPromocionesRequest.class))).thenReturn(
                new SugerirPromocionesResponse(List.of(estrella), List.of(new CanalItem("instagram", "Instagram"))));
    }

    // --- act ---
    private ResultActions whenRedacta(String cuerpo) throws Exception {
        return mockMvc.perform(post(RUTA_TEXTO).principal(sesion)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }
}
