package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.useCase.objetivo.ObjetivoItem;
import com.ensambla.backend.domain.useCase.objetivo.crearObjetivo.CrearObjetivo;
import com.ensambla.backend.domain.useCase.objetivo.crearObjetivo.CrearObjetivoRequest;
import com.ensambla.backend.domain.useCase.objetivo.eliminarObjetivo.EliminarObjetivo;
import com.ensambla.backend.domain.useCase.objetivo.eliminarObjetivo.EliminarObjetivoRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarObjetivos.ListarObjetivos;
import com.ensambla.backend.domain.useCase.objetivo.medirObjetivos.MedirObjetivos;
import com.ensambla.backend.domain.useCase.objetivo.medirObjetivos.MedirObjetivosRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarObjetivos.ListarObjetivosRequest;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.domain.model.objetivo.TipoPasoObjetivo;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasos;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasosRequest;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasosResponse;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasosResponse.PasoItem;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasosResponse.PrecioZonaItem;
import com.ensambla.backend.infrastructure.presentation.mapper.objetivo.ObjetivoPresentationMapper;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static com.ensambla.backend.support.ObjetivoTestData.META;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.TEXTO;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ObjetivoControllerTest {

    private ResultActions result;
    private ResultActions resultado;

    private final ListarObjetivos listarObjetivos = mock(ListarObjetivos.class);
    private final CrearObjetivo crearObjetivo = mock(CrearObjetivo.class);
    private final EliminarObjetivo eliminarObjetivo = mock(EliminarObjetivo.class);
    private final ObtenerPasos obtenerPasos = mock(ObtenerPasos.class);
    private final MedirObjetivos medirObjetivos = mock(MedirObjetivos.class);
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjetivoController controller = new ObjetivoController(
                listarObjetivos,
                crearObjetivo,
                eliminarObjetivo,
                obtenerPasos,
                medirObjetivos,
                new ObjetivoPresentationMapper());
        mockMvc = MockMvcDePrueba.standalone(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET lista los objetivos y mapea todos sus datos a JSON")
    void gETListaLosObjetivosYMapeaTodosSusDatosAJSON() throws Exception {
        when(listarObjetivos.execute(any())).thenReturn(List.of(dadoItem()));

        ResultActions result = cuandoGetListar();

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(OBJETIVO_ID.toString()))
                .andExpect(jsonPath("$[0].texto").value(TEXTO))
                .andExpect(jsonPath("$[0].meta").value(META.doubleValue()))
                .andExpect(jsonPath("$[0].creadoEn").exists());
    }

    @Test
    @DisplayName("GET delega en el caso de uso con el usuario de la sesión")
    void gETDelegaEnElCasoDeUsoConElUsuarioDeLaSesion() throws Exception {
        when(listarObjetivos.execute(any())).thenReturn(List.of());

        cuandoGetListar();

        verify(listarObjetivos).execute(new ListarObjetivosRequest(USUARIO_ID));
    }

    @Test
    @DisplayName("GET devuelve una lista vacía cuando no hay objetivos")
    void gETDevuelveUnaListaVaciaCuandoNoHayObjetivos() throws Exception {
        when(listarObjetivos.execute(any())).thenReturn(List.of());

        ResultActions result = cuandoGetListar();

        result.andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("POST crea el objetivo y devuelve 201 con su ubicación")
    void pOSTCreaElObjetivoYDevuelve201ConSuUbicacion() throws Exception {
        when(crearObjetivo.execute(any())).thenReturn(dadoItem());

        ResultActions result = cuandoPostCrear("{\"texto\":\"" + TEXTO + "\",\"meta\":400000.50}");

        result.andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/objetivos/" + OBJETIVO_ID))
                .andExpect(jsonPath("$.id").value(OBJETIVO_ID.toString()))
                .andExpect(jsonPath("$.texto").value(TEXTO));
    }

    @Test
    @DisplayName("POST pasa el texto y la meta al caso de uso con el usuario de la sesión")
    void pOSTPasaElTextoYLaMetaAlCasoDeUsoConElUsuarioDeLaSesion() throws Exception {
        when(crearObjetivo.execute(any())).thenReturn(dadoItem());

        cuandoPostCrear("{\"texto\":\"" + TEXTO + "\",\"meta\":400000.50}");

        verify(crearObjetivo).execute(new CrearObjetivoRequest(USUARIO_ID, TEXTO, new BigDecimal("400000.50")));
    }

    @Test
    @DisplayName("POST ignora el negocio que indique el navegador")
    void pOSTIgnoraElNegocioQueIndiqueElNavegador() throws Exception {
        when(crearObjetivo.execute(any())).thenReturn(dadoItem());

        ResultActions result = cuandoPostCrear(
                "{\"texto\":\"" + TEXTO + "\",\"meta\":400000.50,\"negocioId\":\"otro\"}");

        result.andExpect(status().isCreated());
        verify(crearObjetivo).execute(new CrearObjetivoRequest(USUARIO_ID, TEXTO, new BigDecimal("400000.50")));
    }

    @Test
    @DisplayName("POST rechaza un texto vacío antes de invocar el caso de uso")
    void pOSTRechazaUnTextoVacioAntesDeInvocarElCasoDeUso() throws Exception {
        ResultActions result = cuandoPostCrear("{\"texto\":\"  \",\"meta\":400000}");

        result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.texto").exists());
        verifyNoInteractions(crearObjetivo);
    }

    @Test
    @DisplayName("POST rechaza un texto de más de 500 caracteres")
    void pOSTRechazaUnTextoDeMasDe500Caracteres() throws Exception {
        ResultActions result = cuandoPostCrear("{\"texto\":\"" + "a".repeat(501) + "\",\"meta\":400000}");

        result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.texto").exists());
        verifyNoInteractions(crearObjetivo);
    }

    @Test
    @DisplayName("POST rechaza un body sin meta")
    void pOSTRechazaUnBodySinMeta() throws Exception {
        ResultActions result = cuandoPostCrear("{\"texto\":\"" + TEXTO + "\"}");

        result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.meta").exists());
        verifyNoInteractions(crearObjetivo);
    }

    @Test
    @DisplayName("POST rechaza una meta menor a 1")
    void pOSTRechazaUnaMetaMenorA1() throws Exception {
        ResultActions result = cuandoPostCrear("{\"texto\":\"" + TEXTO + "\",\"meta\":0}");

        result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.meta").exists());
        verifyNoInteractions(crearObjetivo);
    }

    @Test
    @DisplayName("DELETE devuelve 204 y delega con el usuario de la sesión y el id de la ruta")
    void dELETEDevuelve204YDelegaConElUsuarioDeLaSesionYElIdDeLaRuta() throws Exception {
        cuandoSeEjecutaDELETEDevuelve204YDelegaConElUsuarioDeLaSesionYElIdDeLaRuta();

        entoncesSeVerificaDELETEDevuelve204YDelegaConElUsuarioDeLaSesionYElIdDeLaRuta();
    }

    @Test
    @DisplayName("DELETE de un objetivo ajeno o inexistente devuelve el 404 estándar")
    void dELETEDeUnObjetivoAjenoOInexistenteDevuelveEl404Estandar() throws Exception {
        dadoElEscenarioDeDELETEDeUnObjetivoAjenoOInexistenteDevuelveEl404Estandar();

        cuandoSeEjecutaDELETEDeUnObjetivoAjenoOInexistenteDevuelveEl404Estandar();

        entoncesSeVerificaDELETEDeUnObjetivoAjenoOInexistenteDevuelveEl404Estandar();
    }

    @Test
    @DisplayName("DELETE con un id que no es UUID devuelve 400 sin llegar al caso de uso")
    void dELETEConUnIdQueNoEsUUIDDevuelve400SinLlegarAlCasoDeUso() throws Exception {
        cuandoSeEjecutaDELETEConUnIdQueNoEsUUIDDevuelve400SinLlegarAlCasoDeUso();

        entoncesSeVerificaDELETEConUnIdQueNoEsUUIDDevuelve400SinLlegarAlCasoDeUso();
    }

    @Test
    @DisplayName("POST medir mide los objetivos del negocio de la sesión y devuelve 204")
    void pOSTMedirMideLosObjetivosDelNegocioDeLaSesionYDevuelve204() throws Exception {
        cuandoSeEjecutaPOSTMedirMideLosObjetivosDelNegocioDeLaSesionYDevuelve204();

        entoncesSeVerificaPOSTMedirMideLosObjetivosDelNegocioDeLaSesionYDevuelve204();
    }

    // --- dado ---
    private ObjetivoItem dadoItem() {
        return ObjetivoItem.de(objetivo());
    }

    // --- cuando ---
    private ResultActions cuandoGetListar() throws Exception {
        return mockMvc.perform(get("/api/objetivos").principal(principal));
    }

    private ResultActions cuandoPostCrear(String body) throws Exception {
        return mockMvc.perform(post("/api/objetivos").principal(principal)
                .contentType("application/json")
                .content(body));
    }

    @Test
    @DisplayName("GET pasos devuelve los productos reales con el rango de zona")
    void gETPasosDevuelveLosProductosRealesConElRangoDeZona() throws Exception {
        dadoElEscenarioDeGETPasosDevuelveLosProductosRealesConElRangoDeZona();

        cuandoSeEjecutaGETPasosDevuelveLosProductosRealesConElRangoDeZona();

        entoncesSeVerificaGETPasosDevuelveLosProductosRealesConElRangoDeZona();
    }

    @Test
    @DisplayName("GET pasos devuelve una lista vacía cuando no hay productos para recomendar")
    void gETPasosDevuelveUnaListaVaciaCuandoNoHayProductosParaRecomendar() throws Exception {
        dadoElEscenarioDeGETPasosDevuelveUnaListaVaciaCuandoNoHayProductosParaRecomendar();

        cuandoSeEjecutaGETPasosDevuelveUnaListaVaciaCuandoNoHayProductosParaRecomendar();

        entoncesSeVerificaGETPasosDevuelveUnaListaVaciaCuandoNoHayProductosParaRecomendar();
    }

    private ObtenerPasosResponse response() {
        return new ObtenerPasosResponse(List.of(new PasoItem(
                TipoPasoObjetivo.SUBIR_PRECIO,
                "Yerba",
                decimal("3600"),
                decimal("2700"),
                decimal("4"),
                decimal("60"),
                new PrecioZonaItem(decimal("3500"), decimal("3900"), decimal("4300")))));
    }

    private BigDecimal decimal(String valor) {
        return new BigDecimal(valor);
    }

    // --- dado ---

    private void dadoElEscenarioDeDELETEDeUnObjetivoAjenoOInexistenteDevuelveEl404Estandar() throws Exception {
        doThrow(new ResourceNotFoundException("Objetivo no encontrado")).when(eliminarObjetivo).execute(any());
    }

    private void dadoElEscenarioDeGETPasosDevuelveLosProductosRealesConElRangoDeZona() throws Exception {
        when(obtenerPasos.execute(any())).thenReturn(response());
    }

    private void dadoElEscenarioDeGETPasosDevuelveUnaListaVaciaCuandoNoHayProductosParaRecomendar() throws Exception {
        when(obtenerPasos.execute(any())).thenReturn(new ObtenerPasosResponse(List.of()));
    }

    // --- cuando ---

    private void cuandoSeEjecutaDELETEDevuelve204YDelegaConElUsuarioDeLaSesionYElIdDeLaRuta() throws Exception {
        result = mockMvc.perform(delete("/api/objetivos/{id}", OBJETIVO_ID).principal(principal));
    }

    private void cuandoSeEjecutaDELETEDeUnObjetivoAjenoOInexistenteDevuelveEl404Estandar() throws Exception {
        result = mockMvc.perform(delete("/api/objetivos/{id}", OBJETIVO_ID).principal(principal));
    }

    private void cuandoSeEjecutaDELETEConUnIdQueNoEsUUIDDevuelve400SinLlegarAlCasoDeUso() throws Exception {
        result = mockMvc.perform(delete("/api/objetivos/invalido").principal(principal));
    }

    private void cuandoSeEjecutaPOSTMedirMideLosObjetivosDelNegocioDeLaSesionYDevuelve204() throws Exception {
        resultado = mockMvc.perform(post("/api/objetivos/medir").principal(principal));
    }

    private void cuandoSeEjecutaGETPasosDevuelveLosProductosRealesConElRangoDeZona() throws Exception {
        resultado = mockMvc.perform(get("/api/objetivos/pasos").principal(principal));
    }

    private void cuandoSeEjecutaGETPasosDevuelveUnaListaVaciaCuandoNoHayProductosParaRecomendar() throws Exception {
        resultado = mockMvc.perform(get("/api/objetivos/pasos").principal(principal));
    }

    // --- entonces ---

    private void entoncesSeVerificaDELETEDevuelve204YDelegaConElUsuarioDeLaSesionYElIdDeLaRuta() throws Exception {
        result.andExpect(status().isNoContent());
        verify(eliminarObjetivo).execute(new EliminarObjetivoRequest(USUARIO_ID, OBJETIVO_ID));
    }

    private void entoncesSeVerificaDELETEDeUnObjetivoAjenoOInexistenteDevuelveEl404Estandar() throws Exception {
        result.andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    private void entoncesSeVerificaDELETEConUnIdQueNoEsUUIDDevuelve400SinLlegarAlCasoDeUso() throws Exception {
        result.andExpect(status().isBadRequest());
        verifyNoInteractions(eliminarObjetivo);
    }

    private void entoncesSeVerificaPOSTMedirMideLosObjetivosDelNegocioDeLaSesionYDevuelve204() throws Exception {
        resultado.andExpect(status().isNoContent());
        verify(medirObjetivos).execute(new MedirObjetivosRequest(USUARIO_ID));
    }

    private void entoncesSeVerificaGETPasosDevuelveLosProductosRealesConElRangoDeZona() throws Exception {
        resultado.andExpect(status().isOk())
                        .andExpect(jsonPath("$[0].tipo").value("SUBIR_PRECIO"))
                        .andExpect(jsonPath("$[0].nombre").value("Yerba"))
                        .andExpect(jsonPath("$[0].precio").value(3600))
                        .andExpect(jsonPath("$[0].costo").value(2700))
                        .andExpect(jsonPath("$[0].stock").value(4))
                        .andExpect(jsonPath("$[0].vendidas30Dias").value(60))
                        .andExpect(jsonPath("$[0].precioZona.minimo").value(3500))
                        .andExpect(jsonPath("$[0].precioZona.promedio").value(3900))
                        .andExpect(jsonPath("$[0].precioZona.maximo").value(4300));
        verify(obtenerPasos).execute(new ObtenerPasosRequest(USUARIO_ID));
    }

    private void entoncesSeVerificaGETPasosDevuelveUnaListaVaciaCuandoNoHayProductosParaRecomendar() throws Exception {
        resultado.andExpect(status().isOk())
                        .andExpect(jsonPath("$").isEmpty());
    }
}
