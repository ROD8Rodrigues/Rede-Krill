/*
 * Contrato do token de renovação: gerar um valor opaco e calcular seu resumo.
 *
 * Os dois métodos andam juntos porque a sessão nunca guarda o token — guarda o
 * resumo. Quem cria a sessão gera o texto cru, calcula o hash na hora, persiste o
 * hash e devolve o cru para virar cookie. Na renovação, o caminho é o inverso: o
 * cliente traz o cru, a aplicação recalcula o resumo e procura a linha
 * correspondente. Se a regra de resumo mudasse num dos dois pontos, as sessões
 * antigas deixariam de ser encontradas — por isso o contrato é um só.
 *
 * O hash é um resumo de verificação, não uma cifra: quem tiver a tabela não
 * recupera o token, mas consegue comparar.
 */
package com.redekrill.scheduling.application.port;

/**
 * Geração aleatória e resumo determinístico. Implementada por
 * SecureRefreshTokenGenerator.
 */
public interface RefreshTokenGenerator {

    String generate();

    String hash(String rawToken);
}
