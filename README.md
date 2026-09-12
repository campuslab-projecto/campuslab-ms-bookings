# ms-campuslab-bookings

Microservicio de reservas de CampusLab. Gestiona la creación, consulta y cambio de estado de reservas de laboratorios y equipos académicos.

## Tecnologías

- Java 17
- Spring Boot
- Spring Data JPA
- Spring Security OAuth2 Resource Server
- H2 para desarrollo local
- Driver Oracle incluido para configuración cloud

## Funcionalidades

- Crear reserva en estado `SOLICITADA`.
- Listar reservas.
- Consultar reserva por ID.
- Filtrar reservas por estado.
- Actualizar estado de reserva.
- Regla de negocio: no se puede pasar a `EN_USO` sin estar previamente `APROBADA`.

## Estados

- `SOLICITADA`
- `APROBADA`
- `EN_PREPARACION`
- `EN_USO`
- `DEVUELTA`
- `CANCELADA`

## Endpoints

- `POST /api/bookings`
- `GET /api/bookings`
- `GET /api/bookings/{id}`
- `PUT /api/bookings/{id}/status`

## Ejecutar localmente

```bash
mvn spring-boot:run
```

El microservicio queda disponible en:

```text
http://localhost:8081
```

## Base de datos

Actualmente usa H2 para desarrollo local. Para la entrega cloud se debe reemplazar la configuración `spring.datasource` por los datos de Oracle indicados por el docente o la plataforma cloud utilizada.
