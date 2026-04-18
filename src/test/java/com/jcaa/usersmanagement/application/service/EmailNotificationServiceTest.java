package com.jcaa.usersmanagement.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.jcaa.usersmanagement.application.port.out.EmailSenderPort;
import com.jcaa.usersmanagement.application.port.out.EmailTemplatePort;
import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.enums.UserStatus;
import com.jcaa.usersmanagement.domain.exception.EmailSenderException;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.UserEmail;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import com.jcaa.usersmanagement.domain.valueobject.UserName;
import com.jcaa.usersmanagement.domain.valueobject.UserPassword;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// VIOLACIÓN Regla 11: se eliminó el javadoc de la clase que documentaba los casos cubiertos.
@DisplayName("EmailNotificationService")
@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {

  @Mock private EmailSenderPort emailSenderPort;
  @Mock private EmailTemplatePort emailTemplatePort;

  private EmailNotificationService service;

  private static final String EMAIL = "john@example.com";
  private static final String NAME = "John Arrieta";
  private static final String PASSWORD = "SecurePass1";
  private static final String TEMPLATE_CONTENT =
      "<html>{{name}} {{email}} {{password}} {{role}} {{status}}</html>";

  private UserModel user;

  @BeforeEach
  void setUp() {
    service = new EmailNotificationService(emailSenderPort, emailTemplatePort);

    user =
        new UserModel(
            new UserId("u-001"),
            new UserName(NAME),
            new UserEmail(EMAIL),
            UserPassword.fromPlainText(PASSWORD),
            UserRole.ADMIN,
            UserStatus.ACTIVE);
  }

  // ── notifyUserCreated() — flujo feliz

  // VIOLACIÓN Regla 11: falta @DisplayName en el método.
  @Test
  void shouldSendCreatedNotificationToCorrectEmail() {
    // VIOLACIÓN Regla 11: se eliminaron los comentarios Arrange–Act–Assert.
    stubTemplateOk();
    service.notifyUserCreated(user, PASSWORD);
    verify(emailSenderPort)
        .send(
            argThat(
                dest ->
                    EMAIL.equals(dest.getDestinationEmail())
                        && dest.getSubject().contains("creada")));
  }

  // ── notifyUserUpdated() — flujo feliz

  @Test
  @DisplayName("notifyUserUpdated() invoca el puerto con el email y asunto correctos")
  void shouldSendUpdatedNotificationToCorrectEmail() {
    // Act
    stubTemplateOk();
    service.notifyUserUpdated(user);

    // Assert
    verify(emailSenderPort)
        .send(
            argThat(
                dest ->
                    EMAIL.equals(dest.getDestinationEmail())
                        && dest.getSubject().contains("actualizada")));
  }

  // ── re-lanzar EmailSenderException en notifyUserCreated

  @Test
  @DisplayName("notifyUserCreated() re-lanza EmailSenderException cuando el puerto falla")
  void shouldRethrowEmailSenderExceptionOnCreate() {
    // Arrange
    stubTemplateOk();
    final EmailSenderException cause =
        EmailSenderException.becauseSmtpFailed(EMAIL, "Connection refused");
    doThrow(cause).when(emailSenderPort).send(any());

    // Act & Assert
    assertThrows(EmailSenderException.class, () -> service.notifyUserCreated(user, PASSWORD));
  }

  // ── re-lanzar EmailSenderException en notifyUserUpdated

  @Test
  @DisplayName("notifyUserUpdated() re-lanza EmailSenderException cuando el puerto falla")
  void shouldRethrowEmailSenderExceptionOnUpdate() {
    // Arrange
    stubTemplateOk();
    final EmailSenderException cause =
        EmailSenderException.becauseSmtpFailed(EMAIL, "Connection refused");
    doThrow(cause).when(emailSenderPort).send(any());

    // Act & Assert
    assertThrows(EmailSenderException.class, () -> service.notifyUserUpdated(user));
  }

  // ── loadTemplate() — rama: template no encontrado (is == null)

  @Test
  @DisplayName(
      "loadTemplate() lanza EmailSenderException cuando el template no existe")
  void shouldThrowWhenTemplateNotFound() {
    // Arrange
    doThrow(
            EmailSenderException.becauseSendFailed(
                new IllegalStateException("Template not found")))
        .when(emailTemplatePort)
        .loadTemplate(any());

    // Act & Assert
    assertThrows(EmailSenderException.class, () -> service.notifyUserCreated(user, PASSWORD));
  }

  // ── loadTemplate() — rama: IOException al leer el stream

  @Test
  @DisplayName(
      "loadTemplate() lanza EmailSenderException cuando ocurre un error al cargar")
  void shouldThrowWhenTemplateThrowsIOException() {
    // Arrange
    doThrow(EmailSenderException.becauseSendFailed(new RuntimeException("Disk error")))
        .when(emailTemplatePort)
        .loadTemplate(any());

    // Act & Assert
    assertThrows(EmailSenderException.class, () -> service.notifyUserCreated(user, PASSWORD));
  }

  // ── renderTemplate() — todos los tokens se sustituyen

  @Test
  @DisplayName("renderTemplate() sustituye todos los tokens del template correctamente")
  void shouldRenderAllTokensInTemplate() {
    // Act
    stubTemplateOk();
    service.notifyUserCreated(user, PASSWORD);

    // Assert — el body enviado contiene los valores interpolados
    verify(emailSenderPort)
        .send(argThat(dest -> dest.getBody().contains(NAME) && dest.getBody().contains(EMAIL)));
  }

  private void stubTemplateOk() {
    when(emailTemplatePort.loadTemplate(any())).thenReturn(TEMPLATE_CONTENT);
  }
}
