# Compilador Russo/Russkiy

## Introdução
Este repositório contém a implementação e documentação referentes ao desenvolvimento de um compilador educacional, criado como parte do trabalho acadêmico da disciplina de **Construção de Compiladores**.  

O compilador proposto tem como inspiração a linguagem C e diferencia-se por utilizar **russo transliterado** na definição dos comandos da linguagem fonte. O trabalho segue as etapas clássicas da construção de compiladores, contemplando a análise léxica, a análise sintática e um controle básico de erros.

---

## Objetivos
- Implementar um compilador funcional contendo, no mínimo:
  - Análise léxica e sintática;
  - Relatórios de erros com indicação do token encontrado e do token esperado;
  - Mensagens de erro apresentadas em português.
- Definir uma linguagem com estruturas fundamentais de programação:
  - Estruturas de repetição;
  - Estruturas condicionais;
  - Declaração de variáveis (simples e em lista, com e sem atribuição inicial);
  - Três tipos distintos de dados.  
- Documentar a linguagem criada, incluindo:
  - Gramática formal na notação Backus-Naur (BNF);
  - Exemplos de programas válidos e inválidos.

---

## Tecnologias Utilizadas
Para a implementação do compilador, são utilizadas ferramentas clássicas de apoio, tais como:  
- **Linguagem java** 
- **JavaCC**
- **Biblioteca Swing**   

---

## Estrutura da Linguagem
A linguagem desenvolvida utiliza palavras do russo transliteradas para o alfabeto latino, mantendo a semântica fundamental das linguagens imperativas. A seguir, apresentam-se os principais equivalentes, na linguagem C, dos termos adotados.

- `pusto` = `void`
- `tseloye` = `int`
- `drobnoye` = `float`
- `slova` = `string`
- `dvoichnaya` = `bool`
- `pravda` = `true`
- `nepravda` = `false`
- `yesli` = `if`
- `inache` = `else`
- `kazhday` = `for`
- `poka` = `while`
- `vyvod` = `printf`
- `vvod` = `scanf`
- `funktsiya` = `function`

Além desses termos, outras construções e atribuições da linguagem são definidas e detalhadas formalmente por meio da gramática Backus–Naur Form (BNF) do projeto.
 
