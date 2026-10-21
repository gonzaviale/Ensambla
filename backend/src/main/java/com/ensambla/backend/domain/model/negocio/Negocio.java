package com.ensambla.backend.domain.model.negocio;

import com.ensambla.backend.domain.model.enums.RubroNegocio;
import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.zona.Zona;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.Objects;

/** Espacio del negocio: todo lo que construye el usuario cuelga de él (aislamiento de datos). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Negocio {

    private UUID id;

    private String nombre;

    private UUID duenioId;

    @Builder.Default
    private RubroNegocio rubro = RubroNegocio.OTRO;

    private String direccion;

    private String localidad;

    private BigDecimal latitud;

    private BigDecimal longitud;

    private UUID zonaId;

    /** Si es false, el negocio no aporta a los datos agregados de su zona. */
    @Builder.Default
    private Boolean comparteDatosZona = true;

    private OffsetDateTime creadoEn;

    private OffsetDateTime actualizadoEn;

    /** Los campos ausentes conservan su valor; una ubicación se reemplaza de forma completa. */
    public void configurar(String nombre, RubroNegocio rubro, UbicacionNegocio ubicacion,
                           Boolean comparteDatosZona, OffsetDateTime ahora) {
        if (nombre != null) {
            String limpio = nombre.strip();
            if (limpio.isBlank() || limpio.length() > 120) {
                throw new BusinessRuleException("El nombre debe tener entre 1 y 120 caracteres");
            }
            this.nombre = limpio;
        }
        if (rubro != null) {
            this.rubro = rubro;
        }
        if (ubicacion != null) {
            if (!Objects.equals(latitud, ubicacion.latitud()) || !Objects.equals(longitud, ubicacion.longitud())) {
                zonaId = null;
            }
            direccion = ubicacion.direccion();
            localidad = ubicacion.localidad();
            latitud = ubicacion.latitud();
            longitud = ubicacion.longitud();
        }
        if (comparteDatosZona != null) {
            this.comparteDatosZona = comparteDatosZona;
        }
        actualizadoEn = ahora;
    }

    /** Si la ubicación recibida cambia el punto o la localidad guardados. */
    public boolean cambiaUbicacion(UbicacionNegocio ubicacion) {
        return !Objects.equals(latitud, ubicacion.latitud()) || !Objects.equals(longitud, ubicacion.longitud())
                || !Objects.equals(localidad, ubicacion.localidad());
    }

    /** Un negocio con punto y sin zona necesita que se la resuelvan; un punto nuevo la invalida. */
    public boolean necesitaZona() {
        return latitud != null && zonaId == null;
    }

    public void asignarZona(Zona zona) {
        zonaId = zona == null ? null : zona.getId();
    }
}
