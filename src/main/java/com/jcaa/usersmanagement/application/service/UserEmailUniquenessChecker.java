package com.jcaa.usersmanagement.application.service;

import com.jcaa.usersmanagement.application.port.out.GetUserByEmailPort;
import com.jcaa.usersmanagement.domain.exception.UserAlreadyExistsException;
import com.jcaa.usersmanagement.domain.valueobject.UserEmail;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class UserEmailUniquenessChecker {

  private final GetUserByEmailPort getUserByEmailPort;

  public void ensureEmailNotExists(final UserEmail email) {
    if (getUserByEmailPort.getByEmail(email).isPresent()) {
      throw UserAlreadyExistsException.becauseEmailAlreadyExists(email.value());
    }
  }

  public void ensureEmailIsNotTakenByAnotherUser(final UserEmail newEmail, final UserId ownerId) {
    getUserByEmailPort
        .getByEmail(newEmail)
        .filter(existing -> !existing.getId().equals(ownerId))
        .ifPresent(existing -> {
          throw UserAlreadyExistsException.becauseEmailAlreadyExists(newEmail.value());
        });
  }
}
