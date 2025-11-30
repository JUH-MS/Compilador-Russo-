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
        if (filho != null) {
            filhos.add(filho);
        }
    }

    // --- MÉTODOS DE IMPRESSÃO (ÁRVORE) ---

    public void imprimirRaiz() {
        System.out.println(nome);
        for (int i = 0; i < filhos.size(); i++) {
            boolean ultimo = (i == filhos.size() - 1);
            filhos.get(i).imprimir("", ultimo);
        }
    }

    public void imprimir(String prefixo, boolean eUltimo) {
        System.out.print(prefixo);
        System.out.print(eUltimo ? "\\-- " : "+-- ");

        if (valor == null || valor.isEmpty()) {
            System.out.println(nome);
        } else {
            System.out.println(nome + " (" + valor + ")");
        }

        for (int i = 0; i < filhos.size(); i++) {
            String novoPrefixo = prefixo + (eUltimo ? "    " : "|   ");
            boolean ultimoFilho = (i == filhos.size() - 1);
            filhos.get(i).imprimir(novoPrefixo, ultimoFilho);
        }
    } 

    // --- GERAÇÃO DE CÓDIGO C ---

    public String gerarC() {
        StringBuilder sb = new StringBuilder();

        if (this.nome.equals("PROGRAMA")) {
            sb.append("#include <stdio.h>\n");
            sb.append("int main() {\n");
            for (No filho : filhos) {
                sb.append(filho.gerarC());
            }
            sb.append("return 0;\n}\n");
        } 
        else if (this.nome.equals("IO")) {
            No cmd = filhos.get(0); 
            if (cmd.valor.equals("vyvod")) {
                No conteudo = filhos.get(1);
                
                if (conteudo.nome.equals("String")) {
                    sb.append("printf(").append(conteudo.valor).append(");\n");
                    sb.append("printf(\"\\n\");\n"); 
                } else {
                    sb.append("printf(\"%d\\n\", "); 
                    sb.append(conteudo.gerarC());
                    sb.append(");\n");
                }
            }
            // COMANDO: vvod (INPUT)
            else if (cmd.valor.equals("vvod")) {
                No var = filhos.get(1);
                sb.append("scanf(\"%d\", &").append(var.valor).append(");\n");
            }
        }
        else if (this.nome.equals("Declaracao")) {
            String tipoRusso = filhos.get(0).valor;
            String tipoC = tipoRusso.equals("tseloye") ? "int" : 
                           tipoRusso.equals("drobnoye") ? "float" : "char*";
            
            String varNome = filhos.get(1).valor;
            sb.append(tipoC).append(" ").append(varNome);
            
            if (filhos.size() > 2) {
                sb.append(" = ").append(filhos.get(2).filhos.get(0).gerarC()); 
            }
            sb.append(";\n");
        }
        else if (this.nome.equals("Atribuicao")) {
            sb.append(filhos.get(0).gerarC()); 
            sb.append(" = ");
            sb.append(filhos.get(2).gerarC()); 
            sb.append(";\n");
        }
        else if (this.nome.equals("Incremento")) {
            sb.append(filhos.get(0).gerarC()).append("++;\n");
        }
        else if (this.nome.equals("Decremento")) {
            sb.append(filhos.get(0).gerarC()).append("--;\n");
        }
        else if (this.nome.equals("IF")) {
            sb.append("if (").append(filhos.get(0).gerarC()).append(") {\n");
            sb.append(filhos.get(1).gerarC()); 
            sb.append("}\n");
            
            if (filhos.size() > 2) { 
                sb.append("else {\n");
                sb.append(filhos.get(2).gerarC()); 
                sb.append("}\n");
            }
        }
        else if (this.nome.equals("WHILE")) {
            sb.append("while (").append(filhos.get(0).gerarC()).append(") {\n");
            sb.append(filhos.get(1).gerarC()); 
            sb.append("}\n");
        }
        else if (this.nome.equals("Num") || this.nome.equals("Var") || this.nome.equals("String")) {
            sb.append(this.valor);
        }
        else if (this.nome.startsWith("Op.")) { 
            sb.append(filhos.get(0).gerarC());
            sb.append(" ").append(this.valor).append(" ");
            sb.append(filhos.get(1).gerarC());
        }
        else {
            for (No filho : filhos) {
                sb.append(filho.gerarC());
            }
        }
        
        return sb.toString();
    }
}