package com.jcaa.usersmanagement.application.service;

import com.jcaa.usersmanagement.application.port.out.EmailSenderPort;
import com.jcaa.usersmanagement.application.port.out.EmailTemplatePort;
import com.jcaa.usersmanagement.domain.exception.EmailSenderException;
import com.jcaa.usersmanagement.domain.model.EmailDestinationModel;
import com.jcaa.usersmanagement.domain.model.UserModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

@Log
@RequiredArgsConstructor
public final class EmailNotificationService {

  private static final String SUBJECT_CREATED = "Tu cuenta ha sido creada — Gestión de Usuarios";
  private static final String SUBJECT_UPDATED =
      "Tu cuenta ha sido actualizada — Gestión de Usuarios";

  private static final String TOKEN_NAME     = "name";
  private static final String TOKEN_EMAIL    = "email";
  private static final String TOKEN_PASSWORD = "password";
  private static final String TOKEN_ROLE     = "role";
  private static final String TOKEN_STATUS   = "status";

  private final EmailSenderPort emailSenderPort;
  private final EmailTemplatePort emailTemplatePort;

  public void notifyUserCreated(final UserModel user, final String plainPassword) {
    final Map<String, String> tokens = baseTokens(user);
    tokens.put(TOKEN_PASSWORD, plainPassword);
    notifyUser(user, SUBJECT_CREATED, "user-created.html", tokens);
  }

  public void notifyUserUpdated(final UserModel user) {
    final Map<String, String> tokens = baseTokens(user);
    tokens.put(TOKEN_STATUS, user.getStatus().name());
    notifyUser(user, SUBJECT_UPDATED, "user-updated.html", tokens);
  }

  private void notifyUser(
      final UserModel user,
      final String subject,
      final String templateName,
      final Map<String, String> tokens) {
    final String template = emailTemplatePort.loadTemplate(templateName);
    final String body = renderTemplate(template, tokens);
    final EmailDestinationModel destination = buildDestination(user, subject, body);
    sendEmailAndLogFailure(destination);
  }

  private Map<String, String> baseTokens(final UserModel user) {
    final Map<String, String> tokens = new HashMap<>();
    tokens.put(TOKEN_NAME, user.getName().value());
    tokens.put(TOKEN_EMAIL, user.getEmail().value());
    tokens.put(TOKEN_ROLE, user.getRole().name());
    return tokens;
  }

  private static EmailDestinationModel buildDestination(
      final UserModel user, final String subject, final String body) {
    return new EmailDestinationModel(
        user.getEmail().value(), user.getName().value(), subject, body);
  }

  // VIOLACIÓN Regla 4: método privado que no usa estado de instancia (no usa this ni campos)
  // pero NO está declarado como static. La regla dice: métodos privados sin estado deben ser static.
  private static String renderTemplate(final String template, final Map<String, String> values) {
    String result = template;
    for (final Map.Entry<String, String> tokenEntry : values.entrySet()) {
      final String token = "{{" + tokenEntry.getKey() + "}}";
      result = result.replace(token, tokenEntry.getValue());
    }
    return result;
  }

  // El nombre describe el comportamiento: envia el correo y registra un log en fallos.
  private void sendEmailAndLogFailure(final EmailDestinationModel destination) {
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
}
