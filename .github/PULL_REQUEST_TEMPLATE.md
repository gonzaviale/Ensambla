## 🤔 ¿Por qué pido revisión?
_Describe brevemente qué dudas o áreas específicas te gustaría que el revisor vea._

- [ ] Duda sobre:
- [ ] Cambios extensos / complejos / críticos
- [ ] Otras: _especificar_

## ✅ ¿Qué hace este PR?
_Resumen de lo que hace la tarea y el propósito de los cambios._

## 📸 Screenshots / Evidencia (si aplica)
_Agrega capturas o enlaces a pruebas del resultado (opcional)._

## 🧪 Cómo probarlo
_Pasos claros para probar el cambio localmente._

## 🧩 Tareas relacionadas
- Tarea del backlog (ej. `T-017`) y su historia (ej. `H-04`)

## 📋 Checklist
- [ ] El título del PR sigue Conventional Commits (`tipo(scope): descripcion`)
- [ ] Los tests pasan (`./mvnw verify` / `npm test`)
- [ ] Toda consulta de datos de un negocio filtra por el `negocio_id` de la sesión
- [ ] No edité migraciones Flyway ya aplicadas
- [ ] Código semántico: métodos cortos que se leen como una frase, sin comentarios redundantes y **sin `switch`/`if/else if` por tipo o estado** (polimorfismo); tests dado/cuando/entonces (skills `codigo-semantico` y `codigo-frontend`)
- [ ] Los métodos públicos no llaman a repositorios, puertos ni servicios (van a privados con nombre de intención) y el dominio no conoce formatos de persistencia ni de transporte (`"servidor:"`, JSON, `Map<?, ?>`)
- [ ] Actualicé la documentación (`docs/modelo-datos.md`, README) si corresponde

## 📎 Notas adicionales
_Comentarios, decisiones tomadas, puntos de discusión, etc._
