package com.ensambla.backend.infrastructure.ia;

import java.util.List;

/**
 * Un proveedor de IA arma las tres puertas con las que hablan la lectura y Sami: visión ({@link ClienteModeloVision}),
 * texto ({@link ClienteModeloTexto}) y conversación con herramientas ({@link ClienteModeloConHerramientas}). Quién
 * atiende lo decide la configuración ({@code ensambla.ia.proveedor} y {@code ensambla.sami.ia.proveedor}).
 * <p>
 * Para sumar un proveedor: implementar esta interfaz, registrarla como bean en {@link IaConfig} y poner sus
 * credenciales y modelos en {@code ensambla.ia.proveedores.<nombre>.*}. Nada más cambia.
 */
public interface ProveedorIa {

    /** El valor de {@code ensambla.ia.proveedor} que lo elige. */
    String nombre();

    /** Lee fotos con {@link AjustesIa#modeloVision()}. */
    ClienteModeloVision vision(AjustesIa ajustes);

    /** Texto a JSON con el modelo indicado (el rápido o el avanzado de los ajustes). */
    ClienteModeloTexto texto(AjustesIa ajustes, String modelo, int maxTokens);

    /** Una llamada con herramientas por turno, con {@link AjustesIa#modeloRapido()}. */
    ClienteModeloConHerramientas conHerramientas(AjustesIa ajustes, int maxTokens,
                                                 List<DefinicionHerramienta> herramientas);
}
