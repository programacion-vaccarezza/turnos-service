# turnos-service

Servicio de disponibilidad de turnos y reservas. Lleva adelante el proceso de reserva con el servicio central de la cátedra (REST y Kafka) y consulta a catalogo-service los datos de profesionales y usuarios.

## Requisitos

- Java 25
- Docker y Docker Compose
- Credenciales de la cuenta de integración con la cátedra (REST y Kafka)
- El repositorio de catalogo-service clonado en la misma carpeta que este, para levantar los dos servicios juntos

## Configuración

1. Copiar `.env.example` a `.env`.
2. Completar los valores: credenciales de la base, JWT, cuenta técnica para catalogo-service, CORS y los datos de la cátedra.

## Ejecutar el servicio

Con Docker:

```
docker compose -f compose.service.yaml up -d --build
```

Para levantarlo junto con catalogo-service:

```
docker compose up -d --build
```

Para bajarlo:

```
docker compose down
```

La API queda en `http://localhost:8081`, y Swagger en `http://localhost:8081/swagger-ui.html`.

## Ejecutar los tests

```
./mvnw test
```

## Documentación

Ver `docs/`: casos de uso, arquitectura, contratos, modelo de datos, integración con la cátedra, seguridad y pruebas.
