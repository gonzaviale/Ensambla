package com.ensambla.backend.domain.service.receta;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.receta.Receta;
import com.ensambla.backend.domain.model.receta.RecetaCosteada;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class RecetaService {

    private final CampoRepository campoRepository;

    private final RegistroRepository registroRepository;

    /** Calcula el costo con el precio actual de cada insumo; una receta vacía cuesta cero. */
    public RecetaCosteada costear(Receta receta, NegocioId negocioId) {
        if (receta.esVacia()) {
            return RecetaCosteada.vacia();
        }
        Campo precio = obtenerColumnaDelPrecio(receta, negocioId);
        List<Registro> insumos = obtenerInsumos(receta, precio, negocioId);
        List<Campo> camposDeInsumos = obtenerCamposDeLaTablaDeInsumos(precio, negocioId);
        return RecetaCosteada.de(receta, precio.getBloqueId(), insumos, camposDeInsumos);
    }

    private Campo obtenerColumnaDelPrecio(Receta receta, NegocioId negocioId) {
        return campoRepository.findByIdAndNegocioId(receta.getCampoPrecioId(), negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("La columna del precio no existe"));
    }

    private List<Registro> obtenerInsumos(Receta receta, Campo precio, NegocioId negocioId) {
        return registroRepository.findAllByIdsAndBloque(receta.insumoIds(), precio.getBloqueId(), negocioId);
    }

    private List<Campo> obtenerCamposDeLaTablaDeInsumos(Campo precio, NegocioId negocioId) {
        return campoRepository.findByBloqueIdAndNegocioId(precio.getBloqueId(), negocioId);
    }
}
