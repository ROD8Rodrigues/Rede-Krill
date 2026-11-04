/*
 * Usuários no banco, com a normalização de e-mail resolvida em SQL.
 *
 * As três buscas por e-mail usam a variante IgnoreCase do Spring Data. Isso importa
 * porque o login normaliza o texto para minúsculas antes de consultar, mas a coluna
 * guarda o que foi digitado: conferir sem diferenciar maiúsculas é o que faz o cadastro
 * e a autenticação baterem com a mesma conta.
 *
 * findAll ordena por nome, então a tela de administração vem em ordem alfabética sem que
 * o caso de uso precise saber de Sort. save devolve o modelo com o id gerado, e a
 * optimistic locking fica a cargo da coluna version, que viaja no mapper.
 */
package com.redekrill.scheduling.infrastructure.persistence.adapter;

import com.redekrill.scheduling.domain.model.PageResult;
import com.redekrill.scheduling.domain.model.User;
import com.redekrill.scheduling.domain.repository.UserRepository;
import com.redekrill.scheduling.infrastructure.persistence.entity.UserJpaEntity;
import com.redekrill.scheduling.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.redekrill.scheduling.infrastructure.persistence.repository.UserJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
/**
 * Implementação de UserRepository sobre Spring Data JPA.
 */
public class JpaUserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository repository;

    public JpaUserRepositoryAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<User> findById(Long id) {
        return repository.findById(id).map(UserPersistenceMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmailIgnoreCase(email).map(UserPersistenceMapper::toDomain);
    }

    @Override
    public PageResult<User> findAll(int page, int size) {
        Page<UserJpaEntity> result = repository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name")));

        return PageResult.of(
                result.getContent().stream().map(UserPersistenceMapper::toDomain).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Override
    public User save(User user) {
        UserJpaEntity saved = repository.save(UserPersistenceMapper.toEntity(user));
        return user.withId(saved.getId());
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmailIgnoreCase(email);
    }

    @Override
    public boolean existsByEmailAndIdNot(String email, Long id) {
        return repository.existsByEmailIgnoreCaseAndIdNot(email, id);
    }
}
