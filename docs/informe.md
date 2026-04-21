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

final String correo = command.email();

final String correoElectronico = command.email();
```

**Solucion propuesta**

-   **Cambio recomendado**: Se estandarizó el nombre de la variable a `userEmail` en ambos métodos para mantener la consistencia semántica en toda la clase.
-   **Capa responsable del cambio**: `application`.

**Estado**

-   Resuelto

### Violación 4: Duplicación de Lógica y Código Sobre-compactado

**Ubicacion del codigo**

-   `src/main/java/com/jcaa/usersmanagement/application/service/EmailNotificationService.java`

**Resumen del problema**

-   Los métodos `notifyUserCreated` y `notifyUserUpdated` contenían lógica de orquestación casi idéntica (cargar plantilla, renderizar, construir destinatario, enviar). Además, esta lógica estaba comprimida en una única línea de código anidada, haciéndola extremadamente difícil de leer y depurar.

**Impacto**

-   **Violación del principio DRY (Don't Repeat Yourself)**: Cualquier cambio en el flujo de notificación debía ser replicado en ambos métodos, aumentando el riesgo de inconsistencias.
-   **Baja legibilidad y mantenibilidad**: El código era ininteligible a simple vista, ocultando la secuencia de pasos real. Depurar un fallo en la cadena de llamadas era muy complicado.
-   **Mezcla de niveles de abstracción**: Se combinaba la intención de alto nivel ("notificar") con detalles de bajo nivel (manipulación de strings, carga de archivos) en una sola expresión.

**Evidencia (Antes)**

```java
// En EmailNotificationService.java (antes del refactor)
public void notifyUserCreated(final UserModel user, final String plainPassword) {
    sendOrLog(buildDestination(user, SUBJECT_CREATED,
      renderTemplate(emailTemplatePort.loadTemplate("user-created.html"),
            Map.of(TOKEN_NAME, user.getName().value(), TOKEN_EMAIL, user.getEmail().value(),
                TOKEN_PASSWORD, plainPassword, TOKEN_ROLE, user.getRole().name()))));
}

public void notifyUserUpdated(final UserModel user) {
    sendOrLog(buildDestination(user, SUBJECT_UPDATED,
      renderTemplate(emailTemplatePort.loadTemplate("user-updated.html"),
            Map.of(TOKEN_NAME, user.getName().value(), TOKEN_EMAIL, user.getEmail().value(),
                TOKEN_ROLE, user.getRole().name(), TOKEN_STATUS, user.getStatus().name()))));
}
```

**Solucion propuesta**

-   **Cambio recomendado**: Se extrajo la lógica común a dos métodos privados: `baseTokens` (para crear el mapa de valores base) y `notifyUser` (para orquestar la carga, renderizado y envío). Los métodos públicos ahora solo preparan los datos específicos y llaman al orquestador, resultando en un código limpio, legible y sin duplicación.
-   **Capa responsable del cambio**: `application`.

**Estado**

-   Resuelto

### Violación 5: Uso de Parámetros Booleanos de Control (Flags)

**Ubicacion del codigo**

-   `src/main/java/com/jcaa/usersmanagement/application/service/EmailNotificationService.java`

**Resumen del problema**

-   El método `sendNotificationWithFlag` aceptaba un parámetro booleano (`includePassword`) que actuaba como un "interruptor" para cambiar completamente el comportamiento del método, decidiendo si se notificaba una creación o una actualización.

**Impacto**

-   **Baja legibilidad**: Una llamada como `sendNotificationWithFlag(user, true, password)` es ambigua. No es inmediatamente obvio lo que `true` significa en este contexto.
-   **Múltiples responsabilidades**: La existencia del flag indicaba que el método tenía dos responsabilidades distintas (notificar creación y notificar actualización), violando el Principio de Responsabilidad Única.
-   **Contrato opaco**: El booleano crea un contrato menos explícito. Es preferible tener métodos con nombres que describan claramente su única función.

**Evidencia (Antes)**

```java
// En EmailNotificationService.java (antes del refactor)
public void sendNotificationWithFlag(
    final UserModel user, final boolean includePassword, final String plainPassword) {
  if (includePassword) {
    notifyUserCreated(user, plainPassword);
  } else {
    notifyUserUpdated(user);
  }
}
```

**Solucion propuesta**

-   **Cambio recomendado**: Se eliminó por completo el método `sendNotificationWithFlag`. Esto obliga a los llamadores a utilizar los métodos existentes, `notifyUserCreated` y `notifyUserUpdated`, que son más explícitos y tienen una única responsabilidad.
-   **Capa responsable del cambio**: `application`.

**Estado**

-   Resuelto

---