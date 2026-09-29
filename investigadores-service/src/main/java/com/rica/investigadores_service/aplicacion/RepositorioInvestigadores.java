package com.rica.investigadores_service.aplicacion;

import java.util.List;
import java.util.Optional;

import com.rica.investigadores_service.dominio.Investigador;

public interface RepositorioInvestigadores {

  List<Investigador> listarTodos();

  Optional<Investigador> buscarPorId(Long id);

  boolean existeCorreo(String correoInstitucional);

  Investigador guardar(Investigador investigador);
}
