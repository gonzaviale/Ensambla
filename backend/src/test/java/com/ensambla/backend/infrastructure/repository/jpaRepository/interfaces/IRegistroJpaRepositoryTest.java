package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.infrastructure.repository.entity.RegistroEntity;
import com.ensambla.backend.support.PostgresDatosTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class IRegistroJpaRepositoryTest extends PostgresDatosTestSupport {

    private static final Pageable PRIMERA = PageRequest.of(0, 50);

    private UUID negocioA;
    private UUID negocioB;
    private UUID bloqueA;
    private UUID campoNombre;
    private UUID campoCantidad;
    private UUID campoActivo;
    private UUID otroBloqueRegistro;

    @BeforeEach
    void dadoUnBloqueConTresCampos() {
        negocioA = givenNegocio("a@example.com").getId();
        negocioB = givenNegocio("b@example.com").getId();
        bloqueA = givenBloque(NegocioId.de(negocioA), "Productos");
        campoNombre = givenCampo(NegocioId.de(negocioA), BloqueId.de(bloqueA), "Producto", TipoCampo.TEXTO, 0);
        campoCantidad = givenCampo(NegocioId.de(negocioA), BloqueId.de(bloqueA), "Cantidad", TipoCampo.NUMERO, 1);
        campoActivo = givenCampo(NegocioId.de(negocioA), BloqueId.de(bloqueA), "Activo", TipoCampo.SI_NO, 2);
    }

    private UUID registroId;
    private UUID viejo;
    private UUID medio;
    private UUID nuevo;
    private UUID harina;
    private UUID conCantidad;
    private UUID conPorcentaje;
    private UUID diez;
    private UUID nueve;
    private UUID cien;
    private UUID sinDato;
    private UUID cinco;
    private UUID siete;
    private UUID zeta;
    private UUID banana;
    private UUID apple;
    private UUID si;
    private UUID no;
    private UUID activoHarina;
    private Optional<RegistroEntity> propio;
    private Optional<RegistroEntity> ajeno;
    private Page<RegistroEntity> pagina;
    private Page<RegistroEntity> ascendente;
    private Page<RegistroEntity> descendente;
    private Map<String, BigDecimal> sumas;
    private Map<String, BigDecimal> conBusqueda;
    private Map<String, BigDecimal> sinBusqueda;
    private List<RegistroEntity> traidos;
    private List<RegistroEntity> enRango;
    private List<RegistroEntity> desdeSolo;
    private List<RegistroEntity> hastaSolo;
    private List<RegistroEntity> limitado;
    private List<RegistroEntity> porTextoYColumna;
    private List<RegistroEntity> soloColumna;
    private long cantidadDelReporte;
    private long deOtroNegocio;

    @Test
    @DisplayName("Encuentra el registro por id sólo dentro de su negocio")
    void encuentraElRegistroPorIdSoloDentroDeSuNegocio() {
        dadoUnRegistroVacio();

        cuandoSeBuscaElRegistroEnSuNegocioYEnOtro();

        entoncesSoloSeEncuentraEnSuNegocio();
    }

    @Test
    @DisplayName("Lista los más nuevos primero y sólo los del bloque y negocio pedidos")
    void listaLosMasNuevosPrimeroYSoloLosDelBloqueYNegocioPedidos() {
        dadoDosRegistrosDelBloqueYOtrosDeOtroBloqueYDeOtroNegocio();

        cuandoSeBuscaEnDescendenteSinFiltros();

        entoncesSalenElNuevoYElViejoYElTotalEsDos();
    }

    @Test
    @DisplayName("No devuelve registros cuando el bloque pertenece a otro negocio")
    void noDevuelveRegistrosCuandoElBloquePerteneceAOtroNegocio() {
        dadoUnRegistroVacio();

        cuandoSeBuscaElBloqueDeAConElNegocioB();

        entoncesNoSeDevuelveNingunRegistro();
    }

    @Test
    @DisplayName("Lista los más viejos primero en orden ascendente por fecha de carga")
    void listaLosMasViejosPrimeroEnOrdenAscendentePorFechaDeCarga() {
        dadoUnRegistroViejoYUnoNuevo();

        cuandoSeBuscaEnAscendenteSinFiltros();

        entoncesSalenElViejoYElNuevo();
    }

    @Test
    @DisplayName("Busca el texto entre los valores sin distinguir mayúsculas")
    void buscaElTextoEntreLosValoresSinDistinguirMayusculas() {
        dadoUnRegistroDeHarinaYUnoDeAzucar();

        cuandoSeBuscaElTexto("%HARINA%");

        entoncesSoloSeEncuentraLaHarina();
    }

    @Test
    @DisplayName("Busca también en valores numéricos")
    void buscaTambienEnValoresNumericos() {
        dadoUnRegistroConCantidadCuatroMilQuinientosYOtroConDoce();

        cuandoSeBuscaElTexto("%450%");

        entoncesSoloSeEncuentraElDeCuatroMilQuinientos();
    }

    @Test
    @DisplayName("No busca entre los ids de los campos")
    void noBuscaEntreLosIdsDeLosCampos() {
        dadoUnRegistroDeHarina();

        cuandoSeBuscaUnFragmentoDelIdDelCampo();

        entoncesNoSeDevuelveNingunRegistro();
    }

    @Test
    @DisplayName("Un patrón con comodines escapados busca el símbolo literal")
    void unPatronConComodinesEscapadosBuscaElSimboloLiteral() {
        dadoOfertasConYSinPorcentaje();

        cuandoSeBuscaElTexto("%50\\%%");

        entoncesSoloSeEncuentraLaOfertaConPorcentaje();
    }

    @Test
    @DisplayName("Ordena los números por valor y no como texto")
    void ordenaLosNumerosPorValorYNoComoTexto() {
        dadoRegistrosConCantidadDiezNueveYCien();

        cuandoSeOrdenaAscendentePor(campoCantidad);

        entoncesLosNumerosVienenNueveDiezCien();
    }

    @Test
    @DisplayName("Deja al final a los registros sin el dato, tanto en orden ascendente como descendente")
    void dejaAlFinalALosRegistrosSinElDatoTantoEnOrdenAscendenteComoDescendente() {
        dadoUnRegistroSinDatoYDosConCantidad();

        cuandoSeOrdenaPorCantidadEnAmbosSentidos();

        entoncesLosRegistrosSinDatoQuedanAlFinalEnAmbosSentidos();
    }

    @Test
    @DisplayName("Ordena los textos sin distinguir mayúsculas")
    void ordenaLosTextosSinDistinguirMayusculas() {
        dadoTresNombresZetaBananaYApple();

        cuandoSeOrdenaAscendentePor(campoNombre);

        entoncesLosTextosVienenAppleBananaZeta();
    }

    @Test
    @DisplayName("Ordena los sí/no poniendo primero el no")
    void ordenaLosSiNoPoniendoPrimeroElNo() {
        dadoUnRegistroActivoYUnoInactivo();

        cuandoSeOrdenaAscendentePor(campoActivo);

        entoncesElNoVienePrimero();
    }

    @Test
    @DisplayName("Pagina los resultados e informa el total sin paginar")
    void paginaLosResultadosEInformaElTotalSinPaginar() {
        dadoCincoRegistrosConCantidadDeCeroACuatro();

        cuandoSePideLaUltimaPaginaDeDos();

        entoncesLaUltimaPaginaTieneUnRegistroYElTotalEsCinco();
    }

    @Test
    @DisplayName("Suma los valores numéricos de cada campo y no cuenta los registros que no lo cargaron")
    void sumaLosValoresNumericosDeCadaCampoYNoCuentaLosRegistrosQueNoLoCargaron() {
        dadoDosCantidadesYUnRegistroSinCantidad();

        cuandoSeSumaLaCantidad();

        entoncesSeSumanQuinceYMedio();
    }

    @Test
    @DisplayName("Suma sólo lo que cumple la búsqueda y deja afuera otros bloques y otros negocios")
    void sumaSoloLoQueCumpleLaBusquedaYDejaAfueraOtrosBloquesYOtrosNegocios() {
        dadoProductosEnVariosBloquesYNegocios();

        cuandoSeSumaLaCantidadConYSinBusqueda();

        entoncesLaSumaConBusquedaEsCuarentaYSinBusquedaCuarentaYSiete();
    }

    @Test
    @DisplayName("Sólo suma los campos pedidos y un campo sin números queda en cero")
    void soloSumaLosCamposPedidosYUnCampoSinNumerosQuedaEnCero() {
        dadoUnRegistroConTextoCantidadYActivo();

        cuandoSeSumanCantidadYNombre();

        entoncesSoloSeSumanLosCamposPedidosYElNombreQuedaEnCero();
    }

    @Test
    @DisplayName("Trae sólo los ids pedidos del bloque y negocio, los más nuevos primero")
    void traeSoloLosIdsPedidosDelBloqueYNegocioLosMasNuevosPrimero() {
        dadoRegistrosDelBloqueYUnoDeOtroBloque();

        cuandoSeTraenLosRegistrosPorIds();

        entoncesSeTraenElNuevoYElViejo();
    }

    @Test
    @DisplayName("No trae registros pedidos por id con el negocio equivocado")
    void noTraeRegistrosPedidosPorIdConElNegocioEquivocado() {
        dadoUnRegistroVacio();

        cuandoSeTraeElRegistroPorIdConElNegocioB();

        entoncesNoSeTraeNingunRegistro();
    }

    @Test
    @DisplayName("El reporte trae las filas del rango de fechas, las más nuevas primero, y respeta el límite")
    void elReporteTraeLasFilasDelRangoDeFechasLasMasNuevasPrimeroYRespetaElLimite() {
        dadoTresRegistrosDeHaceDiezCincoYUnDia();

        cuandoSeBuscaElReporteEnVariosRangos();

        entoncesElReporteRespetaElRangoYElLimite();
    }

    @Test
    @DisplayName("El reporte filtra por el texto y por el valor de una columna")
    void elReporteFiltraPorElTextoYPorElValorDeUnaColumna() {
        dadoTresRegistrosConNombreYActivo();

        cuandoSeBuscaElReportePorTextoYColumna();

        entoncesElReporteDevuelveSoloLaHarinaActiva();
    }

    @Test
    @DisplayName("La cantidad y las sumas del reporte coinciden con sus filtros y no incluyen a otros negocios")
    void laCantidadYLasSumasDelReporteCoincidenConSusFiltrosYNoIncluyenAOtrosNegocios() {
        dadoRegistrosActivosEInactivosDeVariasFechasYNegocios();

        cuandoSeCuentaYSumaElReporte();

        entoncesLaCantidadYLaSumaCoincidenConLosFiltros();
    }

    // --- dado ---
    private UUID unRegistroEnA(Map<String, Object> valores, int minutos) {
        return givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), valores, hace(minutos));
    }

    private void dadoUnRegistroVacio() {
        registroId = unRegistroEnA(Map.of(), 0);
    }

    private void dadoDosRegistrosDelBloqueYOtrosDeOtroBloqueYDeOtroNegocio() {
        viejo = unRegistroEnA(Map.of(), 2);
        nuevo = unRegistroEnA(Map.of(), 1);
        UUID otroBloque = givenBloque(NegocioId.de(negocioA), "Clientes");
        givenRegistro(NegocioId.de(negocioA), BloqueId.de(otroBloque), Map.of(), hace(0));
        UUID bloqueDeB = givenBloque(NegocioId.de(negocioB), "Productos");
        givenRegistro(NegocioId.de(negocioB), BloqueId.de(bloqueDeB), Map.of(), hace(0));
    }

    private void dadoUnRegistroViejoYUnoNuevo() {
        viejo = unRegistroEnA(Map.of(), 2);
        nuevo = unRegistroEnA(Map.of(), 1);
    }

    private void dadoUnRegistroDeHarinaYUnoDeAzucar() {
        harina = unRegistroEnA(valores(campoNombre, "Harina 000"), 2);
        unRegistroEnA(valores(campoNombre, "Azúcar"), 1);
    }

    private void dadoUnRegistroConCantidadCuatroMilQuinientosYOtroConDoce() {
        conCantidad = unRegistroEnA(valores(campoCantidad, 4500), 1);
        unRegistroEnA(valores(campoCantidad, 12), 0);
    }

    private void dadoUnRegistroDeHarina() {
        unRegistroEnA(valores(campoNombre, "Harina"), 0);
    }

    private void dadoOfertasConYSinPorcentaje() {
        conPorcentaje = unRegistroEnA(valores(campoNombre, "Oferta 50% off"), 1);
        unRegistroEnA(valores(campoNombre, "Oferta 50 off"), 0);
    }

    private void dadoRegistrosConCantidadDiezNueveYCien() {
        diez = unRegistroEnA(valores(campoCantidad, 10), 3);
        nueve = unRegistroEnA(valores(campoCantidad, 9), 2);
        cien = unRegistroEnA(valores(campoCantidad, 100), 1);
    }

    private void dadoUnRegistroSinDatoYDosConCantidad() {
        sinDato = unRegistroEnA(Map.of(), 0);
        cinco = unRegistroEnA(valores(campoCantidad, 5), 2);
        siete = unRegistroEnA(valores(campoCantidad, 7), 1);
    }

    private void dadoTresNombresZetaBananaYApple() {
        zeta = unRegistroEnA(valores(campoNombre, "Zeta"), 3);
        banana = unRegistroEnA(valores(campoNombre, "banana"), 2);
        apple = unRegistroEnA(valores(campoNombre, "Apple"), 1);
    }

    private void dadoUnRegistroActivoYUnoInactivo() {
        si = unRegistroEnA(valores(campoActivo, true), 2);
        no = unRegistroEnA(valores(campoActivo, false), 1);
    }

    private void dadoCincoRegistrosConCantidadDeCeroACuatro() {
        for (int i = 0; i < 5; i++) {
            unRegistroEnA(valores(campoCantidad, i), 10 - i);
        }
    }

    private void dadoDosCantidadesYUnRegistroSinCantidad() {
        unRegistroEnA(valores(campoCantidad, 10), 3);
        unRegistroEnA(valores(campoCantidad, 5.5), 2);
        unRegistroEnA(valores(campoNombre, "Sin cantidad"), 1);
    }

    private void dadoProductosEnVariosBloquesYNegocios() {
        unRegistroEnA(producto("Harina 000", 40), 4);
        unRegistroEnA(producto("Azúcar", 7), 3);
        givenRegistro(NegocioId.de(negocioA), BloqueId.de(givenBloque(NegocioId.de(negocioA), "Clientes")),
                producto("Harina 000", 100), hace(2));
        givenRegistro(NegocioId.de(negocioB), BloqueId.de(givenBloque(NegocioId.de(negocioB), "Productos")),
                producto("Harina 000", 1000), hace(1));
    }

    private void dadoUnRegistroConTextoCantidadYActivo() {
        Map<String, Object> registro = producto("Texto", 3);
        registro.put(campoActivo.toString(), true);
        unRegistroEnA(registro, 1);
    }

    private void dadoRegistrosDelBloqueYUnoDeOtroBloque() {
        viejo = unRegistroEnA(Map.of(), 2);
        nuevo = unRegistroEnA(Map.of(), 1);
        unRegistroEnA(Map.of(), 0);
        otroBloqueRegistro = givenRegistro(NegocioId.de(negocioA), BloqueId
                .de(givenBloque(NegocioId.de(negocioA), "Clientes")), Map.of(), hace(0));
    }

    private void dadoTresRegistrosDeHaceDiezCincoYUnDia() {
        viejo = unRegistroEnA(producto("Viejo", 1), 60 * 24 * 10);
        medio = unRegistroEnA(producto("Medio", 2), 60 * 24 * 5);
        nuevo = unRegistroEnA(producto("Nuevo", 3), 60 * 24);
    }

    private void dadoTresRegistrosConNombreYActivo() {
        activoHarina = unRegistroEnA(activo("Harina 000", true), 3);
        unRegistroEnA(activo("Harina integral", false), 2);
        unRegistroEnA(activo("Azúcar", true), 1);
    }

    private void dadoRegistrosActivosEInactivosDeVariasFechasYNegocios() {
        unRegistroEnA(activo("Harina", true, 40), 60 * 24 * 3);
        unRegistroEnA(activo("Azúcar", true, 7), 60 * 24 * 3);
        unRegistroEnA(activo("Sal", false, 100), 60 * 24 * 3);
        unRegistroEnA(activo("Harina vieja", true, 1000), 60 * 24 * 30);
        givenRegistro(NegocioId.de(negocioB), BloqueId.de(givenBloque(NegocioId.de(negocioB), "Productos")),
                activo("Harina", true, 5000), hace(60 * 24 * 3));
    }

    private OffsetDateTime hace(int minutos) {
        return AHORA.minusMinutes(minutos);
    }

    private Map<String, Object> valores(UUID campoId, Object valor) {
        Map<String, Object> valores = new HashMap<>();
        valores.put(campoId.toString(), valor);
        return valores;
    }

    private Map<String, Object> producto(String nombre, Object cantidad) {
        Map<String, Object> valores = valores(campoNombre, nombre);
        valores.put(campoCantidad.toString(), cantidad);
        return valores;
    }

    private Map<String, Object> activo(String nombre, boolean activo) {
        Map<String, Object> valores = new HashMap<>();
        valores.put(campoNombre.toString(), nombre);
        valores.put(campoActivo.toString(), activo);
        return valores;
    }

    private Map<String, Object> activo(String nombre, boolean activo, Number cantidad) {
        Map<String, Object> valores = activo(nombre, activo);
        valores.put(campoCantidad.toString(), cantidad);
        return valores;
    }

    private Map<String, BigDecimal> sumasPorCampo(List<IRegistroJpaRepository.SumaPorCampo> filas) {
        return filas.stream().collect(Collectors.toMap(
                IRegistroJpaRepository.SumaPorCampo::getCampo,
                IRegistroJpaRepository.SumaPorCampo::getSuma));
    }

    // --- cuando ---
    private void cuandoSeBuscaElRegistroEnSuNegocioYEnOtro() {
        propio = registros.findByIdAndNegocioId(registroId, negocioA);
        ajeno = registros.findByIdAndNegocioId(registroId, negocioB);
    }

    private void cuandoSeBuscaEnDescendenteSinFiltros() {
        pagina = registros.buscarDescendente(negocioA, bloqueA, null, null, PRIMERA);
    }

    private void cuandoSeBuscaEnAscendenteSinFiltros() {
        pagina = registros.buscarAscendente(negocioA, bloqueA, null, null, PRIMERA);
    }

    private void cuandoSeBuscaElBloqueDeAConElNegocioB() {
        pagina = registros.buscarDescendente(negocioB, bloqueA, null, null, PRIMERA);
    }

    private void cuandoSeBuscaElTexto(String patron) {
        pagina = registros.buscarDescendente(negocioA, bloqueA, patron, null, PRIMERA);
    }

    private void cuandoSeBuscaUnFragmentoDelIdDelCampo() {
        String fragmentoDelId = campoNombre.toString().substring(0, 8);
        pagina = registros.buscarDescendente(negocioA, bloqueA, "%" + fragmentoDelId + "%", null, PRIMERA);
    }

    private void cuandoSeOrdenaAscendentePor(UUID campo) {
        pagina = registros.buscarAscendente(negocioA, bloqueA, null, campo.toString(), PRIMERA);
    }

    private void cuandoSeOrdenaPorCantidadEnAmbosSentidos() {
        ascendente = registros.buscarAscendente(negocioA, bloqueA, null, campoCantidad.toString(), PRIMERA);
        descendente = registros.buscarDescendente(negocioA, bloqueA, null, campoCantidad.toString(), PRIMERA);
    }

    private void cuandoSePideLaUltimaPaginaDeDos() {
        pagina = registros.buscarAscendente(negocioA, bloqueA, null, campoCantidad.toString(), PageRequest.of(2, 2));
    }

    private void cuandoSeSumaLaCantidad() {
        sumas = sumasPorCampo(registros.sumarNumericos(negocioA, bloqueA, null, List.of(campoCantidad.toString())));
    }

    private void cuandoSeSumaLaCantidadConYSinBusqueda() {
        conBusqueda = sumasPorCampo(registros.sumarNumericos(
                negocioA, bloqueA, "%HARINA%", List.of(campoCantidad.toString())));
        sinBusqueda = sumasPorCampo(registros.sumarNumericos(
                negocioA, bloqueA, null, List.of(campoCantidad.toString())));
    }

    private void cuandoSeSumanCantidadYNombre() {
        sumas = sumasPorCampo(registros.sumarNumericos(
                negocioA, bloqueA, null, List.of(campoCantidad.toString(), campoNombre.toString())));
    }

    private void cuandoSeTraenLosRegistrosPorIds() {
        traidos = registros.findAllByIdInAndBloqueIdAndNegocioIdOrderByCreadoEnDescIdDesc(
                List.of(viejo, nuevo, otroBloqueRegistro), bloqueA, negocioA);
    }

    private void cuandoSeTraeElRegistroPorIdConElNegocioB() {
        traidos = registros.findAllByIdInAndBloqueIdAndNegocioIdOrderByCreadoEnDescIdDesc(
                List.of(registroId), bloqueA, negocioB);
    }

    private void cuandoSeBuscaElReporteEnVariosRangos() {
        String desde = AHORA.minusDays(6).toInstant().toString();
        String hasta = AHORA.minusDays(2).toInstant().toString();
        enRango = registros.buscarParaReporte(negocioA, bloqueA, null, desde, hasta, null, null, 100);
        desdeSolo = registros.buscarParaReporte(negocioA, bloqueA, null, desde, null, null, null, 100);
        hastaSolo = registros.buscarParaReporte(negocioA, bloqueA, null, null, hasta, null, null, 100);
        limitado = registros.buscarParaReporte(negocioA, bloqueA, null, null, null, null, null, 2);
    }

    private void cuandoSeBuscaElReportePorTextoYColumna() {
        porTextoYColumna = registros.buscarParaReporte(
                negocioA, bloqueA, "%harina%", null, null, campoActivo.toString(), "true", 100);
        soloColumna = registros.buscarParaReporte(
                negocioA, bloqueA, null, null, null, campoActivo.toString(), "false", 100);
    }

    private void cuandoSeCuentaYSumaElReporte() {
        String desde = AHORA.minusDays(10).toInstant().toString();
        cantidadDelReporte = registros.contarParaReporte(
                negocioA, bloqueA, null, desde, null, campoActivo.toString(), "true");
        sumas = sumasPorCampo(registros.sumarNumericosParaReporte(negocioA, bloqueA, null, desde, null,
                campoActivo.toString(), "true", List.of(campoCantidad.toString())));
        deOtroNegocio = registros.contarParaReporte(negocioB, bloqueA, null, desde, null, null, null);
    }

    // --- entonces ---
    private List<UUID> ids() {
        return pagina.getContent().stream().map(RegistroEntity::getId).toList();
    }

    private void entoncesSoloSeEncuentraEnSuNegocio() {
        assertThat(propio).isPresent();
        assertThat(ajeno).isEmpty();
    }

    private void entoncesSalenElNuevoYElViejoYElTotalEsDos() {
        assertThat(ids()).containsExactly(nuevo, viejo);
        assertThat(pagina.getTotalElements()).isEqualTo(2);
    }

    private void entoncesNoSeDevuelveNingunRegistro() {
        assertThat(pagina.getContent()).isEmpty();
        assertThat(pagina.getTotalElements()).isZero();
    }

    private void entoncesSalenElViejoYElNuevo() {
        assertThat(ids()).containsExactly(viejo, nuevo);
    }

    private void entoncesSoloSeEncuentraLaHarina() {
        assertThat(ids()).containsExactly(harina);
        assertThat(pagina.getTotalElements()).isEqualTo(1);
    }

    private void entoncesSoloSeEncuentraElDeCuatroMilQuinientos() {
        assertThat(ids()).containsExactly(conCantidad);
    }

    private void entoncesSoloSeEncuentraLaOfertaConPorcentaje() {
        assertThat(ids()).containsExactly(conPorcentaje);
    }

    private void entoncesLosNumerosVienenNueveDiezCien() {
        assertThat(ids()).containsExactly(nueve, diez, cien);
    }

    private void entoncesLosRegistrosSinDatoQuedanAlFinalEnAmbosSentidos() {
        assertThat(ascendente.getContent()).extracting(RegistroEntity::getId).containsExactly(cinco, siete, sinDato);
        assertThat(descendente.getContent()).extracting(RegistroEntity::getId).containsExactly(siete, cinco, sinDato);
    }

    private void entoncesLosTextosVienenAppleBananaZeta() {
        assertThat(ids()).containsExactly(apple, banana, zeta);
    }

    private void entoncesElNoVienePrimero() {
        assertThat(ids()).containsExactly(no, si);
    }

    private void entoncesLaUltimaPaginaTieneUnRegistroYElTotalEsCinco() {
        assertThat(pagina.getTotalElements()).isEqualTo(5);
        assertThat(pagina.getTotalPages()).isEqualTo(3);
        assertThat(pagina.getContent()).hasSize(1);
        assertThat(pagina.getContent().getFirst().getValores()).containsEntry(campoCantidad.toString(), 4);
    }

    private void entoncesSeSumanQuinceYMedio() {
        assertThat(sumas).containsOnlyKeys(campoCantidad.toString());
        assertThat(sumas.get(campoCantidad.toString())).isEqualByComparingTo("15.5");
    }

    private void entoncesLaSumaConBusquedaEsCuarentaYSinBusquedaCuarentaYSiete() {
        assertThat(conBusqueda.get(campoCantidad.toString())).isEqualByComparingTo("40");
        assertThat(sinBusqueda.get(campoCantidad.toString())).isEqualByComparingTo("47");
    }

    private void entoncesSoloSeSumanLosCamposPedidosYElNombreQuedaEnCero() {
        assertThat(sumas).containsOnlyKeys(campoCantidad.toString(), campoNombre.toString());
        assertThat(sumas.get(campoCantidad.toString())).isEqualByComparingTo("3");
        assertThat(sumas.get(campoNombre.toString())).isEqualByComparingTo("0");
    }

    private void entoncesSeTraenElNuevoYElViejo() {
        assertThat(traidos).extracting(RegistroEntity::getId).containsExactly(nuevo, viejo);
    }

    private void entoncesNoSeTraeNingunRegistro() {
        assertThat(traidos).isEmpty();
    }

    private void entoncesElReporteRespetaElRangoYElLimite() {
        assertThat(enRango).extracting(RegistroEntity::getId).containsExactly(medio);
        assertThat(desdeSolo).extracting(RegistroEntity::getId).containsExactly(nuevo, medio);
        assertThat(hastaSolo).extracting(RegistroEntity::getId).containsExactly(medio, viejo);
        assertThat(limitado).extracting(RegistroEntity::getId).containsExactly(nuevo, medio);
    }

    private void entoncesElReporteDevuelveSoloLaHarinaActiva() {
        assertThat(porTextoYColumna).extracting(RegistroEntity::getId).containsExactly(activoHarina);
        assertThat(soloColumna).hasSize(1);
    }

    private void entoncesLaCantidadYLaSumaCoincidenConLosFiltros() {
        assertThat(cantidadDelReporte).isEqualTo(2);
        assertThat(sumas.get(campoCantidad.toString())).isEqualByComparingTo("47");
        assertThat(deOtroNegocio).isZero();
    }
}
