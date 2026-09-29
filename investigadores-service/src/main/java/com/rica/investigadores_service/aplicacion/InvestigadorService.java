package com.rica.investigadores_service.aplicacion;

import java.util.List;

import org.springframework.stereotype.Service;

import com.rica.investigadores_service.RecursoNoEncontradoException;
import com.rica.investigadores_service.dominio.Investigador;

@Service
public class InvestigadorService implements InvestigadorUseCase {
    
    private final RepositorioInvestigadores investigadorRepository;
    private final InvestigadorFactory investigadorFactory;  

    public InvestigadorService(RepositorioInvestigadores investigadorRepository, InvestigadorFactory investigadorFactory) {
        this.investigadorRepository = investigadorRepository;
        this.investigadorFactory = investigadorFactory;
    }

    @Override
    public List<Investigador> listarTodos() {
        return investigadorRepository.listarTodos();
    }

    @Override
    public Investigador buscarPorId(Long id) {
        return investigadorRepository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un investigador con id " + id));
    }

    @Override
    public Investigador registrar(String nombreCompleto, String correoInstitucional, String grupoInvestigacion) {
        Investigador investigador = investigadorFactory.crear(nombreCompleto, correoInstitucional, grupoInvestigacion);
        return investigadorRepository.guardar(investigador);
    }
}
