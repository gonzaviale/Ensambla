package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.model.zona.ZonaId;
import com.ensambla.backend.domain.useCase.plan.AccesoContenidoItem;
import org.mockito.stubbing.Answer;

import java.math.BigDecimal;
import java.util.List;

/** Zonas, productos agregados y cupos del plan listos para los tests de Mi zona y de Analizar zona. */
public final class ZonasDePrueba {

    public static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");
    public static final ZonaId ZONA_ID = ZonaId.de("c0000000-0000-4000-8000-000000000001");
    public static final String NOMBRE_ZONA = "San Justo - UNLaM";
    public static final BigDecimal LATITUD = new BigDecimal("-34.670300");
    public static final BigDecimal LONGITUD = new BigDecimal("-58.562800");

    private static final String LOCALIDAD = "San Justo";
    private static final int COMERCIOS_POR_PRODUCTO = 8;

    private ZonasDePrueba() {
    }

    public static Zona unaZonaDeSanJusto() {
        return Zona.builder().id(ZONA_ID).nombre(NOMBRE_ZONA).localidad(LOCALIDAD).build();
    }

    public static Zona unaZonaDeVillaCrespo() {
        return Zona.builder().id(ZonaId.nuevo()).nombre("Villa Crespo").localidad("CABA").build();
    }

    public static Zona unaZonaNombrada(String nombre, String localidad) {
        return Zona.builder().nombre(nombre).localidad(localidad).build();
    }

    public static Zona unaZonaSinLocalidad(String nombre) {
        return Zona.builder().nombre(nombre).build();
    }

    public static Zona unaZonaCentradaEnSanJusto(int radioMetros) {
        return Zona.builder().nombre(NOMBRE_ZONA).latitud(LATITUD).longitud(LONGITUD).radioMetros(radioMetros).build();
    }

    public static Zona unaZonaCentradaEn(String latitud, String longitud, int radioMetros) {
        return Zona.builder().latitud(new BigDecimal(latitud)).longitud(new BigDecimal(longitud))
                .radioMetros(radioMetros).build();
    }

    public static AgregadoZonaProducto unProductoDeZona(String nombre) {
        return unProductoDeZona(nombre, 6, "12.50");
    }

    public static AgregadoZonaProducto unProductoDeZona(String nombre, int comerciosVenden, String unidadesSemana) {
        return unProductoDeZonaCobradoPor(nombre, COMERCIOS_POR_PRODUCTO, comerciosVenden, unidadesSemana);
    }

    public static AgregadoZonaProducto unProductoDeZonaCobradoPor(
            String nombre, int comercios, int comerciosVenden, String unidadesSemana) {
        return new AgregadoZonaProducto(nombre, comercios, comerciosVenden, new BigDecimal("2800.00"),
                new BigDecimal("3100.00"), new BigDecimal("3400.00"), new BigDecimal(unidadesSemana));
    }

    public static AnalisisZona unAnalisisDeZona(long comercios, AgregadoZonaProducto... productos) {
        return new AnalisisZona(NOMBRE_ZONA, comercios, List.of(productos));
    }

    public static List<String> nombresDe(AnalisisZona analisis) {
        return analisis.productos().stream().map(AgregadoZonaProducto::nombre).toList();
    }

    public static Answer<AccesoContenidoItem> autorizaTodo() {
        return invocation -> {
            List<String> claves = invocation.getArgument(2);
            return new AccesoContenidoItem(claves, 0, null, claves.size(), 0, null);
        };
    }

    public static Answer<AccesoContenidoItem> autorizaSoloLaPrimera() {
        return invocation -> {
            List<String> claves = invocation.getArgument(2);
            return new AccesoContenidoItem(claves.subList(0, 1), 1, 1, claves.size(), claves.size() - 1, null);
        };
    }
}
