package com.ensambla.backend.domain.service.sami;

import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.domain.model.plantilla.EstructuraPlantilla;
import com.ensambla.backend.domain.model.plantilla.EstructuraPlantilla.BloquePlantilla;
import com.ensambla.backend.domain.model.plantilla.EstructuraPlantilla.CampoPlantilla;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import com.ensambla.backend.domain.model.sami.DiccionarioSami;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/** Datos compartidos por los tests de Sami: el diccionario real y las cinco plantillas sembradas. */
public final class SamiPruebas {

    private SamiPruebas() {
    }

    public static DiccionarioSami diccionarioReal() {
        try (InputStream json = SamiPruebas.class.getResourceAsStream("/ia/sami-diccionario.json")) {
            return JsonMapper.builder().build().readValue(json, DiccionarioSami.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static List<PlantillaProyecto> plantillas() {
        return List.of(
                plantilla("Inventario", campo("Nombre", TipoCampo.TEXTO), campo("Stock", TipoCampo.NUMERO)),
                plantilla("Pedidos", campo("Cliente", TipoCampo.TEXTO), campo("Entrega", TipoCampo.FECHA)),
                plantilla("Clientes", campo("Nombre", TipoCampo.TEXTO), campo("Teléfono", TipoCampo.TEXTO)),
                plantilla("Ventas", campo("Producto", TipoCampo.TEXTO), campo("Monto", TipoCampo.NUMERO)),
                plantilla("Gastos", campo("Detalle", TipoCampo.TEXTO), campo("Monto", TipoCampo.NUMERO)));
    }

    public static PlantillaProyecto plantilla(String nombre, CampoPlantilla... campos) {
        return new PlantillaProyecto(UUID.randomUUID(), nombre, "",
                new EstructuraPlantilla(List.of(new BloquePlantilla(nombre, List.of(campos)))));
    }

    public static CampoPlantilla campo(String nombre, TipoCampo tipo) {
        return new CampoPlantilla(nombre, tipo, false, null, null);
    }
}
