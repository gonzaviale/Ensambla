package com.ensambla.backend.domain.model.procesamiento;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.support.ProcesamientoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;

import static com.ensambla.backend.support.PropuestasDeLecturaDePrueba.unBloque;
import static com.ensambla.backend.support.PropuestasDeLecturaDePrueba.unBloqueConLosCampos;
import static com.ensambla.backend.support.PropuestasDeLecturaDePrueba.unCampoDeTexto;
import static com.ensambla.backend.support.PropuestasDeLecturaDePrueba.unaColumna;
import static com.ensambla.backend.support.PropuestasDeLecturaDePrueba.unaFila;
import static com.ensambla.backend.support.PropuestasDeLecturaDePrueba.unaLecturaDeVentas;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class PropuestaLecturaTest {

    private static final BloqueId SIN_DESTINO = null;
    private static final List<CampoPropuesto> SIN_LISTA_DE_NUEVOS = null;
    private static final CampoId CELULAR = CampoId.nuevo();
    private static final CampoId TELEFONO = CampoId.nuevo();
    private static final CampoPropuesto TELEFONO_LEIDO = unaColumna("telefono", "Teléfono", TipoCampo.TEXTO);
    private static final ObjectMapper JSON = JsonMapper.builder().build();

    private LecturaCuaderno lectura = ProcesamientoTestData.lectura();
    private List<BloqueLectura> bloques = List.of();
    private BloqueId destino = SIN_DESTINO;
    private PropuestaLectura propuesta;
    private PropuestaLectura releida;
    private Throwable error;

    // --- la propuesta guardada ---

    @Test
    @DisplayName("Una propuesta sin lista de campos nuevos (lectura guardada antes) la lee como vacía")
    void unaPropuestaSinListaDeCamposNuevosLaLeeComoVacia() {
        cuandoSeArmaLaPropuestaCon(SIN_LISTA_DE_NUEVOS);

        entoncesNoHayCamposNuevos();
    }

    @Test
    @DisplayName("Conserva los campos nuevos que se le pasan")
    void conservaLosCamposNuevosQueSeLePasan() {
        cuandoSeArmaLaPropuestaCon(List.of(TELEFONO_LEIDO));

        entoncesLosCamposNuevosSon(TELEFONO_LEIDO);
    }

    @Test
    @DisplayName("El constructor corto deja los campos nuevos vacíos")
    void elConstructorCortoDejaLosCamposNuevosVacios() {
        cuandoSeArmaLaPropuestaSinCamposNuevos();

        entoncesNoHayCamposNuevos();
    }

    @Test
    @DisplayName("Se guarda y se lee como JSON igual que antes")
    void seGuardaYSeLeeComoJsonIgualQueAntes() {
        dadoUnaPropuestaConUnCampoNuevo();

        cuandoSeGuardaYSeVuelveALeer();

        entoncesSeLeyoLaMismaPropuesta();
    }

    @Test
    @DisplayName("Una lectura guardada sin campos nuevos se sigue pudiendo leer")
    void unaLecturaGuardadaSinCamposNuevosSeSiguePudiendoLeer() {
        dadoUnaPropuestaConUnCampoNuevo();

        cuandoSeLeeComoSeGuardabaAntes();

        entoncesLaReleidaNoTieneCamposNuevos();
    }

    // --- sin destino ---

    @Test
    @DisplayName("Propone un bloque nuevo cuando el negocio no tiene bloques")
    void proponeUnBloqueNuevoCuandoElNegocioNoTieneBloques() {
        cuandoSeProponeDondeGuardar();

        entoncesEsUnBloqueNuevo();
        entoncesNoSeMapeaNingunaColumna();
    }

    @Test
    @DisplayName("Reutiliza el mismo bloque y mapea columnas compatibles ignorando acentos")
    void reutilizaElMismoBloqueIgnorandoAcentos() {
        BloqueLectura ventas = dadoElBloque(unBloque(" Véntas ", "Mónto", TipoCampo.NUMERO, List.of()));

        cuandoSeProponeDondeGuardar();

        entoncesSeGuardaEn(ventas);
        entoncesLaColumnaVaAlCampo("monto", ProcesamientoTestData.CAMPO_ID);
        entoncesLasFilasSonLasLeidas();
    }

    @Test
    @DisplayName("Deja una propuesta nueva si el nombre del bloque no coincide")
    void dejaUnaPropuestaNuevaSiElNombreDelBloqueNoCoincide() {
        dadoElBloque(unBloque("Gastos", "Monto", TipoCampo.NUMERO, List.of()));

        cuandoSeProponeDondeGuardar();

        entoncesNoSeMapeaNingunaColumna();
    }

    @Test
    @DisplayName("No reutiliza columnas con nombres distintos")
    void noReutilizaColumnasConNombresDistintos() {
        dadoElBloque(unBloque("Ventas", "Cantidad", TipoCampo.NUMERO, List.of()));

        cuandoSeProponeDondeGuardar();

        entoncesNoSeMapeaNingunaColumna();
    }

    @Test
    @DisplayName("No reutiliza columnas con tipos incompatibles")
    void noReutilizaColumnasConTiposIncompatibles() {
        dadoElBloque(unBloque("Ventas", "Monto", TipoCampo.TEXTO, List.of()));

        cuandoSeProponeDondeGuardar();

        entoncesNoSeMapeaNingunaColumna();
    }

    @Test
    @DisplayName("No reutiliza columnas cuyas opciones no incluyen las leídas")
    void noReutilizaColumnasCuyasOpcionesNoIncluyenLasLeidas() {
        dadoUnaLectura(ProcesamientoTestData.lectura(TipoCampo.OPCION, "B", List.of("B")));
        dadoElBloque(unBloque("Ventas", "Monto", TipoCampo.OPCION, List.of("A")));

        cuandoSeProponeDondeGuardar();

        entoncesNoSeMapeaNingunaColumna();
    }

    @Test
    @DisplayName("No decide automáticamente entre dos bloques compatibles")
    void noDecideAutomaticamenteEntreDosBloquesCompatibles() {
        dadoDosBloquesDeVentasCompatibles();

        cuandoSeProponeDondeGuardar();

        entoncesEsUnBloqueNuevo();
    }

    // --- con destino ---

    @Test
    @DisplayName("Respeta un destino explícito entre varios compatibles")
    void respetaUnDestinoExplicitoEntreVariosCompatibles() {
        BloqueLectura segundo = dadoDosBloquesDeVentasCompatibles();
        dadoQueLaPersonaEligioEntreLosCompatibles(segundo);

        cuandoSeProponeDondeGuardar();

        entoncesSeGuardaEn(segundo);
    }

    @Test
    @DisplayName("Rechaza un destino inexistente")
    void rechazaUnDestinoInexistente() {
        dadoQueLaPersonaEligioUnBloqueQueNoExiste();

        cuandoSeIntentaProponerDondeGuardar();

        entoncesSeRechazaPorIncompatible();
    }

    @Test
    @DisplayName("Con destino propone como campo nuevo la columna que el bloque no tiene, sin duplicar las que tiene")
    void conDestinoProponeComoNuevaLaColumnaQueElBloqueNoTiene() {
        BloqueLectura ventas = dadoQueLaPersonaEligio(unBloque("Ventas", "Monto", TipoCampo.NUMERO, List.of()));
        dadoUnaLecturaDeMontoYTelefono();

        cuandoSeProponeDondeGuardar();

        entoncesLosNuevosSon("telefono");
        entoncesElPrimerNuevoEsDeTipo(TipoCampo.TEXTO);
        entoncesSoloSeMapean("monto");
        entoncesLasColumnasPropuestasSon("monto", "telefono");
        entoncesSeGuardaEn(ventas);
        entoncesLasFilasSonLasLeidas();
    }

    @Test
    @DisplayName("Con destino, una columna que existe pasa a llamarse y tipificarse como el campo del bloque")
    void conDestinoUnaColumnaQueExisteAdoptaElCampoDelBloque() {
        dadoQueLaPersonaEligio(unBloque("Ventas", "Monto", TipoCampo.NUMERO, List.of()));
        dadoUnaLectura(unaLecturaDeVentas(List.of(unaColumna("monto", " MÓNTO ", TipoCampo.TEXTO)),
                unaFila(1, "monto", "100")));

        cuandoSeProponeDondeGuardar();

        entoncesLaPrimeraColumnaEs("Monto", TipoCampo.NUMERO);
        entoncesNoHayCamposNuevos();
    }

    @Test
    @DisplayName("Con destino, el nombre del bloque leído no importa: se usa el del bloque elegido")
    void conDestinoSeUsaElNombreDelBloqueElegido() {
        dadoQueLaPersonaEligio(unBloque("Mis ventas", "Monto", TipoCampo.NUMERO, List.of()));

        cuandoSeProponeDondeGuardar();

        entoncesElBloqueSeLlama("Mis ventas");
    }

    @Test
    @DisplayName("Con destino, un sinónimo del campo existente no se propone como campo nuevo")
    void conDestinoUnSinonimoDelCampoExistenteNoEsNuevo() {
        dadoQueLaPersonaEligio(unBloque("Clientes", "Celular", TipoCampo.TEXTO, List.of()));
        dadoUnaLectura(unaLecturaDeVentas(List.of(TELEFONO_LEIDO), unaFila(1, "telefono", "11 4444-5555")));

        cuandoSeProponeDondeGuardar();

        entoncesLaColumnaVaAlCampo("telefono", ProcesamientoTestData.CAMPO_ID);
        entoncesLaPrimeraColumnaSeLlama("Celular");
        entoncesNoHayCamposNuevos();
    }

    @Test
    @DisplayName("Con destino, el nombre igual gana al sinónimo y cada campo se usa una sola vez")
    void conDestinoElNombreIgualGanaAlSinonimo() {
        dadoQueLaPersonaEligio(unBloqueConLosCampos("Clientes",
                unCampoDeTexto(CELULAR, "Celular"), unCampoDeTexto(TELEFONO, "Teléfono")));
        dadoUnaLectura(unaLecturaDeVentas(
                List.of(unaColumna("cel", "Cel", TipoCampo.TEXTO), unaColumna("tel", "Teléfono", TipoCampo.TEXTO)),
                unaFila(1, "cel", "1", "tel", "2")));

        cuandoSeProponeDondeGuardar();

        entoncesLaColumnaVaAlCampo("tel", TELEFONO);
        entoncesLaColumnaVaAlCampo("cel", CELULAR);
        entoncesNoHayCamposNuevos();
    }

    @Test
    @DisplayName("Con destino, una columna que podría ser dos campos se propone como nueva y se avisa")
    void conDestinoUnaColumnaAmbiguaSeProponeComoNuevaYSeAvisa() {
        dadoQueLaPersonaEligio(unBloqueConLosCampos("Clientes", unCampoDeTexto(CampoId.nuevo(), "Celular"),
                unCampoDeTexto(ProcesamientoTestData.CAMPO_ID, "Teléfono"), unCampoDeTexto(CampoId.nuevo(), "Nombre")));
        dadoUnaLectura(unaLecturaDeVentas(
                List.of(unaColumna("nombre", "Nombre", TipoCampo.TEXTO), unaColumna("tel", "Tel", TipoCampo.TEXTO)),
                unaFila(1, "nombre", "Ana", "tel", "11 4444-5555")));

        cuandoSeProponeDondeGuardar();

        entoncesLosNuevosSon("tel");
        entoncesSeAvisaQueMenciona("«Tel»", "«Clientes»");
    }

    @Test
    @DisplayName("Con destino, una columna nueva sin datos no se propone y sus celdas se descartan")
    void conDestinoUnaColumnaNuevaSinDatosNoSePropone() {
        dadoQueLaPersonaEligio(unBloque("Ventas", "Monto", TipoCampo.NUMERO, List.of()));
        dadoUnaLecturaConColumnasVacias();

        cuandoSeProponeDondeGuardar();

        entoncesNoHayCamposNuevos();
        entoncesLasColumnasPropuestasSon("monto");
        entoncesLasFilasSoloTienen("monto");
        entoncesSeAvisa(
                "La columna «Nota» no tiene datos en la foto, así que no se propone.",
                "La columna «Color» no tiene datos en la foto, así que no se propone.",
                "La columna «Zona» no tiene datos en la foto, así que no se propone.");
    }

    @Test
    @DisplayName("Con destino, si ninguna columna coincide con el bloque la foto no es de ese bloque")
    void conDestinoSiNingunaColumnaCoincideLaFotoNoEsDeEseBloque() {
        dadoQueLaPersonaEligio(unBloque("Ventas", "Monto", TipoCampo.NUMERO, List.of()));
        dadoUnaLectura(unaLecturaDeVentas(List.of(unaColumna("color", "Color", TipoCampo.TEXTO)),
                unaFila(1, "color", "rojo")));

        cuandoSeIntentaProponerDondeGuardar();

        entoncesSeRechazaPorIncompatible();
    }

    // --- dado ---

    private void dadoUnaLectura(LecturaCuaderno leida) {
        lectura = leida;
    }

    private void dadoUnaLecturaDeMontoYTelefono() {
        dadoUnaLectura(unaLecturaDeVentas(
                List.of(unaColumna("monto", "Monto", TipoCampo.NUMERO), TELEFONO_LEIDO),
                unaFila(1, "monto", "100", "telefono", "11 4444-5555")));
    }

    private void dadoUnaLecturaConColumnasVacias() {
        dadoUnaLectura(unaLecturaDeVentas(
                List.of(unaColumna("monto", "Monto", TipoCampo.NUMERO), unaColumna("nota", "Nota", TipoCampo.TEXTO),
                        unaColumna("color", "Color", TipoCampo.TEXTO), unaColumna("zona", "Zona", TipoCampo.TEXTO)),
                unaFila(1, "monto", "100", "nota", null, "color", "  "),
                unaFila(2, "monto", "200", "nota", null)));
    }

    private BloqueLectura dadoElBloque(BloqueLectura bloque) {
        bloques = List.of(bloque);
        return bloque;
    }

    private BloqueLectura dadoDosBloquesDeVentasCompatibles() {
        BloqueLectura segundo = unBloque("Ventas", "Monto", TipoCampo.NUMERO, List.of());
        bloques = List.of(unBloque("Ventas", "Monto", TipoCampo.NUMERO, List.of()), segundo);
        return segundo;
    }

    private BloqueLectura dadoQueLaPersonaEligio(BloqueLectura bloque) {
        dadoElBloque(bloque);
        dadoQueLaPersonaEligioEntreLosCompatibles(bloque);
        return bloque;
    }

    private void dadoQueLaPersonaEligioEntreLosCompatibles(BloqueLectura bloque) {
        destino = bloque.id();
    }

    private void dadoQueLaPersonaEligioUnBloqueQueNoExiste() {
        destino = BloqueId.nuevo();
    }

    private void dadoUnaPropuestaConUnCampoNuevo() {
        propuesta = new PropuestaLectura(lectura, Map.of("monto", ProcesamientoTestData.CAMPO_ID), List.of(TELEFONO_LEIDO));
    }

    // --- cuando ---

    private void cuandoSeArmaLaPropuestaCon(List<CampoPropuesto> nuevos) {
        propuesta = new PropuestaLectura(lectura, Map.of(), nuevos);
    }

    private void cuandoSeArmaLaPropuestaSinCamposNuevos() {
        propuesta = new PropuestaLectura(lectura, Map.of());
    }

    private void cuandoSeGuardaYSeVuelveALeer() {
        releida = JSON.readValue(JSON.writeValueAsString(propuesta), PropuestaLectura.class);
    }

    private void cuandoSeLeeComoSeGuardabaAntes() {
        ObjectNode guardadaAntes = (ObjectNode) JSON.readTree(JSON.writeValueAsString(propuesta));
        guardadaAntes.remove("camposNuevos");
        releida = JSON.readValue(guardadaAntes.toString(), PropuestaLectura.class);
    }

    private void cuandoSeProponeDondeGuardar() {
        propuesta = PropuestaLectura.proponer(lectura, bloques, destino);
    }

    private void cuandoSeIntentaProponerDondeGuardar() {
        error = catchThrowable(this::cuandoSeProponeDondeGuardar);
    }

    // --- entonces ---

    private void entoncesSeLeyoLaMismaPropuesta() {
        assertThat(releida).isEqualTo(propuesta);
    }

    private void entoncesLaReleidaNoTieneCamposNuevos() {
        assertThat(releida.camposNuevos()).isEmpty();
    }

    private void entoncesNoHayCamposNuevos() {
        assertThat(propuesta.camposNuevos()).isEmpty();
    }

    private void entoncesLosCamposNuevosSon(CampoPropuesto... nuevos) {
        assertThat(propuesta.camposNuevos()).containsExactly(nuevos);
    }

    private void entoncesLosNuevosSon(String... claves) {
        assertThat(propuesta.camposNuevos()).extracting(CampoPropuesto::clave).containsExactly(claves);
        assertThat(propuesta.mapeoCampos()).doesNotContainKeys(claves);
    }

    private void entoncesElPrimerNuevoEsDeTipo(TipoCampo tipo) {
        assertThat(propuesta.camposNuevos().getFirst().tipo()).isEqualTo(tipo);
    }

    private void entoncesEsUnBloqueNuevo() {
        assertThat(propuesta.lectura().bloque().bloqueExistenteId()).isNull();
    }

    private void entoncesSeGuardaEn(BloqueLectura bloque) {
        assertThat(propuesta.lectura().bloque().bloqueExistenteId()).isEqualTo(bloque.id());
    }

    private void entoncesElBloqueSeLlama(String nombre) {
        assertThat(propuesta.lectura().bloque().nombre()).isEqualTo(nombre);
    }

    private void entoncesNoSeMapeaNingunaColumna() {
        assertThat(propuesta.mapeoCampos()).isEmpty();
    }

    private void entoncesLaColumnaVaAlCampo(String clave, CampoId campoId) {
        assertThat(propuesta.mapeoCampos()).containsEntry(clave, campoId);
    }

    private void entoncesSoloSeMapean(String... claves) {
        assertThat(propuesta.mapeoCampos()).containsOnlyKeys(claves);
    }

    private void entoncesLasColumnasPropuestasSon(String... claves) {
        assertThat(propuesta.lectura().bloque().campos()).extracting(CampoPropuesto::clave).containsExactly(claves);
    }

    private void entoncesLaPrimeraColumnaEs(String nombre, TipoCampo tipo) {
        entoncesLaPrimeraColumnaSeLlama(nombre);
        assertThat(propuesta.lectura().bloque().campos().getFirst().tipo()).isEqualTo(tipo);
    }

    private void entoncesLaPrimeraColumnaSeLlama(String nombre) {
        assertThat(propuesta.lectura().bloque().campos().getFirst().nombre()).isEqualTo(nombre);
    }

    private void entoncesLasFilasSonLasLeidas() {
        assertThat(propuesta.lectura().filas()).isEqualTo(lectura.filas());
    }

    private void entoncesLasFilasSoloTienen(String... claves) {
        assertThat(propuesta.lectura().filas()).allSatisfy(fila -> assertThat(fila.datos()).containsOnlyKeys(claves));
    }

    private void entoncesSeAvisa(String... advertencias) {
        assertThat(propuesta.lectura().advertencias()).contains(advertencias);
    }

    private void entoncesSeAvisaQueMenciona(String columna, String bloque) {
        assertThat(propuesta.lectura().advertencias()).anyMatch(aviso -> aviso.contains(columna) && aviso.contains(bloque));
    }

    private void entoncesSeRechazaPorIncompatible() {
        assertThat(error).isInstanceOf(BusinessRuleException.class).hasMessageContaining("compatible");
    }
}
