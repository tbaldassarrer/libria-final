package es.prw.repositories;

import es.prw.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    // Busca un usuario por su nombre de usuario
    User findByNombreUsuario(String nombreUsuario);

    // Busca un usuario por su email
    User findByEmail(String email);
}
