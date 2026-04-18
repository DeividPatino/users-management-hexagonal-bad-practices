package com.jcaa.usersmanagement.infrastructure.adapter.email;

import com.jcaa.usersmanagement.application.port.out.EmailTemplatePort;
import com.jcaa.usersmanagement.domain.exception.EmailSenderException;
import com.jcaa.usersmanagement.infrastructure.config.AppProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class ClasspathEmailTemplateAdapter implements EmailTemplatePort {

  private static final String TEMPLATES_PATH_KEY = "templates.path";

  private final AppProperties properties;

  public ClasspathEmailTemplateAdapter(final AppProperties properties) {
    this.properties = properties;
  }

  @Override
  public String loadTemplate(final String templateName) {
    final String basePath = properties.get(TEMPLATES_PATH_KEY);
    final String path = buildPath(basePath, templateName);
    try (InputStream inputStream = openResourceStream(path)) {
      if (Objects.isNull(inputStream)) {
        throw EmailSenderException.becauseSendFailed(
            new IllegalStateException("Template not found: " + path));
      }
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (final IOException ioException) {
      throw EmailSenderException.becauseSendFailed(ioException);
    }
  }

  InputStream openResourceStream(final String path) {
    return getClass().getResourceAsStream(path);
  }

  private static String buildPath(final String basePath, final String templateName) {
    if (basePath.endsWith("/")) {
      return basePath + templateName;
    }
    return basePath + "/" + templateName;
  }
}
