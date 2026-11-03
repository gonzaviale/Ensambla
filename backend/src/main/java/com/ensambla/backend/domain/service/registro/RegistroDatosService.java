package com.ensambla.backend.domain.service.registro;

import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.precio.CambioPrecio;
import com.ensambla.backend.domain.model.registro.ConsultaRegistros;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.precio.HistorialPrecioRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class RegistroDatosService {

    static final int MAXIMO_REFERENCIAS = 20;

    private static final String MENSAJE_FILA_INEXISTENTE = "Esa fila ya no existe";

    private static final String CLAVE_PRECIO = "price";

    private final RegistroRepository registroRepository;

    private final BloqueRepository bloqueRepository;

    private final HistorialPrecioRepository historialPrecioRepository;

    private final TransaccionPort transaccionPort;

    /**
     * Filas del bloque que se ofrecen en un campo RELACION, las más nuevas primero: con {@code ids}, sólo esas (las que
     * existen); sin ids, hasta {@value #MAXIMO_REFERENCIAS} que coinciden con el texto como en el listado.
     */
    public List<Registro> buscarReferenciables(NegocioId negocioId, BloqueId bloqueId, List<Campo> campos, String texto,
                                               List<RegistroId> ids) {
        if (!ids.isEmpty()) {
            return registroRepository.findAllByIdsAndBloque(ids, bloqueId, negocioId);
        }
        ConsultaRegistros consulta = ConsultaRegistros.armar(
                negocioId, bloqueId, campos, texto, null, null, 0, MAXIMO_REFERENCIAS);
        return registroRepository.buscar(consulta).contenido();
    }

    /**
     * Guarda los cambios del registro. Si es un producto del inventario y le cambió el precio, deja anotado el cambio
     * en el historial de precios: el motor de avisos lo usa para saber cuándo hubo un aumento.
     */
    public Registro editar(Registro registro, List<Campo> campos, Map<String, Object> cambios,
                           OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> {
            Map<String, Object> validos = Registro.validar(campos, registro.combinarCon(cambios));
            validarConexiones(registro.getNegocioId(), campos, validos);
            Optional<Campo> campoPrecio = campos.stream().filter(campo -> CLAVE_PRECIO.equals(campo.getClave()))
                    .findFirst();
            BigDecimal anterior = campoPrecio.map(campo -> precioDe(registro, campo)).orElse(null);
            registro.reemplazarValores(validos, ahora);
            Registro guardado = registroRepository.save(registro);
            campoPrecio.ifPresent(campo -> registrarCambioDePrecio(guardado, campo, anterior, ahora));
            return guardado;
        });
    }

    private void registrarCambioDePrecio(Registro registro, Campo campoPrecio, BigDecimal anterior,
                                         OffsetDateTime ahora) {
        CambioPrecio cambio = CambioPrecio.entre(
                registro.getNegocioId(), registro.getId(), anterior, precioDe(registro, campoPrecio), ahora);
        if (cambio != null && esDeInventario(registro)) {
            historialPrecioRepository.save(cambio);
        }
    }

    /** Las filas de ventas también tienen precio, pero sólo el de un producto del inventario es «el precio de venta». */
    private boolean esDeInventario(Registro registro) {
        return bloqueRepository.findByIdAndNegocioId(registro.getBloqueId(), registro.getNegocioId())
                .filter(bloque -> bloque.getTipo() == TipoBloque.INVENTARIO)
                .isPresent();
    }

    private static BigDecimal precioDe(Registro registro, Campo campoPrecio) {
        return registro.getValores().get(campoPrecio.getId().toString()) instanceof Number numero
                ? new BigDecimal(numero.toString())
                : null;
    }

    /**
     * Cada valor de un campo RELACION tiene que ser una fila existente de la tabla conectada. Hace una consulta por
     * tabla conectada, no una por valor. Los valores ya vienen validados como ids.
     *
     * @throws RegistroInvalidoException con "Esa fila ya no existe" en cada campo que apunta a una fila inexistente
     */
    public void validarConexiones(NegocioId negocioId, List<Campo> campos, Map<String, Object> valores) {
        Map<BloqueId, List<Campo>> porBloque = campos.stream()
                .filter(Campo::esRelacion)
                .filter(campo -> valores.containsKey(campo.getId().toString()))
                .collect(Collectors.groupingBy(Campo::getBloqueRelacionadoId));
        Map<String, String> errores = new LinkedHashMap<>();
        porBloque.forEach((bloqueId, conexiones) -> {
            Set<RegistroId> buscados = conexiones.stream()
                    .map(campo -> referenciaDe(campo, valores))
                    .collect(Collectors.toSet());
            Set<RegistroId> existentes = registroRepository.findAllByIdsAndBloque(buscados, bloqueId, negocioId)
                    .stream()
                    .map(Registro::getId)
                    .collect(Collectors.toSet());
            conexiones.stream()
                    .filter(campo -> !existentes.contains(referenciaDe(campo, valores)))
                    .forEach(campo -> errores.put(campo.getId().toString(), MENSAJE_FILA_INEXISTENTE));
        });
        if (!errores.isEmpty()) {
            throw new RegistroInvalidoException(errores);
        }
    }

    private static RegistroId referenciaDe(Campo campo, Map<String, Object> valores) {
        return RegistroId.de((String) valores.get(campo.getId().toString()));
    }
}
