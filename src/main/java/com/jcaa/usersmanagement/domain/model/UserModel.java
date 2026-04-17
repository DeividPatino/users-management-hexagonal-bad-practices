package com.jcaa.usersmanagement.domain.model;

import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.enums.UserStatus;
import com.jcaa.usersmanagement.domain.valueobject.UserEmail;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import com.jcaa.usersmanagement.domain.valueobject.UserName;
import com.jcaa.usersmanagement.domain.valueobject.UserPassword;
import lombok.AllArgsConstructor;
import lombok.Data;

// Clean Code - Regla 15 (inmutabilidad como preferencia de diseño):
// Se cambió @Value por @Data + @AllArgsConstructor, lo que expone setters públicos
// para todos los campos. Un modelo de dominio debe ser inmutable: los setters permiten
// que cualquier clase modifique el estado del objeto sin pasar por invariantes ni
// reglas de negocio.
// Con @Value todos los campos serían final y no habría setters.
// Con @Data + @AllArgsConstructor cualquiera puede hacer userModel.setStatus(BLOCKED)
// desde fuera del dominio, rompiendo el encapsulamiento.
@Data
@AllArgsConstructor
public class UserModel {

  UserId id;
  UserName name;
  UserEmail email;
  UserPassword password;
  UserRole role;
  UserStatus status;

  public static UserModel create(
      final UserId id,
      final UserName name,
      final UserEmail email,
      final UserPassword password,
      final UserRole role) {
    return new UserModel(id, name, email, password, role, UserStatus.PENDING);
  }

  public UserModel activate() {
    return new UserModel(id, name, email, password, role, UserStatus.ACTIVE);
  }

  public UserModel deactivate() {
    return new UserModel(id, name, email, password, role, UserStatus.INACTIVE);
  }

}
