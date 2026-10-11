package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.enums.DireccionOrden;
import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.domain.model.registro.ConsultaRegistros;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.support.PostgresDatosTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RegistroRepositoryImplPostgresTest extends PostgresDatosTestSupport {

    private static final int REGISTROS = 120;

    @Autowired
    private RegistroRepositoryImpl repository;

    private UUID negocioId;
    private UUID bloqueId;
    private UUID campoNombre;
    private UUID campoCantidad;

    @BeforeEach
    void setUp() {
        negocioId = givenNegocio("dueña@example.com").getId();
        bloqueId = givenBloque(negocioId, "Productos");
        campoNombre = givenCampo(negocioId, bloqueId, "Producto", TipoCampo.TEXTO, 0);
        campoCantidad = givenCampo(negocioId, bloqueId, "Cantidad", TipoCampo.NUMERO, 1);
    }

    @Test
    @DisplayName("Pagina 120 registros ordenados por cantidad descendente e informa el total")
    void buscarShouldPaginateOverOneHundredTwentyRegistros() {
        givenProductos(REGISTROS);

        Pagina<Registro> primera = buscar(null, campoCantidad, DireccionOrden.DESC, 0, 50);
        Pagina<Registro> ultima = buscar(null, campoCantidad, DireccionOrden.DESC, 2, 50);

        assertThat(primera.totalElementos()).isEqualTo(REGISTROS);
        assertThat(primera.totalPaginas()).isEqualTo(3);
        assertThat(cantidades(primera)).hasSize(50).startsWith(119, 118).endsWith(70);
        assertThat(cantidades(ultima)).hasSize(20).startsWith(19).endsWith(0);
    }

    @Test
    @DisplayName("Ordena los 120 registros por cantidad como número y no como texto")
    void buscarShouldOrderNumbersNumericallyOverOneHundredTwentyRegistros() {
        givenProductos(REGISTROS);

        Pagina<Registro> pagina = buscar(null, campoCantidad, DireccionOrden.ASC, 0, 100);

        assertThat(cantidades(pagina)).startsWith(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10).endsWith(97, 98, 99);
    }

    @Test
    @DisplayName("Busca texto entre 120 registros y cuenta sólo los que coinciden")
    void buscarShouldFilterOverOneHundredTwentyRegistros() {
        givenProductos(REGISTROS);

        Pagina<Registro> pagina = buscar("PRODUCTO 11", campoCantidad, DireccionOrden.ASC, 0, 50);

        assertThat(pagina.totalElementos()).isEqualTo(11);
        assertThat(cantidades(pagina)).containsExactly(11, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119);
    }

    @Test
    @DisplayName("El porcentaje, el guion bajo y la barra del texto buscado se toman literalmente")
    void buscarShouldTreatWildcardsAsLiteralText() {
        givenNombre("Oferta 100% off");
        givenNombre("Oferta 100 off");
        givenNombre("a_b");
        givenNombre("axb");
        givenNombre("C:\\temp");

        Pagina<Registro> porcentaje = buscar("100%", null, DireccionOrden.DESC, 0, 20);
        Pagina<Registro> guionBajo = buscar("a_b", null, DireccionOrden.DESC, 0, 20);
        Pagina<Registro> barra = buscar("\\", null, DireccionOrden.DESC, 0, 20);

        assertThat(nombres(porcentaje)).containsExactly("Oferta 100% off");
        assertThat(nombres(guionBajo)).containsExactly("a_b");
        assertThat(nombres(barra)).containsExactly("C:\\temp");
    }

    // --- act ---
    private Pagina<Registro> buscar(String texto, UUID campoOrdenId, DireccionOrden direccion, int pagina,
                                    int tamanio) {
        return repository.buscar(new ConsultaRegistros(
                negocioId, bloqueId, texto, campoOrdenId, direccion, pagina, tamanio));
    }

    // --- arrange ---
    private void givenProductos(int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            givenRegistro(negocioId, bloqueId, Map.of(
                    campoNombre.toString(), "Producto " + i,
                    campoCantidad.toString(), i), AHORA.minusSeconds(cantidad - i));
        }
    }

    private void givenNombre(String nombre) {
        givenRegistro(negocioId, bloqueId, Map.of(campoNombre.toString(), nombre), AHORA);
    }

    // --- helpers ---
    private List<Integer> cantidades(Pagina<Registro> pagina) {
        return pagina.contenido().stream()
                .map(registro -> (Integer) registro.getValores().get(campoCantidad.toString()))
                .toList();
    }

    private List<Object> nombres(Pagina<Registro> pagina) {
        return pagina.contenido().stream()
                .map(registro -> registro.getValores().get(campoNombre.toString()))
                .toList();
    }
}
