package com.ensambla.backend.domain.model.negocio;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.model.zona.ZonaId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

/** Espacio del negocio: todo lo que construye el usuario cuelga de él (aislamiento de datos). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Negocio {

    private NegocioId id;

    private String nombre;

    private UsuarioId duenioId;

    @Builder.Default
    private RubroNegocio rubro = RubroNegocio.OTRO;

    private String direccion;

    private String localidad;

    private BigDecimal latitud;

    private BigDecimal longitud;

    private ZonaId zonaId;

    /** Si es false, el negocio no aporta a los datos agregados de su zona. */
    @Builder.Default
    private Boolean comparteDatosZona = true;

    /** Alias o CVU donde el comerciante recibe las transferencias de sus ventas con QR. */
    private String cobroAlias;

    private String cobroTitular;

    private OffsetDateTime creadoEn;

    private OffsetDateTime actualizadoEn;

    /** Los campos ausentes conservan su valor; la ubicación y el destino de cobro se reemplazan de forma completa. */
    public void configurar(String nombre, RubroNegocio rubro, UbicacionNegocio ubicacion,
                           Boolean comparteDatosZona, DestinoDeCobro destinoDeCobro, OffsetDateTime ahora) {
        cambiarNombreSiViene(nombre);
        cambiarRubroSiViene(rubro);
        reubicarSiViene(ubicacion);
        cambiarPrivacidadSiViene(comparteDatosZona);
        cambiarDestinoDeCobroSiViene(destinoDeCobro);
        actualizadoEn = ahora;
    }

    public DestinoDeCobro destinoDeCobro() {
        return new DestinoDeCobro(cobroAlias, cobroTitular);
    }

    private void cambiarDestinoDeCobroSiViene(DestinoDeCobro destino) {
        if (destino != null) {
            cobroAlias = destino.alias();
            cobroTitular = destino.titular();
        }
    }

    private void cambiarNombreSiViene(String nombre) {
        if (nombre != null) {
            this.nombre = nombreValido(nombre);
        }
    }

    private String nombreValido(String nombre) {
        String limpio = nombre.strip();
        if (limpio.isBlank() || limpio.length() > 120) {
            throw new BusinessRuleException("El nombre debe tener entre 1 y 120 caracteres");
        }
        return limpio;
    }

    private void cambiarRubroSiViene(RubroNegocio rubro) {
        if (rubro != null) {
            this.rubro = rubro;
        }
    }

    private void reubicarSiViene(UbicacionNegocio ubicacion) {
        if (ubicacion == null) {
            return;
        }
        quitarZonaSiCambiaElPunto(ubicacion);
        direccion = ubicacion.direccion();
        localidad = ubicacion.localidad();
        latitud = ubicacion.latitud();
        longitud = ubicacion.longitud();
    }

    private void quitarZonaSiCambiaElPunto(UbicacionNegocio ubicacion) {
        if (cambiaElPunto(ubicacion)) {
            zonaId = null;
        }
    }

    private void cambiarPrivacidadSiViene(Boolean comparteDatosZona) {
        if (comparteDatosZona != null) {
            this.comparteDatosZona = comparteDatosZona;
        }
    }

    /** Si la ubicación recibida cambia el punto o la localidad guardados. */
    public boolean cambiaUbicacion(UbicacionNegocio ubicacion) {
        return cambiaElPunto(ubicacion) || !Objects.equals(localidad, ubicacion.localidad());
    }

    private boolean cambiaElPunto(UbicacionNegocio ubicacion) {
        return !Objects.equals(latitud, ubicacion.latitud()) || !Objects.equals(longitud, ubicacion.longitud());
    }

    /** Un negocio con punto y sin zona necesita que se la resuelvan; un punto nuevo la invalida. */
    public boolean necesitaZona() {
        return latitud != null && zonaId == null;
    }

    public void asignarZona(Zona zona) {
        zonaId = zona == null ? null : zona.getId();
    }
}
