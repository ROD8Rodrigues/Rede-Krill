/*
 * Em que ponto da fila a mensagem de confirmação está.
 *
 * Quatro estados, e a divisão que importa é entre os transitórios e os
 * terminais. PENDING é o único que o processador consulta — a busca por pendentes
 * filtra por este valor e pela data da próxima tentativa. SENT, FAILED e CANCELED
 * não voltam para a fila sozinhos: para insistir depois de um desses, teria que
 * nascer outra linha.
 *
 * FAILED e CANCELED param por motivos diferentes. FAILED significa "tentamos o
 * número acordado de vezes e o SMTP não aceitou"; CANCELED significa "o
 * agendamento foi cancelado antes de sair, então ninguém deve receber aquilo".
 * Os dois são terminais, mas a pergunta "por que esse fornecedor não foi
 * avisado?" só responde com eles separados.
 */
package com.redekrill.scheduling.domain.model;

public enum EmailOutboxStatus {
    // Na fila: pode ser enviado agora ou ainda está no tempo de espera do retry.
    PENDING,
    // Entregue. Terminal, com sentAt preenchido.
    SENT,
    // Esgotou as tentativas sem entregar. Terminal, guarda o último erro lido.
    FAILED,
    // Anulada porque o agendamento foi cancelado. Terminal, sem culpa e sem erro novo.
    CANCELED
}
