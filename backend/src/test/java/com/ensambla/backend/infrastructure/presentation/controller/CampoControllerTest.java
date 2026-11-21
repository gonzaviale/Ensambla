package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.campo.CampoItem;
import com.ensambla.backend.domain.useCase.campo.crearCampo.CrearCampo;
import com.ensambla.backend.domain.useCase.campo.crearCampo.CrearCampoRequest;
import com.ensambla.backend.domain.useCase.campo.editarCampo.EditarCampo;
import com.ensambla.backend.domain.useCase.campo.editarCampo.EditarCampoRequest;
import com.ensambla.backend.domain.useCase.campo.eliminarCampo.EliminarCampo;
import com.ensambla.backend.domain.useCase.campo.eliminarCampo.EliminarCampoRequest;
import com.ensambla.backend.domain.useCase.campo.listarCampos.ListarCampos;
import com.ensambla.backend.domain.useCase.campo.listarCampos.ListarCamposRequest;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.campo.CampoPresentationMapper;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CampoControllerTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final CampoId CAMPO_ID = CampoId.de("d0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-10T12:00:00Z");
    private static final BloqueId RELACIONADO_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    private static final List<String> TALLES = List.of("Chico", "Grande");
    private static final String MENSAJE_TIPO = "Elegí un tipo válido: texto, número, fecha, opción o sí/no";
    private static final String CUERPO_TALLE = """
            {"nombre":"Talle","tipo":"OPCION","obligatorio":true,"valorInicial":"Chico","opciones":["Chico","Grande"]}
            """;

    private final ListarCampos listarCampos = mock(ListarCampos.class);
    private final CrearCampo crearCampo = mock(CrearCampo.class);
    private final EditarCampo editarCampo = mock(EditarCampo.class);
    private final EliminarCampo eliminarCampo = mock(EliminarCampo.class);
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);

    private MockMvc mockMvc;
    private ResultActions respuesta;

    @BeforeEach
    void setUp() {
        var controller = new CampoController(
                listarCampos,
                crearCampo,
                editarCampo,
                eliminarCampo,
                new CampoPresentationMapper());
        mockMvc = MockMvcDePrueba.standalone(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET lista los campos del bloque con todos sus datos")
    void getListaLosCamposDelBloqueConTodosSusDatos() throws Exception {
        dadoUnBloqueConElCampo("Talle");

        cuandoSeListanLosCamposDelBloque();

        entoncesSeListaElCampoConTodosSusDatos();
        entoncesSeConsultaElBloqueDelUsuario();
    }

    @Test
    @DisplayName("GET devuelve una lista vacía cuando el bloque no tiene campos")
    void getDevuelveUnaListaVaciaCuandoElBloqueNoTieneCampos() throws Exception {
        dadoUnBloqueSinCampos();

        cuandoSeListanLosCamposDelBloque();

        entoncesSeDevuelveUnaListaVacia();
    }

    @Test
    @DisplayName("GET de un bloque ajeno devuelve 404 estándar")
    void getDeUnBloqueAjenoDevuelve404Estandar() throws Exception {
        dadoUnBloqueAjeno();

        cuandoSeListanLosCamposDelBloque();

        entoncesRespondeNoEncontradoEstandar();
    }

    @Test
    @DisplayName("Un id de bloque que no es UUID no llega al caso de uso")
    void unIdDeBloqueQueNoEsUuidNoLlegaAlCasoDeUso() throws Exception {
        cuandoSeListanLosCamposDeUnBloqueConIdInvalido();

        entoncesSeRechazaSinLlegarA(listarCampos);
    }

    @Test
    @DisplayName("POST crea el campo y devuelve 201 con su ubicación")
    void postCreaElCampoYDevuelve201ConSuUbicacion() throws Exception {
        dadoUnCampoCreable();

        cuandoSeCreaElCampo(CUERPO_TALLE);

        entoncesSeCreaElCampoConSuUbicacion();
        entoncesElCasoDeUsoCreaElCampoTalle();
    }

    @Test
    @DisplayName("POST deja en null lo que el body no manda para que el dominio aplique los valores por defecto")
    void postDejaEnNullLoQueElBodyNoMandaParaQueElDominioApliqueLosValoresPorDefecto() throws Exception {
        dadoUnCampoCreable();

        cuandoSeCreaElCampo("{\"nombre\":\"Producto\",\"tipo\":\"TEXTO\"}");

        entoncesSeCreaElCampoSinOpcionales();
    }

    @Test
    @DisplayName("POST pasa al caso de uso la tabla de un campo de conexión y la devuelve en el JSON")
    void postPasaAlCasoDeUsoLaTablaDeUnCampoDeConexionYLaDevuelveEnElJson() throws Exception {
        dadoUnCampoDeConexionCreable();

        cuandoSeCreaElCampo(
                "{\"nombre\":\"Cliente\",\"tipo\":\"RELACION\",\"bloqueRelacionadoId\":\"" + RELACIONADO_ID + "\"}");

        entoncesSeDevuelveLaConexionConSuTabla();
        entoncesElCasoDeUsoRecibeLaTablaRelacionada();
    }

    @Test
    @DisplayName("POST rechaza un nombre vacío antes de invocar el caso de uso")
    void postRechazaUnNombreVacioAntesDeInvocarElCasoDeUso() throws Exception {
        cuandoSeCreaElCampo("{\"nombre\":\"  \",\"tipo\":\"TEXTO\"}");

        entoncesSeRechazaElNombreSinLlegarA(crearCampo);
    }

    @Test
    @DisplayName("POST rechaza un nombre de más de 120 caracteres")
    void postRechazaUnNombreDeMasDe120Caracteres() throws Exception {
        cuandoSeCreaElCampo("{\"nombre\":\"" + "a".repeat(121) + "\",\"tipo\":\"TEXTO\"}");

        entoncesSeRechazaElNombreSinLlegarA(crearCampo);
    }

    @Test
    @DisplayName("POST con un tipo fuera de la lista devuelve 400 con el error en tipo")
    void postConUnTipoFueraDeLaListaDevuelve400ConElErrorEnTipo() throws Exception {
        dadoQueCrearRechazaElDato("tipo", MENSAJE_TIPO);

        cuandoSeCreaElCampo("{\"nombre\":\"Dato\",\"tipo\":\"MONEDA\"}");

        entoncesSeRechazaLaConfiguracionConElError("tipo", MENSAJE_TIPO);
    }

    @Test
    @DisplayName("POST de una opción sin opciones devuelve 400 con el error en opciones")
    void postDeUnaOpcionSinOpcionesDevuelve400ConElErrorEnOpciones() throws Exception {
        dadoQueCrearRechazaElDato("opciones", "Agregá al menos una opción");

        cuandoSeCreaElCampo("{\"nombre\":\"Talle\",\"tipo\":\"OPCION\",\"opciones\":[]}");

        entoncesSeRechazaLaConfiguracionConElError("opciones", "Agregá al menos una opción");
    }

    @Test
    @DisplayName("POST con un valor inicial que no corresponde al tipo devuelve 400 con el error en valorInicial")
    void postConUnValorInicialQueNoCorrespondeAlTipoDevuelve400ConElErrorEnValorInicial() throws Exception {
        dadoQueCrearRechazaElDato("valorInicial", "Tiene que ser un número, por ejemplo 12.5");

        cuandoSeCreaElCampo("{\"nombre\":\"Precio\",\"tipo\":\"NUMERO\",\"valorInicial\":\"doce\"}");

        entoncesSeRechazaLaConfiguracionConElError("valorInicial", "Tiene que ser un número, por ejemplo 12.5");
    }

    @Test
    @DisplayName("POST con un nombre repetido en el bloque devuelve 409")
    void postConUnNombreRepetidoEnElBloqueDevuelve409() throws Exception {
        dadoUnNombreRepetidoAlCrear();

        cuandoSeCreaElCampo(CUERPO_TALLE);

        entoncesRespondeConflictoPorElNombreRepetido();
    }

    @Test
    @DisplayName("POST en un bloque ajeno devuelve 404")
    void postEnUnBloqueAjenoDevuelve404() throws Exception {
        dadoUnBloqueAjenoAlCrear();

        cuandoSeCreaElCampo(CUERPO_TALLE);

        entoncesRespondeNoEncontrado();
    }

    @Test
    @DisplayName("PATCH delega la configuración completa con el campo y el usuario de la sesión")
    void patchDelegaLaConfiguracionCompletaConElCampoYElUsuarioDeLaSesion() throws Exception {
        dadoUnCampoEditable();

        cuandoSeEditaElCampo(CUERPO_TALLE);

        entoncesSeDevuelveElCampoEditado();
        entoncesElCasoDeUsoRecibeLaConfiguracionCompleta();
    }

    @Test
    @DisplayName("PATCH rechaza un nombre vacío antes de invocar el caso de uso")
    void patchRechazaUnNombreVacioAntesDeInvocarElCasoDeUso() throws Exception {
        cuandoSeEditaElCampo("{\"nombre\":\"\",\"tipo\":\"TEXTO\"}");

        entoncesSeRechazaElNombreSinLlegarA(editarCampo);
    }

    @Test
    @DisplayName("PATCH con una configuración inválida devuelve 400 con el error por dato")
    void patchConUnaConfiguracionInvalidaDevuelve400ConElErrorPorDato() throws Exception {
        dadoUnaConfiguracionInvalidaAlEditar();

        cuandoSeEditaElCampo("{\"nombre\":\"Talle\",\"tipo\":\"OPCION\",\"opciones\":[\"A\",\"a\"]}");

        entoncesSeRechazaLaConfiguracionConElError("opciones", "Hay opciones repetidas");
    }

    @Test
    @DisplayName("PATCH con el nombre de otro campo devuelve 409")
    void patchConElNombreDeOtroCampoDevuelve409() throws Exception {
        dadoUnNombreRepetidoAlEditar();

        cuandoSeEditaElCampo(CUERPO_TALLE);

        entoncesRespondeConflicto();
    }

    @Test
    @DisplayName("PATCH de un campo ajeno devuelve 404")
    void patchDeUnCampoAjenoDevuelve404() throws Exception {
        dadoUnCampoAjenoAlEditar();

        cuandoSeEditaElCampo(CUERPO_TALLE);

        entoncesRespondeNoEncontrado();
    }

    @Test
    @DisplayName("Un id de campo que no es UUID no llega al caso de uso de editar")
    void unIdDeCampoQueNoEsUuidNoLlegaAlCasoDeUsoDeEditar() throws Exception {
        cuandoSeEditaUnCampoConIdInvalido();

        entoncesSeRechazaSinLlegarA(editarCampo);
    }

    @Test
    @DisplayName("DELETE devuelve 204 después de delegar el borrado")
    void deleteDevuelve204DespuesDeDelegarElBorrado() throws Exception {
        cuandoSeEliminaElCampo();

        entoncesSeEliminaElCampoDelUsuario();
    }

    @Test
    @DisplayName("DELETE de un campo ajeno devuelve 404")
    void deleteDeUnCampoAjenoDevuelve404() throws Exception {
        dadoUnCampoAjenoAlEliminar();

        cuandoSeEliminaElCampo();

        entoncesRespondeNoEncontrado();
    }

    @Test
    @DisplayName("Un id de campo que no es UUID no llega al caso de uso de eliminar")
    void unIdDeCampoQueNoEsUuidNoLlegaAlCasoDeUsoDeEliminar() throws Exception {
        cuandoSeEliminaUnCampoConIdInvalido();

        entoncesSeRechazaSinLlegarA(eliminarCampo);
    }

    // --- dado ---

    private void dadoUnBloqueConElCampo(String nombre) {
        when(listarCampos.execute(any())).thenReturn(List.of(unCampo(nombre)));
    }

    private void dadoUnBloqueSinCampos() {
        when(listarCampos.execute(any())).thenReturn(List.of());
    }

    private void dadoUnBloqueAjeno() {
        when(listarCampos.execute(any())).thenThrow(new ResourceNotFoundException("El bloque no existe"));
    }

    private void dadoUnCampoCreable() {
        when(crearCampo.execute(any())).thenReturn(unCampo("Talle"));
    }

    private void dadoUnCampoDeConexionCreable() {
        when(crearCampo.execute(any())).thenReturn(unaConexion());
    }

    private void dadoQueCrearRechazaElDato(String dato, String mensaje) {
        when(crearCampo.execute(any())).thenThrow(new CampoInvalidoException(Map.of(dato, mensaje)));
    }

    private void dadoUnNombreRepetidoAlCrear() {
        when(crearCampo.execute(any())).thenThrow(nombreRepetido());
    }

    private void dadoUnBloqueAjenoAlCrear() {
        when(crearCampo.execute(any())).thenThrow(new ResourceNotFoundException("El bloque no existe"));
    }

    private void dadoUnCampoEditable() {
        when(editarCampo.execute(any())).thenReturn(unCampo("Talle"));
    }

    private void dadoUnaConfiguracionInvalidaAlEditar() {
        when(editarCampo.execute(any()))
                .thenThrow(new CampoInvalidoException(Map.of("opciones", "Hay opciones repetidas")));
    }

    private void dadoUnNombreRepetidoAlEditar() {
        when(editarCampo.execute(any())).thenThrow(nombreRepetido());
    }

    private void dadoUnCampoAjenoAlEditar() {
        when(editarCampo.execute(any())).thenThrow(new ResourceNotFoundException("El campo no existe"));
    }

    private void dadoUnCampoAjenoAlEliminar() {
        doThrow(new ResourceNotFoundException("El campo no existe")).when(eliminarCampo).execute(any());
    }

    // --- cuando ---

    private void cuandoSeListanLosCamposDelBloque() throws Exception {
        respuesta = mockMvc.perform(get("/api/bloques/{id}/campos", BLOQUE_ID).principal(principal));
    }

    private void cuandoSeListanLosCamposDeUnBloqueConIdInvalido() throws Exception {
        respuesta = mockMvc.perform(get("/api/bloques/invalido/campos").principal(principal));
    }

    private void cuandoSeCreaElCampo(String cuerpo) throws Exception {
        respuesta = mockMvc.perform(post("/api/bloques/{id}/campos", BLOQUE_ID).principal(principal)
                .contentType("application/json").content(cuerpo));
    }

    private void cuandoSeEditaElCampo(String cuerpo) throws Exception {
        respuesta = mockMvc.perform(patch("/api/campos/{id}", CAMPO_ID).principal(principal)
                .contentType("application/json").content(cuerpo));
    }

    private void cuandoSeEditaUnCampoConIdInvalido() throws Exception {
        respuesta = mockMvc.perform(patch("/api/campos/invalido").principal(principal)
                .contentType("application/json").content(CUERPO_TALLE));
    }

    private void cuandoSeEliminaElCampo() throws Exception {
        respuesta = mockMvc.perform(delete("/api/campos/{id}", CAMPO_ID).principal(principal));
    }

    private void cuandoSeEliminaUnCampoConIdInvalido() throws Exception {
        respuesta = mockMvc.perform(delete("/api/campos/invalido").principal(principal));
    }

    // --- entonces ---

    private void entoncesSeListaElCampoConTodosSusDatos() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(CAMPO_ID.toString()))
                .andExpect(jsonPath("$[0].bloqueId").value(BLOQUE_ID.toString()))
                .andExpect(jsonPath("$[0].nombre").value("Talle"))
                .andExpect(jsonPath("$[0].tipo").value("OPCION"))
                .andExpect(jsonPath("$[0].obligatorio").value(true))
                .andExpect(jsonPath("$[0].valorInicial").value("Chico"))
                .andExpect(jsonPath("$[0].opciones[1]").value("Grande"))
                .andExpect(jsonPath("$[0].posicion").value(0))
                .andExpect(jsonPath("$[0].creadoEn").exists())
                .andExpect(jsonPath("$[0].actualizadoEn").exists());
    }

    private void entoncesSeConsultaElBloqueDelUsuario() {
        verify(listarCampos).execute(new ListarCamposRequest(USUARIO_ID, BLOQUE_ID));
    }

    private void entoncesSeDevuelveUnaListaVacia() throws Exception {
        respuesta.andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    private void entoncesRespondeNoEncontradoEstandar() throws Exception {
        respuesta.andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    private void entoncesRespondeNoEncontrado() throws Exception {
        respuesta.andExpect(status().isNotFound());
    }

    private void entoncesRespondeConflicto() throws Exception {
        respuesta.andExpect(status().isConflict());
    }

    private void entoncesRespondeConflictoPorElNombreRepetido() throws Exception {
        respuesta.andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya hay un campo llamado \"Talle\" en este bloque"));
    }

    private void entoncesSeRechazaSinLlegarA(Object casoDeUso) throws Exception {
        respuesta.andExpect(status().isBadRequest());
        verifyNoInteractions(casoDeUso);
    }

    private void entoncesSeRechazaElNombreSinLlegarA(Object casoDeUso) throws Exception {
        respuesta.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.nombre").exists());
        verifyNoInteractions(casoDeUso);
    }

    private void entoncesSeRechazaLaConfiguracionConElError(String dato, String mensaje) throws Exception {
        respuesta.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors." + dato).value(mensaje));
    }

    private void entoncesSeCreaElCampoConSuUbicacion() throws Exception {
        respuesta.andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/campos/" + CAMPO_ID))
                .andExpect(jsonPath("$.nombre").value("Talle"));
    }

    private void entoncesElCasoDeUsoCreaElCampoTalle() {
        verify(crearCampo).execute(
                new CrearCampoRequest(USUARIO_ID, BLOQUE_ID, "Talle", "OPCION", true, "Chico", TALLES, null, null));
    }

    private void entoncesSeCreaElCampoSinOpcionales() {
        verify(crearCampo).execute(
                new CrearCampoRequest(USUARIO_ID, BLOQUE_ID, "Producto", "TEXTO", null, null, null, null, null));
    }

    private void entoncesSeDevuelveLaConexionConSuTabla() throws Exception {
        respuesta.andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("RELACION"))
                .andExpect(jsonPath("$.bloqueRelacionadoId").value(RELACIONADO_ID.toString()));
    }

    private void entoncesElCasoDeUsoRecibeLaTablaRelacionada() {
        verify(crearCampo).execute(new CrearCampoRequest(
                USUARIO_ID, BLOQUE_ID, "Cliente", "RELACION", null, null, null, RELACIONADO_ID, null));
    }

    private void entoncesSeDevuelveElCampoEditado() throws Exception {
        respuesta.andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Talle"));
    }

    private void entoncesElCasoDeUsoRecibeLaConfiguracionCompleta() {
        verify(editarCampo).execute(
                new EditarCampoRequest(USUARIO_ID, CAMPO_ID, "Talle", "OPCION", true, "Chico", TALLES, null, null));
    }

    private void entoncesSeEliminaElCampoDelUsuario() throws Exception {
        respuesta.andExpect(status().isNoContent());
        verify(eliminarCampo).execute(new EliminarCampoRequest(USUARIO_ID, CAMPO_ID));
    }

    // --- datos ---

    private DuplicateResourceException nombreRepetido() {
        return new DuplicateResourceException("Ya hay un campo llamado \"Talle\" en este bloque");
    }

    private CampoItem unCampo(String nombre) {
        return new CampoItem(
                CAMPO_ID, BLOQUE_ID, nombre, "OPCION", true, "Chico", TALLES, null, null, (short) 0, AHORA, AHORA);
    }

    private CampoItem unaConexion() {
        return new CampoItem(CAMPO_ID, BLOQUE_ID, "Cliente", "RELACION", false, null, List.of(), RELACIONADO_ID,
                null, (short) 0, AHORA, AHORA);
    }
}
