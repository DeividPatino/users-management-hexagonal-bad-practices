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

**Violacion detectada 6: Nombre de Método Engañoso y Efectos Secundarios Ocultos**

-   Ubicacion del codigo: `src/main/java/com/jcaa/usersmanagement/application/service/EmailNotificationService.java`
-   Resumen del problema: El método privado `sendOrLog` tenía un nombre que no describía fielmente su comportamiento. Prometía "enviar o loguear", pero en realidad:
    1.  En caso de éxito, solo enviaba, no logueaba.
    2.  En caso de fallo, logueaba Y además re-lanzaba la excepción, un efecto secundario importante no comunicado en el nombre.
-   Impacto: Código engañoso. El nombre del método creaba una expectativa incorrecta en el lector sobre su funcionamiento. Violación del Principio de Menor Sorpresa. El comportamiento de re-lanzar la excepción era un efecto secundario no evidente a partir del nombre, lo que puede llevar a un manejo de errores incorrecto por parte de quien lo usa.

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

### Violación 6: Nombre de Método Engañoso y Efectos Secundarios Ocultos

**Ubicacion del codigo**

-   `src/main/java/com/jcaa/usersmanagement/application/service/EmailNotificationService.java`

**Resumen del problema**

-   El método privado `sendOrLog` tenía un nombre que no describía fielmente su comportamiento. Prometía "enviar o loguear", pero en realidad:
    1.  En caso de éxito, solo enviaba, no logueaba.
    2.  En caso de fallo, logueaba Y además re-lanzaba la excepción, un efecto secundario importante no comunicado en el nombre.

**Impacto**

-   **Código engañoso**: El nombre del método creaba una expectativa incorrecta en el lector sobre su funcionamiento.
-   **Violación del Principio de Menor Sorpresa**: El comportamiento de re-lanzar la excepción era un efecto secundario no evidente a partir del nombre, lo que puede llevar a un manejo de errores incorrecto por parte de quien lo usa.

**Evidencia (Antes)**

```java
// En EmailNotificationService.java (antes del refactor)
private void sendOrLog(final EmailDestinationModel destination) {
  try {
    emailSenderPort.send(destination);
  } catch (final EmailSenderException senderException) {
    log.log(
        Level.WARNING,
        "[EmailNotificationService] No se pudo enviar correo a: {0}. Causa: {1}",
        new Object[] {destination.getDestinationEmail(), senderException.getMessage()});
    throw senderException;
  }
}
```

**Solucion propuesta**

-   **Cambio recomendado**: Se renombró el método a `sendEmailAndLogFailure`. Este nombre es más largo pero mucho más preciso: describe claramente que la acción principal es enviar un email y que el log solo ocurre en caso de fallo.
-   **Capa responsable del cambio**: `application`.

**Estado**

-   Resuelto

### Violación 7: Mutabilidad en Modelos de Dominio

**Ubicacion del codigo**

-   `src/main/java/com/jcaa/usersmanagement/domain/model/UserModel.java`

**Resumen del problema**

-   La clase `UserModel`, el objeto central del dominio, estaba anotada con `@Data` de Lombok. Esta anotación genera `setters` públicos para todos los campos, convirtiendo al modelo en un objeto **mutable**.

**Impacto**

-   **Ruptura de la encapsulación**: Cualquier capa, incluida la de infraestructura, podía modificar el estado de un objeto `UserModel` directamente (ej. `user.setStatus(...)`), saltándose por completo las reglas y validaciones del dominio.
-   **Estado impredecible**: Al ser mutable, es muy difícil rastrear quién y cuándo se modifica el estado de un objeto, lo que conduce a errores complejos y comportamientos inesperados. Un modelo de dominio debe ser una representación consistente y protegida de su estado.

**Evidencia (Antes)**

```java
// En UserModel.java (antes del refactor)
@Data
@AllArgsConstructor
public class UserModel {
  UserId id;
  UserName name;
  // ... y el resto de campos no finales
}
```

**Solucion propuesta**

-   **Cambio recomendado**: Se reemplazó la anotación `@Data` por `@Value`. `@Value` es una anotación de Lombok que crea una clase inmutable: todos los campos se vuelven `private` y `final`, y no se generan `setters`. Esto garantiza que una vez que se crea un objeto `UserModel`, su estado no puede ser alterado.
-   **Capa responsable del cambio**: `domain`.

**Estado**

-   Resuelto

### Violación 8: Parámetro de control booleano (Boolean Flag)

**Ubicacion del codigo**

-   `src/main/java/com/jcaa/usersmanagement/application/service/UpdateUserService.java`

**Resumen del problema**

-   El método `notifyIfRequired(UserModel user, boolean notify)` utilizaba un parámetro booleano (`notify`) para decidir si debía enviar una notificación por correo o simplemente registrar un mensaje en el log.

**Impacto**

-   **Violación del Principio de Responsabilidad Única (SRP)**: El método tenía dos responsabilidades distintas (notificar y registrar silenciosamente) y el booleano actuaba como un interruptor para elegir entre ellas.
-   **Reduce la legibilidad**: Quien lee el código en el punto de llamada, como `notifyIfRequired(updatedUser, true)`, no sabe lo que `true` significa sin navegar a la definición del método. El código no se auto-documenta.
-   **Oculta efectos secundarios**: El nombre del método (`notifyIfRequired`) era engañoso. No dejaba claro que cuando el booleano es `false`, se produce un efecto secundario diferente (escribir en el log) en lugar de no hacer nada.

**Evidencia (Antes)**

```java
// En UpdateUserService.java (antes del refactor)
@Override
public UserModel execute(final UpdateUserCommand command) {
  // ... código de actualización ...

  // El 'true' es un "boolean flag" que controla el comportamiento
  notifyIfRequired(updatedUser, true);

  return updatedUser;
}

private void notifyIfRequired(final UserModel user, final boolean notify) {
  if (notify) {
    emailNotificationService.notifyUserUpdated(user);
  } else {
    // Efecto secundario oculto cuando notify = false
    log.info("Actualización silenciosa para usuario: " + user.getId().value());
  }
}
```

**Solucion propuesta**

-   **Cambio recomendado**: Se eliminó el método `notifyIfRequired` y se crearon dos métodos con nombres explícitos: `notifyUserUpdated` y `logSilentUpdate`. La llamada en el método `execute` se cambió a `notifyUserUpdated(updatedUser)`, haciendo la intención clara y directa.
-   **Capa responsable del cambio**: `application`.

**Estado**

-   Resuelto

### Violación 9: Condición Booleana Compleja, Ineficiente y Redundante

**Ubicacion del codigo**

-   `src/main/java/com/jcaa/usersmanagement/application/service/UpdateUserService.java`

**Resumen del problema**

-   El método `ensureEmailIsNotTakenByAnotherUser` contenía una única y extensa condición `if` para validar si un email ya estaba en uso por otro usuario.

**Impacto**

-   **Ilegibilidad**: La lógica era extremadamente difícil de seguir, mezclando múltiples `&&` y `||` sin una estructura clara.
-   **Ineficiencia grave**: La expresión `getUserByEmailPort.getByEmail(newEmail)` se ejecutaba hasta **cuatro veces** para una sola validación, resultando en múltiples e innecesarias llamadas a la base de datos.
-   **Lógica redundante y propensa a errores**: La condición contenía partes que parecían repetirse, aumentando el riesgo de errores lógicos y dificultando el mantenimiento.

**Evidencia (Antes)**

```java
// En UpdateUserService.java (antes del refactor)
private void ensureEmailIsNotTakenByAnotherUser(final UserEmail newEmail, final UserId ownerId) {
    if (getUserByEmailPort.getByEmail(newEmail).isPresent()
        && !getUserByEmailPort.getByEmail(newEmail).get().getId().equals(ownerId)
        && !getUserByEmailPort.getByEmail(newEmail).get().getEmail().value().equals(newEmail.value())
            || (getUserByEmailPort.getByEmail(newEmail).isPresent()
                && !getUserByEmailPort.getByEmail(newEmail).get().getId().value().equals(ownerId.value()))) {
      throw UserAlreadyExistsException.becauseEmailAlreadyExists(newEmail.value());
    }
}
```

**Solucion propuesta**

-   **Cambio recomendado**: Se refactorizó la lógica para realizar una **única consulta** a la base de datos. Se utiliza la API de `Optional` para encadenar la lógica de forma fluida y legible:
    1.  Se busca el usuario por email.
    2.  Se filtra el resultado para quedarse solo si el ID del usuario encontrado es **diferente** al del usuario que se está actualizando.
    3.  Si después del filtro todavía existe un usuario, se lanza la excepción.
-   **Capa responsable del cambio**: `application`.

**Evidencia (Después)**

```java
// En UpdateUserService.java (después del refactor)
private void ensureEmailIsNotTakenByAnotherUser(final UserEmail newEmail, final UserId ownerId) {
    getUserByEmailPort
        .getByEmail(newEmail)
        .filter(existing -> !existing.getId().equals(ownerId))
        .ifPresent(existing -> {
          throw UserAlreadyExistsException.becauseEmailAlreadyExists(newEmail.value());
        });
}
```

**Estado**

-   Resuelto

### Violación 10: Múltiples Responsabilidades, Inconsistencia y Acoplamiento en `Main`

**Ubicacion del codigo**

-   `src/main/java/com/jcaa/usersmanagement/Main.java`

**Resumen del problema**

-   La clase `Main` presentaba un cúmulo de malas prácticas de Clean Code:
    1.  **Múltiples Responsabilidades**: El método `main` se encargaba de construir el contenedor de dependencias, la I/O de la consola, instanciar la CLI y arrancar la aplicación, violando el Principio de Responsabilidad Única.
    2.  **Inconsistencia de Frameworks**: Utilizaba `org.slf4j.Logger` mientras que el resto del proyecto usaba `java.util.logging.Logger`, creando una inconsistencia innecesaria.
    3.  **Acoplamiento a Clases Concretas**: Estaba directamente acoplado a las implementaciones `DependencyContainer`, `UserManagementCli` y `ConsoleIO`, lo que hacía el punto de entrada de la aplicación rígido y difícil de modificar.

**Impacto**

-   **Baja cohesión y alta complejidad**: El método `main` era difícil de leer al mezclar la orquestación de alto nivel con la construcción de bajo nivel.
-   **Mantenimiento confuso**: La inconsistencia en el logging obligaba a los desarrolladores a preguntarse cuál era el estándar del proyecto.
-   **Rigidez en el diseño**: Cambiar una parte fundamental de la aplicación (como la interfaz de usuario) requería modificar el punto de entrada, lo que es una señal de un diseño poco flexible.

**Evidencia (Antes)**

```java
// En Main.java (antes del refactor)
public final class Main {
  private static final Logger log = LoggerFactory.getLogger(Main.class);

  public static void main(final String[] args) {
    log.info("Starting Users Management System...");
    final DependencyContainer container = new DependencyContainer();
    try (final Scanner scanner = new Scanner(System.in)) {
      new UserManagementCli(container.userController(), new ConsoleIO(scanner, System.out)).start();
    }
  }
}
```

**Solucion propuesta**

-   **Cambio recomendado**:
    1.  Se descompuso el método `main` en varios métodos privados con responsabilidades únicas: `buildContainer()`, `buildConsole()` y `runCli()`. El método `main` ahora solo orquesta estas llamadas.
    2.  Se cambió el logger a `java.util.logging.Logger` para mantener la consistencia con el resto del proyecto.
-   **Capa responsable del cambio**: `infrastructure` (punto de entrada).

**Estado**

-   Resuelto

### Violación 11: Violación del Principio de Separación de Comandos y Consultas (CQS)

**Ubicacion del codigo**

-   `src/main/java/com/jcaa/usersmanagement/application/service/UpdateUserService.java`
-   `src/main/java/com/jcaa/usersmanagement/application/port/in/UpdateUserUseCase.java`
-   `src/main/java/com/jcaa/usersmanagement/infrastructure/entrypoint/desktop/controller/UserController.java`

**Resumen del problema**

-   El método `execute` del caso de uso `UpdateUserUseCase` violaba el principio de **Separación de Comandos y Consultas (CQS)**. Este método realizaba una acción de escritura (modificar un usuario) y, al mismo tiempo, devolvía datos (el `UserModel` actualizado).

**Impacto**

-   **Efectos secundarios ocultos**: Un método que devuelve un valor da la impresión de ser una consulta segura (una "pregunta"). Sin embargo, en este caso, tenía el efecto secundario de modificar datos en la base de datos, lo que puede causar sorpresas y errores difíciles de rastrear.
-   **Baja predictibilidad**: El código se vuelve menos predecible. Un desarrollador que lee `UserModel user = service.execute(...)` podría no ser consciente de que esa línea está cambiando el estado del sistema.

**Evidencia (Antes)**

```java
// En UpdateUserUseCase.java (antes)
public interface UpdateUserUseCase {
  UserModel execute(@NotNull @Valid UpdateUserCommand command);
}

// En UpdateUserService.java (antes)
@Override
public UserModel execute(final UpdateUserCommand command) {
  // ... lógica de actualización ...
  final UserModel updatedUser = updateUserPort.update(userToUpdate);
  return updatedUser;
}

// En UserController.java (antes)
public UserResponse updateUser(final UpdateUserRequest request) {
  final var command = UserDesktopMapper.toUpdateCommand(request);
  final var user = updateUserUseCase.execute(command); // Llama y espera un retorno
  return UserDesktopMapper.toResponse(user);
}
```

**Solucion propuesta**

-   **Cambio recomendado**:
    1.  Se modificó la interfaz `UpdateUserUseCase` para que el método `execute` devuelva `void`, convirtiéndolo en un comando puro.
    2.  Se actualizó `UpdateUserService` para que ya no devuelva ningún valor.
    3.  Se ajustó el `UserController` para que primero ejecute el comando de actualización y luego, si necesita los datos actualizados para la respuesta, realice una consulta explícita por separado usando `getUserByIdUseCase`.
-   **Capa responsable del cambio**: `application` e `infrastructure`.

**Estado**

-   Resuelto

---