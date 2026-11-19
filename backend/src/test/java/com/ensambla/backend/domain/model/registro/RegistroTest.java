package com.ensambla.backend.domain.model.registro;

import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.lote.FotoId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class RegistroTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final BloqueId OTRO_BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    private static final CampoId NOMBRE_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final CampoId CANTIDAD_ID = CampoId.de("c0000000-0000-4000-8000-000000000002");
    private static final CampoId TOTAL_ID = CampoId.de("c0000000-0000-4000-8000-000000000003");
    private static final FotoId FOTO_ID = FotoId.de("f0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-10T12:00:00Z");
    private static final OffsetDateTime ANTES = OffsetDateTime.parse("2026-10-09T12:00:00Z");

    private Registro registro;
    private Registro conFormulas;
    private Map<String, Object> validos;
    private Map<String, Object> combinados;
    private FotoId fotoDeOrigen;
    private Throwable rechazo;

    @Test
    @DisplayName("Un registro nuevo nace sin id y con la misma fecha de creación y de modificación")
    void unRegistroNuevoNaceSinIdYConLaMismaFechaDeCreacionYDeModificacion() {
        cuandoSeCreaUnRegistroNuevo();

        entoncesNaceSinIdConLosDatosYLasDosFechas();
    }

    @Test
    @DisplayName("Un registro del cuaderno guarda la foto de la que salió")
    void unRegistroDelCuadernoGuardaLaFotoDeLaQueSalio() {
        cuandoSeCreaUnRegistroDesdeElCuaderno();

        entoncesGuardaLaFotoDeOrigen();
    }

    @Test
    @DisplayName("Devuelve la foto de origen del registro del cuaderno")
    void devuelveLaFotoDeOrigenDelRegistroDelCuaderno() {
        dadoUnRegistroDelCuaderno();

        cuandoSePideLaFotoDeOrigen();

        entoncesLaFotoDeOrigenEsLaDelCuaderno();
    }

    @Test
    @DisplayName("Un registro cargado a mano no tiene foto de origen")
    void unRegistroCargadoAManoNoTieneFotoDeOrigen() {
        dadoUnRegistroCargadoAMano();

        cuandoSeIntentaPedirLaFotoDeOrigen();

        entoncesSeRechazaPorFaltaDeFotoDeOrigen();
    }

    @Test
    @DisplayName("Valida y devuelve los valores de los campos sin espacios en los bordes")
    void validaYDevuelveLosValoresDeLosCamposSinEspaciosEnLosBordes() {
        cuandoSeValida(campos(true), Map.of(NOMBRE_ID.toString(), "  Harina  ", CANTIDAD_ID.toString(), 12));

        entoncesLosValoresValidosSon(Map.of(NOMBRE_ID.toString(), "Harina", CANTIDAD_ID.toString(), 12));
    }

    @Test
    @DisplayName("Descarta las claves que no son campos del bloque")
    void descartaLasClavesQueNoSonCamposDelBloque() {
        cuandoSeValida(campos(true), Map.of(NOMBRE_ID.toString(), "Harina", "otro", "dato"));

        entoncesSoloQuedaElValorDe(NOMBRE_ID);
    }

    @Test
    @DisplayName("Omite los datos vacíos de los campos opcionales")
    void omiteLosDatosVaciosDeLosCamposOpcionales() {
        cuandoSeValida(campos(true), Map.of(NOMBRE_ID.toString(), "Harina", CANTIDAD_ID.toString(), "   "));

        entoncesSoloQuedaElValorDe(NOMBRE_ID);
    }

    @Test
    @DisplayName("Marca como obligatorio al campo requerido que no se cargó")
    void marcaComoObligatorioAlCampoRequeridoQueNoSeCargo() {
        cuandoSeIntentaValidar(campos(true), Map.of(CANTIDAD_ID.toString(), 3));

        entoncesSeRechazaConLosErrores(Map.of(NOMBRE_ID.toString(), "Este dato es obligatorio"));
    }

    @Test
    @DisplayName("Junta el error de cada campo inválido")
    void juntaElErrorDeCadaCampoInvalido() {
        cuandoSeIntentaValidar(campos(true), Map.of(NOMBRE_ID.toString(), 5, CANTIDAD_ID.toString(), "muchos"));

        entoncesSeRechazaConLosErrores(
                Map.of(NOMBRE_ID.toString(), "Tiene que ser un texto", CANTIDAD_ID.toString(), "Tiene que ser un número"));
    }

    @Test
    @DisplayName("Acepta un registro sin datos cuando ningún campo es obligatorio")
    void aceptaUnRegistroSinDatosCuandoNingunCampoEsObligatorio() {
        cuandoSeValida(campos(false), Map.of());

        entoncesNoQuedaNingunValor();
    }

    @Test
    @DisplayName("Combina los valores actuales con los cambios sin modificar el registro")
    void combinaLosValoresActualesConLosCambiosSinModificarElRegistro() {
        dadoUnRegistroConNombreYCantidad();

        cuandoSeCombinaConUnaCantidadNueva();

        entoncesSeCombinanLosValoresSinModificarElRegistro();
    }

    @Test
    @DisplayName("Un cambio nulo reemplaza el valor actual para poder borrarlo")
    void unCambioNuloReemplazaElValorActualParaPoderBorrarlo() {
        dadoUnRegistroConUnaCantidadDeTres();

        cuandoSeCombinaConUnCambioNulo();

        entoncesLaCantidadCombinadaEsNula();
    }

    @Test
    @DisplayName("Reemplazar los valores actualiza la fecha de modificación y respeta la de creación")
    void reemplazarLosValoresActualizaLaFechaDeModificacionYRespetaLaDeCreacion() {
        dadoUnRegistroCreadoAyer();

        cuandoSeReemplazanLosValores();

        entoncesSeActualizaLaModificacionYSeRespetaLaCreacion();
    }

    @Test
    @DisplayName("Calcula el valor de los campos fórmula sin tocar los valores guardados")
    void calculaElValorDeLosCamposFormulaSinTocarLosValoresGuardados() {
        dadoUnRegistroConUnaCantidadDeCuatro();

        cuandoSeCalculaUnaFormulaDeUnoYMedio();

        entoncesLaCopiaTieneElTotalSeisYElOriginalNo();
    }

    @Test
    @DisplayName("Una fórmula a la que le falta un dato queda sin valor")
    void unaFormulaALaQueLeFaltaUnDatoQuedaSinValor() {
        dadoUnRegistroConUnaCantidadQueNoEsNumero();

        cuandoSeCalculaUnaFormulaQueDuplicaLaCantidad();

        entoncesLaFormulaQuedaSinValor();
    }

    @Test
    @DisplayName("Ignora lo que se mande en un campo fórmula al validar")
    void ignoraLoQueSeMandeEnUnCampoFormulaAlValidar() {
        Campo formula = dadoUnCampoFormulaObligatorio();

        cuandoSeValida(List.of(formula), Map.of(formula.getId().toString(), 99));

        entoncesNoQuedaNingunValor();
    }

    @Test
    @DisplayName("Un registro del bloque pasa la comprobación")
    void unRegistroDelBloquePasaLaComprobacion() {
        dadoUnRegistroNuevoDelBloque();

        cuandoSeIntentaExigirElBloque(BLOQUE_ID);

        entoncesNoSeRechaza();
    }

    @Test
    @DisplayName("Un registro de otro bloque responde 'no existe'")
    void unRegistroDeOtroBloqueRespondeNoExiste() {
        dadoUnRegistroNuevoDelBloque();

        cuandoSeIntentaExigirElBloque(OTRO_BLOQUE_ID);

        entoncesSeRechazaPorRegistroInexistente();
    }

    @Test
    @DisplayName("Una fila marcada como ejemplo de la plantilla es de ejemplo")
    void unaFilaMarcadaComoEjemploDeLaPlantillaEsDeEjemplo() {
        dadoUnRegistroMarcadoComoEjemplo(true);

        entoncesEsDeEjemplo(true);
    }

    @Test
    @DisplayName("Una fila cargada por el negocio, o sin la marca, no es de ejemplo")
    void unaFilaCargadaPorElNegocioOSinLaMarcaNoEsDeEjemplo() {
        dadoUnRegistroMarcadoComoEjemplo(false);
        entoncesEsDeEjemplo(false);

        dadoUnRegistroMarcadoComoEjemplo(null);
        entoncesEsDeEjemplo(false);
    }

    // --- dado ---
    private void dadoUnRegistroDelCuaderno() {
        registro = Registro.desdeCuaderno(NEGOCIO_ID, BLOQUE_ID, Map.of(), FOTO_ID, AHORA);
    }

    private void dadoUnRegistroCargadoAMano() {
        registro = Registro.nuevo(NEGOCIO_ID, BLOQUE_ID, Map.of(), AHORA);
    }

    private void dadoUnRegistroNuevoDelBloque() {
        registro = Registro.nuevo(NEGOCIO_ID, BLOQUE_ID, Map.of(), AHORA);
    }

    private void dadoUnRegistroConNombreYCantidad() {
        registro = unRegistroConValores(Map.of(NOMBRE_ID.toString(), "Harina", CANTIDAD_ID.toString(), 3));
    }

    private void dadoUnRegistroConUnaCantidadDeTres() {
        registro = unRegistroConValores(Map.of(CANTIDAD_ID.toString(), 3));
    }

    private void dadoUnRegistroConUnaCantidadDeCuatro() {
        registro = unRegistroConValores(Map.of(CANTIDAD_ID.toString(), 4));
    }

    private void dadoUnRegistroConUnaCantidadQueNoEsNumero() {
        registro = unRegistroConValores(Map.of(CANTIDAD_ID.toString(), "no es número"));
    }

    private void dadoUnRegistroCreadoAyer() {
        registro = unRegistroConValores(Map.of("k", "v"));
    }

    private void dadoUnRegistroMarcadoComoEjemplo(Boolean esEjemplo) {
        registro = Registro.builder().esEjemplo(esEjemplo).build();
    }

    private Campo dadoUnCampoFormulaObligatorio() {
        return Campo.builder().id(CampoId.nuevo()).tipo(TipoCampo.FORMULA).obligatorio(true).formula("1 + 1").build();
    }

    private List<Campo> campos(boolean nombreObligatorio) {
        return List.of(
                Campo.builder().id(NOMBRE_ID).tipo(TipoCampo.TEXTO).obligatorio(nombreObligatorio).build(),
                Campo.builder().id(CANTIDAD_ID).tipo(TipoCampo.NUMERO).obligatorio(false).build());
    }

    private Campo unCampoFormula(String formula) {
        return Campo.builder().id(TOTAL_ID).tipo(TipoCampo.FORMULA).formula(formula).build();
    }

    private Registro unRegistroConValores(Map<String, Object> valores) {
        return Registro.builder()
                .id(RegistroId.nuevo())
                .valores(valores)
                .creadoEn(ANTES)
                .actualizadoEn(ANTES)
                .build();
    }

    // --- cuando ---
    private void cuandoSeCreaUnRegistroNuevo() {
        registro = Registro.nuevo(NEGOCIO_ID, BLOQUE_ID, Map.of("k", "v"), AHORA);
    }

    private void cuandoSeCreaUnRegistroDesdeElCuaderno() {
        registro = Registro.desdeCuaderno(NEGOCIO_ID, BLOQUE_ID, Map.of("k", "v"), FOTO_ID, AHORA);
    }

    private void cuandoSePideLaFotoDeOrigen() {
        fotoDeOrigen = registro.exigirFotoOrigen();
    }

    private void cuandoSeIntentaPedirLaFotoDeOrigen() {
        rechazo = catchThrowable(this::cuandoSePideLaFotoDeOrigen);
    }

    private void cuandoSeValida(List<Campo> campos, Map<String, Object> valores) {
        validos = Registro.validar(campos, valores);
    }

    private void cuandoSeIntentaValidar(List<Campo> campos, Map<String, Object> valores) {
        rechazo = catchThrowable(() -> cuandoSeValida(campos, valores));
    }

    private void cuandoSeCombinaConUnaCantidadNueva() {
        combinados = registro.combinarCon(Map.of(CANTIDAD_ID.toString(), 8));
    }

    private void cuandoSeCombinaConUnCambioNulo() {
        Map<String, Object> cambios = new HashMap<>();
        cambios.put(CANTIDAD_ID.toString(), null);
        combinados = registro.combinarCon(cambios);
    }

    private void cuandoSeReemplazanLosValores() {
        registro.reemplazarValores(Map.of("k", "nuevo"), AHORA);
    }

    private void cuandoSeCalculaUnaFormulaDeUnoYMedio() {
        conFormulas = registro.conFormulas(List.of(campos(false).get(1), unCampoFormula("{" + CANTIDAD_ID + "} * 1.5")));
    }

    private void cuandoSeCalculaUnaFormulaQueDuplicaLaCantidad() {
        conFormulas = registro.conFormulas(List.of(unCampoFormula("{" + CANTIDAD_ID + "} * 2")));
    }

    private void cuandoSeIntentaExigirElBloque(BloqueId bloqueId) {
        rechazo = catchThrowable(() -> registro.exigirDelBloque(bloqueId));
    }

    // --- entonces ---
    private void entoncesNaceSinIdConLosDatosYLasDosFechas() {
        assertThat(registro.getId()).isNull();
        assertThat(registro.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(registro.getBloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(registro.getValores()).containsEntry("k", "v");
        assertThat(registro.getFotoOrigenId()).isNull();
        assertThat(registro.getCreadoEn()).isEqualTo(AHORA);
        assertThat(registro.getActualizadoEn()).isEqualTo(AHORA);
    }

    private void entoncesGuardaLaFotoDeOrigen() {
        assertThat(registro.getId()).isNull();
        assertThat(registro.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(registro.getBloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(registro.getValores()).containsEntry("k", "v");
        assertThat(registro.getFotoOrigenId()).isEqualTo(FOTO_ID);
        assertThat(registro.getCreadoEn()).isEqualTo(AHORA);
    }

    private void entoncesLaFotoDeOrigenEsLaDelCuaderno() {
        assertThat(fotoDeOrigen).isEqualTo(FOTO_ID);
    }

    private void entoncesSeRechazaPorFaltaDeFotoDeOrigen() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El registro no tiene foto de origen");
    }

    private void entoncesLosValoresValidosSon(Map<String, Object> esperados) {
        assertThat(validos).containsAllEntriesOf(esperados).hasSize(esperados.size());
    }

    private void entoncesSoloQuedaElValorDe(CampoId campoId) {
        assertThat(validos).containsOnlyKeys(campoId.toString());
    }

    private void entoncesNoQuedaNingunValor() {
        assertThat(validos).isEmpty();
    }

    private void entoncesSeRechazaConLosErrores(Map<String, String> errores) {
        assertThat(rechazo).isInstanceOfSatisfying(RegistroInvalidoException.class,
                error -> assertThat(error.getErrores()).containsExactlyInAnyOrderEntriesOf(errores));
    }

    private void entoncesSeCombinanLosValoresSinModificarElRegistro() {
        assertThat(combinados)
                .containsEntry(NOMBRE_ID.toString(), "Harina")
                .containsEntry(CANTIDAD_ID.toString(), 8);
        assertThat(registro.getValores()).containsEntry(CANTIDAD_ID.toString(), 3);
    }

    private void entoncesLaCantidadCombinadaEsNula() {
        assertThat(combinados).containsEntry(CANTIDAD_ID.toString(), null);
    }

    private void entoncesSeActualizaLaModificacionYSeRespetaLaCreacion() {
        assertThat(registro.getValores()).containsEntry("k", "nuevo");
        assertThat(registro.getActualizadoEn()).isEqualTo(AHORA);
        assertThat(registro.getCreadoEn()).isEqualTo(ANTES);
    }

    private void entoncesLaCopiaTieneElTotalSeisYElOriginalNo() {
        assertThat(conFormulas.getValores()).containsEntry(TOTAL_ID.toString(), new java.math.BigDecimal("6"));
        assertThat(conFormulas.getId()).isEqualTo(registro.getId());
        assertThat(registro.getValores()).doesNotContainKey(TOTAL_ID.toString());
    }

    private void entoncesLaFormulaQuedaSinValor() {
        assertThat(conFormulas.getValores()).doesNotContainKey(TOTAL_ID.toString());
    }

    private void entoncesNoSeRechaza() {
        assertThat(rechazo).isNull();
    }

    private void entoncesSeRechazaPorRegistroInexistente() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class).hasMessage("El registro no existe");
    }

    private void entoncesEsDeEjemplo(boolean esperado) {
        assertThat(registro.esDeEjemplo()).isEqualTo(esperado);
    }
}
