/*
 * O cadastro de paletização, cuja coluna mais interessante é a que não é unique.
 *
 * code é enumerado por nome e pode se repetir: é o que permite vários tipos com código
 * OUTRO, cada um com seu multiplicador. A unicidade que sobrou é a do name, garantida no
 * banco pelo índice único em minúsculas e reforçada na edição pelo caso de uso.
 *
 * capacity_multiplier é int simples aqui. A regra de qual código aceita qual número é do
 * modelo PalletizationType, e a coluna aceita o que a tabela já continha antes de qualquer
 * validação de aplicação.
 */
package com.redekrill.scheduling.infrastructure.persistence.entity;

import com.redekrill.scheduling.domain.model.PalletizationCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "palletization_type")
@Getter
@Setter
@NoArgsConstructor
/**
 * Mapeamento JPA da tabela palletization_type.
 */
public class PalletizationTypeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    // V5 removeu a constraint única de code para permitir mais de um tipo OUTRO;
    // a unicidade restante é do name, garantida por uk_palletization_type_name_lower.
    @Column(nullable = false, length = 30)
    private PalletizationCode code;

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @Column(name = "capacity_multiplier", nullable = false)
    private int capacityMultiplier;
}
