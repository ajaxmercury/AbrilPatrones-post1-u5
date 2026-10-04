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
└── ReservasLabsApplication.java
```
*(Nota: la carpeta `web/` se agrega en la Parte 2 para los controladores MVC)*

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
4. Accede a la consola de la base de datos (H2) en: `http://localhost:8080/h2-console`
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
