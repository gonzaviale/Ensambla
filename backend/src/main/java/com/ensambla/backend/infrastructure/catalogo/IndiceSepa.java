package com.ensambla.backend.infrastructure.catalogo;

import com.ensambla.backend.domain.model.catalogo.CodigoBarras;
import com.ensambla.backend.domain.model.catalogo.ProductoCatalogo;
import org.springframework.core.io.Resource;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

public class IndiceSepa {

    private final Map<String, ProductoCatalogo> productos;

    public IndiceSepa(Resource recurso) {
        try (InputStream archivo = recurso.getInputStream();
                InputStream entrada = recurso.getFilename().endsWith(".gz") ? new GZIPInputStream(archivo) : archivo) {
            byte[] contenido = entrada.readNBytes(50_000_001);
            if (contenido.length > 50_000_000) {
                throw new IOException("El índice SEPA supera el tamaño admitido");
            }
            var raiz = JsonMapper.builder().build().readTree(contenido);
            var registros = raiz.path("productos");
            if (!registros.isObject() || registros.isEmpty()) {
                throw new IOException("El índice SEPA no contiene productos");
            }
            Map<String, ProductoCatalogo> cargados = new HashMap<>();
            for (var registro : registros.properties()) {
                var dato = registro.getValue();
                String codigo = dato.path("codigo").asString();
                if (!new CodigoBarras(codigo).identidad().equals(registro.getKey())
                        || dato.path("nombre").asString("").isBlank()) {
                    throw new IOException("El índice SEPA contiene un producto inválido");
                }
                cargados.put(registro.getKey(), new ProductoCatalogo(codigo, dato.path("nombre").asString(),
                        dato.path("marca").asString(""), dato.path("presentacion").asString(""), null));
            }
            productos = Map.copyOf(cargados);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("No se pudo cargar el índice SEPA configurado", exception);
        }
    }

    public Optional<ProductoCatalogo> buscar(CodigoBarras codigo) {
        return Optional.ofNullable(productos.get(codigo.identidad()));
    }
}
