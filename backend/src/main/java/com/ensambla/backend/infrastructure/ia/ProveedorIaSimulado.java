package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.sami.taller.KitsRubro;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.function.Supplier;

/**
 * Sin costo y sin red: la lectura de ejemplo, la primera plantilla y el taller que arma el kit. Es el de por defecto.
 * Los kits se piden recién al armar el taller, así la lectura sola (la evaluación) no necesita a Sami.
 */
public class ProveedorIaSimulado implements ProveedorIa {

    static final String NOMBRE = "simulado";

    private final Resource lecturaDeEjemplo;
    private final Supplier<KitsRubro> kitsRubro;

    public ProveedorIaSimulado(Resource lecturaDeEjemplo, Supplier<KitsRubro> kitsRubro) {
        this.lecturaDeEjemplo = lecturaDeEjemplo;
        this.kitsRubro = kitsRubro;
    }

    @Override
    public String nombre() {
        return NOMBRE;
    }

    @Override
    public ClienteModeloVision vision(AjustesIa ajustes) {
        return new ClienteModeloVisionSimulado(lecturaDeEjemplo);
    }

    @Override
    public ClienteModeloTexto texto(AjustesIa ajustes, String modelo, int maxTokens) {
        return new ClienteModeloTextoSimulado();
    }

    @Override
    public ClienteModeloConHerramientas conHerramientas(AjustesIa ajustes, int maxTokens,
                                                        List<DefinicionHerramienta> herramientas) {
        return new ClienteModeloConHerramientasSimulado(kitsRubro.get());
    }
}
