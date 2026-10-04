# Sistema de Reservas de Laboratorios

Este proyecto implementa una API REST y una interfaz web MVC (Thymeleaf) para la gestión de reservas de laboratorios, utilizando Spring Boot y una arquitectura basada en capas.

## Arquitectura en capas

El proyecto sigue una estricta separación de responsabilidades:
- **Controladores (`controller/` y `web/`)**: Manejan las peticiones HTTP (REST o MVC) y delegan la lógica al servicio.
- **Servicio (`service/`)**: Contiene la lógica de negocio y las reglas de validación complejas (horarios, duraciones, validación de solapamientos). No depende del protocolo de comunicación web.
- **Repositorios (`repository/`)**: Encargados de la persistencia de datos (JPA/Hibernate) y de ejecutar consultas SQL/JPQL optimizadas.
- **Modelo (`model/`)**: Entidades JPA que representan la estructura de datos.
- **Excepciones (`exception/`)**: Clases personalizadas para errores de dominio (404 No Encontrado, 400 Inválido, 409 Conflicto).

### Estructura de paquetes

```text
com.universidad.reservaslabs
├── controller/         # Endpoints REST (@RestController)
├── exception/          # Excepciones de dominio (ej: ReservaConflictException)
├── model/              # Entidades JPA (Laboratorio, Reserva)
├── repository/         # Repositorios Spring Data (LaboratorioRepository, ReservaRepository)
├── service/            # Reglas de negocio (ReservaService)
└── web/                # Controladores MVC Thymeleaf (@Controller)
```

## Rutas y Endpoints

### API REST
- `GET /api/laboratorios`: Listar laboratorios.
- `GET /api/laboratorios/{id}`: Obtener laboratorio por ID.
- `POST /api/laboratorios`: Crear laboratorio.
- `GET /api/reservas`: Listar reservas.
- `GET /api/reservas/{id}`: Obtener reserva por ID.
- `GET /api/reservas/laboratorio/{id}`: Reservas por laboratorio.
- `POST /api/reservas`: Crear reserva.
- `DELETE /api/reservas/{id}`: Cancelar reserva.

### MVC (Thymeleaf)
- `GET /reservas`: Vista de lista de reservas.
- `GET /reservas/nueva`: Formulario de nueva reserva.
- `POST /reservas`: Procesar creación de reserva.
- `POST /reservas/{id}/cancelar`: Procesar cancelación de reserva.

## Cómo ejecutar

1. Asegúrate de tener JDK 17 instalado.
2. Compila el proyecto y ejecuta las pruebas:
   ```bash
   ./mvnw clean package
   ```
3. Inicia la aplicación:
   ```bash
   ./mvnw spring-boot:run
   ```
4. Accede a la interfaz web en tu navegador:
   - `http://localhost:8080/reservas`
5. Accede a la consola de la base de datos (H2) en: `http://localhost:8080/h2-console`
   - URL: `jdbc:h2:mem:reservas_labs_db`
   - Usuario: `sa`, sin contraseña.

## Decisiones de diseño

### Punto de decisión 1: Solapamiento de reservas (Repositorio vs. Servicio)
Para determinar si una nueva reserva choca con una existente, se decidió delegar la consulta de datos al **Repositorio** (usando JPQL) para obtener solo las reservas que efectivamente se solapan con el rango de tiempo solicitado. Si bien el **Controlador** podría haber inyectado y llamado directamente al repositorio, se encapsuló esta verificación dentro del método `crear` del **Servicio**. 

Si el Controlador llamara a `buscarSolapamientos()` directamente, se estaría saltando el paso de validación centralizado y sería más difícil asegurar que todas las reglas (horarios permitidos, duración) se cumplen consistentemente antes de crear la reserva. Además, la alternativa de traer todas las reservas del laboratorio a la memoria (`findAll()`) y filtrarlas usando Streams en Java se descartó porque escalaría muy mal a medida que crezca el número de reservas, consumiendo memoria y procesador de forma innecesaria cuando la base de datos está optimizada precisamente para este tipo de filtros (`r.inicio < :fin AND r.fin > :inicio`).

### Punto de decisión 2: Reglas de horario y duración en el Servicio
Las reglas relacionadas con que la duración de una reserva esté entre 30 minutos y 3 horas, y que el horario esté dentro del margen de atención (07:00 a 21:00), se implementaron puramente en Java dentro del **Servicio** y no en el **Repositorio**.
El criterio utilizado fue el siguiente: dado que estas reglas solo dependen de los datos de la propia reserva en memoria (su inicio y su fin) y no requieren consultar el estado del resto del sistema, es mucho más eficiente y lógico validarlas en Java mediante `Duration` y `LocalTime`. Intentar validar esto a nivel de base de datos complicaría innecesariamente el esquema.

**Nota técnica sobre LaboratorioController:**
El controlador `LaboratorioController` inyecta directamente el `LaboratorioRepository`. Esta es una excepción intencional a la arquitectura estricta: al tratarse de un CRUD simple sin reglas de negocio adicionales (solo guardar y consultar), crear un `LaboratorioService` anémico únicamente para pasar la llamada al repositorio introduciría complejidad innecesaria. El servicio de reserva sí fue creado porque concentra una lógica de negocio fundamental.

### Punto de decisión 3: Reutilización de ReservaService en Web y REST
Tanto `ReservaWebController` como `ReservaController` inyectan exactamente la misma clase `ReservaService`, la cual es un bean Singleton en Spring. 

- En `ReservaController`:
  ```java
  public ReservaController(ReservaService reservaService) { ... }
  ```
- En `ReservaWebController`:
  ```java
  public ReservaWebController(ReservaService reservaService, LaboratorioRepository laboratorioRepository) { ... }
  ```

Crear un segundo "ReservaWebService" o copiar la validación en el controlador web habría sido un grave error arquitectónico. Si las reglas de negocio (ej. la duración máxima de la reserva) cambiasen, obligaría a corregir las reglas en dos lugares distintos, violando el principio DRY (Don't Repeat Yourself) y aumentando el riesgo de que la API y la web funcionen con lógicas distintas.

### Punto de decisión 4: Manejadores de excepciones separados
Se implementaron dos manejadores distintos: `GlobalRestExceptionHandler` (para REST que retorna JSON) y `ReservaWebExceptionHandler` (para MVC que retorna una redirección con mensaje flash), en lugar de tener un único manejador complejo que inspeccione la cabecera `Accept`.

Ambos manejadores capturan exactamente las mismas excepciones de dominio (como `ReservaConflictException`), lo que provee un vocabulario común para los errores de negocio, pero la presentación es distinta. 

**Ejemplo de consistencia:**
Como se observa en `docs/rest-solapamiento-409.png` y `docs/web-solapamiento-error.png`, un intento de reserva solapada muestra exactamente el mismo mensaje de negocio: *"El laboratorio X ya tiene una reserva en ese horario"*, sin importar el canal por el que ingresó la petición.

### Conclusiones
La separación de responsabilidades a través de las capas demostró ser sumamente útil para adaptar la aplicación a múltiples interfaces (REST y Web). Fue difícil decidir la frontera entre lo que debe validar la base de datos (solapamientos) y lo que debe validar el servicio en memoria (horarios), pero el criterio de "dependencia del estado global vs local" resultó clave. Finalmente, mantener las excepciones de dominio agnósticas a la presentación permitió reaccionar de forma limpia y consistente tanto en JSON como en Thymeleaf.

## Notas Técnicas (Correcciones Obligatorias)
- Se creó la excepción `ReservaInvalidaException` (código HTTP 400) para las reglas de horario de atención, duración de reserva, y laboratorio faltante. `ReservaConflictException` (código HTTP 409) se dejó exclusivamente para solapamientos y cancelación tardía.
- Se agregó `@NotNull` a la relación con laboratorio.
- Las pruebas de ejemplo con Fechas utilizan dinámicamente fechas futuras, evitando los datos fijos caducos de 2026 de la guía original.
