/*
 * Usuário entre tabela e modelo, com as duas colunas que não podem escapar.
 *
 * passwordHash atravessa porque o modelo o recebe como dado — ele não cifra nem
 * compara, só carrega. O campo não aparece em UserResult, então este mapper é o único
 * lugar onde a credencial cifrada circula entre camadas, sempre dentro de
 * infraestrutura.
 *
 * version vai nas duas direções porque a entidade carrega @Version: a leitura traz o
 * número que a escrita devolve, e é assim que duas edições concorrentes da mesma conta se
 * denunciam em vez de virar sobrescrita silenciosa.
 */
package com.redekrill.scheduling.infrastructure.persistence.mapper;

import com.redekrill.scheduling.domain.model.User;
import com.redekrill.scheduling.infrastructure.persistence.entity.UserJpaEntity;

/**
 * Conversão entre UserJpaEntity e o modelo User.
 */
public final class UserPersistenceMapper {

    private UserPersistenceMapper() {
    }

    public static User toDomain(UserJpaEntity entity) {
        return new User(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getRole(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.isMustChangePassword(),
                entity.getVersion()
        );
    }

    public static UserJpaEntity toEntity(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.id());
        entity.setName(user.name());
        entity.setEmail(user.email());
        entity.setPasswordHash(user.passwordHash());
        entity.setRole(user.role());
        entity.setActive(user.active());
        entity.setMustChangePassword(user.mustChangePassword());
        entity.setVersion(user.version());
        entity.setCreatedAt(user.createdAt());
        entity.setUpdatedAt(user.updatedAt());
        return entity;
    }
}
