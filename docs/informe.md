# Informe de Refactorización

Plantilla para documentar violaciones de arquitectura hexagonal.

## Regla 1 - Dependencias hacia el centro

**Descripcion de la regla**

Breve descripcion de la regla y su objetivo.

**Violacion detectada**

- Ubicacion del codigo: src/main/java/com/jcaa/usersmanagement/domain/model/UserModel.java
- Resumen del problema: el modelo de dominio importaba `UserEntity` y contenia `toEntity()`, acoplando el dominio con infraestructura.
- Impacto: rompe la direccion de dependencias, dificulta cambios en persistencia y reduce la pureza del dominio.

**Violacion detectada 2: Acoplamiento de la capa de aplicación con utilidades de validación**

- Ubicacion del codigo: `src/main/java/com/jcaa/usersmanagement/application/service/CreateUserService.java`
- Resumen del problema: El servicio de aplicación `CreateUserService` dependía de `UserValidationUtils` para validar reglas de negocio (ej. formato de email), en lugar de que el dominio se autovalidara.
- Impacto: Acopla la capa de aplicación a una implementación concreta, saca la lógica de negocio fuera del dominio y reduce la encapsulación.

**Violacion detectada 3: Acoplamiento del Entrypoint con la construcción de Commands de aplicación**

- Ubicacion del codigo: `src/main/java/com/jcaa/usersmanagement/infrastructure/entrypoint/desktop/controller/UserController.java`
- Resumen del problema: El `UserController` (infraestructura) creaba `Commands` de la capa de aplicación (`CreateUserCommand`, etc.) directamente con `new`, acoplando el entrypoint a los detalles de construcción de la capa de aplicación.
- Impacto: Acoplamiento fuerte entre capas. Si el constructor de un `Command` cambia, el controlador se rompe. La responsabilidad de mapeo estaba fugada en el controlador.

**Violacion detectada 4: Acoplamiento del Entrypoint con la construcción de Commands de aplicación**

-   Ubicacion del codigo: `src/main/java/com/jcaa/usersmanagement/infrastructure/entrypoint/desktop/controller/UserController.java`
-   Resumen del problema: El `UserController` (infraestructura) creaba `Commands` de la capa de aplicación (`CreateUserCommand`, etc.) directamente con `new`, acoplando el entrypoint a los detalles de construcción de la capa de aplicación.
-   Impacto: Acoplamiento fuerte entre capas. Si el constructor de un `Command` cambia, el controlador se rompe. La responsabilidad de mapeo estaba fugada en el controlador.

**Violacion detectada 5: Acoplamiento del Contenedor de Dependencias con la implementación del Repositorio**

-   Ubicacion del codigo: `src/main/java/com/jcaa/usersmanagement/infrastructure/config/DependencyContainer.java`
-   Resumen del problema: El `DependencyContainer` llamaba a un método `init()` que solo existía en la clase concreta `UserRepositoryMySQL`. Esto acopla el contenedor a una implementación específica, en lugar de depender solo de las interfaces (puertos).
-   Impacto: Impide la intercambiabilidad de los adaptadores (por ejemplo, usar un repositorio en memoria para tests) y crea una dependencia temporal frágil (el objeto debe ser inicializado antes de usarse).

**Evidencia**

```java
// En UserController.java
public UserResponse createUser(final CreateUserRequest request) {
    // ...
    final var command = new CreateUserCommand(
        request.id(), request.name(), request.email(), request.password(), request.role());
    final var user = createUserUseCase.execute(command);
    // ...
}
```

**Solucion propuesta**

-   Cambio recomendado: Delegar la creación de `Commands` a la clase `UserDesktopMapper` dentro de la misma capa de `entrypoint`. El controlador ahora invoca al mapper, que es el único que conoce los detalles de construcción.
-   Capa responsable del cambio: Infraestructura (entrypoint).

**Estado**

- Resuelto

## Regla 2

### Violación 2: Retorno de "Números Mágicos" y Códigos de Error

**Ubicacion del codigo**

-   `src/main/java/com/jcaa/usersmanagement/application/service/mapper/UserApplicationMapper.java`

**Resumen del problema**

-   El método `roleToCode` devolvía números enteros (`1`, `2`, `3`) para representar roles de usuario y un código de error (`-1`) para entradas inválidas. Este enfoque, conocido como "números mágicos", oculta la intención del código y crea un contrato frágil.

**Impacto**

-   **Baja legibilidad**: El código que llama a este método se llena de comparaciones como `if (codigo == 1)` o `if (codigo == -1)`, lo que hace que no sea claro a simple vista qué se está evaluando.
-   **Mantenimiento propenso a errores**: Si se añade un nuevo rol, es fácil olvidar actualizar todas las partes del código que dependen de estos números mágicos.
-   **Errores silenciosos**: Obliga a quien lo usa a recordar siempre comprobar el valor `-1`. Si se olvida, el error se propaga silenciosamente por el sistema, causando fallos en lugares inesperados.

**Evidencia (Antes)**

```java
// En UserApplicationMapper.java (antes del refactor)
public static int roleToCode(final String role) {
    if (Objects.isNull(role) || role.isBlank()) {
      return -1;
    }
    if ("ADMIN".equalsIgnoreCase(role)) {
      return 1;
    } else if ("MEMBER".equalsIgnoreCase(role)) {
      return 2;
    } else if ("REVIEWER".equalsIgnoreCase(role)) {
      return 3;
    }
    return -1;
}
```

**Solucion propuesta**

-   **Cambio recomendado**: Se modificó el método para que, en lugar de devolver un código de error, lance una `IllegalArgumentException` si el rol no es válido. Esto hace que el fallo sea explícito e inmediato, adhiriéndose al principio de "fail-fast".
-   **Capa responsable del cambio**: `application`.

**Estado**

-   Resuelto

### Violación 3: Inconsistencia Semántica en Nombres de Variables

**Ubicacion del codigo**

-   `src/main/java/com/jcaa/usersmanagement/application/service/mapper/UserApplicationMapper.java`

**Resumen del problema**

-   Dentro de la misma clase, se utilizaban dos nombres de variable diferentes (`correo` y `correoElectronico`) para representar el mismo concepto: el email de un usuario.

**Impacto**

-   **Confusión para el lector**: Obliga a quien lee el código a preguntarse si existe una diferencia sutil entre los dos nombres, aumentando la carga cognitiva.
-   **Falta de consistencia**: El código se vuelve menos predecible y más difícil de mantener. Clean Code aboga por usar un vocabulario único y preciso para los conceptos.

**Evidencia (Antes)**

```java
// En UserApplicationMapper.java (antes del refactor)

// En el método fromCreateCommandToModel:
final String correo = command.email();

// En el método fromUpdateCommandToModel:
final String correoElectronico = command.email();
```

**Solucion propuesta**

-   **Cambio recomendado**: Se estandarizó el nombre de la variable a `userEmail` en ambos métodos para mantener la consistencia semántica en toda la clase.
-   **Capa responsable del cambio**: `application`.

**Estado**

-   Resuelto

---