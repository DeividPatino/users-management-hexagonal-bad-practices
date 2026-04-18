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

**Violacion detectada 3: Acoplamiento de la capa de aplicación con configuración de infraestructura**

- Ubicacion del codigo: `src/main/java/com/jcaa/usersmanagement/application/service/EmailNotificationService.java`
- Resumen del problema: El servicio de aplicación `EmailNotificationService` dependía directamente de `AppProperties` (infraestructura) para obtener la ruta de las plantillas de correo.
- Impacto: Acopla la capa de aplicación a detalles de configuración de la infraestructura, violando la inversión de dependencias y dificultando el cambio de la fuente de configuración.

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

// En EmailNotificationService.java
import com.jcaa.usersmanagement.infrastructure.config.AppProperties;

public final class EmailNotificationService {
  private final AppProperties appProperties; // Inyección de infraestructura

  private String buildEmailBody(...) {
    final String templatePath = appProperties.get("templates.path") + templateName;
    // ...
  }
}
```

**Solucion propuesta**

- Cambio recomendado: Eliminar la clase `UserValidationUtils` y mover toda la lógica de validación a los constructores de los Value Objects del dominio (`UserEmail`, `UserName`, etc.).
- Cambio recomendado: Crear una interfaz `EmailTemplatePort` en la capa de aplicación. Crear un `ClasspathEmailTemplateAdapter` en la infraestructura que implemente el puerto y use `AppProperties` internamente. El servicio ahora depende del puerto, invirtiendo la dependencia.
- Capa responsable del cambio: Dominio (Value Objects) y Aplicación (eliminar la dependencia).

**Estado**

- Resuelto

---