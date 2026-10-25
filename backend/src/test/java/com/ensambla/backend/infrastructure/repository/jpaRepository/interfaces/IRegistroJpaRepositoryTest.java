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

    @BeforeEach
    void setUp() {
        negocioA = givenNegocio("a@example.com").getId();
        negocioB = givenNegocio("b@example.com").getId();
        bloqueA = givenBloque(NegocioId.de(negocioA), "Productos");
        campoNombre = givenCampo(NegocioId.de(negocioA), BloqueId.de(bloqueA), "Producto", TipoCampo.TEXTO, 0);
        campoCantidad = givenCampo(NegocioId.de(negocioA), BloqueId.de(bloqueA), "Cantidad", TipoCampo.NUMERO, 1);
        campoActivo = givenCampo(NegocioId.de(negocioA), BloqueId.de(bloqueA), "Activo", TipoCampo.SI_NO, 2);
    }

    @Test
    @DisplayName("Encuentra el registro por id sólo dentro de su negocio")
    void findByIdAndNegocioIdShouldRespectNegocio() {
        UUID registroId = givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), Map.of(), hace(0));

        Optional<RegistroEntity> propio = registros.findByIdAndNegocioId(registroId, negocioA);
        Optional<RegistroEntity> ajeno = registros.findByIdAndNegocioId(registroId, negocioB);

        assertThat(propio).isPresent();
        assertThat(ajeno).isEmpty();
    }

    @Test
    @DisplayName("Lista los más nuevos primero y sólo los del bloque y negocio pedidos")
    void buscarDescendenteShouldListNewestFirstScopedToBloqueAndNegocio() {
        UUID viejo = givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), Map.of(), hace(2));
        UUID nuevo = givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), Map.of(), hace(1));
        UUID otroBloque = givenBloque(NegocioId.de(negocioA), "Clientes");
        givenRegistro(NegocioId.de(negocioA), BloqueId.de(otroBloque), Map.of(), hace(0));
        UUID bloqueDeB = givenBloque(NegocioId.de(negocioB), "Productos");
        givenRegistro(NegocioId.de(negocioB), BloqueId.de(bloqueDeB), Map.of(), hace(0));

        Page<RegistroEntity> result = registros.buscarDescendente(negocioA, bloqueA, null, null, PRIMERA);

        assertThat(result.getContent()).extracting(RegistroEntity::getId).containsExactly(nuevo, viejo);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("No devuelve registros cuando el bloque pertenece a otro negocio")
    void buscarShouldReturnNothingWhenBloqueBelongsToOtherNegocio() {
        givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), Map.of(), hace(0));

        Page<RegistroEntity> result = registros.buscarDescendente(negocioB, bloqueA, null, null, PRIMERA);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("Lista los más viejos primero en orden ascendente por fecha de carga")
    void buscarAscendenteShouldListOldestFirst() {
        UUID viejo = givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), Map.of(), hace(2));
        UUID nuevo = givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), Map.of(), hace(1));

        Page<RegistroEntity> result = registros.buscarAscendente(negocioA, bloqueA, null, null, PRIMERA);

        assertThat(result.getContent()).extracting(RegistroEntity::getId).containsExactly(viejo, nuevo);
    }

    @Test
    @DisplayName("Busca el texto entre los valores sin distinguir mayúsculas")
    void buscarShouldMatchValuesIgnoringCase() {
        UUID harina = givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), valores(campoNombre, "Harina 000"),
                hace(2));
        givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), valores(campoNombre, "Azúcar"), hace(1));

        Page<RegistroEntity> result = registros.buscarDescendente(negocioA, bloqueA, "%HARINA%", null, PRIMERA);

        assertThat(result.getContent()).extracting(RegistroEntity::getId).containsExactly(harina);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Busca también en valores numéricos")
    void buscarShouldMatchNumericValues() {
        UUID conCantidad = givenRegistro(NegocioId.de(negocioA), BloqueId
            .de(bloqueA), valores(campoCantidad, 4500), hace(1));
        givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), valores(campoCantidad, 12), hace(0));

        Page<RegistroEntity> result = registros.buscarDescendente(negocioA, bloqueA, "%450%", null, PRIMERA);

        assertThat(result.getContent()).extracting(RegistroEntity::getId).containsExactly(conCantidad);
    }

    @Test
    @DisplayName("No busca entre los ids de los campos")
    void buscarShouldNotMatchFieldIds() {
        givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), valores(campoNombre, "Harina"), hace(0));
        String fragmentoDelId = campoNombre.toString().substring(0, 8);

        Page<RegistroEntity> result = registros.buscarDescendente(
                negocioA, bloqueA, "%" + fragmentoDelId + "%", null, PRIMERA);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Un patrón con comodines escapados busca el símbolo literal")
    void buscarShouldMatchEscapedWildcardsLiterally() {
        UUID conPorcentaje = givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA),
                valores(campoNombre, "Oferta 50% off"), hace(1));
        givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), valores(campoNombre, "Oferta 50 off"), hace(0));

        Page<RegistroEntity> result = registros.buscarDescendente(negocioA, bloqueA, "%50\\%%", null, PRIMERA);

        assertThat(result.getContent()).extracting(RegistroEntity::getId).containsExactly(conPorcentaje);
    }

    @Test
    @DisplayName("Ordena los números por valor y no como texto")
    void buscarAscendenteShouldOrderNumbersNumerically() {
        UUID diez = givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), valores(campoCantidad, 10), hace(3));
        UUID nueve = givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), valores(campoCantidad, 9), hace(2));
        UUID cien = givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), valores(campoCantidad, 100), hace(1));

        Page<RegistroEntity> result = registros.buscarAscendente(
                negocioA, bloqueA, null, campoCantidad.toString(), PRIMERA);

        assertThat(result.getContent()).extracting(RegistroEntity::getId).containsExactly(nueve, diez, cien);
    }

    @Test
    @DisplayName("Deja al final a los registros sin el dato, tanto en orden ascendente como descendente")
    void buscarShouldPutRegistrosWithoutValueLast() {
        UUID sinDato = givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), Map.of(), hace(0));
        UUID cinco = givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), valores(campoCantidad, 5), hace(2));
        UUID siete = givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), valores(campoCantidad, 7), hace(1));

        Page<RegistroEntity> ascendente = registros.buscarAscendente(
                negocioA, bloqueA, null, campoCantidad.toString(), PRIMERA);
        Page<RegistroEntity> descendente = registros.buscarDescendente(
                negocioA, bloqueA, null, campoCantidad.toString(), PRIMERA);

        assertThat(ascendente.getContent()).extracting(RegistroEntity::getId).containsExactly(cinco, siete, sinDato);
        assertThat(descendente.getContent()).extracting(RegistroEntity::getId).containsExactly(siete, cinco, sinDato);
    }

    @Test
    @DisplayName("Ordena los textos sin distinguir mayúsculas")
    void buscarAscendenteShouldOrderTextsIgnoringCase() {
        UUID zeta = givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), valores(campoNombre, "Zeta"), hace(3));
        UUID banana = givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), valores(campoNombre, "banana"),
                hace(2));
        UUID apple = givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), valores(campoNombre, "Apple"),
                hace(1));

        Page<RegistroEntity> result = registros.buscarAscendente(
                negocioA, bloqueA, null, campoNombre.toString(), PRIMERA);

        assertThat(result.getContent()).extracting(RegistroEntity::getId).containsExactly(apple, banana, zeta);
    }

    @Test
    @DisplayName("Ordena los sí/no poniendo primero el no")
    void buscarAscendenteShouldOrderBooleansFalseFirst() {
        UUID si = givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), valores(campoActivo, true), hace(2));
        UUID no = givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), valores(campoActivo, false), hace(1));

        Page<RegistroEntity> result = registros.buscarAscendente(
                negocioA, bloqueA, null, campoActivo.toString(), PRIMERA);

        assertThat(result.getContent()).extracting(RegistroEntity::getId).containsExactly(no, si);
    }

    @Test
    @DisplayName("Pagina los resultados e informa el total sin paginar")
    void buscarShouldPaginateAndCountTotal() {
        for (int i = 0; i < 5; i++) {
            givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), valores(campoCantidad, i), hace(10 - i));
        }

        Page<RegistroEntity> ultima = registros.buscarAscendente(
                negocioA, bloqueA, null, campoCantidad.toString(), PageRequest.of(2, 2));

        assertThat(ultima.getTotalElements()).isEqualTo(5);
        assertThat(ultima.getTotalPages()).isEqualTo(3);
        assertThat(ultima.getContent()).hasSize(1);
        assertThat(ultima.getContent().getFirst().getValores()).containsEntry(campoCantidad.toString(), 4);
    }

    // --- helpers ---
    private OffsetDateTime hace(int minutos) {
        return AHORA.minusMinutes(minutos);
    }

    private Map<String, Object> valores(UUID campoId, Object valor) {
        Map<String, Object> valores = new HashMap<>();
        valores.put(campoId.toString(), valor);
        return valores;
    }

    @Test
    @DisplayName("Suma los valores numéricos de cada campo y no cuenta los registros que no lo cargaron")
    void sumarNumericosShouldAddNumbersPerCampo() {
        givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), valores(campoCantidad, 10), hace(3));
        givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), valores(campoCantidad, 5.5), hace(2));
        givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), valores(campoNombre, "Sin cantidad"), hace(1));

        Map<String, BigDecimal> sumas = sumasPorCampo(registros.sumarNumericos(
                negocioA, bloqueA, null, List.of(campoCantidad.toString())));

        assertThat(sumas).containsOnlyKeys(campoCantidad.toString());
        assertThat(sumas.get(campoCantidad.toString())).isEqualByComparingTo("15.5");
    }

    @Test
    @DisplayName("Suma sólo lo que cumple la búsqueda y deja afuera otros bloques y otros negocios")
    void sumarNumericosShouldRespectSearchBloqueAndNegocio() {
        givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), producto("Harina 000", 40), hace(4));
        givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), producto("Azúcar", 7), hace(3));
        givenRegistro(NegocioId.de(negocioA), BloqueId.de(givenBloque(NegocioId.de(negocioA), "Clientes")),
                producto("Harina 000", 100), hace(2));
        givenRegistro(NegocioId.de(negocioB), BloqueId.de(givenBloque(NegocioId.de(negocioB), "Productos")),
                producto("Harina 000", 1000), hace(1));

        Map<String, BigDecimal> conBusqueda = sumasPorCampo(registros.sumarNumericos(
                negocioA, bloqueA, "%HARINA%", List.of(campoCantidad.toString())));
        Map<String, BigDecimal> sinBusqueda = sumasPorCampo(registros.sumarNumericos(
                negocioA, bloqueA, null, List.of(campoCantidad.toString())));

        assertThat(conBusqueda.get(campoCantidad.toString())).isEqualByComparingTo("40");
        assertThat(sinBusqueda.get(campoCantidad.toString())).isEqualByComparingTo("47");
    }

    @Test
    @DisplayName("Sólo suma los campos pedidos y un campo sin números queda en cero")
    void sumarNumericosShouldOnlyReturnRequestedCamposAndZeroWithoutNumbers() {
        Map<String, Object> registro = producto("Texto", 3);
        registro.put(campoActivo.toString(), true);
        givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), registro, hace(1));

        Map<String, BigDecimal> sumas = sumasPorCampo(registros.sumarNumericos(
                negocioA, bloqueA, null, List.of(campoCantidad.toString(), campoNombre.toString())));

        assertThat(sumas).containsOnlyKeys(campoCantidad.toString(), campoNombre.toString());
        assertThat(sumas.get(campoCantidad.toString())).isEqualByComparingTo("3");
        assertThat(sumas.get(campoNombre.toString())).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Trae sólo los ids pedidos del bloque y negocio, los más nuevos primero")
    void findAllByIdInShouldReturnRequestedRegistrosOfBloqueAndNegocioNewestFirst() {
        UUID viejo = givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), Map.of(), hace(2));
        UUID nuevo = givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), Map.of(), hace(1));
        givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), Map.of(), hace(0));
        UUID otroBloque = givenRegistro(NegocioId.de(negocioA), BloqueId
                .de(givenBloque(NegocioId.de(negocioA), "Clientes")), Map.of(), hace(0));

        List<RegistroEntity> result = registros.findAllByIdInAndBloqueIdAndNegocioIdOrderByCreadoEnDescIdDesc(
                List.of(viejo, nuevo, otroBloque), bloqueA, negocioA);

        assertThat(result).extracting(RegistroEntity::getId).containsExactly(nuevo, viejo);
    }

    @Test
    @DisplayName("No trae registros pedidos por id con el negocio equivocado")
    void findAllByIdInShouldReturnNothingForOtherNegocio() {
        UUID registroId = givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), Map.of(), hace(0));

        List<RegistroEntity> result = registros.findAllByIdInAndBloqueIdAndNegocioIdOrderByCreadoEnDescIdDesc(
                List.of(registroId), bloqueA, negocioB);

        assertThat(result).isEmpty();
    }

    private Map<String, Object> producto(String nombre, Object cantidad) {
        Map<String, Object> valores = valores(campoNombre, nombre);
        valores.put(campoCantidad.toString(), cantidad);
        return valores;
    }

    private Map<String, BigDecimal> sumasPorCampo(List<IRegistroJpaRepository.SumaPorCampo> filas) {
        return filas.stream().collect(Collectors.toMap(
                IRegistroJpaRepository.SumaPorCampo::getCampo,
                IRegistroJpaRepository.SumaPorCampo::getSuma));
    }

    @Test
    @DisplayName("El reporte trae las filas del rango de fechas, las más nuevas primero, y respeta el límite")
    void buscarParaReporteShouldFilterByDateRangeNewestFirst() {
        UUID viejo = givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), producto("Viejo", 1),
                hace(60 * 24 * 10));
        UUID medio = givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), producto("Medio", 2),
                hace(60 * 24 * 5));
        UUID nuevo = givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), producto("Nuevo", 3), hace(60 * 24));
        String desde = AHORA.minusDays(6).toInstant().toString();
        String hasta = AHORA.minusDays(2).toInstant().toString();

        List<RegistroEntity> enRango = registros.buscarParaReporte(negocioA, bloqueA, null, desde, hasta, null, null, 100);
        List<RegistroEntity> desdeSolo = registros.buscarParaReporte(negocioA, bloqueA, null, desde, null, null, null, 100);
        List<RegistroEntity> hastaSolo = registros.buscarParaReporte(negocioA, bloqueA, null, null, hasta, null, null, 100);
        List<RegistroEntity> limitado = registros.buscarParaReporte(negocioA, bloqueA, null, null, null, null, null, 2);

        assertThat(enRango).extracting(RegistroEntity::getId).containsExactly(medio);
        assertThat(desdeSolo).extracting(RegistroEntity::getId).containsExactly(nuevo, medio);
        assertThat(hastaSolo).extracting(RegistroEntity::getId).containsExactly(medio, viejo);
        assertThat(limitado).extracting(RegistroEntity::getId).containsExactly(nuevo, medio);
    }

    @Test
    @DisplayName("El reporte filtra por el texto y por el valor de una columna")
    void buscarParaReporteShouldFilterByTextAndColumnValue() {
        UUID activoHarina = givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), activo("Harina 000", true),
                hace(3));
        givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), activo("Harina integral", false), hace(2));
        givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), activo("Azúcar", true), hace(1));

        List<RegistroEntity> result = registros.buscarParaReporte(
                negocioA, bloqueA, "%harina%", null, null, campoActivo.toString(), "true", 100);
        List<RegistroEntity> soloColumna = registros.buscarParaReporte(
                negocioA, bloqueA, null, null, null, campoActivo.toString(), "false", 100);

        assertThat(result).extracting(RegistroEntity::getId).containsExactly(activoHarina);
        assertThat(soloColumna).hasSize(1);
    }

    @Test
    @DisplayName("La cantidad y las sumas del reporte coinciden con sus filtros y no incluyen a otros negocios")
    void reporteTotalsShouldMatchTheFiltersAndExcludeOtherNegocios() {
        givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), activo("Harina", true, 40), hace(60 * 24 * 3));
        givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), activo("Azúcar", true, 7), hace(60 * 24 * 3));
        givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), activo("Sal", false, 100), hace(60 * 24 * 3));
        givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), activo("Harina vieja", true, 1000),
                hace(60 * 24 * 30));
        givenRegistro(NegocioId.de(negocioB), BloqueId.de(givenBloque(NegocioId.de(negocioB), "Productos")),
                activo("Harina", true, 5000), hace(60 * 24 * 3));
        String desde = AHORA.minusDays(10).toInstant().toString();
        String ids = campoCantidad.toString();

        long cantidad = registros.contarParaReporte(negocioA, bloqueA, null, desde, null, campoActivo.toString(), "true");
        Map<String, BigDecimal> sumas = sumasPorCampo(registros.sumarNumericosParaReporte(
                negocioA, bloqueA, null, desde, null, campoActivo.toString(), "true", List.of(ids)));
        long deOtroNegocio = registros.contarParaReporte(negocioB, bloqueA, null, desde, null, null, null);

        assertThat(cantidad).isEqualTo(2);
        assertThat(sumas.get(ids)).isEqualByComparingTo("47");
        assertThat(deOtroNegocio).isZero();
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
}
