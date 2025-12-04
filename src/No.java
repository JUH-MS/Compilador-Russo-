import java.util.ArrayList;
import java.util.List;

public class No {
    private String nome;
    private String valor;
    private List<No> filhos;

    public No(String nome) {
        this.nome = nome;
        this.valor = "";
        this.filhos = new ArrayList<>();
    }

    public No(String nome, String valor) {
        this.nome = nome;
        this.valor = valor;
        this.filhos = new ArrayList<>();
    }

    public void addFilho(No filho) {
        if (filho != null) filhos.add(filho);
    }

    public void imprimirRaiz() {
        System.out.println(nome);
        for (int i = 0; i < filhos.size(); i++) {
            filhos.get(i).imprimir("", i == filhos.size() - 1);
        }
    }

    public void imprimir(String prefixo, boolean eUltimo) {
        System.out.print(prefixo + (eUltimo ? "\\-- " : "+-- "));
        System.out.println(valor.isEmpty() ? nome : nome + " (" + valor + ")");
        for (int i = 0; i < filhos.size(); i++) {
            filhos.get(i).imprimir(prefixo + (eUltimo ? "    " : "|   "), i == filhos.size() - 1);
        }
    }

    public String gerarC() {
        StringBuilder sb = new StringBuilder();

        if (this.nome.startsWith("[ERRO")) return "";

        if (this.nome.equals("PROGRAMA")) {
            sb.append("#include <stdio.h>\nint main() {\n");
            for (No f : filhos) sb.append(f.gerarC());
            sb.append("return 0;\n}\n");
        } 
        else if (this.nome.equals("IO")) {
            if (filhos.isEmpty()) return ""; 
            No cmd = filhos.get(0);
            
            if (cmd.valor.equals("vyvod")) {
                sb.append("printf(");
                if (filhos.size() > 1) {
                    No param = filhos.get(1);
                    if (param.nome.equals("String")) {
                        sb.append(param.valor).append("); printf(\"\\n\");\n");
                    } else {
                        sb.append("\"%d\\n\", ").append(param.gerarC()).append(");\n");
                    }
                }
            } else if (cmd.valor.equals("vvod")) {
                if (filhos.size() > 1) {
                    sb.append("scanf(\"%d\", &").append(filhos.get(1).valor).append(");\n");
                }
            }
        } 
        else if (this.nome.equals("Declaracao")) {
            if (filhos.size() >= 2) {
                String tipo = filhos.get(0).valor.equals("tseloye") ? "int" : "float";
                sb.append(tipo).append(" ").append(filhos.get(1).valor);
                
                if (filhos.size() > 3) {
                    sb.append(" = ").append(filhos.get(3).gerarC());
                }
                sb.append(";\n");
            }
        } 
        else if (this.nome.equals("Atribuicao")) {
            if (filhos.size() > 2) {
                sb.append(filhos.get(0).gerarC())
                  .append(" = ")
                  .append(filhos.get(2).gerarC())
                  .append(";\n");
            }
        } 
        else if (this.nome.equals("Incremento")) {
            if (!filhos.isEmpty()) sb.append(filhos.get(0).gerarC()).append("++;\n");
        } 
        else if (this.nome.equals("Decremento")) {
            if (!filhos.isEmpty()) sb.append(filhos.get(0).gerarC()).append("--;\n");
        } 
        else if (this.nome.equals("IF")) {
            if (filhos.size() > 1) {
                sb.append("if (").append(filhos.get(0).gerarC()).append(") {\n");
                sb.append(filhos.get(1).gerarC()).append("}\n");
                if (filhos.size() > 2) {
                    sb.append("else {\n").append(filhos.get(2).gerarC()).append("}\n");
                }
            }
        } 
        else if (this.nome.equals("WHILE")) {
            if (filhos.size() > 1) {
                sb.append("while (").append(filhos.get(0).gerarC()).append(") {\n");
                sb.append(filhos.get(1).gerarC()).append("}\n");
            }
        } 
        else if (this.nome.equals("Num") || this.nome.equals("Var") || this.nome.equals("String")) {
            sb.append(this.valor);
        } 
        else if (this.nome.startsWith("Op.")) {
            if (filhos.size() > 1) {
                sb.append(filhos.get(0).gerarC())
                  .append(" ")
                  .append(this.valor)
                  .append(" ")
                  .append(filhos.get(1).gerarC());
            }
        } 
        else {
            for (No f : filhos) sb.append(f.gerarC());
        }
        return sb.toString();
    }
}