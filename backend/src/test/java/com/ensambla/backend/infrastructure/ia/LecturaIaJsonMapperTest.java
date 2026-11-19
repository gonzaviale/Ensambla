package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.lectura.BloqueConocido;
import com.ensambla.backend.domain.model.lectura.CampoContexto;
import com.ensambla.backend.domain.model.lectura.ConsumoIa;
import com.ensambla.backend.domain.model.lectura.DatoLeido;
import com.ensambla.backend.domain.model.lectura.FilaLeida;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.lectura.TipoRegistroCuaderno;
import com.ensambla.backend.infrastructure.ia.LecturaIaJson.BloqueJson;
import com.ensambla.backend.infrastructure.ia.LecturaIaJson.CampoJson;
import com.ensambla.backend.infrastructure.ia.LecturaIaJson.DatoJson;
import com.ensambla.backend.infrastructure.ia.LecturaIaJson.FilaJson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class LecturaIaJsonMapperTest {

    private static final int PAGINAS = 2;
    private static final BloqueId BLOQUE_VENTAS_ID = BloqueId.nuevo();
    private static final List<BloqueConocido> BLOQUES = List.of(
            new BloqueConocido(BLOQUE_VENTAS_ID, "Ventas", List.of(
                    new CampoContexto("Fecha", TipoCampo.FECHA, List.of()),
                    new CampoContexto("Monto", TipoCampo.NUMERO, List.of()))));
    private static final ConsumoIa CONSUMO = new ConsumoIa("simulado", "modelo", "v1", PAGINAS, 10, 20, 5);
    private static final CampoJson FECHA = new CampoJson("fecha", "Fecha", "FECHA", List.of());
    private static final CampoJson MONTO = new CampoJson("monto", "Monto", "numero", null);

    private LecturaIaJsonMapper mapper;
    private LecturaIaJson json;
    private List<BloqueConocido> bloquesDelNegocio = BLOQUES;
    private LecturaCuaderno lectura;
    private Throwable rechazo;

    @BeforeEach
    void setUp() {
        mapper = new LecturaIaJsonMapper();
    }

    @Test
    @DisplayName("Convierte una lectura válida con tipo, bloque, filas, advertencias y consumo")
    void convierteUnaLecturaValidaConTipoBloqueFilasAdvertenciasYConsumo() {
        dadoUnaLecturaValidaConUnaFilaYUnaAdvertencia();

        cuandoSeConvierte();

        entoncesLaLecturaQuedaConvertida();
    }

    @Test
    @DisplayName("Sin filas ni advertencias devuelve listas vacías")
    void sinFilasNiAdvertenciasDevuelveListasVacias() {
        dadoUnaLecturaSinFilasNiAdvertencias();

        cuandoSeConvierte();

        entoncesNoHayFilasNiAdvertencias();
    }

    @Test
    @DisplayName("Una fila sin datos queda vacía")
    void unaFilaSinDatosQuedaVacia() {
        dadoUnaLecturaConUnaFilaSinDatos();

        cuandoSeConvierte();

        entoncesLaFilaQuedaVaciaEnLaPaginaYRenglonPedidos();
    }

    @Test
    @DisplayName("Acepta el bloque existente que sugiere la IA si es del negocio")
    void aceptaElBloqueExistenteQueSugiereLaIaSiEsDelNegocio() {
        dadoUnaLecturaQueSugiereUnBloqueDelNegocioEnMayusculas();

        cuandoSeConvierte();

        entoncesSeConservaElBloqueExistente();
    }

    @Test
    @DisplayName("Descarta un bloque existente que no es del negocio y propone uno nuevo")
    void descartaUnBloqueExistenteQueNoEsDelNegocioYProponeUnoNuevo() {
        dadoUnaLecturaQueSugiereUnBloqueAjeno();

        cuandoSeConvierte();

        entoncesSeProponeUnBloqueNuevo();
    }

    @Test
    @DisplayName("Acepta un campo de conexión que el bloque existente elegido ya tiene")
    void aceptaUnCampoDeConexionQueElBloqueExistenteElegidoYaTiene() {
        dadoUnBloqueExistenteConUnCampoDeConexionYUnaLecturaQueLoUsa();

        cuandoSeConvierte();

        entoncesElCampoSigueSiendoUnaConexion();
    }

    @Test
    @DisplayName("Una conexión que el bloque no tiene se lee como el texto escrito")
    void unaConexionQueElBloqueNoTieneSeLeeComoElTextoEscrito() {
        dadoUnaLecturaConUnaConexionInventada();

        cuandoSeConvierte();

        entoncesElCampoSeLeeComoTexto();
    }

    @Test
    @DisplayName("Una conexión sin nombre se rechaza como cualquier campo sin nombre")
    void unaConexionSinNombreSeRechazaComoCualquierCampoSinNombre() {
        dadoUnaLecturaConUnaConexionSinNombre();

        cuandoSeIntentaConvertir();

        entoncesSeRechazaPor("falta el nombre del campo cliente");
    }

    @Test
    @DisplayName("Acepta un campo OPCION con opciones")
    void aceptaUnCampoOpcionConOpciones() {
        dadoUnaLecturaConUnCampoOpcionConOpciones();

        cuandoSeConvierte();

        entoncesElCampoTieneSusOpciones();
    }

    @Test
    @DisplayName("Rechaza un tipo de registro desconocido")
    void rechazaUnTipoDeRegistroDesconocido() {
        json = new LecturaIaJson("RECETAS", bloqueConCampos(null, FECHA), List.of(), List.of());

        cuandoSeIntentaConvertir();

        entoncesSeRechazaPor("tipoRegistro es desconocido: RECETAS");
    }

    @Test
    @DisplayName("Rechaza una lectura sin tipo de registro")
    void rechazaUnaLecturaSinTipoDeRegistro() {
        json = new LecturaIaJson(null, bloqueConCampos(null, FECHA), List.of(), List.of());

        cuandoSeIntentaConvertir();

        entoncesSeRechazaPor("falta tipoRegistro");
    }

    @Test
    @DisplayName("Rechaza una lectura sin bloque")
    void rechazaUnaLecturaSinBloque() {
        json = lecturaDe(null, List.of(), List.of());

        cuandoSeIntentaConvertir();

        entoncesSeRechazaPor("falta el bloque propuesto");
    }

    @Test
    @DisplayName("Rechaza un bloque sin campos")
    void rechazaUnBloqueSinCampos() {
        json = lecturaDe(new BloqueJson("Ventas", null, null), List.of(), List.of());

        cuandoSeIntentaConvertir();

        entoncesSeRechazaPor("el bloque no tiene campos");
    }

    @Test
    @DisplayName("Rechaza un bloque con nombre en blanco")
    void rechazaUnBloqueConNombreEnBlanco() {
        json = lecturaDe(new BloqueJson("  ", null, List.of(FECHA)), List.of(), List.of());

        cuandoSeIntentaConvertir();

        entoncesSeRechazaPor("falta el nombre del bloque");
    }

    @Test
    @DisplayName("Rechaza un campo con tipo desconocido")
    void rechazaUnCampoConTipoDesconocido() {
        dadoUnaLecturaConUnCampo(new CampoJson("foto", "Foto", "IMAGEN", List.of()));

        cuandoSeIntentaConvertir();

        entoncesSeRechazaPor("el tipo del campo foto es desconocido");
    }

    @Test
    @DisplayName("Rechaza un campo OPCION sin opciones")
    void rechazaUnCampoOpcionSinOpciones() {
        dadoUnaLecturaConUnCampo(new CampoJson("producto", "Producto", "OPCION", List.of()));

        cuandoSeIntentaConvertir();

        entoncesSeRechazaPor("es OPCION y no tiene opciones");
    }

    @Test
    @DisplayName("Rechaza un campo sin clave")
    void rechazaUnCampoSinClave() {
        dadoUnaLecturaConUnCampo(new CampoJson(null, "Fecha", "FECHA", List.of()));

        cuandoSeIntentaConvertir();

        entoncesSeRechazaPor("falta la clave de un campo");
    }

    @Test
    @DisplayName("Rechaza claves de campo repetidas")
    void rechazaClavesDeCampoRepetidas() {
        json = lecturaDe(bloqueConCampos(null, FECHA, FECHA), List.of(), List.of());

        cuandoSeIntentaConvertir();

        entoncesSeRechazaPor("la clave fecha está repetida");
    }

    @Test
    @DisplayName("Rechaza una fila que menciona una página que no se envió")
    void rechazaUnaFilaQueMencionaUnaPaginaQueNoSeEnvio() {
        dadoUnaLecturaConUnaFila(filaDe(3, 1, Map.of()));

        cuandoSeIntentaConvertir();

        entoncesSeRechazaPor("la página 3 y sólo se enviaron 2");
    }

    @Test
    @DisplayName("Rechaza una fila sin página")
    void rechazaUnaFilaSinPagina() {
        dadoUnaLecturaConUnaFila(filaDe(null, 1, Map.of()));

        cuandoSeIntentaConvertir();

        entoncesSeRechazaPor("la página de una fila debe ser 1 o mayor");
    }

    @Test
    @DisplayName("Rechaza una fila con renglón cero")
    void rechazaUnaFilaConRenglonCero() {
        dadoUnaLecturaConUnaFila(filaDe(1, 0, Map.of()));

        cuandoSeIntentaConvertir();

        entoncesSeRechazaPor("el renglón de una fila debe ser 1 o mayor");
    }

    @Test
    @DisplayName("Rechaza una fila que usa una clave que no está entre los campos")
    void rechazaUnaFilaQueUsaUnaClaveQueNoEstaEntreLosCampos() {
        dadoUnaLecturaConUnaFila(filaDe(1, 1, Map.of("cliente", new DatoJson("Marta", 0.9))));

        cuandoSeIntentaConvertir();

        entoncesSeRechazaPor("la clave cliente que no está entre los campos");
    }

    @Test
    @DisplayName("Rechaza un dato nulo")
    void rechazaUnDatoNulo() {
        dadoUnaLecturaConUnDatoNulo();

        cuandoSeIntentaConvertir();

        entoncesSeRechazaPor("la confianza de fecha debe estar entre 0 y 1");
    }

    @Test
    @DisplayName("Rechaza un dato sin confianza")
    void rechazaUnDatoSinConfianza() {
        dadoUnaLecturaConUnDatoDeConfianza(null);

        cuandoSeIntentaConvertir();

        entoncesSeRechazaPor("la confianza de fecha debe estar entre 0 y 1");
    }

    @Test
    @DisplayName("Rechaza una confianza negativa")
    void rechazaUnaConfianzaNegativa() {
        dadoUnaLecturaConUnDatoDeConfianza(-0.1);

        cuandoSeIntentaConvertir();

        entoncesSeRechazaPor("la confianza de fecha debe estar entre 0 y 1");
    }

    @Test
    @DisplayName("Rechaza una confianza mayor a 1")
    void rechazaUnaConfianzaMayorA1() {
        dadoUnaLecturaConUnDatoDeConfianza(1.5);

        cuandoSeIntentaConvertir();

        entoncesSeRechazaPor("la confianza de fecha debe estar entre 0 y 1");
    }

    // --- dado ---

    private void dadoUnaLecturaValidaConUnaFilaYUnaAdvertencia() {
        json = lecturaDe(bloqueConCampos(null, FECHA, MONTO),
                List.of(filaDe(1, 1, Map.of("fecha", new DatoJson("2026-10-03", 0.9),
                        "monto", new DatoJson("100", 0.5)))),
                List.of("Página borrosa"));
    }

    private void dadoUnaLecturaSinFilasNiAdvertencias() {
        json = lecturaDe(bloqueConCampos(null, FECHA), null, null);
    }

    private void dadoUnaLecturaConUnaFilaSinDatos() {
        json = lecturaDe(bloqueConCampos(null, FECHA), List.of(filaDe(2, 3, null)), List.of());
    }

    private void dadoUnaLecturaQueSugiereUnBloqueDelNegocioEnMayusculas() {
        json = lecturaDe(bloqueConCampos(BLOQUE_VENTAS_ID.toString().toUpperCase(), FECHA), List.of(), List.of());
    }

    private void dadoUnaLecturaQueSugiereUnBloqueAjeno() {
        json = lecturaDe(bloqueConCampos(UUID.randomUUID().toString(), FECHA), List.of(), List.of());
    }

    private void dadoUnBloqueExistenteConUnCampoDeConexionYUnaLecturaQueLoUsa() {
        bloquesDelNegocio = List.of(new BloqueConocido(BLOQUE_VENTAS_ID, "Ventas", List.of(
                new CampoContexto("Cliente", TipoCampo.RELACION, List.of(), "Clientes"))));
        CampoJson cliente = new CampoJson("cliente", " cliente ", "RELACION", List.of());
        json = lecturaDe(bloqueConCampos(BLOQUE_VENTAS_ID.toString(), cliente), List.of(), List.of());
    }

    private void dadoUnaLecturaConUnaConexionInventada() {
        dadoUnaLecturaConUnCampo(new CampoJson("cliente", "Cliente", "RELACION", List.of()));
    }

    private void dadoUnaLecturaConUnaConexionSinNombre() {
        dadoUnaLecturaConUnCampo(new CampoJson("cliente", null, "RELACION", List.of()));
    }

    private void dadoUnaLecturaConUnCampoOpcionConOpciones() {
        dadoUnaLecturaConUnCampo(new CampoJson("producto", "Producto", "OPCION", List.of("Vela", "Difusor")));
    }

    private void dadoUnaLecturaConUnCampo(CampoJson campo) {
        json = lecturaDe(bloqueConCampos(null, campo), List.of(), List.of());
    }

    private void dadoUnaLecturaConUnaFila(FilaJson fila) {
        json = lecturaDe(bloqueConCampos(null, FECHA), List.of(fila), List.of());
    }

    private void dadoUnaLecturaConUnDatoNulo() {
        Map<String, DatoJson> datos = new HashMap<>();
        datos.put("fecha", null);
        dadoUnaLecturaConUnaFila(filaDe(1, 1, datos));
    }

    private void dadoUnaLecturaConUnDatoDeConfianza(Double confianza) {
        dadoUnaLecturaConUnaFila(filaDe(1, 1, Map.of("fecha", new DatoJson("2026-10-03", confianza))));
    }

    // --- cuando ---

    private void cuandoSeConvierte() {
        lectura = mapper.toDomain(json, bloquesDelNegocio, PAGINAS, CONSUMO);
    }

    private void cuandoSeIntentaConvertir() {
        rechazo = catchThrowable(this::cuandoSeConvierte);
    }

    // --- entonces ---

    private void entoncesLaLecturaQuedaConvertida() {
        assertThat(lectura.tipoRegistro()).isEqualTo(TipoRegistroCuaderno.VENTAS);
        assertThat(lectura.bloque().nombre()).isEqualTo("Ventas");
        assertThat(lectura.bloque().bloqueExistente()).isEmpty();
        assertThat(lectura.bloque().campos()).extracting(campo -> campo.tipo())
                .containsExactly(TipoCampo.FECHA, TipoCampo.NUMERO);
        assertThat(lectura.bloque().campos().get(1).opciones()).isEmpty();
        FilaLeida fila = lectura.filas().getFirst();
        assertThat(fila.datos()).containsEntry("fecha", new DatoLeido("2026-10-03", 0.9))
                .containsEntry("monto", new DatoLeido("100", 0.5));
        assertThat(lectura.advertencias()).containsExactly("Página borrosa");
        assertThat(lectura.consumo()).isEqualTo(CONSUMO);
    }

    private void entoncesNoHayFilasNiAdvertencias() {
        assertThat(lectura.filas()).isEmpty();
        assertThat(lectura.advertencias()).isEmpty();
    }

    private void entoncesLaFilaQuedaVaciaEnLaPaginaYRenglonPedidos() {
        FilaLeida fila = lectura.filas().getFirst();
        assertThat(fila.pagina()).isEqualTo(2);
        assertThat(fila.renglon()).isEqualTo(3);
        assertThat(fila.datos()).isEmpty();
    }

    private void entoncesSeConservaElBloqueExistente() {
        assertThat(lectura.bloque().bloqueExistente()).contains(BLOQUE_VENTAS_ID);
    }

    private void entoncesSeProponeUnBloqueNuevo() {
        assertThat(lectura.bloque().bloqueExistente()).isEmpty();
    }

    private void entoncesElCampoSigueSiendoUnaConexion() {
        assertThat(lectura.bloque().campos().getFirst().tipo()).isEqualTo(TipoCampo.RELACION);
    }

    private void entoncesElCampoSeLeeComoTexto() {
        assertThat(lectura.bloque().campos().getFirst().tipo()).isEqualTo(TipoCampo.TEXTO);
    }

    private void entoncesElCampoTieneSusOpciones() {
        assertThat(lectura.bloque().campos().getFirst().opciones()).containsExactly("Vela", "Difusor");
    }

    private void entoncesSeRechazaPor(String detalle) {
        assertThat(rechazo)
                .isInstanceOf(LecturaCuadernoException.class)
                .hasMessageStartingWith(LecturaIaJsonMapper.PREFIJO_ERROR)
                .hasMessageContaining(detalle);
    }

    // --- datos ---

    private static LecturaIaJson lecturaDe(BloqueJson bloque, List<FilaJson> filas, List<String> advertencias) {
        return new LecturaIaJson("ventas", bloque, filas, advertencias);
    }

    private static BloqueJson bloqueConCampos(String bloqueExistenteId, CampoJson... campos) {
        return new BloqueJson(" Ventas ", bloqueExistenteId, List.of(campos));
    }

    private static FilaJson filaDe(Integer pagina, Integer renglon, Map<String, DatoJson> datos) {
        return new FilaJson(pagina, renglon, datos);
    }
}
