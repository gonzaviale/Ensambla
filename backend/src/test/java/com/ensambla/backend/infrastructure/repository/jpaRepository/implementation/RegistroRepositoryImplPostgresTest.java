package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.ConsultaRegistros;
import com.ensambla.backend.domain.model.registro.DireccionOrden;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.support.PostgresDatosTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RegistroRepositoryImplPostgresTest extends PostgresDatosTestSupport {

    private static final int REGISTROS = 120;

    @Autowired
    private RegistroRepositoryImpl repository;

    private NegocioId negocioId;
    private BloqueId bloqueId;
    private CampoId campoNombre;
    private CampoId campoCantidad;

    @BeforeEach
    void dadoUnBloqueConNombreYCantidad() {
        negocioId = NegocioId.de(givenNegocio("dueña@example.com").getId());
        bloqueId = BloqueId.de(givenBloque(negocioId, "Productos"));
        campoNombre = CampoId.de(givenCampo(negocioId, bloqueId, "Producto", TipoCampo.TEXTO, 0));
        campoCantidad = CampoId.de(givenCampo(negocioId, bloqueId, "Cantidad", TipoCampo.NUMERO, 1));
    }

    private Pagina<Registro> primera;
    private Pagina<Registro> ultima;
    private Pagina<Registro> pagina;
    private Pagina<Registro> porcentaje;
    private Pagina<Registro> guionBajo;
    private Pagina<Registro> barra;
    private Map<CampoId, BigDecimal> sumas;

    @Test
    @DisplayName("Pagina 120 registros ordenados por cantidad descendente e informa el total")
    void paginaCientoVeinteRegistrosOrdenadosPorCantidadDescendenteEInformaElTotal() {
        dadoCientoVeinteProductos();

        cuandoSePidenLaPrimeraYLaUltimaPaginaDeCincuenta();

        entoncesElTotalEsCientoVeinteEnTresPaginasYLasCantidadesBajan();
    }

    @Test
    @DisplayName("Ordena los 120 registros por cantidad como número y no como texto")
    void ordenaLosCientoVeinteRegistrosPorCantidadComoNumeroYNoComoTexto() {
        dadoCientoVeinteProductos();

        cuandoSeOrdenanAscendentePorCantidad();

        entoncesLasCantidadesSubenDeCeroANoventaYNueve();
    }

    @Test
    @DisplayName("Busca texto entre 120 registros y cuenta sólo los que coinciden")
    void buscaTextoEntreCientoVeinteRegistrosYCuentaSoloLosQueCoinciden() {
        dadoCientoVeinteProductos();

        cuandoSeBuscaElTextoProducto11();

        entoncesCoincidenOnceRegistros();
    }

    @Test
    @DisplayName("El porcentaje, el guion bajo y la barra del texto buscado se toman literalmente")
    void elPorcentajeElGuionBajoYLaBarraDelTextoBuscadoSeTomanLiteralmente() {
        dadoNombresConPorcentajeGuionBajoYBarra();

        cuandoSeBuscaPorPorcentajeGuionBajoYBarra();

        entoncesCadaBusquedaEncuentraSoloSuNombreLiteral();
    }

    @Test
    @DisplayName("Suma la cantidad de los 120 registros sin paginar")
    void sumaLaCantidadDeLosCientoVeinteRegistrosSinPaginar() {
        dadoCientoVeinteProductos();

        cuandoSeSumaLaCantidadSinBusqueda();

        entoncesLaSumaEsSieteMilCientoCuarenta();
    }

    @Test
    @DisplayName("Suma sólo los registros que cumplen la búsqueda, no los de la página")
    void sumaSoloLosRegistrosQueCumplenLaBusquedaNoLosDeLaPagina() {
        dadoCientoVeinteProductos();

        cuandoSeSumaLaCantidadConLaBusquedaProducto11();

        entoncesLaSumaEsMilCientoCincuentaYSeis();
    }

    @Test
    @DisplayName("Sin coincidencias no hay sumas")
    void sinCoincidenciasNoHaySumas() {
        dadoCientoVeinteProductos();

        cuandoSeSumaLaCantidadConLaBusquedaNoExiste();

        entoncesNoHaySumas();
    }

    // --- dado ---
    private void dadoCientoVeinteProductos() {
        for (int i = 0; i < REGISTROS; i++) {
            givenRegistro(negocioId, bloqueId, Map.of(
                    campoNombre.toString(), "Producto " + i,
                    campoCantidad.toString(), i), AHORA.minusSeconds(REGISTROS - i));
        }
    }

    private void dadoNombresConPorcentajeGuionBajoYBarra() {
        for (String nombre : List.of("Oferta 100% off", "Oferta 100 off", "a_b", "axb", "C:\\temp")) {
            givenRegistro(negocioId, bloqueId, Map.of(campoNombre.toString(), nombre), AHORA);
        }
    }

    // --- cuando ---
    private Pagina<Registro> buscar(String texto, CampoId campoOrdenId, DireccionOrden direccion, int numero,
                                    int tamanio) {
        return repository.buscar(new ConsultaRegistros(
                negocioId, bloqueId, texto, campoOrdenId, direccion, numero, tamanio));
    }

    private Map<CampoId, BigDecimal> sumar(String texto) {
        return repository.sumarNumericos(
                new ConsultaRegistros(negocioId, bloqueId, texto, null, DireccionOrden.DESC, 0, 20),
                List.of(campoCantidad));
    }

    private void cuandoSePidenLaPrimeraYLaUltimaPaginaDeCincuenta() {
        primera = buscar(null, campoCantidad, DireccionOrden.DESC, 0, 50);
        ultima = buscar(null, campoCantidad, DireccionOrden.DESC, 2, 50);
    }

    private void cuandoSeOrdenanAscendentePorCantidad() {
        pagina = buscar(null, campoCantidad, DireccionOrden.ASC, 0, 100);
    }

    private void cuandoSeBuscaElTextoProducto11() {
        pagina = buscar("PRODUCTO 11", campoCantidad, DireccionOrden.ASC, 0, 50);
    }

    private void cuandoSeBuscaPorPorcentajeGuionBajoYBarra() {
        porcentaje = buscar("100%", null, DireccionOrden.DESC, 0, 20);
        guionBajo = buscar("a_b", null, DireccionOrden.DESC, 0, 20);
        barra = buscar("\\", null, DireccionOrden.DESC, 0, 20);
    }

    private void cuandoSeSumaLaCantidadSinBusqueda() {
        sumas = sumar(null);
    }

    private void cuandoSeSumaLaCantidadConLaBusquedaProducto11() {
        sumas = sumar("PRODUCTO 11");
    }

    private void cuandoSeSumaLaCantidadConLaBusquedaNoExiste() {
        sumas = sumar("no existe");
    }

    // --- entonces ---
    private List<Integer> cantidades(Pagina<Registro> deLaPagina) {
        return deLaPagina.contenido().stream()
                .map(registro -> (Integer) registro.getValores().get(campoCantidad.toString()))
                .toList();
    }

    private List<Object> nombres(Pagina<Registro> deLaPagina) {
        return deLaPagina.contenido().stream()
                .map(registro -> registro.getValores().get(campoNombre.toString()))
                .toList();
    }

    private void entoncesElTotalEsCientoVeinteEnTresPaginasYLasCantidadesBajan() {
        assertThat(primera.totalElementos()).isEqualTo(REGISTROS);
        assertThat(primera.totalPaginas()).isEqualTo(3);
        assertThat(cantidades(primera)).hasSize(50).startsWith(119, 118).endsWith(70);
        assertThat(cantidades(ultima)).hasSize(20).startsWith(19).endsWith(0);
    }

    private void entoncesLasCantidadesSubenDeCeroANoventaYNueve() {
        assertThat(cantidades(pagina)).startsWith(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10).endsWith(97, 98, 99);
    }

    private void entoncesCoincidenOnceRegistros() {
        assertThat(pagina.totalElementos()).isEqualTo(11);
        assertThat(cantidades(pagina)).containsExactly(11, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119);
    }

    private void entoncesCadaBusquedaEncuentraSoloSuNombreLiteral() {
        assertThat(nombres(porcentaje)).containsExactly("Oferta 100% off");
        assertThat(nombres(guionBajo)).containsExactly("a_b");
        assertThat(nombres(barra)).containsExactly("C:\\temp");
    }

    private void entoncesLaSumaEsSieteMilCientoCuarenta() {
        assertThat(sumas).containsOnlyKeys(campoCantidad);
        assertThat(sumas.get(campoCantidad)).isEqualByComparingTo("7140");
    }

    private void entoncesLaSumaEsMilCientoCincuentaYSeis() {
        assertThat(sumas.get(campoCantidad)).isEqualByComparingTo("1156");
    }

    private void entoncesNoHaySumas() {
        assertThat(sumas).isEmpty();
    }
}
