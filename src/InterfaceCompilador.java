import java.awt.*;
import java.io.*;
import java.nio.file.*;
import javax.swing.*;

public class InterfaceCompilador extends JFrame {

    private JTextArea editorArea;
    private JTextArea tokensArea;
    private JTextArea errosArea;
    private JTextArea arvoreArea;

    public InterfaceCompilador() {
        super("Compilador Russo - Interface");

        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        //area onde o usuario coda
        editorArea = new JTextArea();
        editorArea.setFont(new Font("Consolas", Font.PLAIN, 16));

        //paineis laterais
        tokensArea = new JTextArea();
        tokensArea.setEditable(false);

        errosArea = new JTextArea();
        errosArea.setEditable(false);

        arvoreArea = new JTextArea();
        arvoreArea.setEditable(false);

        //scrolls
        JScrollPane scrollEditor  = new JScrollPane(editorArea);
        JScrollPane scrollTokens  = new JScrollPane(tokensArea);
        JScrollPane scrollErros   = new JScrollPane(errosArea);
        JScrollPane scrollArvore  = new JScrollPane(arvoreArea);

        //painel direito (tokens + erros + arvore)
        JTabbedPane painelDireita = new JTabbedPane();
        painelDireita.add("Tokens", scrollTokens);
        painelDireita.add("Erros", scrollErros);
        painelDireita.add("Árvore Sintática", scrollArvore);

        add(scrollEditor, BorderLayout.CENTER);
        add(painelDireita, BorderLayout.EAST);

        //botoes inferiores
        JButton compilarBtn = new JButton("Compilar");
        compilarBtn.addActionListener(e -> compilarCodigo());

        JButton abrirBtn = new JButton("Abrir Arquivo");
        abrirBtn.addActionListener(e -> abrirArquivo());

        JPanel painelBotoes = new JPanel();
        painelBotoes.add(compilarBtn);
        painelBotoes.add(abrirBtn);

        add(painelBotoes, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void abrirArquivo() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);

        if(result == JFileChooser.APPROVE_OPTION){
            try {
                String texto = Files.readString(chooser.getSelectedFile().toPath());
                editorArea.setText(texto);
            } catch (Exception ex) {
                errosArea.setText("Erro ao abrir arquivo:\n" + ex.getMessage());
            }
        }
    }

    private void compilarCodigo() {
        String codigo = editorArea.getText();
    
        try {
            File temp = File.createTempFile("russo", ".txt");
            Files.writeString(temp.toPath(), codigo);
    
            FileInputStream fis = new FileInputStream(temp);
            RusskiyCompiler parser = new RusskiyCompiler(fis);
    
            errosArea.setText("");
            tokensArea.setText("");
            arvoreArea.setText("");
    
            //redireciona a saída do parser para arvoreArea
            PrintStream printStream = new PrintStream(new OutputStream() {
                @Override
                public void write(int b) {
                    arvoreArea.append(String.valueOf((char) b));
                }
            });
            System.setOut(printStream);
            System.setErr(printStream);
    
            parser.Programa();  //executa o parser
            errosArea.setText("✔ Código aceito!");
    
        } catch (ParseException e) {
            errosArea.setText("Erro de sintaxe:\n" + e.getMessage());
        } catch (TokenMgrError e) {
            errosArea.setText("Erro léxico:\n" + e.getMessage());
        } catch (Exception e) {
            errosArea.setText("Erro inesperado:\n" + e.getMessage());
        }
    }
    public static void main(String[] args) {
        new InterfaceCompilador();
    }
}
