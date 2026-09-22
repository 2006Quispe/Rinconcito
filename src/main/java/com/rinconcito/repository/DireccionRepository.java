package com.rinconcito.repository;

import com.rinconcito.entity.Direccion;
import com.rinconcito.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DireccionRepository extends JpaRepository<Direccion, Integer> {

    List<Direccion> findByUsuario(Usuario usuario);

    Optional<Direccion> findByUsuarioAndPrincipalTrue(Usuario usuario);
}