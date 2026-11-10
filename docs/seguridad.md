# Seguridad

## Autenticación

turnos-service no tiene login propio. Maneja tres tokens distintos:

- **JWT de usuario final:** lo emite catalogo-service en el login. turnos-service lo valida localmente (firma y vigencia) con el mismo `JWT_SECRET`, y toma el login del usuario del token.
- **JWT técnico de servicio:** turnos-service lo pide a catalogo-service con client credentials (`SERVICE_CLIENT_ID` y `SERVICE_CLIENT_SECRET`) para llamar a `/api/internal/**`. Se guarda en memoria y se renueva si catalogo-service lo rechaza.
- **JWT técnico de la cátedra:** lo emitió la cátedra para la cuenta de integración del proyecto, con vigencia de un año. Se guarda en `CATEDRA_JWT` y se envía en cada llamada a la API de la cátedra. Nunca llega a la app.

## Autorización

- Swagger (`/swagger-ui.html`, `/v3/api-docs/**`) y `/error`: públicos.
- El resto de los endpoints: requieren un JWT de usuario válido. Sin token, o con uno inválido o vencido, responden `401`.
- Cada usuario solo puede ver, consultar o cancelar sus propias reservas. El dueño de cada proceso es el login del JWT, nunca uno recibido en el body o por parámetro.
- Una reserva de otro usuario responde `404`, igual que una que no existe, para no revelar que existe.
- La cátedra devuelve las reservas de toda la cuenta del proyecto, así que turnos-service se queda solo con las que corresponden a procesos del usuario.

## Otras decisiones

- **CORS:** orígenes permitidos configurables por variable de entorno (`CORS_ALLOWED_ORIGINS`). La app Android no envía `Origin`, así que CORS no la afecta; solo aplica a clientes web en un navegador.
- **Secretos:** todos por variable de entorno, nunca hardcodeados ni versionados.
- **Validación de datos:** los bodies se validan al recibirlos (campos obligatorios, motivo de cancelación de hasta 500 caracteres) y el teléfono se valida antes de enviarlo a la cátedra.
- **Errores:** los errores conocidos responden solo con un `code`, sin mensajes internos ni datos de la cátedra.

## Limitaciones conocidas

- **Sin HTTPS:** el tráfico entre la app y los servicios no va cifrado en tránsito (ni la contraseña del login ni el JWT). Queda como mejora a futuro; es una limitación.
- **Secreto de usuario compartido entre servicios:** turnos-service necesita el mismo `JWT_SECRET` para validar JWT de usuario localmente. Esto significa que, en teoría, turnos-service podría generar un JWT válido para cualquier usuario. Mejora futura: usar un esquema de clave asimétrica (RS256) para los JWT de usuario, donde catalogo-service firma con la clave privada y turnos-service solo valida con la pública.
- **JWT de la cátedra de larga duración:** vale un año y se renueva a mano en el `.env`. Si se expone, hay que reemplazarlo.
- **Swagger público:** la documentación de la API se puede ver sin token, aunque los endpoints siguen exigiéndolo.
