/*
 * O tipo de caminhão, o cadastro sem regra nenhuma além do nome.
 *
 * description é a única coluna que aceita null, e é null mesmo quando o formulário envia
 * espaços: o modelo transforma vazio em ausência antes de chegar aqui. Os dois carimbos de
 * tempo existem porque a edição deste cadastro substitui o registro inteiro, e a
 * diferença entre "criado" e "retocado" é o que faz updatedAt valer a coluna.
 *
 * Sem unique declarado nesta classe: quem garante é o índice único uk_truck_type_name_lower,
 * criado na migration, e a checagem de duplicidade corre antes, no caso de uso.
 */
package com.redekrill.scheduling.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "truck_type")
@Getter
@Setter
@NoArgsConstructor
/**
 * Mapeamento JPA da tabela truck_type.
 */
public class TruckTypeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
