package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.negocio.DestinoDeCobro;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.negocio.UbicacionNegocio;
import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.model.zona.ZonaId;

import java.math.BigDecimal;

import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;

/** Negocios, ubicaciones y zonas armados en memoria para probar la configuración del negocio. */
public final class NegociosDePrueba {

    private NegociosDePrueba() {
    }

    public static Negocio unNegocioSinUbicacion() {
        return Negocio.builder().id(NEGOCIO_ID).build();
    }

    public static Negocio unNegocioSinUbicacionEnUnaZonaAnterior() {
        return Negocio.builder().id(NEGOCIO_ID).zonaId(ZonaId.nuevo()).build();
    }

    public static Negocio unNegocioConDestinoDeCobro(String alias, String titular) {
        return Negocio.builder().id(NEGOCIO_ID).cobroAlias(alias).cobroTitular(titular).build();
    }

    public static Negocio unNegocioLlamado(String nombre) {
        return Negocio.builder().id(NEGOCIO_ID).nombre(nombre).build();
    }

    public static Negocio unNegocioConPuntoSinZona(BigDecimal latitud, BigDecimal longitud) {
        return Negocio.builder().id(NEGOCIO_ID).latitud(latitud).longitud(longitud).build();
    }

    public static Negocio unNegocioConPuntoYZona(
            BigDecimal latitud, BigDecimal longitud, String localidad, ZonaId zonaId) {
        return Negocio.builder().id(NEGOCIO_ID).latitud(latitud).longitud(longitud)
                .localidad(localidad).zonaId(zonaId).build();
    }

    public static Negocio unNegocioDeSanJusto(ZonaId zonaId) {
        return Negocio.builder().nombre("Mi negocio").rubro(RubroNegocio.ALMACEN)
                .direccion("Calle 1").localidad("San Justo")
                .latitud(new BigDecimal("-34.000000")).longitud(new BigDecimal("-58.000000"))
                .zonaId(zonaId).build();
    }

    public static UbicacionNegocio unaUbicacionConEspacios(String latitud, String longitud) {
        return new UbicacionNegocio("  Nueva dirección  ", "  San Justo  ",
                new BigDecimal(latitud), new BigDecimal(longitud));
    }

    public static Zona unaZonaLlamada(String nombre) {
        return Zona.builder().id(ZonaId.nuevo()).nombre(nombre).build();
    }

    public static Zona unaZonaConId(ZonaId zonaId) {
        return Zona.builder().id(zonaId).build();
    }
}
