/*
 * Sessões de login no banco, incluindo as três operações em lote.
 *
 * revokeIfActive, revokeAllByUserId e deleteExpiredBefore são updates e deletes
 * coletivos expressos como consulta única, e por isso cada um carrega @Transactional
 * próprio: fora de um caso de uso que já abriu transação, é o adaptador quem garante o
 * contexto para a modificação em massa.
 *
 * revokeIfActive devolve boolean a partir da contagem de linhas afetadas, e essa
 * tradução é o coração da detecção de corrida na rotação de token: zero linhas
 * significou que outro fio chegou primeiro, e o chamador trata isso como reuso.
 *
 * As duas chaves de leitura são o id (para o filtro conferir o claim sid) e o hash do
 * refresh token (para a rotação achar a linha). A tabela não guarda o token cru, então
 * não existe caminho para "buscar por token".
 */
package com.redekrill.scheduling.infrastructure.persistence.adapter;

import com.redekrill.scheduling.domain.model.AuthSession;
import com.redekrill.scheduling.domain.repository.AuthSessionRepository;
import com.redekrill.scheduling.infrastructure.persistence.mapper.AuthSessionPersistenceMapper;
import com.redekrill.scheduling.infrastructure.persistence.repository.AuthSessionJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
/**
 * Implementação de AuthSessionRepository sobre Spring Data JPA.
 */
public class JpaAuthSessionRepositoryAdapter implements AuthSessionRepository {

    private final AuthSessionJpaRepository repository;

    public JpaAuthSessionRepositoryAdapter(AuthSessionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public AuthSession save(AuthSession session) {
        return AuthSessionPersistenceMapper.toDomain(
                repository.save(AuthSessionPersistenceMapper.toEntity(session)));
    }

    @Override
    public Optional<AuthSession> findById(Long id) {
        return repository.findById(id).map(AuthSessionPersistenceMapper::toDomain);
    }

    @Override
    public Optional<AuthSession> findByRefreshTokenHash(String refreshTokenHash) {
        return repository.findByRefreshTokenHash(refreshTokenHash)
                .map(AuthSessionPersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public void revokeAllByUserId(Long userId, Instant now) {
        repository.revokeAllByUserId(userId, now);
    }

    @Override
    @Transactional
    public boolean revokeIfActive(Long id, Instant now) {
        return repository.revokeIfActive(id, now) > 0;
    }

    @Override
    @Transactional
    public int deleteExpiredBefore(Instant instant) {
        return repository.deleteExpiredBefore(instant);
    }
}
