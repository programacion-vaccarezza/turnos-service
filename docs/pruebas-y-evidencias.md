# Pruebas

## Estrategia

Tests unitarios con JUnit 5 + Mockito + AssertJ: cada test mockea los ports/dependencias de la clase bajo prueba. Cubren:

- Máquina de estados: cada transición válida e inválida, los estados finales y la búsqueda de caminos entre estados.
- Inicio de una reserva: hold, confirm y cada rechazo o falta de respuesta de la cátedra.
- Envío del teléfono: dueño, formato, vencimiento, estado y falla al publicar en Kafka.
- Eventos de Kafka: cada handler, el despacho por tipo de evento, y los eventos duplicados, tardíos o fuera de orden.
- Idempotencia: el mismo `eventId` se procesa una sola vez.
- Consulta, listado y cancelación de reservas, incluido el respaldo local cuando la cátedra no responde.
- Vencimiento local y recuperación de los procesos abiertos al arrancar, con sus reintentos.
- Aislamiento entre usuarios: un usuario no ve ni cancela reservas de otro.
- Autorización: el filtro JWT, la validación del token y el uso del login del token en el controller.
- Clientes REST de catalogo-service y de la cátedra, incluida la renovación del token de servicio.
- Mappers (entidad ⇄ dominio, DTO ⇄ dominio), incluyendo los casos con `null`.

## Cómo ejecutarlas

Los tests no usan la base ni servicios externos, así que no hace falta levantar nada:

```
./mvnw test
```