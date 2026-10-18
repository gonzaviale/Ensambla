package com.ensambla.backend.domain.mapper.procesamiento;

import com.ensambla.backend.domain.dto.procesamiento.LecturaItem;
import com.ensambla.backend.domain.dto.procesamiento.PropuestaItem;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.ConsumoIa;
import com.ensambla.backend.domain.model.lectura.FilaLeida;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.procesamiento.EstadoLectura;
import com.ensambla.backend.domain.model.procesamiento.PropuestaLectura;

import java.util.LinkedHashMap;
import java.util.Map;

public class ObtenerLecturaMapper {

    public LecturaItem toResponse(EstadoLectura estado) {
        return new LecturaItem(estado.id(), estado.estado().name(), estado.motivoError(), estado.intentos(),
                propuesta(estado.propuesta()));
    }

    private PropuestaItem propuesta(PropuestaLectura propuesta) {
        if (propuesta == null) {
            return null;
        }
        LecturaCuaderno lectura = propuesta.lectura();
        ConsumoIa consumo = lectura.consumo();
        return new PropuestaItem(lectura.tipoRegistro().name(), lectura.bloque().nombre(),
                lectura.bloque().bloqueExistenteId(), lectura.bloque().campos().stream().map(this::campo).toList(),
                lectura.filas().stream().map(this::fila).toList(), lectura.advertencias(), propuesta.mapeoCampos(),
                propuesta.camposNuevos().stream().map(this::campo).toList(),
                new PropuestaItem.ConsumoItem(consumo.proveedor(), consumo.modelo(), consumo.versionPrompt(),
                        consumo.paginas(), consumo.tokensEntrada(), consumo.tokensSalida(), consumo.duracionMs()));
    }

    private PropuestaItem.CampoItem campo(CampoPropuesto campo) {
        return new PropuestaItem.CampoItem(campo.clave(), campo.nombre(), campo.tipo().name(), campo.opciones());
    }

    private PropuestaItem.FilaItem fila(FilaLeida fila) {
        Map<String, PropuestaItem.DatoItem> datos = new LinkedHashMap<>();
        fila.datos().forEach((clave, dato) -> datos.put(clave,
                new PropuestaItem.DatoItem(dato.valor(), dato.confianza())));
        return new PropuestaItem.FilaItem(fila.pagina(), fila.renglon(), datos);
    }
}
