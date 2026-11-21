package com.ensambla.backend.domain.model.bloque;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static com.ensambla.backend.support.SistemasDePrueba.unaSeccionVaciaDe;
import static org.assertj.core.api.Assertions.assertThat;

class ColumnaFijaTest {

    private static final List<Bloque> SIN_SECCIONES = List.of();

    private ColumnaFija columna;
    private Campo campo;
    private Campo creado;
    private Bloque stock;
    private Bloque clientes;

    @Test
    @DisplayName("Un campo del tipo de dato correcto cumple la columna")
    void unCampoDelTipoDeDatoCorrectoCumpleLaColumna() {
        dadaLaColumna(ColumnaFija.moneda("price", "Precio", "cobrar"));
        dadoUnCampo(TipoCampo.MONEDA);

        entoncesLaCumple();
    }

    @Test
    @DisplayName("Un campo de otro tipo de dato no cumple la columna")
    void unCampoDeOtroTipoDeDatoNoCumpleLaColumna() {
        dadaLaColumna(ColumnaFija.moneda("price", "Precio", "cobrar"));
        dadoUnCampo(TipoCampo.TEXTO);

        entoncesNoLaCumple();
    }

    @Test
    @DisplayName("Una columna siempre obligatoria no la cumple un campo opcional aunque sea del tipo correcto")
    void unaColumnaSiempreObligatoriaNoLaCumpleUnCampoOpcional() {
        dadaLaColumna(ColumnaFija.numero("quantity", "Cantidad", "descontar").siempreObligatoria());
        dadoUnCampoOpcional(TipoCampo.NUMERO);

        entoncesTieneElTipoCorrectoPeroNoCumpleLoDemas();
    }

    @Test
    @DisplayName("Una columna siempre obligatoria la cumple un campo obligatorio")
    void unaColumnaSiempreObligatoriaLaCumpleUnCampoObligatorio() {
        dadaLaColumna(ColumnaFija.numero("quantity", "Cantidad", "descontar").siempreObligatoria());
        dadoUnCampo(TipoCampo.NUMERO);

        entoncesLaCumple();
    }

    @Test
    @DisplayName("Una lista tiene que conservar las opciones que Ensambla necesita")
    void unaListaTieneQueConservarLasOpcionesQueEnsamblaNecesita() {
        dadaLaColumna(categoria());
        dadoUnCampoDeListaCon("Servicios");

        entoncesTieneElTipoCorrectoPeroNoCumpleLoDemas();
    }

    @Test
    @DisplayName("Una lista que conserva sus opciones y suma otras cumple la columna")
    void unaListaQueConservaSusOpcionesYSumaOtrasCumpleLaColumna() {
        dadaLaColumna(categoria());
        dadoUnCampoDeListaCon("Mercadería", "Otros");

        entoncesLaCumple();
    }

    @Test
    @DisplayName("Una conexión tiene que apuntar a una sección del tipo que corresponde")
    void unaConexionTieneQueApuntarAUnaSeccionDelTipoQueCorresponde() {
        dadaLaColumna(ColumnaFija.vinculo("product", "Producto", TipoBloque.INVENTARIO, "vender"));
        dadasLasSeccionesDeStockYClientes();
        dadaUnaConexionA(stock);

        entoncesCumpleConLasSecciones();
    }

    @Test
    @DisplayName("Una conexión a una sección de otro tipo no cumple la columna")
    void unaConexionAUnaSeccionDeOtroTipoNoCumpleLaColumna() {
        dadaLaColumna(ColumnaFija.vinculo("product", "Producto", TipoBloque.INVENTARIO, "vender"));
        dadasLasSeccionesDeStockYClientes();
        dadaUnaConexionA(clientes);

        entoncesNoCumpleConLasSecciones();
    }

    @Test
    @DisplayName("Una conexión a una sección que no existe no cumple la columna")
    void unaConexionAUnaSeccionQueNoExisteNoCumpleLaColumna() {
        dadaLaColumna(ColumnaFija.vinculo("product", "Producto", TipoBloque.INVENTARIO, "vender"));
        dadasLasSeccionesDeStockYClientes();
        dadaUnaConexionA(unaSeccionVaciaDe(TipoBloque.INVENTARIO));

        entoncesNoCumpleConLasSecciones();
    }

    @Test
    @DisplayName("El cliente de una venta puede ser un texto o una conexión a Clientes")
    void elClienteDeUnaVentaPuedeSerUnTextoOUnaConexionAClientes() {
        dadaLaColumna(ColumnaFija.textoOVinculo("customer", "Cliente", TipoBloque.CLIENTES, "fiar"));
        dadasLasSeccionesDeStockYClientes();

        entoncesAceptaTextoYConexionAClientes();
    }

    @Test
    @DisplayName("Una columna nueva nace con el nombre, el tipo y la clave de la columna")
    void unaColumnaNuevaNaceConElNombreElTipoYLaClaveDeLaColumna() {
        dadaLaColumna(ColumnaFija.moneda("price", "Precio", "cobrar"));
        dadasLasSeccionesDeStockYClientes();

        cuandoSeCreaLaColumnaEnStock();

        entoncesNaceComoMonedaOpcionalConLaClave("price", "Precio");
    }

    @Test
    @DisplayName("Una conexión nueva apunta a la primera sección del tipo de destino")
    void unaConexionNuevaApuntaALaPrimeraSeccionDelTipoDeDestino() {
        dadaLaColumna(ColumnaFija.vinculo("product", "Producto", TipoBloque.INVENTARIO, "vender"));
        dadasLasSeccionesDeStockYClientes();

        cuandoSeCreaLaColumnaEnClientes();

        entoncesApuntaAStock();
    }

    @Test
    @DisplayName("Una conexión nueva sin la sección de destino queda sin destino")
    void unaConexionNuevaSinLaSeccionDeDestinoQuedaSinDestino() {
        dadaLaColumna(ColumnaFija.vinculo("product", "Producto", TipoBloque.INVENTARIO, "vender"));
        dadasLasSeccionesDeStockYClientes();

        cuandoSeCreaLaColumnaSinSecciones();

        entoncesQuedaSinDestino();
    }

    @Test
    @DisplayName("Una columna que puede ser texto o conexión nace como texto")
    void unaColumnaQuePuedeSerTextoOConexionNaceComoTexto() {
        dadaLaColumna(ColumnaFija.textoOVinculo("customer", "Cliente", TipoBloque.CLIENTES, "fiar"));
        dadasLasSeccionesDeStockYClientes();

        cuandoSeCreaLaColumnaEnStock();

        entoncesNaceComoTextoSinDestino();
    }

    @Test
    @DisplayName("Ajustar una columna de la persona le suma lo que le faltaba y conserva su id")
    void ajustarUnaColumnaDeLaPersonaLeSumaLoQueLeFaltabaYConservaSuId() {
        dadaLaColumna(categoria().siempreObligatoria());
        dadoUnCampoDeListaOpcionalCon("Servicios");

        cuandoSeAjustaElCampo();

        entoncesQuedaObligatoriaConLasOpcionesRequeridasYElMismoId();
    }

    @Test
    @DisplayName("Ajustar una columna que la persona ya exigía conserva que sea obligatoria")
    void ajustarUnaColumnaQueLaPersonaYaExigiaConservaQueSeaObligatoria() {
        dadaLaColumna(categoria());
        dadoUnCampoDeListaCon("Servicios");

        cuandoSeAjustaElCampo();

        entoncesLaColumnaAjustadaEsObligatoria(true);
    }

    @Test
    @DisplayName("Ajustar una columna que nadie exige no la vuelve obligatoria")
    void ajustarUnaColumnaQueNadieExigeNoLaVuelveObligatoria() {
        dadaLaColumna(categoria());
        dadoUnCampoDeListaOpcionalCon("Servicios");

        cuandoSeAjustaElCampo();

        entoncesLaColumnaAjustadaEsObligatoria(false);
    }

    @Test
    @DisplayName("Ajustar una columna no repite las opciones que ya tenía")
    void ajustarUnaColumnaNoRepiteLasOpcionesQueYaTenia() {
        dadaLaColumna(categoria().siempreObligatoria());
        dadoUnCampoDeListaOpcionalCon("Mercadería");

        cuandoSeAjustaElCampo();

        entoncesConservaLasOpciones("Mercadería");
    }

    @Test
    @DisplayName("Los mensajes nombran la columna y para qué la necesita Ensambla")
    void losMensajesNombranLaColumnaYParaQueLaNecesitaEnsambla() {
        dadaLaColumna(ColumnaFija.moneda("cost", "Costo", "calcular la ganancia"));

        entoncesLosMensajesDicen("No se puede quitar «Costo»: Ensambla la necesita para calcular la ganancia.",
                "No se puede cambiar «Costo»: Ensambla la necesita para calcular la ganancia.");
    }

    @Test
    @DisplayName("El mensaje al renombrar nombra la columna y para qué la necesita Ensambla")
    void elMensajeAlRenombrarNombraLaColumnaYParaQueLaNecesitaEnsambla() {
        dadaLaColumna(ColumnaFija.moneda("cost", "Costo", "calcular la ganancia"));

        entoncesElMensajeAlRenombrarDice("No se puede renombrar «Costo»: Ensambla la necesita para calcular la ganancia.");
    }

    @Test
    @DisplayName("Conserva el nombre cuando el campo se llama igual que antes")
    void conservaElNombreCuandoElCampoSeLlamaIgualQueAntes() {
        dadaLaColumna(ColumnaFija.moneda("cost", "Costo", "calcular la ganancia"));

        entoncesConservaElNombre("Costo", "Costo", true);
    }

    @Test
    @DisplayName("No conserva el nombre cuando el campo se llama distinto que antes")
    void noConservaElNombreCuandoElCampoSeLlamaDistintoQueAntes() {
        dadaLaColumna(ColumnaFija.moneda("cost", "Costo", "calcular la ganancia"));

        entoncesConservaElNombre("Costo", "Costo de compra", false);
    }

    @Test
    @DisplayName("Un campo sin clave no es la columna fija")
    void unCampoSinClaveNoEsLaColumnaFija() {
        dadaLaColumna(ColumnaFija.moneda("price", "Precio", "cobrar"));
        dadoUnCampoSinClave();

        entoncesNoEsLaColumna();
    }

    private static ColumnaFija categoria() {
        return ColumnaFija.lista("category", "Categoría", "separar gastos", List.of("Mercadería", "Otros"),
                List.of("Mercadería"));
    }

    private void dadaLaColumna(ColumnaFija dada) {
        columna = dada;
    }

    private void dadoUnCampo(TipoCampo tipo) {
        campo = Campo.builder().id(CampoId.nuevo()).tipo(tipo).clave(columna.clave()).obligatorio(true).build();
    }

    private void dadoUnCampoOpcional(TipoCampo tipo) {
        campo = Campo.builder().id(CampoId.nuevo()).tipo(tipo).clave(columna.clave()).obligatorio(false).build();
    }

    private void dadoUnCampoSinClave() {
        campo = Campo.builder().id(CampoId.nuevo()).tipo(TipoCampo.MONEDA).build();
    }

    private void dadoUnCampoDeListaCon(String... opciones) {
        campo = Campo.builder().id(CampoId.nuevo()).tipo(TipoCampo.OPCION).clave(columna.clave()).obligatorio(true)
                .opciones(List.of(opciones)).build();
    }

    private void dadoUnCampoDeListaOpcionalCon(String... opciones) {
        campo = Campo.builder().id(CampoId.nuevo()).tipo(TipoCampo.OPCION).clave(columna.clave()).obligatorio(false)
                .opciones(List.of(opciones)).build();
    }

    private void dadasLasSeccionesDeStockYClientes() {
        stock = unaSeccionVaciaDe(TipoBloque.INVENTARIO);
        clientes = unaSeccionVaciaDe(TipoBloque.CLIENTES);
    }

    private void dadaUnaConexionA(Bloque destino) {
        campo = Campo.builder().id(CampoId.nuevo()).tipo(TipoCampo.RELACION).clave(columna.clave()).obligatorio(true)
                .bloqueRelacionadoId(destino.getId()).build();
    }

    private void cuandoSeCreaLaColumnaEnStock() {
        creado = columna.nueva(stock, List.of(stock, clientes), CampoId.nuevo(), (short) 3, AHORA);
    }

    private void cuandoSeCreaLaColumnaEnClientes() {
        creado = columna.nueva(clientes, List.of(clientes, stock), CampoId.nuevo(), (short) 0, AHORA);
    }

    private void cuandoSeCreaLaColumnaSinSecciones() {
        creado = columna.nueva(clientes, SIN_SECCIONES, CampoId.nuevo(), (short) 0, AHORA);
    }

    private void cuandoSeAjustaElCampo() {
        creado = columna.ajustada(campo, AHORA);
    }

    private List<Bloque> lasSecciones() {
        return List.of(stock, clientes);
    }

    private void entoncesLaCumple() {
        assertThat(columna.laCumple(campo, SIN_SECCIONES)).isTrue();
    }

    private void entoncesNoLaCumple() {
        assertThat(columna.laCumple(campo, SIN_SECCIONES)).isFalse();
    }

    private void entoncesTieneElTipoCorrectoPeroNoCumpleLoDemas() {
        assertThat(columna.esDelTipoCorrecto(campo, SIN_SECCIONES)).isTrue();
        assertThat(columna.cumpleLoDemas(campo)).isFalse();
    }

    private void entoncesCumpleConLasSecciones() {
        assertThat(columna.laCumple(campo, lasSecciones())).isTrue();
    }

    private void entoncesNoCumpleConLasSecciones() {
        assertThat(columna.laCumple(campo, lasSecciones())).isFalse();
    }

    private void entoncesAceptaTextoYConexionAClientes() {
        dadoUnCampo(TipoCampo.TEXTO);
        assertThat(columna.laCumple(campo, lasSecciones())).isTrue();
        dadaUnaConexionA(clientes);
        assertThat(columna.laCumple(campo, lasSecciones())).isTrue();
    }

    private void entoncesNaceComoMonedaOpcionalConLaClave(String clave, String nombre) {
        assertThat(creado.getClave()).isEqualTo(clave);
        assertThat(creado.getNombre()).isEqualTo(nombre);
        assertThat(creado.getTipo()).isEqualTo(TipoCampo.MONEDA);
        assertThat(creado.esObligatorio()).isFalse();
        assertThat(creado.getBloqueId()).isEqualTo(stock.getId());
        assertThat(creado.getPosicion()).isEqualTo((short) 3);
    }

    private void entoncesApuntaAStock() {
        assertThat(creado.getTipo()).isEqualTo(TipoCampo.RELACION);
        assertThat(creado.getBloqueRelacionadoId()).isEqualTo(stock.getId());
    }

    private void entoncesQuedaSinDestino() {
        assertThat(creado.getBloqueRelacionadoId()).isNull();
    }

    private void entoncesNaceComoTextoSinDestino() {
        assertThat(creado.getTipo()).isEqualTo(TipoCampo.TEXTO);
        assertThat(creado.getBloqueRelacionadoId()).isNull();
    }

    private void entoncesQuedaObligatoriaConLasOpcionesRequeridasYElMismoId() {
        assertThat(creado.getId()).isEqualTo(campo.getId());
        assertThat(creado.esObligatorio()).isTrue();
        assertThat(creado.getOpciones()).containsExactly("Servicios", "Mercadería");
    }

    private void entoncesLaColumnaAjustadaEsObligatoria(boolean obligatoria) {
        assertThat(creado.esObligatorio()).isEqualTo(obligatoria);
    }

    private void entoncesConservaLasOpciones(String... opciones) {
        assertThat(creado.getOpciones()).containsExactly(opciones);
    }

    private void entoncesLosMensajesDicen(String alQuitar, String alCambiar) {
        assertThat(columna.mensajeAlQuitar("Costo")).isEqualTo(alQuitar);
        assertThat(columna.mensajeAlCambiar("Costo")).isEqualTo(alCambiar);
    }

    private void entoncesElMensajeAlRenombrarDice(String mensaje) {
        assertThat(columna.mensajeAlRenombrar("Costo")).isEqualTo(mensaje);
    }

    private void entoncesConservaElNombre(String antes, String ahora, boolean conserva) {
        Campo campoAntes = Campo.builder().id(CampoId.nuevo()).nombre(antes).clave(columna.clave()).build();
        Campo campoAhora = Campo.builder().id(CampoId.nuevo()).nombre(ahora).clave(columna.clave()).build();
        assertThat(columna.conservaElNombre(campoAntes, campoAhora)).isEqualTo(conserva);
    }

    private void entoncesNoEsLaColumna() {
        assertThat(columna.esLaDe(campo)).isFalse();
    }
}
