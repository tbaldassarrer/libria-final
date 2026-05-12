# Libria

Acceso web: **https://libria-final.onrender.com**. En el plan gratuito puede tardar unos minutos en arrancar.

Para acceder a todo el contenido debes crear un usuario, indicar un email real y elegir una contrasena. La cuenta se activa desde el enlace enviado por email.

**Donde viven tus libros.**

Libria es una aplicacion web para organizar lecturas, descubrir libros y guardar resenas con una experiencia visual limpia y centrada en el lector.

## Funcionalidades

- Registro e inicio de sesion de usuarios.
- Activacion de cuenta por email.
- Busqueda de libros por titulo desde la base de datos.
- Integracion con Google Books para completar informacion de libros externos.
- Biblioteca personal de libros leidos.
- Lectura actual y proximas lecturas.
- Sistema de resenas y puntuaciones.
- Favoritos.
- Vista `Explora` organizada por generos.
- Diseno responsive para escritorio, portatil y movil.

## Tecnologias

- Java 17
- Spring Boot 3.4.0
- Spring Security
- Spring Data JPA
- MySQL
- Thymeleaf
- HTML, CSS y JavaScript
- Maven
- Google Books API

## Estructura

```text
src/
  main/
    java/es/prw/
      config/          Configuracion principal
      controllers/     Controladores web y endpoints
      models/          Entidades de dominio
      repositories/    Repositorios JPA
      security/        Seguridad y autenticacion
      services/        Integraciones y logica auxiliar
    resources/
      static/          CSS, JS e imagenes
      templates/       Vistas Thymeleaf
      application.properties
```

## Requisitos

- Java 17 o superior.
- MySQL 8.
- Maven Wrapper incluido en el proyecto.
- Base de datos `db_libria` creada localmente.
- Servidor SMTP para enviar los emails de activacion.

## Configuracion

El proyecto usa MySQL. Revisa `src/main/resources/application.properties` y ajusta estos valores si tu entorno es distinto:

```properties
server.port=8081
spring.datasource.url=jdbc:mysql://localhost:3306/db_libria?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=1234
```

Para enviar emails reales configura estas variables:

```properties
MAIL_HOST=smtp.tu-proveedor.com
MAIL_PORT=587
MAIL_USERNAME=tu-correo@dominio.com
MAIL_PASSWORD=tu-password-o-app-password
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
```

## Ejecucion

En Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

En macOS/Linux:

```bash
./mvnw spring-boot:run
```

Despues abre:

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

Proyecto academico en evolucion, con foco en una experiencia de lectura personal, sencilla y visualmente agradable.
