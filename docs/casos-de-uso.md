# Casos de uso

Actores: **Usuario**, **Cátedra**, **Sistema** (el propio servicio).

## CU-01: Consultar turnos disponibles

- **Actor:** Usuario
- **Precondición:** el usuario está autenticado.
- **Flujo principal:** el usuario pide los turnos de un profesional en una fecha → el sistema valida en catalogo-service que el profesional exista y esté habilitado → arma los turnos del día con sus horarios habilitados → quita los que ya empezaron → consulta a la cátedra los turnos ocupados y los quita.
- **Postcondición:** el usuario ve los turnos libres que puede reservar.
- **Excepción:** el profesional no existe → error 404. Está deshabilitado → error 400. catalogo-service o la cátedra no responden → error 503.

## CU-02: Iniciar una reserva

- **Actor:** Usuario
- **Precondición:** el usuario está autenticado y eligió un turno libre.
- **Flujo principal:** el usuario envía profesional, fecha y hora → el sistema valida que el turno no haya pasado, que el profesional esté habilitado y obtiene el nombre del usuario de catalogo-service → guarda el proceso → crea el hold en la cátedra → lo confirma con los datos del paciente → el proceso queda esperando el teléfono.
- **Postcondición:** el turno queda bloqueado en la cátedra y el usuario recibe el id del proceso.
- **Excepción:** el turno ya pasó o no existe → error 400. El turno ya está tomado → error 409 y el proceso queda fallido. La cátedra no responde al crear el hold → error 503 y el proceso queda fallido. La cátedra no responde al confirmar → el proceso queda en confirmación y se resuelve con el evento de la cátedra o con la recuperación (CU-13).

## CU-03: Recibir el pedido de teléfono

- **Actor:** Cátedra (evento `AdditionalInformationRequested` por Kafka)
- **Precondición:** el proceso existe y espera el pedido de teléfono.
- **Flujo principal:** llega el evento → si ya se procesó, se ignora → si no, el sistema guarda el id del pedido y el nuevo vencimiento → el proceso queda esperando el teléfono.
- **Postcondición:** el usuario ya puede enviar su teléfono.
- **Excepción:** el proceso no existe o su estado no lo permite → se ignora el evento.

## CU-04: Enviar el teléfono

- **Actor:** Usuario
- **Precondición:** la cátedra pidió el teléfono y el proceso no venció.
- **Flujo principal:** el usuario envía su teléfono → el sistema valida que el proceso sea suyo, el formato, el vencimiento y el estado → publica el teléfono en Kafka → el proceso queda a la espera del resultado.
- **Postcondición:** la cátedra recibe el teléfono y responde con el resultado (CU-05 a CU-08).
- **Excepción:** el proceso no existe o es de otro usuario → error 404. Formato inválido → error 400. Ya venció → error 409 y el proceso queda vencido. La cátedra todavía no pidió el teléfono → error 409, y se puede reintentar. No se pudo publicar en Kafka → error 503, sin cambiar el estado.

## CU-05: Recibir la confirmación de la reserva

- **Actor:** Cátedra (evento `AppointmentConfirmed` por Kafka)
- **Precondición:** el proceso existe y no está cerrado.
- **Flujo principal:** llega el evento → si ya se procesó, se ignora → si no, el sistema guarda el id de la reserva y el proceso queda confirmado.
- **Postcondición:** la reserva queda confirmada.
- **Excepción:** el evento llega duplicado, tardío o fuera de orden → se ignora.

## CU-06: Recibir el rechazo del teléfono

- **Actor:** Cátedra (evento `AdditionalInformationRejected` por Kafka)
- **Precondición:** el proceso existe y espera el resultado del teléfono.
- **Flujo principal:** llega el evento → si ya se procesó, se ignora → si no, el sistema guarda el motivo y el nuevo vencimiento → el proceso queda con el teléfono rechazado.
- **Postcondición:** el usuario puede volver a enviar el teléfono (CU-04) antes del vencimiento.
- **Excepción:** el evento llega duplicado, tardío o fuera de orden → se ignora.

## CU-07: Vencer un proceso

- **Actor:** Cátedra (evento `AppointmentProcessExpired` por Kafka) o Sistema
- **Precondición:** el proceso no está cerrado.
- **Flujo principal:** llega el evento de vencimiento → si ya se procesó, se ignora → si no, el proceso queda vencido con el motivo. Además, al consultar un proceso que ya pasó su vencimiento mientras se esperaba al usuario, el sistema lo vence localmente.
- **Postcondición:** el proceso queda cerrado y el turno vuelve a estar libre en la cátedra.
- **Excepción:** el evento llega duplicado, tardío o fuera de orden → se ignora.

## CU-08: Recibir un proceso inválido

- **Actor:** Cátedra (evento `AppointmentProcessInvalid` por Kafka)
- **Precondición:** el proceso no está cerrado.
- **Flujo principal:** llega el evento → si ya se procesó, se ignora → si no, el proceso queda inválido con el motivo.
- **Postcondición:** el proceso queda cerrado.
- **Excepción:** el evento llega duplicado, tardío o fuera de orden → se ignora.

## CU-09: Consultar el estado de una reserva

- **Actor:** Usuario
- **Precondición:** el usuario está autenticado.
- **Flujo principal:** el usuario pide un proceso por id → el sistema valida que sea suyo → lo vence si corresponde → si venció sin conocerse el resultado, consulta a la cátedra y actualiza el estado → devuelve el estado local.
- **Postcondición:** el usuario ve en qué estado está su reserva.
- **Excepción:** el proceso no existe o es de otro usuario → error 404, sin revelar cuál de los dos. La cátedra no responde → se devuelve el estado local.

## CU-10: Listar mis reservas

- **Actor:** Usuario
- **Precondición:** el usuario está autenticado.
- **Flujo principal:** el usuario pide sus reservas → el sistema vence los procesos atrasados → pide a la cátedra las reservas del usuario → se queda solo con las que corresponden a procesos propios → las combina con los datos locales.
- **Postcondición:** el usuario ve sus reservas, nunca las de otro usuario.
- **Excepción:** la cátedra no responde → se devuelven los procesos locales con su estado local.

## CU-11: Cancelar una reserva

- **Actor:** Usuario
- **Precondición:** la reserva es del usuario y está confirmada.
- **Flujo principal:** el usuario pide cancelar, con un motivo opcional → el sistema valida que sea suya y que esté confirmada → la cancela en la cátedra → el proceso queda cancelado.
- **Postcondición:** la reserva queda cancelada y el turno vuelve a estar libre.
- **Excepción:** el proceso no existe o es de otro usuario → error 404. No está confirmada → error 409. Ya estaba cancelada → se devuelve como está. La cátedra la rechaza → error 409. La cátedra no responde → error 503, sin cambiar el estado.

## CU-12: Recibir una cancelación

- **Actor:** Cátedra (evento `AppointmentCancelled` por Kafka)
- **Precondición:** la reserva está confirmada.
- **Flujo principal:** llega el evento → si ya se procesó, se ignora → si no, el proceso queda cancelado.
- **Postcondición:** la reserva queda cancelada aunque la cancelación no haya pasado por este servicio.
- **Excepción:** la reserva ya estaba cancelada, o el evento llega tardío o fuera de orden → se ignora.

## CU-13: Recuperar los procesos abiertos

- **Actor:** Sistema (al arrancar)
- **Precondición:** hay procesos que quedaron abiertos.
- **Flujo principal:** el sistema busca los procesos que todavía pueden cambiar → pide a la cátedra las reservas de sus dueños → lleva cada proceso al estado que informa la cátedra, solo si la transición es válida → vence los que la cátedra no tiene y ya pasaron su vencimiento.
- **Postcondición:** los procesos locales quedan alineados con la cátedra, aunque se hayan perdido eventos mientras el servicio estaba apagado.
- **Excepción:** la cátedra no responde → se reintenta hasta 3 veces y después se deja como está. Cualquier error inesperado → se loguea y el servicio arranca igual.
