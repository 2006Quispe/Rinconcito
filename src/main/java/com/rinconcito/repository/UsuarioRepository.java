package com.rinconcito.repository;

import com.rinconcito.entity.Usuario;
import com.rinconcito.entity.Usuario.EstadoUsuario;
import com.rinconcito.entity.Usuario.TipoRegistro;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository
        extends JpaRepository<Usuario, Integer> {

    /*
     * Busca el usuario por correo y carga
     * también su rol.
     */
    @EntityGraph(attributePaths = {
            "rol"
    })
    Optional<Usuario> findByCorreo(
            String correo
    );

    Optional<Usuario> findByDni(
            String dni
    );

    boolean existsByCorreo(
            String correo
    );

    boolean existsByCorreoIgnoreCase(
            String correo
    );

    boolean existsByCorreoIgnoreCaseAndIdUsuarioNot(
            String correo,
            Integer idUsuario
    );

    boolean existsByDni(
            String dni
    );

    boolean existsByDniAndIdUsuarioNot(
            String dni,
            Integer idUsuario
    );

    long countByEstado(
            EstadoUsuario estado
    );

    long countByTipoRegistro(
            TipoRegistro tipoRegistro
    );

    /*
     * Obtiene un usuario por su ID cargando
     * también el rol.
     */
    @EntityGraph(attributePaths = {
            "rol"
    })
    @Query("""
            SELECT usuario
            FROM Usuario usuario
            WHERE usuario.idUsuario = :idUsuario
            """)
    Optional<Usuario> findWithRolByIdUsuario(
            @Param("idUsuario")
            Integer idUsuario
    );

    /*
     * Consulta de usuarios utilizada por
     * el panel administrativo.
     */
    @EntityGraph(attributePaths = {
            "rol"
    })
    @Query("""
            SELECT usuario
            FROM Usuario usuario
            WHERE (
                :busqueda IS NULL
                OR LOWER(usuario.nombres)
                    LIKE LOWER(
                        CONCAT('%', :busqueda, '%')
                    )
                OR LOWER(usuario.apellidos)
                    LIKE LOWER(
                        CONCAT('%', :busqueda, '%')
                    )
                OR LOWER(
                    CONCAT(
                        usuario.nombres,
                        ' ',
                        usuario.apellidos
                    )
                )
                    LIKE LOWER(
                        CONCAT('%', :busqueda, '%')
                    )
                OR LOWER(usuario.correo)
                    LIKE LOWER(
                        CONCAT('%', :busqueda, '%')
                    )
                OR usuario.dni
                    LIKE CONCAT('%', :busqueda, '%')
                OR usuario.telefono
                    LIKE CONCAT('%', :busqueda, '%')
            )
            AND (
                :idRol IS NULL
                OR usuario.rol.idRol = :idRol
            )
            AND (
                :estado IS NULL
                OR usuario.estado = :estado
            )
            AND (
                :tipoRegistro IS NULL
                OR usuario.tipoRegistro = :tipoRegistro
            )
            ORDER BY usuario.fechaRegistro DESC,
                     usuario.idUsuario DESC
            """)
    List<Usuario> buscarParaAdministracion(
            @Param("busqueda")
            String busqueda,

            @Param("idRol")
            Integer idRol,

            @Param("estado")
            EstadoUsuario estado,

            @Param("tipoRegistro")
            TipoRegistro tipoRegistro
    );
}