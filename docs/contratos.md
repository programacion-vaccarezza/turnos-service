# Contratos entre los dos servicios

turnos-service consume estos endpoints de catalogo-service. Protegidos con un JWT técnico (`ROLE_SERVICE`), distinto del JWT de usuario final.

## Obtener el JWT técnico

```
POST /api/auth/service-token
Body:  { "clientId": "...", "clientSecret": "..." }
200:   { "token": "<JWT>" }
401:   client id o client secret incorrectos
```

turnos-service toma `clientId` y `clientSecret` del `.env` (`SERVICE_CLIENT_ID` y `SERVICE_CLIENT_SECRET`), guarda el token en memoria y lo reutiliza en cada pedido.

## Datos de usuario

```
GET /api/internal/users/{login}
Header: Authorization: Bearer <JWT técnico>

200: { "login": "...", "firstName": "...", "lastName": "..." }
403: el token no tiene ROLE_SERVICE
404: no existe un usuario con ese login
```

turnos-service lo usa al iniciar una reserva, para enviar a la cátedra el nombre y apellido del paciente. El login sale siempre del JWT del usuario. Un `404` se responde a la app como `404 PATIENT_NOT_FOUND`.

## Detalle de un profesional

```
GET /api/internal/professionals/{id}

200: { "id": 1, "categoryId": 1, "firstName": "...", "lastName": "...", "enabled": true }
403: el token no tiene ROLE_SERVICE
404: no existe un profesional con ese id
```

turnos-service lo usa antes de consultar la disponibilidad y antes de iniciar una reserva. Un `404` se responde a la app como `404 PROFESSIONAL_NOT_FOUND`, y un profesional con `enabled: false` como `400 PROFESSIONAL_DISABLED`.

## Horarios semanales de un profesional

```
GET /api/internal/professionals/{id}/schedules

200: [ { "id": 1, "dayOfWeek": "MONDAY", "startTime": "09:00:00", "endTime": "12:00:00", "slotDurationMinutes": 30, "enabled": true }, ... ]
403: el token no tiene ROLE_SERVICE
404: no existe un profesional con ese id
```

turnos-service lo usa para calcular la disponibilidad: se queda con los horarios habilitados y los divide en turnos según `slotDurationMinutes`.

## Notas

- Todos devuelven `401` si falta el token o es inválido.
- Si catalogo-service responde `401` o `403`, turnos-service descarta el token guardado, pide uno nuevo y reintenta una sola vez.
- Si catalogo-service no responde en 5 segundos o da otro error, turnos-service responde a la app `503 CATALOG_UNAVAILABLE`.
- turnos-service nunca accede a la base de catalogo-service: solo a estos endpoints.
