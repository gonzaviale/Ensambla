package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.objetivo.DatoUsado;
import com.ensambla.backend.domain.model.objetivo.FuenteDato;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivoPropuesto;
import com.ensambla.backend.domain.model.objetivo.ModoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.OrigenPropuesta;
import com.ensambla.backend.domain.model.objetivo.PropuestaObjetivo;
import com.ensambla.backend.domain.model.objetivo.RolMensaje;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.domain.model.plan.CuotaObjetivosIa;
import com.ensambla.backend.domain.useCase.objetivo.ObjetivoItem;
import com.ensambla.backend.domain.useCase.objetivo.guardarObjetivoConSami.GuardarObjetivoConSami;
import com.ensambla.backend.domain.useCase.objetivo.guardarObjetivoConSami.GuardarObjetivoConSamiRequest;
import com.ensambla.backend.domain.useCase.objetivo.proponerObjetivoConSami.ProponerObjetivoConSami;
import com.ensambla.backend.domain.useCase.objetivo.proponerObjetivoConSami.ProponerObjetivoConSamiRequest;
import com.ensambla.backend.domain.useCase.objetivo.proponerObjetivoConSami.ProponerObjetivoConSamiResponse;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.objetivo.ObjetivoPresentationMapper;
import com.ensambla.backend.infrastructure.presentation.mapper.objetivo.SamiObjetivoPresentationMapper;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static com.ensambla.backend.support.ConversacionObjetivoTestData.ENTENDIDO;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.OBJETIVO;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.RESPUESTA_IA;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidato;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivoSinMeta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SamiObjetivoControllerTest {

    private static final String PROPUESTA = "/api/objetivos/sami/propuesta";
    private static final String GUARDAR = "/api/objetivos/sami";
    private static final String CONVERSACION = "{\"mensajes\":[{\"rol\":\"PERSONA\",\"texto\":\"" + OBJETIVO + "\"}]}";

    private final ProponerObjetivoConSami proponerObjetivoConSami = mock(ProponerObjetivoConSami.class);
    private final GuardarObjetivoConSami guardarObjetivoConSami = mock(GuardarObjetivoConSami.class);
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SamiObjetivoController controller = new SamiObjetivoController(
                proponerObjetivoConSami, guardarObjetivoConSami,
                new SamiObjetivoPresentationMapper(new ObjetivoPresentationMapper()));
        mockMvc = MockMvcDePrueba.standalone(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST propuesta responde 200 con lo que contesta Sami, lo que entendió y los mini-objetivos")
    void proponerShouldReturnTheProposal() throws Exception {
        PropuestaObjetivo propuesta = new PropuestaObjetivo(RESPUESTA_IA, ENTENDIDO, false,
                List.of(MiniObjetivoPropuesto.medido(candidato())), OrigenPropuesta.IA);
        when(proponerObjetivoConSami.execute(any())).thenReturn(ProponerObjetivoConSamiResponse.de(
                propuesta, List.of(new DatoUsado(FuenteDato.ZONA, false)), 5, new CuotaObjetivosIa(2, 5)));

        ResultActions result = whenPropuesta(CONVERSACION);

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta").value(RESPUESTA_IA))
                .andExpect(jsonPath("$.entendido").value(ENTENDIDO))
                .andExpect(jsonPath("$.tipo").value("CON_DATOS"))
                .andExpect(jsonPath("$.esMonetario").value(false))
                .andExpect(jsonPath("$.origen").value("IA"))
                .andExpect(jsonPath("$.mensajesRestantes").value(5))
                .andExpect(jsonPath("$.puedeGuardar").value(true))
                .andExpect(jsonPath("$.minis[0].modo").value("MEDIDO"))
                .andExpect(jsonPath("$.minis[0].tipo").value("SUBIR_PRECIO"))
                .andExpect(jsonPath("$.minis[0].registroId").value(PRODUCTO_ID.toString()))
                .andExpect(jsonPath("$.minis[0].referencia").value(PRODUCTO))
                .andExpect(jsonPath("$.datos[0].fuente").value("ZONA"))
                .andExpect(jsonPath("$.datos[0].hay").value(false))
                .andExpect(jsonPath("$.cuota.usadas").value(2))
                .andExpect(jsonPath("$.cuota.limite").value(5))
                .andExpect(jsonPath("$.cuota.restantes").value(3));
    }

    @Test
    @DisplayName("POST propuesta manda al caso de uso el usuario de la sesión y los mensajes")
    void proponerShouldPassTheRequestToTheUseCase() throws Exception {
        when(proponerObjetivoConSami.execute(any())).thenReturn(ProponerObjetivoConSamiResponse.de(
                PropuestaObjetivo.respaldo(OBJETIVO), List.of(), 5, new CuotaObjetivosIa(0, 5)));

        whenPropuesta(CONVERSACION).andExpect(status().isOk());

        ArgumentCaptor<ProponerObjetivoConSamiRequest> captor =
                ArgumentCaptor.forClass(ProponerObjetivoConSamiRequest.class);
        verify(proponerObjetivoConSami).execute(captor.capture());
        assertThat(captor.getValue().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(captor.getValue().mensajes()).singleElement().satisfies(mensaje -> {
            assertThat(mensaje.rol()).isEqualTo(RolMensaje.PERSONA);
            assertThat(mensaje.texto()).isEqualTo(OBJETIVO);
        });
    }

    @Test
    @DisplayName("POST propuesta responde 400 sin mensajes, con un mensaje vacío o sin rol, y no llega al caso de uso")
    void proponerShouldReturn400ForAnInvalidConversation() throws Exception {
        whenPropuesta("{\"mensajes\":[]}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.mensajes").value("Contale a Sami qué querés lograr"));
        whenPropuesta("{}").andExpect(status().isBadRequest());
        whenPropuesta("{\"mensajes\":[{\"rol\":\"PERSONA\",\"texto\":\"  \"}]}").andExpect(status().isBadRequest());
        whenPropuesta("{\"mensajes\":[{\"texto\":\"Hola\"}]}").andExpect(status().isBadRequest());
        whenPropuesta("{\"mensajes\":[null]}").andExpect(status().isBadRequest());
        verifyNoInteractions(proponerObjetivoConSami);
    }

    @Test
    @DisplayName("POST propuesta responde 400 con un mensaje de más de 500 caracteres")
    void proponerShouldReturn400ForATooLongMessage() throws Exception {
        String largo = "a".repeat(501);

        whenPropuesta("{\"mensajes\":[{\"rol\":\"PERSONA\",\"texto\":\"" + largo + "\"}]}")
                .andExpect(status().isBadRequest());
        verifyNoInteractions(proponerObjetivoConSami);
    }

    @Test
    @DisplayName("POST propuesta responde 400 cuando la conversación llegó al tope de mensajes")
    void proponerShouldReturn400WhenTheConversationIsFull() throws Exception {
        when(proponerObjetivoConSami.execute(any())).thenThrow(new BusinessRuleException("Llegaste al máximo"));

        whenPropuesta(CONVERSACION).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST guardar responde 201 con el objetivo sin meta y la ubicación del nuevo")
    void guardarShouldReturnCreatedWithTheObjetivo() throws Exception {
        when(guardarObjetivoConSami.execute(any())).thenReturn(ObjetivoItem.de(objetivoSinMeta()));

        ResultActions result = whenGuardar("{\"texto\":\"" + OBJETIVO + "\",\"minis\":[]}");

        result.andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/objetivos/" + OBJETIVO_ID))
                .andExpect(jsonPath("$.id").value(OBJETIVO_ID.toString()))
                .andExpect(jsonPath("$.tipo").value("SIN_DATOS"))
                .andExpect(jsonPath("$.meta").doesNotExist())
                .andExpect(jsonPath("$.armadoConSami").value(true));
    }

    @Test
    @DisplayName("POST guardar manda al caso de uso el usuario de la sesión, el texto y los mini-objetivos elegidos")
    void guardarShouldPassTheRequestToTheUseCase() throws Exception {
        when(guardarObjetivoConSami.execute(any())).thenReturn(ObjetivoItem.de(objetivoSinMeta()));

        whenGuardar("{\"texto\":\"" + OBJETIVO + "\",\"seguimientoManual\":false,\"minis\":["
                + "{\"modo\":\"MEDIDO\",\"tipo\":\"SUBIR_PRECIO\",\"registroId\":\"" + PRODUCTO_ID + "\","
                + "\"referencia\":\"" + PRODUCTO + "\"},"
                + "{\"modo\":\"MANUAL\",\"titulo\":\"Un cartel\",\"justificacion\":\"Que te vean\"}]}")
                .andExpect(status().isCreated());

        ArgumentCaptor<GuardarObjetivoConSamiRequest> captor =
                ArgumentCaptor.forClass(GuardarObjetivoConSamiRequest.class);
        verify(guardarObjetivoConSami).execute(captor.capture());
        assertThat(captor.getValue().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(captor.getValue().texto()).isEqualTo(OBJETIVO);
        assertThat(captor.getValue().seguimientoManual()).isFalse();
        assertThat(captor.getValue().minis()).hasSize(2);
        assertThat(captor.getValue().minis().get(0).modo()).isEqualTo(ModoMiniObjetivo.MEDIDO);
        assertThat(captor.getValue().minis().get(0).tipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(captor.getValue().minis().get(0).registroId()).isEqualTo(PRODUCTO_ID);
        assertThat(captor.getValue().minis().get(1).titulo()).isEqualTo("Un cartel");
    }

    @Test
    @DisplayName("POST guardar ignora el negocio del body: el caso de uso sólo recibe el usuario de la sesión")
    void guardarShouldIgnoreTheNegocioOfTheBody() throws Exception {
        when(guardarObjetivoConSami.execute(any())).thenReturn(ObjetivoItem.de(objetivoSinMeta()));

        whenGuardar("{\"texto\":\"" + OBJETIVO + "\",\"seguimientoManual\":true,"
                + "\"negocioId\":\"00000000-0000-4000-8000-0000000000ff\"}").andExpect(status().isCreated());

        ArgumentCaptor<GuardarObjetivoConSamiRequest> captor =
                ArgumentCaptor.forClass(GuardarObjetivoConSamiRequest.class);
        verify(guardarObjetivoConSami).execute(captor.capture());
        assertThat(captor.getValue().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(captor.getValue().seguimientoManual()).isTrue();
    }

    @Test
    @DisplayName("POST guardar responde 400 sin texto, con más de 3 mini-objetivos o con uno sin modo")
    void guardarShouldReturn400ForAnInvalidBody() throws Exception {
        String mini = "{\"modo\":\"MANUAL\",\"titulo\":\"a\",\"justificacion\":\"b\"}";

        whenGuardar("{\"minis\":[]}").andExpect(status().isBadRequest());
        whenGuardar("{\"texto\":\"  \",\"minis\":[]}").andExpect(status().isBadRequest());
        whenGuardar("{\"texto\":\"" + "a".repeat(501) + "\",\"minis\":[]}").andExpect(status().isBadRequest());
        whenGuardar("{\"texto\":\"x\",\"minis\":[" + mini + "," + mini + "," + mini + "," + mini + "]}")
                .andExpect(status().isBadRequest());
        whenGuardar("{\"texto\":\"x\",\"minis\":[{\"titulo\":\"a\"}]}").andExpect(status().isBadRequest());
        whenGuardar("{\"texto\":\"x\",\"minis\":[{\"modo\":\"MANUAL\",\"titulo\":\"" + "a".repeat(81) + "\"}]}")
                .andExpect(status().isBadRequest());
        whenGuardar("{\"texto\":\"x\",\"minis\":[null]}").andExpect(status().isBadRequest());
        verifyNoInteractions(guardarObjetivoConSami);
    }

    @Test
    @DisplayName("POST guardar responde 400 cuando el caso de uso rechaza los mini-objetivos")
    void guardarShouldReturn400WhenTheUseCaseRejectsTheMinis() throws Exception {
        when(guardarObjetivoConSami.execute(any()))
                .thenThrow(new BusinessRuleException("Ese mini-objetivo ya no está disponible"));

        whenGuardar("{\"texto\":\"x\",\"minis\":[]}").andExpect(status().isBadRequest());
    }

    // --- act ---
    private ResultActions whenPropuesta(String cuerpo) throws Exception {
        return mockMvc.perform(post(PROPUESTA).principal(principal)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private ResultActions whenGuardar(String cuerpo) throws Exception {
        return mockMvc.perform(post(GUARDAR).principal(principal)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }
}
