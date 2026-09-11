# campuslab-ms-bookings

Microservicio de reservas del sistema CampusLab, desarrollado con Spring Boot.

## Descripción

Este microservicio administra las reservas de laboratorios y equipos académicos.  
Permite crear reservas, listarlas, consultar por ID y actualizar el estado de una reserva.

Según el caso CampusLab, el módulo de gestión de reservas debe manejar el flujo:

```text
SOLICITADA → APROBADA → EN_PREPARACIÓN → EN_USO → DEVUELTA / CANCELADA
```

Además, una regla clave es que no se puede pasar una reserva a `EN_USO` sin haber sido aprobada previamente.

## Tecnologías utilizadas

- Java
- Spring Boot
- Spring Web
- Spring Data JPA
- H2 Database
- Oracle JDBC Driver
- Docker
- Docker Hub
- GitHub

## Responsabilidad del servicio

- Crear reservas.
- Listar reservas existentes.
- Buscar reservas por ID.
- Actualizar estado de una reserva.
- Mantener la lógica base del flujo de estados.
- Dejar preparada la conexión a base de datos cloud mediante variables de entorno.

## Endpoints

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/api/bookings` | Crea una reserva |
| GET | `/api/bookings` | Lista reservas |
| GET | `/api/bookings/{id}` | Obtiene una reserva por ID |
| PUT | `/api/bookings/{id}/status` | Actualiza el estado de una reserva |

## Ejemplo de creación de reserva

```json
{
  "userEmail": "estudiante@duocuc.cl",
  "resourceId": "LAB-REDES-01",
  "status": "SOLICITADA"
}
```

## Estados disponibles

```text
SOLICITADA
APROBADA
EN_PREPARACIÓN
EN_USO
DEVUELTA
CANCELADA
```

## Variables de entorno

| Variable | Descripción | Valor local |
|---|---|---|
| `DB_URL` | URL de conexión a la base de datos | `jdbc:h2:mem:testdb` |
| `DB_DRIVER` | Driver JDBC | `org.h2.Driver` |
| `DB_USERNAME` | Usuario de base de datos | `sa` |
| `DB_PASSWORD` | Contraseña de base de datos | `password` |
| `DB_DIALECT` | Dialecto Hibernate | `org.hibernate.dialect.H2Dialect` |

## Configuración local

Por defecto, el microservicio usa H2 en memoria:

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:h2:mem:testdb}
    driver-class-name: ${DB_DRIVER:org.h2.Driver}
    username: ${DB_USERNAME:sa}
    password: ${DB_PASSWORD:password}
```

Esto permite ejecutar el servicio localmente sin depender de una base externa.

## Ejecución local

```bash
mvn spring-boot:run
```

Puerto local:

```text
http://localhost:8081
```

## Ejecución con Docker

Construir imagen:

```bash
docker build -t campuslab-ms-bookings .
```

Ejecutar contenedor:

```bash
docker run --name campuslab-ms-bookings -p 8081:8081 campuslab-ms-bookings
```

## Imagen Docker Hub

```text
lukmezac/campuslab-ms-bookings:latest
```

Para descargar la imagen:

```bash
docker pull lukmezac/campuslab-ms-bookings:latest
```

## Verificación

```bash
curl http://localhost:8081/api/bookings
```

Respuesta esperada inicial:

```json
[]
```

## Relación con el BFF

Este microservicio no se expone directamente al frontend.  
El flujo esperado es:

```text
Angular Frontend → campuslab-bff → campuslab-ms-bookings
```

El BFF es responsable de validar el JWT antes de derivar la solicitud hacia este microservicio.

## Gestión del proyecto

Este repositorio se gestiona mediante GitHub Projects y metodología Kanban.

Flujo utilizado:

```text
Issue → Rama feature → Commit → Pull Request → Revisión → Merge a main
```

La rama `main` se mantiene protegida y los cambios se integran mediante Pull Request.
