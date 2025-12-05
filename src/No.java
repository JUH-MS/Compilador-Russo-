import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class No {
    private String nome;
    private String valor;
    private List<No> filhos;
    
    // Mapa estático para tipos (usado no gerador C)
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

    // --- INTERPRETADOR ---
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
            
            // --- PRINT (VYVOD) ---
            if (cmd.valor.equals("vyvod")) { 
                Object res = filhos.get(1).executar(memoria);
                String saida = res.toString().replace("\"", "");
                if (saida.endsWith(".0")) saida = saida.substring(0, saida.length() - 2);
                System.out.println(saida);
            } 
            
            // --- INPUT (VVOD) ---
            else if (cmd.valor.equals("vvod")) { 
                String nomeVar = filhos.get(1).valor;
                
                // MUDANÇA 1: Usei print() em vez de println() para não pular linha
                // O espaço no final é para o número não colar no texto
                System.out.print(">> Digite valor para '" + nomeVar + "': ");
                
                // Pausa e espera o input da interface
                String input = InterfaceCompilador.lerEntradaUsuario();
                
                try {
                    Double valorNum = Double.parseDouble(input);
                    memoria.put(nomeVar, valorNum);
                    
                    // MUDANÇA 2: Removi o "System.out.println(Ok...)" 
                    // Agora ele processa silenciosamente, como no C.
                    
                } catch (NumberFormatException e) {
                    throw new Exception("Entrada invalida! Esperava um numero.");
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
            sb.append("#include <stdio.h>\n#include <stdlib.h>\n\n");
            sb.append("int main() {\n");
            for (No f : filhos) sb.append(f.gerarC());
            sb.append("    return 0;\n}\n");
            return sb.toString();
        }

        switch (nome) {
            case "IO":
                if (filhos.get(0).valor.equals("vyvod")) {   // PRINT
                    No expr = filhos.get(1);
                    if (expr.nome.equals("String")) {
                        sb.append("    printf(").append(expr.valor).append(");\n");
                        sb.append("    printf(\"\\n\");\n");
                    } else if (expr.nome.equals("Var")) {
                        String varName = expr.valor;
                        String t = tipoVars.get(varName);
                        if ("int".equals(t)) sb.append("    printf(\"%d\\n\", ").append(varName).append(");\n");
                        else if ("char*".equals(t)) sb.append("    printf(\"%s\\n\", ").append(varName).append(");\n");
                        else sb.append("    printf(\"%g\\n\", (float)(").append(varName).append("));\n");
                    } else {
                        sb.append("    printf(\"%g\\n\", (float)(").append(expr.gerarC()).append("));\n");
                    }
                } else { // INPUT
                    sb.append("    scanf(\"%f\", &").append(filhos.get(1).valor).append(");\n");
                }
                break;

            case "Declaracao":
                String tipoC = traduzTipo(filhos.get(0).valor);
                String nomeVar = filhos.get(1).valor;
                tipoVars.put(nomeVar, tipoC);
                sb.append("    ").append(tipoC).append(" ").append(nomeVar);
                if (filhos.size() > 3) sb.append(" = ").append(filhos.get(3).gerarC());
                sb.append(";\n");
                break;

            case "Atribuicao":
                sb.append("    ").append(filhos.get(0).gerarC()).append(" = ").append(filhos.get(2).gerarC()).append(";\n");
                break;

            case "Incremento":
                sb.append("    ").append(filhos.get(0).gerarC()).append("++;\n");
                break;

            case "Decremento":
                sb.append("    ").append(filhos.get(0).gerarC()).append("--;\n");
                break;

            case "IF":
                sb.append("    if (").append(filhos.get(0).gerarC()).append(") {\n").append(filhos.get(1).gerarC()).append("    }\n");
                if (filhos.size() > 2) sb.append("    else {\n").append(filhos.get(2).gerarC()).append("    }\n");
                break;

            case "WHILE":
                sb.append("    while (").append(filhos.get(0).gerarC()).append(") {\n").append(filhos.get(1).gerarC()).append("    }\n");
                break;

            case "Num": return valor;
            case "Var": return valor;
            case "String": return valor;

            case "Op.+": case "Op.-": case "Op.*": case "Op./": case "Op.%": 
            case "Op.>": case "Op.<": case "Op.>=": case "Op.<=": case "Op.==": case "Op.!=":
                return "(" + filhos.get(0).gerarC() + " " + valor + " " + filhos.get(1).gerarC() + ")";

            default:
                for (No f : filhos) sb.append(f.gerarC());
        }
        return sb.toString();
    }
}