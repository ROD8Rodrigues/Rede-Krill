/*
 * As conferências que os registros deste pacote repetem entre si.
 *
 * Fica aqui, e não dentro de cada modelo, por dois motivos: os critérios têm que
 * ser iguais para todo mundo — um e-mail aceito em usuário tem que ser aceito em
 * fornecedor — e o pacote precisa de um ponto único para mudar a regra. Toda
 * validação de formato existe para produzir um DomainException com a mensagem
 * que o usuário lê, então nenhuma dessas funções devolve booleano.
 *
 * É package-private de propósito. Quem está fora de domain.model escreve a
 * própria checagem ou, melhor ainda, delega ao construtor do modelo. Se isto
 * fosse público, viraria utilitário genérico e as regras escapariam para camadas
 * que não deveriam decidir formato.
 */
package com.redekrill.scheduling.domain.model;

import com.redekrill.scheduling.domain.exception.DomainException;

import java.util.regex.Pattern;

/**
 * Utilitário interno dos modelos: texto obrigatório, tamanho máximo, formato de
 * e-mail e contagem positiva.
 *
 * O que esta classe faz: normaliza (trim, minúscula no e-mail) e recusa o que não
 * passa, sempre com DomainException.
 *
 * O que ela NÃO faz: regra de negócio. Limite de pallets, sobreposição de faixa e
 * política de senha têm dono próprio; nada aqui sabe se o valor faz sentido para o
 * agendamento, só se tem forma.
 */
final class DomainValidation {

    /**
     * O mínimo que separa um e-mail utilizável de um texto qualquer: algo antes
     * do @, algo depois, e um ponto no domínio. Não se tenta validar contra a RFC
     * nem confirmar que a caixa existe — seria falsificação de precisão.
     */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private DomainValidation() {
    }

    /**
     * Exige texto com conteúdo e devolve a versão sem as bordas.
     *
     * @param value   o valor cru vindo da requisição, do seed ou do teste.
     * @param message mensagem lida pelo usuário quando falhar.
     * @return o texto já com trim.
     */
    static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new DomainException(message);
        }
        return value.trim();
    }

    /**
     * Conferência de tamanho que aceita ausência: nulo passa, porque a
     * obrigatoriedade é de outra função.
     *
     * @param value     texto a medir; nulo é permitido.
     * @param maxLength limite inclusive, combinado com a coluna do banco.
     * @param message   mensagem quando estourar o limite.
     * @return o valor exatamente como chegou.
     */
    static String requireMaxLength(String value, int maxLength, String message) {
        if (value != null && value.length() > maxLength) {
            throw new DomainException(message);
        }
        return value;
    }

    /**
     * Exige um e-mail bem-formado e o devolve normalizado para comparação.
     *
     * A caixa-baixa acontece antes da conferência e no resultado, então dois
     * cadastros que diferem só por maiúscula não viram contas diferentes.
     *
     * @param value   o endereço informado.
     * @param message mensagem usada quando o campo vem vazio.
     * @return o endereço em minúscula, sem espaços nas bordas.
     */
    static String requireEmail(String value, String message) {
        String email = requireText(value, message).toLowerCase(java.util.Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new DomainException("E-mail inválido: " + email);
        }
        return email;
    }

    /**
     * Campo de texto que pode ficar vazio.
     *
     * Vazio aqui é null, não string em branco: assim a listagem não precisa
     * limpar espaço em cada tela e o banco não acumula texto que ninguém escreveu.
     *
     * @param value     o texto opcional.
     * @param maxLength limite inclusive após o trim.
     * @param message   mensagem quando o conteúdo ultrapassar o limite.
     * @return null quando não havia nada; o texto limpo caso contrário.
     */
    static String optionalText(String value, int maxLength, String message) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim();
        if (text.length() > maxLength) {
            throw new DomainException(message);
        }
        return text;
    }

    /**
     * Exige contagem maior que zero.
     *
     * Zero não é permitido nem como "sem carga": um agendamento de zero pallets
     * ocuparia uma faixa sem motivo e poluiria o relatório de ocupação.
     *
     * @param value   a quantidade informada.
     * @param message mensagem lida pelo usuário.
     * @return o valor validado.
     */
    static int requirePositive(int value, String message) {
        if (value <= 0) {
            throw new DomainException(message);
        }
        return value;
    }

    /**
     * Idem, para identificador embrulhado — que pode chegar nulo de um request
     * com corpo incompleto.
     *
     * @param value   o id informado.
     * @param message mensagem lida pelo usuário.
     * @return o id validado.
     */
    static long requirePositive(Long value, String message) {
        if (value == null || value <= 0) {
            throw new DomainException(message);
        }
        return value;
    }
}
