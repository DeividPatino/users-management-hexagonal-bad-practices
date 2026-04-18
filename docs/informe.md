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

---