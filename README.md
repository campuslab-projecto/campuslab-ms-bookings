## Ejecución local

```bash
mvn spring-boot:run
```

El microservicio queda disponible en:

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

## Base de datos

Actualmente el microservicio usa H2 para desarrollo local y ejecución en Docker.

La configuración queda preparada para una base de datos cloud mediante variables de entorno:

```text
DB_URL
DB_DRIVER
DB_USERNAME
DB_PASSWORD
DB_DIALECT
```

Para una entrega cloud, estas variables pueden reemplazarse por los datos de Oracle u otra base de datos indicada por el docente o la plataforma utilizada.

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