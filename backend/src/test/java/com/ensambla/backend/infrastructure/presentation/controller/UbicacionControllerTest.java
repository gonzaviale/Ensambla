package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.ServicioUbicacionException;
import com.ensambla.backend.domain.useCase.ubicacion.buscarDirecciones.BuscarDirecciones;
import com.ensambla.backend.domain.useCase.ubicacion.buscarDirecciones.BuscarDireccionesRequest;
import com.ensambla.backend.domain.useCase.ubicacion.buscarDirecciones.BuscarDireccionesResponse.DireccionItem;
import com.ensambla.backend.domain.useCase.ubicacion.buscarDirecciones.BuscarDireccionesResponse;
import com.ensambla.backend.domain.useCase.ubicacion.listarRubros.ListarRubros;
import com.ensambla.backend.domain.useCase.ubicacion.listarRubros.ListarRubrosResponse.RubroItem;
import com.ensambla.backend.domain.useCase.ubicacion.listarRubros.ListarRubrosResponse;
import com.ensambla.backend.domain.useCase.ubicacion.resolverLocalidad.ResolverLocalidad;
import com.ensambla.backend.domain.useCase.ubicacion.resolverLocalidad.ResolverLocalidadRequest;
import com.ensambla.backend.domain.useCase.ubicacion.resolverLocalidad.ResolverLocalidadResponse;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.ubicacion.UbicacionPresentationMapper;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.ResultActions;
import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UbicacionControllerTest {

    private ResultActions resultado;

    private static final String CONSULTA = "Varela 1903, San Justo";
    private static final BuscarDireccionesRequest REQUEST = new BuscarDireccionesRequest(CONSULTA);
    private static final ResolverLocalidadRequest PUNTO = new ResolverLocalidadRequest(
            new BigDecimal("-34.6701"), new BigDecimal("-58.5625"));
    private BuscarDirecciones buscarDirecciones;
    private ResolverLocalidad resolverLocalidad;
    private ListarRubros listarRubros;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        buscarDirecciones = mock(BuscarDirecciones.class);
        resolverLocalidad = mock(ResolverLocalidad.class);
        listarRubros = mock(ListarRubros.class);
        mockMvc = MockMvcDePrueba.standalone(new UbicacionController(
                        buscarDirecciones, resolverLocalidad, listarRubros,
                        new UbicacionPresentationMapper()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    @DisplayName("La búsqueda devuelve opciones completas y prohíbe la caché del navegador")
    void laBusquedaDevuelveOpcionesCompletasYProhibeLaCacheDelNavegador() throws Exception {
        dadoElEscenarioDeLaBusquedaDevuelveOpcionesCompletasYProhibeLaCacheDelNavegador();

        cuandoSeEjecutaLaBusquedaDevuelveOpcionesCompletasYProhibeLaCacheDelNavegador();

        entoncesSeVerificaLaBusquedaDevuelveOpcionesCompletasYProhibeLaCacheDelNavegador();
    }

    @Test
    @DisplayName("La localidad del punto se devuelve sin caché del navegador")
    void laLocalidadDelPuntoSeDevuelveSinCacheDelNavegador() throws Exception {
        dadoElEscenarioDeLaLocalidadDelPuntoSeDevuelveSinCacheDelNavegador();

        cuandoSeEjecutaLaLocalidadDelPuntoSeDevuelveSinCacheDelNavegador();

        entoncesSeVerificaLaLocalidadDelPuntoSeDevuelveSinCacheDelNavegador();
    }

    @Test
    @DisplayName("Un punto sin localidad reconocida responde localidad nula y lista vacía")
    void unPuntoSinLocalidadReconocidaRespondeLocalidadNulaYListaVacia() throws Exception {
        dadoElEscenarioDeUnPuntoSinLocalidadReconocidaRespondeLocalidadNulaYListaVacia();

        cuandoSeEjecutaUnPuntoSinLocalidadReconocidaRespondeLocalidadNulaYListaVacia();

        entoncesSeVerificaUnPuntoSinLocalidadReconocidaRespondeLocalidadNulaYListaVacia();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}", "{\"latitud\":-34.6}", "{\"longitud\":-58.5}",
            "{\"latitud\":91,\"longitud\":0}", "{\"latitud\":-91,\"longitud\":0}",
            "{\"latitud\":0,\"longitud\":181}", "{\"latitud\":0,\"longitud\":-181}"
    })
    @DisplayName("Un punto inválido no llega al caso de uso")
    void unPuntoInvalidoNoLlegaAlCasoDeUso(String body) throws Exception {
        cuandoSeResuelveLaLocalidadDelPunto(body);

        entoncesSeRechazaElPuntoSinLlegarAlCasoDeUso();
    }

    @Test
    @DisplayName("El proveedor caído al resolver la localidad responde 503")
    void elProveedorCaidoAlResolverLaLocalidadResponde503() throws Exception {
        dadoElEscenarioDeElProveedorCaidoAlResolverLaLocalidadResponde503();

        cuandoSeEjecutaElProveedorCaidoAlResolverLaLocalidadResponde503();

        entoncesSeVerificaElProveedorCaidoAlResolverLaLocalidadResponde503();
    }

    @Test
    @DisplayName("La búsqueda sin coincidencias responde con una lista vacía")
    void laBusquedaSinCoincidenciasRespondeConUnaListaVacia() throws Exception {
        dadoElEscenarioDeLaBusquedaSinCoincidenciasRespondeConUnaListaVacia();

        cuandoSeEjecutaLaBusquedaSinCoincidenciasRespondeConUnaListaVacia();

        entoncesSeVerificaLaBusquedaSinCoincidenciasRespondeConUnaListaVacia();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"consulta\":null}", "{\"consulta\":\"    \"}", "{\"consulta\":\"abcd\"}"})
    @DisplayName("Una dirección inválida no llega al caso de uso")
    void unaDireccionInvalidaNoLlegaAlCasoDeUso(String body) throws Exception {
        cuandoSeBuscanLasDireccionesCon(body);

        entoncesSeRechazaLaConsultaSinLlegarAlCasoDeUso();
    }

    @Test
    @DisplayName("El proveedor caído responde 503 con el formato de error de la aplicación")
    void elProveedorCaidoResponde503ConElFormatoDeErrorDeLaAplicacion() throws Exception {
        dadoElEscenarioDeElProveedorCaidoResponde503ConElFormatoDeErrorDeLaAplicacion();

        cuandoSeEjecutaElProveedorCaidoResponde503ConElFormatoDeErrorDeLaAplicacion();

        entoncesSeVerificaElProveedorCaidoResponde503ConElFormatoDeErrorDeLaAplicacion();
    }

    @Test
    @DisplayName("El catálogo devuelve código estable y nombre legible")
    void elCatalogoDevuelveCodigoEstableYNombreLegible() throws Exception {
        dadoElEscenarioDeElCatalogoDevuelveCodigoEstableYNombreLegible();

        cuandoSeEjecutaElCatalogoDevuelveCodigoEstableYNombreLegible();

        entoncesSeVerificaElCatalogoDevuelveCodigoEstableYNombreLegible();
    }

    @Test
    @DisplayName("El listado respeta un catálogo vacío")
    void elListadoRespetaUnCatalogoVacio() throws Exception {
        dadoElEscenarioDeElListadoRespetaUnCatalogoVacio();

        cuandoSeEjecutaElListadoRespetaUnCatalogoVacio();

        entoncesSeVerificaElListadoRespetaUnCatalogoVacio();
    }

    // --- dado ---

    private void dadoElEscenarioDeLaBusquedaDevuelveOpcionesCompletasYProhibeLaCacheDelNavegador() throws Exception {
        when(buscarDirecciones.execute(REQUEST)).thenReturn(new BuscarDireccionesResponse(List.of(
                        new DireccionItem(CONSULTA, new BigDecimal("-34.67"), new BigDecimal("-58.56"), "San Justo"))));
    }

    private void dadoElEscenarioDeLaLocalidadDelPuntoSeDevuelveSinCacheDelNavegador() throws Exception {
        when(resolverLocalidad.execute(PUNTO)).thenReturn(
                        new ResolverLocalidadResponse("San Justo", List.of("San Justo", "Villa Luzuriaga")));
    }

    private void dadoElEscenarioDeUnPuntoSinLocalidadReconocidaRespondeLocalidadNulaYListaVacia() throws Exception {
        when(resolverLocalidad.execute(PUNTO)).thenReturn(new ResolverLocalidadResponse(null, List.of()));
    }

    private void dadoElEscenarioDeElProveedorCaidoAlResolverLaLocalidadResponde503() throws Exception {
        when(resolverLocalidad.execute(PUNTO)).thenThrow(new ServicioUbicacionException("Sin servicio"));
    }

    private void dadoElEscenarioDeLaBusquedaSinCoincidenciasRespondeConUnaListaVacia() throws Exception {
        when(buscarDirecciones.execute(REQUEST)).thenReturn(new BuscarDireccionesResponse(List.of()));
    }

    private void dadoElEscenarioDeElProveedorCaidoResponde503ConElFormatoDeErrorDeLaAplicacion() throws Exception {
        when(buscarDirecciones.execute(REQUEST)).thenThrow(new ServicioUbicacionException("Sin servicio"));
    }

    private void dadoElEscenarioDeElCatalogoDevuelveCodigoEstableYNombreLegible() throws Exception {
        when(listarRubros.execute()).thenReturn(new ListarRubrosResponse(List.of(
                        new RubroItem("ALMACEN", "Almacén"), new RubroItem("OTRO", "Otro"))));
    }

    private void dadoElEscenarioDeElListadoRespetaUnCatalogoVacio() throws Exception {
        when(listarRubros.execute()).thenReturn(new ListarRubrosResponse(List.of()));
    }

    // --- cuando ---

    private void cuandoSeResuelveLaLocalidadDelPunto(String body) throws Exception {
        resultado = mockMvc.perform(post("/api/ubicacion/localidad")
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void cuandoSeBuscanLasDireccionesCon(String body) throws Exception {
        resultado = mockMvc.perform(post("/api/ubicacion/direcciones")
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void cuandoSeEjecutaLaBusquedaDevuelveOpcionesCompletasYProhibeLaCacheDelNavegador() throws Exception {
        resultado = mockMvc.perform(post("/api/ubicacion/direcciones").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"consulta\":\"" + CONSULTA + "\"}"));
    }

    private void cuandoSeEjecutaLaLocalidadDelPuntoSeDevuelveSinCacheDelNavegador() throws Exception {
        resultado = mockMvc.perform(post("/api/ubicacion/localidad").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"latitud\":-34.6701,\"longitud\":-58.5625}"));
    }

    private void cuandoSeEjecutaUnPuntoSinLocalidadReconocidaRespondeLocalidadNulaYListaVacia() throws Exception {
        resultado = mockMvc.perform(post("/api/ubicacion/localidad").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"latitud\":-34.6701,\"longitud\":-58.5625}"));
    }

    private void cuandoSeEjecutaElProveedorCaidoAlResolverLaLocalidadResponde503() throws Exception {
        resultado = mockMvc.perform(post("/api/ubicacion/localidad").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"latitud\":-34.6701,\"longitud\":-58.5625}"));
    }

    private void cuandoSeEjecutaLaBusquedaSinCoincidenciasRespondeConUnaListaVacia() throws Exception {
        resultado = mockMvc.perform(post("/api/ubicacion/direcciones").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"consulta\":\"" + CONSULTA + "\"}"));
    }

    private void cuandoSeEjecutaElProveedorCaidoResponde503ConElFormatoDeErrorDeLaAplicacion() throws Exception {
        resultado = mockMvc.perform(post("/api/ubicacion/direcciones").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"consulta\":\"" + CONSULTA + "\"}"));
    }

    private void cuandoSeEjecutaElCatalogoDevuelveCodigoEstableYNombreLegible() throws Exception {
        resultado = mockMvc.perform(get("/api/ubicacion/rubros"));
    }

    private void cuandoSeEjecutaElListadoRespetaUnCatalogoVacio() throws Exception {
        resultado = mockMvc.perform(get("/api/ubicacion/rubros"));
    }

    // --- entonces ---

    private void entoncesSeRechazaElPuntoSinLlegarAlCasoDeUso() throws Exception {
        resultado.andExpect(status().isBadRequest());
        verifyNoInteractions(resolverLocalidad);
    }

    private void entoncesSeRechazaLaConsultaSinLlegarAlCasoDeUso() throws Exception {
        resultado.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.consulta").exists());
        verifyNoInteractions(buscarDirecciones);
    }

    private void entoncesSeVerificaLaBusquedaDevuelveOpcionesCompletasYProhibeLaCacheDelNavegador() throws Exception {
        resultado.andExpect(status().isOk())
                        .andExpect(header().string("Cache-Control", "no-store"))
                        .andExpect(jsonPath("$.resultados[0].direccion").value(CONSULTA))
                        .andExpect(jsonPath("$.resultados[0].latitud").value(-34.67))
                        .andExpect(jsonPath("$.resultados[0].longitud").value(-58.56))
                        .andExpect(jsonPath("$.resultados[0].localidad").value("San Justo"));
        verify(buscarDirecciones).execute(REQUEST);
    }

    private void entoncesSeVerificaLaLocalidadDelPuntoSeDevuelveSinCacheDelNavegador() throws Exception {
        resultado.andExpect(status().isOk())
                        .andExpect(header().string("Cache-Control", "no-store"))
                        .andExpect(jsonPath("$.localidad").value("San Justo"))
                        .andExpect(jsonPath("$.localidades[1]").value("Villa Luzuriaga"));
    }

    private void entoncesSeVerificaUnPuntoSinLocalidadReconocidaRespondeLocalidadNulaYListaVacia() throws Exception {
        resultado.andExpect(status().isOk())
                        .andExpect(jsonPath("$.localidad").isEmpty())
                        .andExpect(jsonPath("$.localidades").isEmpty());
    }

    private void entoncesSeVerificaElProveedorCaidoAlResolverLaLocalidadResponde503() throws Exception {
        resultado.andExpect(status().isServiceUnavailable())
                        .andExpect(jsonPath("$.message").value("Sin servicio"));
    }

    private void entoncesSeVerificaLaBusquedaSinCoincidenciasRespondeConUnaListaVacia() throws Exception {
        resultado.andExpect(status().isOk()).andExpect(jsonPath("$.resultados").isEmpty());
    }

    private void entoncesSeVerificaElProveedorCaidoResponde503ConElFormatoDeErrorDeLaAplicacion() throws Exception {
        resultado.andExpect(status().isServiceUnavailable())
                        .andExpect(jsonPath("$.message").value("Sin servicio"))
                        .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    private void entoncesSeVerificaElCatalogoDevuelveCodigoEstableYNombreLegible() throws Exception {
        resultado.andExpect(status().isOk())
                        .andExpect(jsonPath("$.rubros[0].codigo").value("ALMACEN"))
                        .andExpect(jsonPath("$.rubros[0].nombre").value("Almacén"))
                        .andExpect(jsonPath("$.rubros[1].codigo").value("OTRO"));
    }

    private void entoncesSeVerificaElListadoRespetaUnCatalogoVacio() throws Exception {
        resultado.andExpect(status().isOk()).andExpect(jsonPath("$.rubros").isEmpty());
    }
}
