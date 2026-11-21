package com.ensambla.backend.domain.model.procesamiento;

import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.lectura.BloquePropuesto;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.ConsumoIa;
import com.ensambla.backend.domain.model.lectura.DatoLeido;
import com.ensambla.backend.domain.model.lectura.FilaLeida;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.lectura.TipoRegistroCuaderno;
import com.ensambla.backend.domain.model.procesamiento.BloqueDeImagen.CampoDeImagen;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class LecturaImagenTest {

    private final BloqueDeImagen bloque = BloqueDeImagen.de("Ventas", List.of(
            CampoDeImagen.nuevo("importe", "Monto", TipoCampo.NUMERO, List.of()),
            CampoDeImagen.nuevo("cliente", "Cliente", TipoCampo.TEXTO, List.of())));

    private PropuestaLectura propuesta;
    private BloqueDeImagen bloqueUsado = bloque;
    private LecturaImagen lectura;

    @Test
    @DisplayName("Los datos leídos van por la clave del campo del bloque y la columna que falta se propone como nueva")
    void losDatosLeidosVanPorLaClaveDelCampoDelBloqueYLaColumnaQueFaltaSeProponeComoNueva() {
        dadoUnaPropuestaConUnaColumnaNuevaDeTelefono();

        cuandoSeArmaLaLecturaDeUnBloqueExistente();

        entoncesLaColumnaNuevaSeProponeYLosDatosVanPorClave();
    }

    @Test
    @DisplayName("Si el bloque todavía no existe no se proponen columnas nuevas ni se devuelven sus valores")
    void siElBloqueTodaviaNoExisteNoSeProponenColumnasNuevasNiSeDevuelvenSusValores() {
        dadoUnaPropuestaConMontoYUnaColumnaNuevaDeTelefono();

        cuandoSeArmaLaLecturaDeUnBloqueInexistente();

        entoncesNoHayColumnasNuevasYSoloQuedaElImporte();
    }

    @Test
    @DisplayName("La clave de una columna nueva no pisa la de un campo del bloque ni la de otra columna nueva")
    void laClaveDeUnaColumnaNuevaNoPisaLaDeUnCampoDelBloqueNiLaDeOtraColumnaNueva() {
        dadoUnaPropuestaConColumnasNuevasDeNombresRepetidos();

        cuandoSeArmaLaLecturaDeUnBloqueConMarca();

        entoncesLasColumnasNuevasTienenClavesUnicas();
    }

    @Test
    @DisplayName("Un dato de una columna que no es del bloque ni se propuso como nueva se descarta")
    void unDatoDeUnaColumnaQueNoEsDelBloqueNiSePropusoComoNuevaSeDescarta() {
        dadoUnaPropuestaConUnaColumnaDesconocida();

        cuandoSeArmaLaLecturaDeUnBloqueExistente();

        entoncesSoloQuedaElImporte();
    }

    @Test
    @DisplayName("Devuelve los avisos de la lectura")
    void devuelveLosAvisosDeLaLectura() {
        dadoUnaPropuestaConUnAviso();

        cuandoSeArmaLaLecturaDeUnBloqueExistente();

        entoncesSeConservaElAvisoYNoHayFilas();
    }

    // --- dado ---
    private void dadoUnaPropuestaConUnaColumnaNuevaDeTelefono() {
        propuesta = unaPropuesta(List.of(columna("telefono", "Teléfono", TipoCampo.TEXTO)),
                fila(dato("monto", "1500", 0.95), dato("cliente", "Ana", 0.9), dato("telefono", "1155", 0.5)));
    }

    private void dadoUnaPropuestaConMontoYUnaColumnaNuevaDeTelefono() {
        propuesta = unaPropuesta(List.of(columna("telefono", "Teléfono", TipoCampo.TEXTO)),
                fila(dato("monto", "1500", 0.9), dato("telefono", "1155", 0.9)));
    }

    private void dadoUnaPropuestaConUnaColumnaDesconocida() {
        propuesta = unaPropuesta(List.of(), fila(dato("monto", "1", 0.9), dato("otra", "x", 0.9)));
    }

    private void dadoUnaPropuestaConColumnasNuevasDeNombresRepetidos() {
        bloqueUsado = BloqueDeImagen.de("Ventas", List.of(
                CampoDeImagen.nuevo("marca", "Producto", TipoCampo.TEXTO, List.of())));
        propuesta = new PropuestaLectura(
                unaLectura(List.of(columna("a", "Producto", TipoCampo.TEXTO)),
                        fila(dato("a", "Yerba", 0.9), dato("b", "Taragüí", 0.9), dato("c", "Otra", 0.9),
                                dato("d", "x", 0.9))),
                Map.of("a", bloqueUsado.campos().getFirst().id()),
                List.of(columna("b", "Marca", TipoCampo.TEXTO), columna("c", "¡Marca!", TipoCampo.TEXTO),
                        columna("d", "¿?", TipoCampo.TEXTO)));
    }

    private void dadoUnaPropuestaConUnAviso() {
        propuesta = new PropuestaLectura(
                new LecturaCuaderno(TipoRegistroCuaderno.OTRO, new BloquePropuesto("Hoja", null, List.of()),
                        List.of(), List.of("Página torcida"), consumo()),
                Map.of(), List.of());
    }

    private PropuestaLectura unaPropuesta(List<CampoPropuesto> nuevas, FilaLeida... filas) {
        Map<String, CampoId> mapeo = Map.of(
                "monto", bloque.campos().get(0).id(), "cliente", bloque.campos().get(1).id());
        return new PropuestaLectura(unaLectura(List.of(), filas), mapeo, nuevas);
    }

    private LecturaCuaderno unaLectura(List<CampoPropuesto> campos, FilaLeida... filas) {
        return new LecturaCuaderno(TipoRegistroCuaderno.OTRO, new BloquePropuesto("Ventas", null, campos),
                List.of(filas), List.of(), consumo());
    }

    private ConsumoIa consumo() {
        return new ConsumoIa("simulado", "demo", "v3", 1, 0, 0, 1);
    }

    private CampoPropuesto columna(String clave, String nombre, TipoCampo tipo) {
        return new CampoPropuesto(clave, nombre, tipo, List.of());
    }

    private Map.Entry<String, DatoLeido> dato(String clave, String valor, double confianza) {
        return Map.entry(clave, new DatoLeido(valor, confianza));
    }

    @SafeVarargs
    private FilaLeida fila(Map.Entry<String, DatoLeido>... datos) {
        Map<String, DatoLeido> mapa = new LinkedHashMap<>();
        for (Map.Entry<String, DatoLeido> dato : datos) {
            mapa.put(dato.getKey(), dato.getValue());
        }
        return new FilaLeida(1, 1, mapa);
    }

    // --- cuando ---
    private void cuandoSeArmaLaLecturaDeUnBloqueExistente() {
        lectura = LecturaImagen.desde(propuesta, bloqueUsado, true);
    }

    private void cuandoSeArmaLaLecturaDeUnBloqueInexistente() {
        lectura = LecturaImagen.desde(propuesta, bloqueUsado, false);
    }

    private void cuandoSeArmaLaLecturaDeUnBloqueConMarca() {
        lectura = LecturaImagen.desde(propuesta, bloqueUsado, true);
    }

    // --- entonces ---
    private void entoncesLaColumnaNuevaSeProponeYLosDatosVanPorClave() {
        assertThat(lectura.camposNuevos())
                .extracting(CampoPropuesto::clave, CampoPropuesto::nombre, CampoPropuesto::tipo)
                .containsExactly(tuple("telefono", "Teléfono", TipoCampo.TEXTO));
        assertThat(lectura.filas()).hasSize(1);
        Map<String, DatoLeido> datos = lectura.filas().getFirst();
        assertThat(datos.keySet()).containsExactlyInAnyOrder("importe", "cliente", "telefono");
        assertThat(datos.get("importe").valor()).isEqualTo("1500");
        assertThat(datos.get("telefono").confianza()).isEqualTo(0.5);
    }

    private void entoncesNoHayColumnasNuevasYSoloQuedaElImporte() {
        assertThat(lectura.camposNuevos()).isEmpty();
        assertThat(lectura.filas().getFirst().keySet()).containsExactly("importe");
    }

    private void entoncesLasColumnasNuevasTienenClavesUnicas() {
        assertThat(lectura.camposNuevos()).extracting(CampoPropuesto::clave)
                .containsExactly("marca_2", "marca_3", "columna");
        assertThat(lectura.filas().getFirst().keySet())
                .containsExactlyInAnyOrder("marca", "marca_2", "marca_3", "columna");
    }

    private void entoncesSoloQuedaElImporte() {
        assertThat(lectura.filas().getFirst().keySet()).containsExactly("importe");
    }

    private void entoncesSeConservaElAvisoYNoHayFilas() {
        assertThat(lectura.advertencias()).containsExactly("Página torcida");
        assertThat(lectura.filas()).isEmpty();
    }
}
