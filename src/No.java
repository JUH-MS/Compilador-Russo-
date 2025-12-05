import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane; // Necessário para o input visual

public class No {
    private String nome;
    private String valor;
    private List<No> filhos;

    private static java.util.Map<String,String> tipoVars = new java.util.HashMap<>();

    public No(String nome) { this.nome = nome; this.valor = ""; this.filhos = new ArrayList<>(); }
    public No(String nome, String valor) { this.nome = nome; this.valor = valor; this.filhos = new ArrayList<>(); }
    public void addFilho(No filho) { if (filho != null) filhos.add(filho); }

    public void imprimirRaiz() {
        System.out.println(nome);
        for (int i = 0; i < filhos.size(); i++) filhos.get(i).imprimir("", i == filhos.size() - 1);
    }

    public void imprimir(String prefixo, boolean eUltimo) {
        System.out.print(prefixo + (eUltimo ? "\\-- " : "+-- "));
        System.out.println(valor.isEmpty() ? nome : nome + " (" + valor + ")");
        for (int i = 0; i < filhos.size(); i++) filhos.get(i).imprimir(prefixo + (eUltimo ? "    " : "|   "), i == filhos.size() - 1);
    }

    // --- INTERPRETADOR (Executa o código) ---
    public Object executar(Map<String, Object> memoria) throws Exception {
        if (nome.equals("PROGRAMA")) {
            for (No filho : filhos) filho.executar(memoria);
            return null;
        }
        else if (nome.equals("Declaracao")) {
            String nomeVar = filhos.get(1).valor;
            Object valorInit = 0.0; 
            if (filhos.size() > 3) valorInit = filhos.get(3).executar(memoria);
            memoria.put(nomeVar, valorInit);
        }
        else if (nome.equals("Atribuicao")) {
            String nomeVar = filhos.get(0).valor;
            Object novoValor = filhos.get(2).executar(memoria);
            memoria.put(nomeVar, novoValor);
        }
        else if (nome.equals("Incremento")) {
            String nomeVar = filhos.get(0).valor;
            Number atual = (Number) memoria.get(nomeVar);
            memoria.put(nomeVar, atual.doubleValue() + 1);
        }
        else if (nome.equals("Decremento")) {
            String nomeVar = filhos.get(0).valor;
            Number atual = (Number) memoria.get(nomeVar);
            memoria.put(nomeVar, atual.doubleValue() - 1);
        }
        else if (nome.equals("IO")) {
            No cmd = filhos.get(0);
            
            // --- COMANDO VYVOD (PRINT) ---
            if (cmd.valor.equals("vyvod")) { 
                Object res = filhos.get(1).executar(memoria);
                // Remove aspas se for string e imprime
                String saida = res.toString().replace("\"", "");
                // Se for número, remove o ".0" do final para ficar bonito
                if (saida.endsWith(".0")) saida = saida.substring(0, saida.length() - 2);
                System.out.println(saida);
            } 
            
            // --- COMANDO VVOD (INPUT) - CORRIGIDO ---
            else if (cmd.valor.equals("vvod")) { 
                String nomeVar = filhos.get(1).valor;
                String input = "";
                boolean valido = false;
                
                // Loop para garantir que o usuário digite um número válido
                while (!valido) {
                    input = JOptionPane.showInputDialog(null, 
                        "O programa pede valor para: '" + nomeVar + "'", 
                        "Entrada de Dados (vvod)", 
                        JOptionPane.QUESTION_MESSAGE);
                    
                    if (input == null) throw new Exception("Execucao cancelada pelo usuario.");
                    
                    try {
                        // Tenta converter para número
                        Double valorNum = Double.parseDouble(input);
                        memoria.put(nomeVar, valorNum);
                        System.out.println("> Entrada recebida: " + valorNum); // Feedback no console
                        valido = true;
                    } catch (NumberFormatException e) {
                        JOptionPane.showMessageDialog(null, "Erro: Voce deve digitar um NUMERO valido!");
                    }
                }
            }
        }
        else if (nome.equals("IF")) {
            Object cond = filhos.get(0).executar(memoria);
            if (cond instanceof Boolean && (Boolean)cond) {
                filhos.get(1).executar(memoria); 
            } else if (filhos.size() > 2) {
                filhos.get(2).executar(memoria); 
            }
        }
        else if (nome.equals("WHILE")) {
            while ((Boolean) filhos.get(0).executar(memoria)) {
                filhos.get(1).executar(memoria);
            }
        }
        else if (nome.startsWith("Op.")) {
            double esq = Double.parseDouble(filhos.get(0).executar(memoria).toString());
            double dir = Double.parseDouble(filhos.get(1).executar(memoria).toString());
            String op = this.valor;
            
            if (op.equals("+")) return esq + dir;
            if (op.equals("-")) return esq - dir;
            if (op.equals("*")) return esq * dir;
            if (op.equals("/")) return esq / dir;
            if (op.equals("%")) return esq % dir;
            if (op.equals(">")) return esq > dir;
            if (op.equals("<")) return esq < dir;
            if (op.equals(">=")) return esq >= dir;
            if (op.equals("<=")) return esq <= dir;
            if (op.equals("==")) return esq == dir;
            if (op.equals("!=")) return esq != dir;
        }
        else if (nome.equals("Num")) return Double.parseDouble(valor);
        else if (nome.equals("String")) return valor;
        else if (nome.equals("Var")) return memoria.get(valor);
        
        return null;
    }

    private String traduzTipo(String russo) {
        return switch (russo) {
            case "pusto"   -> "void";
            case "tseloye" -> "int";
            case "drobnoye"-> "float";
            case "slova"   -> "char*";
            default        -> "float";
        };
    }

// --- GERADOR C ---
public String gerarC() {
    StringBuilder sb = new StringBuilder();
    if (nome.startsWith("[ERRO")) return "";

    if (nome.startsWith("PROGRAMA")) {

        sb.append("#include <stdio.h>\n");
        sb.append("#include <stdlib.h>\n\n");

        sb.append("int main() {\n");

        for (No f : filhos)
            sb.append(f.gerarC());

        sb.append("    return 0;\n");
        sb.append("}\n");

        return sb.toString();
    }

    // -------------------------
    // OUTROS NÓS (switch)
    // -------------------------
    switch (nome) {

        // ENTRADA E SAÍDA
        case "IO":
            if (filhos.get(0).valor.equals("vyvod")) {   // PRINT
                No expr = filhos.get(1);

                if (expr.nome.equals("String")) {
                    sb.append("    printf(").append(expr.valor).append(");\n");
                } 
                else if (expr.nome.equals("Var")) {
                    String varName = expr.valor;
                    String t = tipoVars.get(varName);

                    if ("int".equals(t)) {
                        sb.append("    printf(\"%d\", ").append(varName).append(");\n");
                    } else if ("char*".equals(t)) {
                        sb.append("    printf(\"%s\", ").append(varName).append(");\n");
                    } else {
                        sb.append("    printf(\"%g\", (float)(").append(varName).append("));\n");
                    }
                } 
                else {
                    sb.append("    printf(\"%g\", (float)(")
                      .append(expr.gerarC())
                      .append("));\n");
                }

            } else { // INPUT (vvod)
                sb.append("    scanf(\"%f\", &").append(filhos.get(1).valor).append(");\n");
            }
            break;


        // DECLARACÃO
        case "Declaracao":
            String tipoC = traduzTipo(filhos.get(0).valor);
            String nomeVar = filhos.get(1).valor;

            tipoVars.put(nomeVar, tipoC); // registra tipo

            sb.append("    ").append(tipoC).append(" ").append(nomeVar);

            if (filhos.size() > 3) {
                sb.append(" = ").append(filhos.get(3).gerarC());
            }

            sb.append(";\n");
            break;

        case "Atribuicao":
            sb.append("    ")
              .append(filhos.get(0).gerarC())
              .append(" = ")
              .append(filhos.get(2).gerarC())
              .append(";\n");
            break;

        case "Incremento":
            sb.append("    ").append(filhos.get(0).gerarC()).append("++;\n");
            break;

        case "Decremento":
            sb.append("    ").append(filhos.get(0).gerarC()).append("--;\n");
            break;

        case "IF":
            sb.append("    if (")
              .append(filhos.get(0).gerarC()).append(") {\n")
              .append(filhos.get(1).gerarC())
              .append("    }\n");
            if (filhos.size() > 2) {
                sb.append("    else {\n")
                  .append(filhos.get(2).gerarC())
                  .append("    }\n");
            }
            break;

        case "WHILE":
            sb.append("    while (")
              .append(filhos.get(0).gerarC()).append(") {\n")
              .append(filhos.get(1).gerarC())
              .append("    }\n");
            break;


        // EXPRESSÕES
        case "Num": return valor;
        case "Var": return valor;
        case "String": return valor;

        // OPERADORES BINÁRIOS
        case "Op.+":
        case "Op.-":
        case "Op.*":
        case "Op./":
        case "Op.%":
        case "Op.>":
        case "Op.<":
        case "Op.>=":
        case "Op.<=":
        case "Op.==":
        case "Op.!=":
        case "Op.//":
            return "(" + filhos.get(0).gerarC() + " " + valor + " " + filhos.get(1).gerarC() + ")";

        case "Op.**":
            return "pow(" + filhos.get(0).gerarC() + ", " + filhos.get(1).gerarC() + ")";

        default:
            for (No f : filhos)
                sb.append(f.gerarC());
    }

    return sb.toString();
}
}