package com.rica.investigadores_service.aplicacion;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.rica.investigadores_service.aplicacion.puertos.RepositorioInvestigadores;
import com.rica.investigadores_service.dominio.CorreoDuplicadoException;
import com.rica.investigadores_service.dominio.EstadoInvestigador;
import com.rica.investigadores_service.dominio.Investigador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Taller 3 (Factory) y Taller 4 (puerto secundario): la Factory se prueba sin
 * Spring
 * ni base de datos, con un adaptador en memoria del puerto
 * RepositorioInvestigadores.
 */
class InvestigadorFactoryTest {

    /** Adaptador falso del puerto secundario: solo lo que la Factory necesita. */
    static class RepositorioEnMemoria implements RepositorioInvestigadores {
        private final List<Investigador> datos = new ArrayList<>();

        @Override
        public List<Investigador> listarTodos() {
            return datos;
        }

        @Override
        public Optional<Investigador> buscarPorId(Long id) {
            return datos.stream().filter(i -> id.equals(i.getId())).findFirst();
        }

        @Override
        public boolean existeCorreo(String correo) {
            return datos.stream().anyMatch(i -> i.getCorreoInstitucional().valor().equals(correo));
        }

        @Override
        public Investigador guardar(Investigador investigador) {
            datos.add(investigador);
            return investigador;
        }

        @Override
        public Optional<Investigador> buscarPorCorreo(String correo) {
            return datos.stream().filter(i -> i.getCorreoInstitucional().valor().equals(correo)).findFirst();
        }

        @Override
        public void eliminar(Investigador investigador) {
            datos.remove(investigador);
        }
    }

    private final RepositorioEnMemoria repositorio = new RepositorioEnMemoria();
    private final InvestigadorFactory factory = new InvestigadorFactory(repositorio);

    @Test
    void creaUnInvestigadorCompletoYActivo() {
        Investigador creado = factory.crear("Ana Torres", "ana.torres@uptc.edu.co", "GIT-UPTC");

        assertNull(creado.getId(), "el id lo asigna la base de datos al guardar");
        assertEquals("Ana Torres", creado.getNombreCompleto());
        assertEquals("ana.torres@uptc.edu.co", creado.getCorreoInstitucional().valor());
        assertEquals("GIT-UPTC", creado.getGrupoInvestigacion());
        assertEquals(EstadoInvestigador.ACTIVO, creado.getEstado());
    }

    @Test
    void rechazaUnCorreoQueNoEsInstitucional() {
        assertThrows(IllegalArgumentException.class,
                () -> factory.crear("Carlos Ruiz", "carlos@gmail.com", "GIT-UPTC"));
    }

    @Test
    void rechazaUnCorreoYaRegistrado() {
        repositorio.guardar(factory.crear("Ana Torres", "ana.torres@uptc.edu.co", "GIT-UPTC"));

        CorreoDuplicadoException error = assertThrows(CorreoDuplicadoException.class,
                () -> factory.crear("Otra Ana", "ana.torres@uptc.edu.co", "GIT-UPTC"));
        assertEquals("Ya existe un investigador registrado con el correo ana.torres@uptc.edu.co",
                error.getMessage());
    }

}