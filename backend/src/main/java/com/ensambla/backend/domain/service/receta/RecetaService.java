package com.ensambla.backend.domain.service.receta;

import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.receta.Receta;
import com.ensambla.backend.domain.model.receta.RecetaCosteada;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.receta.RecetaRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class RecetaService {

    private static final String MENSAJE_INSUMO_INEXISTENTE = "Ese insumo ya no existe";

    private final RecetaRepository recetaRepository;

    private final CampoRepository campoRepository;

    private final RegistroRepository registroRepository;

    private final TransaccionPort transaccionPort;

    /** La receta del producto; si todavía no tiene, una receta vacía. */
    public Receta obtener(UUID productoId, UUID negocioId) {
        return recetaRepository.findByProductoIdAndNegocioId(productoId, negocioId)
                .orElseGet(() -> Receta.builder()
                        .negocioId(negocioId)
                        .productoId(productoId)
                        .lineas(List.of())
                        .build());
    }

    /**
     * Guarda la receta ya armada (reemplaza la anterior del producto); una receta sin líneas quita la que hubiera. La
     * columna del precio tiene que ser numérica y cada insumo, una fila de su tabla.
     *
     * @throws RegistroInvalidoException si la columna del precio no sirve o algún insumo no existe en su tabla
     */
    public Receta guardar(Receta receta) {
        return transaccionPort.ejecutar(() -> {
            if (receta.esVacia()) {
                recetaRepository.deleteByProductoIdAndNegocioId(receta.getProductoId(), receta.getNegocioId());
                return receta;
            }
            Campo precio = campoRepository.findByIdAndNegocioId(receta.getCampoPrecioId(), receta.getNegocioId())
                    .filter(campo -> campo.getTipo() == TipoCampo.NUMERO)
                    .orElseThrow(() -> new RegistroInvalidoException(
                            Map.of("campoPrecioId", "Elegí una columna numérica con el precio de los insumos")));
            validarInsumos(receta, precio);
            recetaRepository.findByProductoIdAndNegocioId(receta.getProductoId(), receta.getNegocioId())
                    .ifPresent(receta::reemplazar);
            return recetaRepository.save(receta);
        });
    }

    /** Calcula el costo con el precio actual de cada insumo; una receta vacía cuesta cero. */
    public RecetaCosteada costear(Receta receta, UUID negocioId) {
        if (receta.esVacia()) {
            return RecetaCosteada.vacia();
        }
        Campo precio = campoRepository.findByIdAndNegocioId(receta.getCampoPrecioId(), negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("La columna del precio no existe"));
        List<Registro> insumos = registroRepository.findAllByIdsAndBloque(
                receta.insumoIds(), precio.getBloqueId(), negocioId);
        List<Campo> camposDeInsumos = campoRepository.findByBloqueIdAndNegocioId(precio.getBloqueId(), negocioId);
        return RecetaCosteada.de(receta, precio.getBloqueId(), insumos, camposDeInsumos);
    }

    private void validarInsumos(Receta receta, Campo precio) {
        Set<UUID> existentes = registroRepository.findAllByIdsAndBloque(
                        receta.insumoIds(), precio.getBloqueId(), receta.getNegocioId()).stream()
                .map(Registro::getId)
                .collect(Collectors.toSet());
        Map<String, String> errores = new LinkedHashMap<>();
        for (int indice = 0; indice < receta.getLineas().size(); indice++) {
            if (!existentes.contains(receta.getLineas().get(indice).insumoId())) {
                errores.put("lineas[" + indice + "]", MENSAJE_INSUMO_INEXISTENTE);
            }
        }
        if (!errores.isEmpty()) {
            throw new RegistroInvalidoException(errores);
        }
    }
}
