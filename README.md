# Libria

**Donde viven tus libros.**

Libria es una aplicación web para organizar lecturas, descubrir libros y guardar reseñas con una experiencia visual suave, limpia y centrada en el lector.

![Java](https://img.shields.io/badge/Java-17-8A48CC?style=for-the-badge)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.0-8A48CC?style=for-the-badge)
![MySQL](https://img.shields.io/badge/MySQL-8-8A48CC?style=for-the-badge)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-Templates-8A48CC?style=for-the-badge)

## Vista General

Libria permite llevar el control de tus libros leídos, lecturas actuales, próximas lecturas, favoritos y reseñas personales. También incluye una sección de exploración por géneros, con libros seleccionados y enriquecidos con datos de Google Books cuando están disponibles.

La idea es sencilla: tener una biblioteca personal bonita, práctica y fácil de consultar.

## Funcionalidades

- Registro e inicio de sesión de usuarios.
- Búsqueda de libros por título desde la base de datos.
- Integración con Google Books para completar información de libros externos.
- Biblioteca personal de libros leídos.
- Lectura actual y próximas lecturas.
- Sistema de reseñas y puntuaciones.
- Favoritos.
- Vista `Explora` organizada por géneros.
- Diseño responsive para escritorio, portátil y móvil.

## Tecnologías

- **Java 17**
- **Spring Boot 3.4.0**
- **Spring Security**
- **Spring Data JPA**
- **MySQL**
- **Thymeleaf**
- **HTML, CSS y JavaScript**
- **Maven**
- **Google Books API**

## Estructura

```text
src/
  main/
    java/es/prw/
      config/          Configuración principal
      controllers/     Controladores web y endpoints
      models/          Entidades de dominio
      repositories/    Repositorios JPA
      security/        Seguridad y autenticación
      services/        Integraciones y lógica auxiliar
    resources/
      static/          CSS, JS e imágenes
      templates/       Vistas Thymeleaf
      application.properties
```

## Requisitos

- Java 17 o superior.
- MySQL 8.
- Maven Wrapper incluido en el proyecto.
- Base de datos `db_libria` creada localmente.

## Configuración

El proyecto usa MySQL. Revisa `src/main/resources/application.properties` y ajusta estos valores si tu entorno es distinto:

```properties
server.port=8081
spring.datasource.url=jdbc:mysql://localhost:3306/db_libria?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=1234
```

## Ejecución

En Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

En macOS/Linux:

```bash
./mvnw spring-boot:run
```

Después abre:

```text
http://localhost:8081
```

## Tests

```powershell
.\mvnw.cmd test
```

## Autoras

- [tbaldassarrer](https://github.com/tbaldassarrer)
- [YexeniaRosales](https://github.com/YexeniaRosales)

## Estado

Proyecto académico en evolución, con foco en una experiencia de lectura personal, sencilla y visualmente agradable.
