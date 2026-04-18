# Informe de Refactorización

Plantilla para documentar violaciones de arquitectura hexagonal.

## Regla 1 - Dependencias hacia el centro

**Descripcion de la regla**

Breve descripcion de la regla y su objetivo.

**Violacion detectada**

- Ubicacion del codigo: src/main/java/com/jcaa/usersmanagement/domain/model/UserModel.java
- Resumen del problema: el modelo de dominio importaba `UserEntity` y contenia `toEntity()`, acoplando el dominio con infraestructura.
- Impacto: rompe la direccion de dependencias, dificulta cambios en persistencia y reduce la pureza del dominio.

**Evidencia**

```java
import com.jcaa.usersmanagement.infrastructure.adapter.persistence.entity.UserEntity;

public UserEntity toEntity() {
	return new UserEntity(
			id.value(),
			name.value(),
			email.value(),
			password.value(),
			role.name(),
			status.name(),
			null,
			null);
}
```

**Solucion propuesta**

- Cambio recomendado: eliminar el import y el metodo `toEntity()`, mover el mapeo a un mapper/adapter de persistencia.
- Capa responsable del cambio: infraestructura (adapter de persistencia o mapper).

**Estado**

- Resuelto

---