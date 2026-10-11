package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.port.ArchivoCuadernoPort;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class ArchivoCuadernoLocalAdapter implements ArchivoCuadernoPort {

    private final Path raiz;
    private final int maxBytes;

    public ArchivoCuadernoLocalAdapter(Path raiz, int maxBytes) {
        this.raiz = raiz.toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
    }

    @Override
    public byte[] leer(UUID negocioId, String clave) {
        try {
            Path base = raiz.toRealPath().resolve(negocioId.toString());
            Path archivo = raiz.resolve(clave).toRealPath();
            if (!archivo.startsWith(base)) {
                throw new LecturaCuadernoException("La foto no pertenece al negocio del lote", null, false);
            }
            try (InputStream stream = Files.newInputStream(archivo)) {
                byte[] bytes = stream.readNBytes(maxBytes + 1);
                if (bytes.length == 0 || bytes.length > maxBytes) {
                    throw new LecturaCuadernoException("La foto está vacía o supera el tamaño permitido", null, false);
                }
                return bytes;
            }
        } catch (IOException e) {
            throw new LecturaCuadernoException("No se pudo acceder a la foto privada del lote", e, false);
        }
    }

    @Override
    public void guardar(UUID negocioId, String clave, byte[] contenido) {
        Path destino = aArchivoDelNegocio(negocioId, clave);
        try {
            Files.createDirectories(destino.getParent());
            Files.write(destino, contenido);
        } catch (IOException e) {
            throw new LecturaCuadernoException("No se pudo guardar la foto privada del lote", e, false);
        }
    }

    @Override
    public void eliminar(UUID negocioId, String clave) {
        try {
            Files.deleteIfExists(aArchivoDelNegocio(negocioId, clave));
        } catch (IOException e) {
            throw new LecturaCuadernoException("No se pudo eliminar la foto privada del lote", e, false);
        }
    }

    private Path aArchivoDelNegocio(UUID negocioId, String clave) {
        Path base = raiz.resolve(negocioId.toString()).normalize();
        Path archivo = raiz.resolve(clave).normalize();
        if (!archivo.startsWith(base)) {
            throw new LecturaCuadernoException("La foto no pertenece al negocio del lote", null, false);
        }
        return archivo;
    }
}
