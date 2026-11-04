/*
 * O modelo de veículo que o fornecedor avisa que vai mandar.
 *
 * É cadastro livre: Carreta, Truck e VUC vêm semeados no banco, e o ADMIN pode
 * criar quantos quiser. Por isso esta classe é bem mais leve que as outras do
 * pacote — não há regra de negócio ligada ao tipo de caminhão. Ele não altera a
 * conta de pallets nem limita nada; serve para a doca saber o que esperar e para
 * o e-mail de confirmação levar a informação.
 *
 * A única exigência é que o nome exista. Descrição é opcional e, quando vem
 * vazia, é guardada como null em vez de string em branco, para o cadastro não
 * acumular espaços que a tela depois precisa limpar.
 */
package com.redekrill.scheduling.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Tipo de caminhão cadastrável, com nome obrigatório e descrição opcional.
 *
 * O que esta classe faz: normaliza a descrição e exige nome, além de produzir as
 * variações usadas por criação e edição.
 *
 * O que ela NÃO faz: influenciar capacidade. Os limites de 480 pallets por dia e
 * 120 por faixa não olham para este tipo — quem decide isso é o
 * {@link com.redekrill.scheduling.domain.policy.CapacityPolicy}, a partir da
 * quantidade de pallets e do tipo de paletização.
 */
public record TruckType(
        Long id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {

    public TruckType {
        name = DomainValidation.requireText(name, "Nome do tipo de caminhão é obrigatório.");
        description = description == null || description.isBlank() ? null : description.trim();
        Objects.requireNonNull(createdAt, "Data de criação é obrigatória.");
        Objects.requireNonNull(updatedAt, "Data de atualização é obrigatória.");
    }

    /**
     * Monta o tipo para o insert: id ainda não existe e as duas marcas de tempo
     * nascem com o mesmo instante, porque ele acabou de ser criado.
     *
     * @param name        nome exibido na lista de cadastro.
     * @param description detalhe opcional sobre o veículo.
     * @param now         instante injetado por quem chama, para o teste poder fixar o relógio.
     * @return o tipo sem id, pronto para ser persistido.
     */
    public static TruckType create(String name, String description, Instant now) {
        return new TruckType(null, name, description, now, now);
    }

    /**
     * Copia o valor recebendo o id gerado pelo banco. Nada mais muda.
     *
     * @param newId identificador devolvido pela persistência.
     * @return uma instância igual, com o id preenchido.
     */
    public TruckType withId(Long newId) {
        return new TruckType(newId, name, description, createdAt, updatedAt);
    }

    /**
     * Aplica a edição preservando a data de criação: um registro novo só nasce no
     * insert, então aqui muda o conteúdo e a marca de atualização.
     *
     * @param newName        novo nome, obrigatório como no cadastro inicial.
     * @param newDescription nova descrição, normalizada pelo construtor.
     * @param now            instante da atualização.
     * @return o tipo editado, com o mesmo id e a mesma {@code createdAt}.
     */
    public TruckType update(String newName, String newDescription, Instant now) {
        return new TruckType(id, newName, newDescription, createdAt, now);
    }
}
