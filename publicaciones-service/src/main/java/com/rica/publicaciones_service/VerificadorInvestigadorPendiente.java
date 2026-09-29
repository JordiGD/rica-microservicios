package com.rica.publicaciones_service;

import org.springframework.stereotype.Component;

@Component
public class VerificadorInvestigadorPendiente implements VerificadorInvestigador {

  @Override
  public boolean existe(String correoInstitucional) {
    return true;
  }
}
