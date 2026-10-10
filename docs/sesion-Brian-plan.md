# Acceso y sesión — ENS-2

Plan aprobado el 10 de octubre de 2026. Alcance: ENS-33 a ENS-37 (T-005 a T-009).

- Registro: validar nombre, negocio, email y contraseña; normalizar email; BCrypt; usuario y negocio en una transacción. Responder 201 o 409 para email existente. Nunca devolver hashes.
- Acceso: login 200, credenciales inválidas 401, sesión JDBC por cookie HttpOnly, rotación al entrar, expiración configurable (30 minutos), logout 204 que invalida la sesión y /me 401 después.
- Seguridad: sesión para toda ruta privada, incluyendo escrituras; CSRF obligatorio para mutaciones. Mantener plantillas y documentación de desarrollo públicas.
- Interfaz: completar registro con nombre del negocio, iniciar sesión después de registrar, mostrar validación y errores, volver a la URL privada solicitada. Logout vuelve a /login y limpia datos privados.
- Calidad: tests por capa, integración con PostgreSQL real (rollback, cookie y logout), 100% de líneas y ramas por clase, controles de frontend y revisión en navegador.
- Entrega: commits separados en sesion-Brian y push a origin. ENS-8 queda fuera de este cambio.

