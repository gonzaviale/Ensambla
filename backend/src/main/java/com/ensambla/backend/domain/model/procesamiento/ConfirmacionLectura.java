package com.ensambla.backend.domain.model.procesamiento;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lo que la persona dejó después de revisar la lectura: adónde guardar, la estructura final y las filas. Los valores
 * llegan ya tipados (número, sí/no, texto) y se controlan contra los campos al guardar.
 *
 * @param proyectoId     proyecto existente donde crear el bloque; null para crear uno nuevo
 * @param bloqueId       bloque existente al que sumar las filas; null para crear un bloque nuevo
 * @param nombreProyecto nombre del proyecto nuevo (cuando no hay proyecto ni bloque existente)
 * @param nombreBloque   nombre del bloque nuevo (cuando no hay bloque existente)
 */
public record ConfirmacionLectura(
        ProyectoId proyectoId,
        String nombreProyecto,
        BloqueId bloqueId,
        String nombreBloque,
        List<CampoConfirmado> campos,
        List<FilaConfirmada> filas
) {

    public static final int MAX_FILAS = 500;

    /**
     * @param clave   identifica la columna dentro de la confirmación; las filas indexan sus valores por ella
     * @param campoId campo existente del bloque destino; null si la columna es nueva
     */
    public record CampoConfirmado(String clave, CampoId campoId, String nombre, String tipo, List<String> opciones) {
    }

    /**
     * @param pagina  página del cuaderno de la que salió (1 = la primera foto); null si se agregó a mano
     * @param valores valores por clave de columna
     */
    public record FilaConfirmada(Integer pagina, Map<String, Object> valores) {
    }

    /**
     * Reglas que no necesitan la base: hay destino, hay columnas, las claves no se repiten, dos columnas no van al
     * mismo campo del bloque y no son demasiadas filas.
     *
     * @throws BusinessRuleException con un mensaje que explica qué falta
     */
    public void validar() {
        validarDestino();
        validarColumnas();
        if (filas.size() > MAX_FILAS) {
            throw new BusinessRuleException("Podés guardar hasta " + MAX_FILAS + " filas por vez");
        }
    }

    private void validarDestino() {
        if (bloqueId == null && esVacio(nombreBloque)) {
            throw new BusinessRuleException("Escribí un nombre para la tabla");
        }
        if (bloqueId == null && proyectoId == null && esVacio(nombreProyecto)) {
            throw new BusinessRuleException("Escribí un nombre para el proyecto");
        }
    }

    private void validarColumnas() {
        if (campos.isEmpty()) {
            throw new BusinessRuleException("Dejá al menos una columna");
        }
        Set<String> vistas = new HashSet<>();
        Set<CampoId> camposUsados = new HashSet<>();
        for (CampoConfirmado campo : campos) {
            if (esVacio(campo.clave()) || !vistas.add(campo.clave())) {
                throw new BusinessRuleException("Las columnas necesitan una clave distinta cada una");
            }
            if (campo.campoId() != null && !camposUsados.add(campo.campoId())) {
                throw new BusinessRuleException("Dos columnas no pueden guardarse en el mismo campo de la tabla");
            }
            if (TipoCampo.RELACION.name().equals(campo.tipo()) && campo.campoId() == null) {
                throw new BusinessRuleException(
                        "Una conexión con otra tabla no se puede crear acá: elegí una columna de tu tabla");
            }
        }
    }

    private static boolean esVacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
