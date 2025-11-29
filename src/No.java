import java.util.ArrayList;
import java.util.List;

public class No {
    private String nome;          // Ex: "IF", "WHILE", "SOMA"
    private String valor;         // Ex: "x", "10", "+"
    private List<No> filhos;      // Lista de nós abaixo deste

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

    public void imprimir(String prefixo, boolean eUltimo) {

    System.out.print(prefixo);
    System.out.print(eUltimo ? "\\-- " : "+-- ");

    if (valor.isEmpty()) {
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
}