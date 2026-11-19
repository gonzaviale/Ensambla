package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.model.plantilla.PlantillaId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.sami.taller.ImagenTaller;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.proyecto.ProyectoItem;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.ConversarConSami;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.ConversarConSamiRequest;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.ConversarConSamiResponse;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.OpcionSamiItem;
import com.ensambla.backend.domain.useCase.sami.conversarConSami.PreguntaSamiItem;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.ConversarEnTaller;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.ConversarEnTallerRequest;
import com.ensambla.backend.domain.useCase.sami.conversarEnTaller.ConversarEnTallerResponse;
import com.ensambla.backend.domain.useCase.sami.crearProyectoDesdeSami.CrearProyectoDesdeSami;
import com.ensambla.backend.domain.useCase.sami.crearProyectoDesdeSami.CrearProyectoDesdeSamiRequest;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.proyecto.ProyectoPresentationMapper;
import com.ensambla.backend.infrastructure.presentation.mapper.sami.SamiPresentationMapper;
import com.ensambla.backend.infrastructure.presentation.mapper.sami.TallerPresentationMapper;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SamiControllerTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.nuevo();
    private static final PlantillaId PLANTILLA_ID = PlantillaId.nuevo();
    private static final ProyectoId PROYECTO_ID = ProyectoId.nuevo();

    private ConversarConSami conversarConSami;
    private CrearProyectoDesdeSami crearProyectoDesdeSami;
    private ConversarEnTaller conversarEnTaller;

    private MockMvc mockMvc;

    private ResultActions resultadoDeLaLlamada;
    private ResultActions segundaResultadoDeLaLlamada;
    private ResultActions terceraResultadoDeLaLlamada;
    private ResultActions result;
    private String datos;

    @BeforeEach
    void setUp() {
        conversarConSami = mock(ConversarConSami.class);
        crearProyectoDesdeSami = mock(CrearProyectoDesdeSami.class);
        conversarEnTaller = mock(ConversarEnTaller.class);
        mockMvc = MockMvcDePrueba.standalone(new SamiController(
                        conversarConSami,
                        crearProyectoDesdeSami,
                        conversarEnTaller,
                        new SamiPresentationMapper(),
                        new ProyectoPresentationMapper(),
                        new TallerPresentationMapper()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    @DisplayName("Un turno del taller devuelve la respuesta, las acciones y la botonera, con el usuario de la sesión")
    void unTurnoDelTallerDevuelveLaRespuestaLasAccionesYLaBotoneraConElUsuarioDeLaSesion() throws Exception {
        dadoElEscenarioDeUnTurnoDelTallerDevuelveLaRespuestaLasAccionesYLaBotoneraConElUsuarioDeLaSesion();

        cuandoSeEjecutaUnTurnoDelTallerDevuelveLaRespuestaLasAccionesYLaBotoneraConElUsuarioDeLaSesion();

        entoncesSeVerificaUnTurnoDelTallerDevuelveLaRespuestaLasAccionesYLaBotoneraConElUsuarioDeLaSesion();
    }

    @Test
    @DisplayName("Sin historial ni opciones el turno igual responde")
    void sinHistorialNiOpcionesElTurnoIgualResponde() throws Exception {
        dadoElEscenarioDeSinHistorialNiOpcionesElTurnoIgualResponde();

        cuandoSeEjecutaSinHistorialNiOpcionesElTurnoIgualResponde();

        entoncesSeVerificaSinHistorialNiOpcionesElTurnoIgualResponde();
    }

    @Test
    @DisplayName("Rechaza con 400 un mensaje vacío, un pedido sin estado o un historial con un autor inválido")
    void rechazaCon400UnMensajeVacioUnPedidoSinEstadoOUnHistorialConUnAutorInvalido() throws Exception {
        cuandoSeEjecutaRechazaCon400UnMensajeVacioUnPedidoSinEstadoOUnHistorialConUnAutorInvalido();

        entoncesSeVerificaRechazaCon400UnMensajeVacioUnPedidoSinEstadoOUnHistorialConUnAutorInvalido();
    }

    @Test
    @DisplayName("Un turno con foto le pasa al use case la imagen decodificada y su tipo")
    void unTurnoConFotoLePasaAlUseCaseLaImagenDecodificadaYSuTipo() throws Exception {
        dadoElEscenarioDeUnTurnoConFotoLePasaAlUseCaseLaImagenDecodificadaYSuTipo();

        cuandoSeEjecutaUnTurnoConFotoLePasaAlUseCaseLaImagenDecodificadaYSuTipo();

        entoncesSeVerificaUnTurnoConFotoLePasaAlUseCaseLaImagenDecodificadaYSuTipo();
    }

    @Test
    @DisplayName("Rechaza con 400 y un mensaje para la persona una foto de un tipo que no va")
    void rechazaCon400YUnMensajeParaLaPersonaUnaFotoDeUnTipoQueNoVa() throws Exception {
        cuandoSeEjecutaRechazaCon400YUnMensajeParaLaPersonaUnaFotoDeUnTipoQueNoVa();

        entoncesSeVerificaRechazaCon400YUnMensajeParaLaPersonaUnaFotoDeUnTipoQueNoVa();
    }

    @Test
    @DisplayName("Rechaza con 400 y un mensaje para la persona una foto de más de 5 MB")
    void rechazaCon400YUnMensajeParaLaPersonaUnaFotoDeMasDe5MB() throws Exception {
        dadoElEscenarioDeRechazaCon400YUnMensajeParaLaPersonaUnaFotoDeMasDe5MB();

        cuandoSeEjecutaRechazaCon400YUnMensajeParaLaPersonaUnaFotoDeMasDe5MB();

        entoncesSeVerificaRechazaCon400YUnMensajeParaLaPersonaUnaFotoDeMasDe5MB();
    }

    @Test
    @DisplayName("Si el límite diario se agotó responde 400 con el mensaje para la persona")
    void siElLimiteDiarioSeAgotoResponde400ConElMensajeParaLaPersona() throws Exception {
        dadoElEscenarioDeSiElLimiteDiarioSeAgotoResponde400ConElMensajeParaLaPersona();

        cuandoSeEjecutaSiElLimiteDiarioSeAgotoResponde400ConElMensajeParaLaPersona();

        entoncesSeVerificaSiElLimiteDiarioSeAgotoResponde400ConElMensajeParaLaPersona();
    }

    @Test
    @DisplayName("Devuelve la pregunta de Sami y le pasa el usuario de la sesión, nunca uno del body")
    void devuelveLaPreguntaDeSamiYLePasaElUsuarioDeLaSesionNuncaUnoDelBody() throws Exception {
        dadoElEscenarioDeDevuelveLaPreguntaDeSamiYLePasaElUsuarioDeLaSesionNuncaUnoDelBody();

        cuandoSeEjecutaDevuelveLaPreguntaDeSamiYLePasaElUsuarioDeLaSesionNuncaUnoDelBody();

        entoncesSeVerificaDevuelveLaPreguntaDeSamiYLePasaElUsuarioDeLaSesionNuncaUnoDelBody();
    }

    @Test
    @DisplayName("Rechaza con 400 un texto vacío")
    void rechazaCon400UnTextoVacio() throws Exception {
        cuandoSeEjecutaRechazaCon400UnTextoVacio();

        entoncesSeVerificaRechazaCon400UnTextoVacio();
    }

    @Test
    @DisplayName("Rechaza con 400 una respuesta sin opción")
    void rechazaCon400UnaRespuestaSinOpcion() throws Exception {
        cuandoSeEjecutaRechazaCon400UnaRespuestaSinOpcion();

        entoncesSeVerificaRechazaCon400UnaRespuestaSinOpcion();
    }

    @Test
    @DisplayName("Crea el proyecto con las columnas confirmadas, responde 201 con su ubicación y usa la sesión")
    void creaElProyectoConLasColumnasConfirmadasResponde201ConSuUbicacionYUsaLaSesion() throws Exception {
        dadoElEscenarioDeCreaElProyectoConLasColumnasConfirmadasResponde201ConSuUbicacionYUsaLaSesion();

        cuandoSeEjecutaCreaElProyectoConLasColumnasConfirmadasResponde201ConSuUbicacionYUsaLaSesion();

        entoncesSeVerificaCreaElProyectoConLasColumnasConfirmadasResponde201ConSuUbicacionYUsaLaSesion();
    }

    @Test
    @DisplayName("Rechaza con 400 un pedido sin plantilla o sin lista de columnas")
    void rechazaCon400UnPedidoSinPlantillaOSinListaDeColumnas() throws Exception {
        cuandoSeEjecutaRechazaCon400UnPedidoSinPlantillaOSinListaDeColumnas();

        entoncesSeVerificaRechazaCon400UnPedidoSinPlantillaOSinListaDeColumnas();
    }

    @Test
    @DisplayName("Rechaza con 400 una columna sin nombre o sin tipo")
    void rechazaCon400UnaColumnaSinNombreOSinTipo() throws Exception {
        cuandoPosting("/api/sami/proyectos", """
                {"plantillaId":"%s","columnas":[{"nombre":"","tipo":"TEXTO"}]}
                """.formatted(PLANTILLA_ID)).andExpect(status().isBadRequest());
        cuandoPosting("/api/sami/proyectos", """
                {"plantillaId":"%s","columnas":[{"nombre":"Saldo"}]}
                """.formatted(PLANTILLA_ID)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Si una columna no es válida responde 400 con el error ubicado en la columna")
    void siUnaColumnaNoEsValidaResponde400ConElErrorUbicadoEnLaColumna() throws Exception {
        dadoElEscenarioDeSiUnaColumnaNoEsValidaResponde400ConElErrorUbicadoEnLaColumna();

        cuandoSeEjecutaSiUnaColumnaNoEsValidaResponde400ConElErrorUbicadoEnLaColumna();

        entoncesSeVerificaSiUnaColumnaNoEsValidaResponde400ConElErrorUbicadoEnLaColumna();
    }

    private ProyectoItem proyecto() {
        return new ProyectoItem(PROYECTO_ID, "Velas", null, PLANTILLA_ID,
                OffsetDateTime.parse("2026-10-19T12:00:00Z"), OffsetDateTime.parse("2026-10-19T12:00:00Z"), List.of());
    }

    private ResultActions cuandoPosting(String url, String json) throws Exception {
        return mockMvc.perform(post(url)
                .principal(new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), "n/a"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    /** Un turno del taller con el sistema vacío y una foto adjunta. */
    private String conImagen(String tipo, String datos) {
        return """
                {"mensaje":"te paso la foto","estado":{"distribucion":"mosaic","piezas":[]},
                 "imagen":{"tipo":"%s","datos":"%s"}}
                """.formatted(tipo, datos);
    }

    // --- dado ---

    private void dadoElEscenarioDeUnTurnoDelTallerDevuelveLaRespuestaLasAccionesYLaBotoneraConElUsuarioDeLaSesion() throws Exception {
        when(conversarEnTaller.execute(any())).thenReturn(new ConversarEnTallerResponse(
                "Listo, sumé gastos.",
                List.of(new ConversarEnTallerResponse.AccionTallerItem("agregar_pieza", Map.of("tipo", "expenses"))),
                List.of(new ConversarEnTallerResponse.OpcionesTallerItem(null, "¿Seguimos?", List.of("Sí", "No")))));
    }

    private void dadoElEscenarioDeSinHistorialNiOpcionesElTurnoIgualResponde() throws Exception {
        when(conversarEnTaller.execute(any()))
                .thenReturn(new ConversarEnTallerResponse("Hola", List.of(), List.of()));
    }

    private void dadoElEscenarioDeUnTurnoConFotoLePasaAlUseCaseLaImagenDecodificadaYSuTipo() throws Exception {
        when(conversarEnTaller.execute(any())).thenReturn(new ConversarEnTallerResponse("Leí 3", List.of(), List.of()));
    }

    private void dadoElEscenarioDeRechazaCon400YUnMensajeParaLaPersonaUnaFotoDeMasDe5MB() throws Exception {
        datos = Base64.getEncoder().encodeToString(new byte[ImagenTaller.MAX_BYTES + 1]);
    }

    private void dadoElEscenarioDeSiElLimiteDiarioSeAgotoResponde400ConElMensajeParaLaPersona() throws Exception {
        when(conversarEnTaller.execute(any()))
                .thenThrow(new BusinessRuleException("Hoy ya usaste todas las consultas con IA."));
    }

    private void dadoElEscenarioDeDevuelveLaPreguntaDeSamiYLePasaElUsuarioDeLaSesionNuncaUnoDelBody() throws Exception {
        when(conversarConSami.execute(any())).thenReturn(new ConversarConSamiResponse("PREGUNTA",
                new PreguntaSamiItem("principal", "¿Qué querés anotar?", List.of(new OpcionSamiItem("ventas", "Vender"))),
                null, 0));
    }

    private void dadoElEscenarioDeCreaElProyectoConLasColumnasConfirmadasResponde201ConSuUbicacionYUsaLaSesion() throws Exception {
        when(crearProyectoDesdeSami.execute(any())).thenReturn(proyecto());
    }

    private void dadoElEscenarioDeSiUnaColumnaNoEsValidaResponde400ConElErrorUbicadoEnLaColumna() throws Exception {
        when(crearProyectoDesdeSami.execute(any())).thenThrow(
                new CampoInvalidoException(Map.of("columnas[0].nombre", "Escribí un nombre")));
    }

    // --- cuando ---

    private void cuandoSeEjecutaUnTurnoDelTallerDevuelveLaRespuestaLasAccionesYLaBotoneraConElUsuarioDeLaSesion() throws Exception {
        result = cuandoPosting("/api/sami/taller", """
                {"mensaje":"  sumá gastos ","usuarioId":"otro",
                 "estado":{"nombre":"Mi almacén","distribucion":"mosaic","piezas":[
                   {"id":"e1","tipo":"inventory","nombre":"Inventario","seccion":"General",
                    "campos":[{"etiqueta":"Producto","tipo":"text"}]}]},
                 "historial":[{"quien":"sami","texto":"Hola"},{"quien":"you","texto":"hola"}]}
                """);
    }

    private void cuandoSeEjecutaSinHistorialNiOpcionesElTurnoIgualResponde() throws Exception {
        result = cuandoPosting("/api/sami/taller", """
                {"mensaje":"hola","estado":{"nombre":"","distribucion":"mosaic","piezas":[]}}
                """);
    }

    private void cuandoSeEjecutaRechazaCon400UnMensajeVacioUnPedidoSinEstadoOUnHistorialConUnAutorInvalido() throws Exception {
        resultadoDeLaLlamada = cuandoPosting("/api/sami/taller", "{\"mensaje\":\"  \",\"estado\":{\"distribucion\":\"mosaic\",\"piezas\":[]}}");
        segundaResultadoDeLaLlamada = cuandoPosting("/api/sami/taller", "{\"mensaje\":\"hola\"}");
        terceraResultadoDeLaLlamada = cuandoPosting("/api/sami/taller", """
                {"mensaje":"hola","estado":{"distribucion":"mosaic","piezas":[]},
                 "historial":[{"quien":"robot","texto":"x"}]}
                """);
    }

    private void cuandoSeEjecutaUnTurnoConFotoLePasaAlUseCaseLaImagenDecodificadaYSuTipo() throws Exception {
        result = cuandoPosting("/api/sami/taller", conImagen("image/jpeg", "AQID"));
    }

    private void cuandoSeEjecutaRechazaCon400YUnMensajeParaLaPersonaUnaFotoDeUnTipoQueNoVa() throws Exception {
        result = cuandoPosting("/api/sami/taller", conImagen("application/pdf", "AQID"));
    }

    private void cuandoSeEjecutaRechazaCon400YUnMensajeParaLaPersonaUnaFotoDeMasDe5MB() throws Exception {
        result = cuandoPosting("/api/sami/taller", conImagen("image/png", datos));
    }

    private void cuandoSeEjecutaSiElLimiteDiarioSeAgotoResponde400ConElMensajeParaLaPersona() throws Exception {
        result = cuandoPosting("/api/sami/taller", """
                {"mensaje":"hola","estado":{"distribucion":"mosaic","piezas":[]}}
                """);
    }

    private void cuandoSeEjecutaDevuelveLaPreguntaDeSamiYLePasaElUsuarioDeLaSesionNuncaUnoDelBody() throws Exception {
        result = cuandoPosting("/api/sami/conversacion",
                "{\"texto\":\"quiero ordenar mi negocio\",\"usuarioId\":\"otro\"}");
    }

    private void cuandoSeEjecutaRechazaCon400UnTextoVacio() throws Exception {
        result = cuandoPosting("/api/sami/conversacion", "{\"texto\":\"   \"}");
    }

    private void cuandoSeEjecutaRechazaCon400UnaRespuestaSinOpcion() throws Exception {
        result = cuandoPosting("/api/sami/conversacion",
                "{\"texto\":\"hola\",\"respuestas\":[{\"preguntaId\":\"principal\"}]}");
    }

    private void cuandoSeEjecutaCreaElProyectoConLasColumnasConfirmadasResponde201ConSuUbicacionYUsaLaSesion() throws Exception {
        result = cuandoPosting("/api/sami/proyectos", """
                {"plantillaId":"%s","nombre":"Velas","columnas":[
                  {"nombre":"Saldo pendiente","tipo":"NUMERO"}]}
                """.formatted(PLANTILLA_ID));
    }

    private void cuandoSeEjecutaRechazaCon400UnPedidoSinPlantillaOSinListaDeColumnas() throws Exception {
        resultadoDeLaLlamada = cuandoPosting("/api/sami/proyectos", "{\"columnas\":[]}");
        segundaResultadoDeLaLlamada = cuandoPosting("/api/sami/proyectos", "{\"plantillaId\":\"%s\"}".formatted(PLANTILLA_ID));
    }

    private void cuandoSeEjecutaSiUnaColumnaNoEsValidaResponde400ConElErrorUbicadoEnLaColumna() throws Exception {
        result = cuandoPosting("/api/sami/proyectos", """
                {"plantillaId":"%s","columnas":[{"nombre":"x","tipo":"TEXTO"}]}
                """.formatted(PLANTILLA_ID));
    }

    // --- entonces ---

    private void entoncesSeVerificaUnTurnoDelTallerDevuelveLaRespuestaLasAccionesYLaBotoneraConElUsuarioDeLaSesion() throws Exception {
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta").value("Listo, sumé gastos."))
                .andExpect(jsonPath("$.acciones[0].herramienta").value("agregar_pieza"))
                .andExpect(jsonPath("$.acciones[0].argumentos.tipo").value("expenses"))
                .andExpect(jsonPath("$.preguntas[0].pregunta").value("¿Seguimos?"))
                .andExpect(jsonPath("$.preguntas[0].opciones[1]").value("No"));
        ArgumentCaptor<ConversarEnTallerRequest> captor = ArgumentCaptor.forClass(ConversarEnTallerRequest.class);
        verify(conversarEnTaller).execute(captor.capture());
        assertThat(captor.getValue().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(captor.getValue().mensaje()).isEqualTo("sumá gastos");
        assertThat(captor.getValue().estado().piezas().getFirst().campos()).hasSize(1);
        assertThat(captor.getValue().historial()).hasSize(2);
    }

    private void entoncesSeVerificaSinHistorialNiOpcionesElTurnoIgualResponde() throws Exception {
        result.andExpect(status().isOk()).andExpect(jsonPath("$.preguntas").isEmpty());
        ArgumentCaptor<ConversarEnTallerRequest> captor = ArgumentCaptor.forClass(ConversarEnTallerRequest.class);
        verify(conversarEnTaller).execute(captor.capture());
        assertThat(captor.getValue().historial()).isEmpty();
    }

    private void entoncesSeVerificaRechazaCon400UnMensajeVacioUnPedidoSinEstadoOUnHistorialConUnAutorInvalido() throws Exception {
        resultadoDeLaLlamada
                .andExpect(status().isBadRequest());
        segundaResultadoDeLaLlamada
                .andExpect(status().isBadRequest());
        terceraResultadoDeLaLlamada
                .andExpect(status().isBadRequest());
        verify(conversarEnTaller, never()).execute(any());
    }

    private void entoncesSeVerificaUnTurnoConFotoLePasaAlUseCaseLaImagenDecodificadaYSuTipo() throws Exception {
        result.andExpect(status().isOk()).andExpect(jsonPath("$.respuesta").value("Leí 3"));
        ArgumentCaptor<ConversarEnTallerRequest> captor = ArgumentCaptor.forClass(ConversarEnTallerRequest.class);
        verify(conversarEnTaller).execute(captor.capture());
        assertThat(captor.getValue().imagen().tipo()).isEqualTo("image/jpeg");
        assertThat(captor.getValue().imagen().datos()).containsExactly(1, 2, 3);
    }

    private void entoncesSeVerificaRechazaCon400YUnMensajeParaLaPersonaUnaFotoDeUnTipoQueNoVa() throws Exception {
        result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(ImagenTaller.TIPO_INVALIDO));
        verifyNoInteractions(conversarEnTaller);
    }

    private void entoncesSeVerificaRechazaCon400YUnMensajeParaLaPersonaUnaFotoDeMasDe5MB() throws Exception {
        result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(ImagenTaller.MUY_GRANDE));
        verifyNoInteractions(conversarEnTaller);
    }

    private void entoncesSeVerificaSiElLimiteDiarioSeAgotoResponde400ConElMensajeParaLaPersona() throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Hoy ya usaste todas las consultas con IA."));
    }

    private void entoncesSeVerificaDevuelveLaPreguntaDeSamiYLePasaElUsuarioDeLaSesionNuncaUnoDelBody() throws Exception {
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("PREGUNTA"))
                .andExpect(jsonPath("$.pregunta.opciones[0].id").value("ventas"))
                .andExpect(jsonPath("$.propuesta").doesNotExist());
        ArgumentCaptor<ConversarConSamiRequest> captor = ArgumentCaptor.forClass(ConversarConSamiRequest.class);
        verify(conversarConSami).execute(captor.capture());
        assertThat(captor.getValue().usuarioId()).isEqualTo(USUARIO_ID);
    }

    private void entoncesSeVerificaRechazaCon400UnTextoVacio() throws Exception {
        result.andExpect(status().isBadRequest());
        verify(conversarConSami, never()).execute(any());
    }

    private void entoncesSeVerificaRechazaCon400UnaRespuestaSinOpcion() throws Exception {
        result.andExpect(status().isBadRequest());
    }

    private void entoncesSeVerificaCreaElProyectoConLasColumnasConfirmadasResponde201ConSuUbicacionYUsaLaSesion() throws Exception {
        result.andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/proyectos/" + PROYECTO_ID))
                .andExpect(jsonPath("$.id").value(PROYECTO_ID.toString()))
                .andExpect(jsonPath("$.nombre").value("Velas"));
        ArgumentCaptor<CrearProyectoDesdeSamiRequest> captor =
                ArgumentCaptor.forClass(CrearProyectoDesdeSamiRequest.class);
        verify(crearProyectoDesdeSami).execute(captor.capture());
        assertThat(captor.getValue().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(captor.getValue().plantillaId()).isEqualTo(PLANTILLA_ID);
        assertThat(captor.getValue().columnas()).extracting("nombre", "tipo")
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Saldo pendiente", "NUMERO"));
    }

    private void entoncesSeVerificaRechazaCon400UnPedidoSinPlantillaOSinListaDeColumnas() throws Exception {
        resultadoDeLaLlamada
                .andExpect(status().isBadRequest());
        segundaResultadoDeLaLlamada
                .andExpect(status().isBadRequest());
        verify(crearProyectoDesdeSami, never()).execute(any());
    }

    private void entoncesSeVerificaSiUnaColumnaNoEsValidaResponde400ConElErrorUbicadoEnLaColumna() throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['columnas[0].nombre']").value("Escribí un nombre"));
    }
}
