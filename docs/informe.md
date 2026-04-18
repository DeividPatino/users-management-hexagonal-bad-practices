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

**Evidencia**

```java
import com.jcaa.usersmanagement.application.service.UserValidationUtils;

public class CreateUserService implements CreateUserUseCase {
    public UserModel createUser(final CreateUserCommand command) {
        if (!UserValidationUtils.isValidEmail(command.email())) {
            throw new IllegalArgumentException("Invalid email format");
        }
        // ...
    }
}
```

**Solucion propuesta**

- Cambio recomendado: Eliminar la clase `UserValidationUtils` y mover toda la lógica de validación a los constructores de los Value Objects del dominio (`UserEmail`, `UserName`, etc.).
- Capa responsable del cambio: Dominio (Value Objects) y Aplicación (eliminar la dependencia).

**Estado**

- Resuelto

---