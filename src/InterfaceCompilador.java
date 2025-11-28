import java.awt.*;
import java.io.*;
import java.nio.file.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class InterfaceCompilador extends JFrame {

    private JTextArea editorArea;
    private JTextPane painelGeral;
    private JTextArea arvoreArea;

    public InterfaceCompilador() {
        super("Compilador Russo - Interface");

        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // área de código
        editorArea = new JTextArea();
        editorArea.setFont(new Font("Consolas", Font.PLAIN, 16));

        LineNumberPanel lineNumbers = new LineNumberPanel(editorArea);
        JScrollPane scrollEditor = new JScrollPane(editorArea);
        scrollEditor.setRowHeaderView(lineNumbers);

        // painel geral (HTML)
        painelGeral = new JTextPane();
        painelGeral.setContentType("text/html");
        painelGeral.setEditable(false);

        JScrollPane scrollGeral = new JScrollPane(painelGeral);

        // árvore sintática
        arvoreArea = new JTextArea();
        arvoreArea.setEditable(false);
        JScrollPane scrollArvore = new JScrollPane(arvoreArea);

        // painel inferior horizontal
        JSplitPane painelInferior = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                scrollGeral,
                scrollArvore
        );
        painelInferior.setDividerLocation(450);
        painelInferior.setResizeWeight(0.6);

        add(scrollEditor, BorderLayout.CENTER);
        add(painelInferior, BorderLayout.SOUTH);

        // botões
        JButton compilarBtn = new JButton("Compilar");
        compilarBtn.addActionListener(e -> compilarCodigo());

        JButton abrirBtn = new JButton("Abrir Arquivo");
        abrirBtn.addActionListener(e -> abrirArquivo());

        JPanel painelBotoes = new JPanel();
        painelBotoes.add(compilarBtn);
        painelBotoes.add(abrirBtn);

        add(painelBotoes, BorderLayout.NORTH);

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
                painelGeral.setText("<html><span style='color:red;'>Erro ao abrir arquivo:<br>" 
                                    + ex.getMessage() + "</span></html>");
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
    
            painelGeral.setText("");
            arvoreArea.setText("");
    
            // redireciona saída do parser
            PrintStream printStream = new PrintStream(new OutputStream() {
                @Override
                public void write(int b) {
                    arvoreArea.append(String.valueOf((char) b));
                }
            });

            System.setOut(printStream);
            System.setErr(printStream);
    
            parser.Programa();
    
            painelGeral.setText("<html><span style='color:green;'>✔ Código aceito!</span></html>");
    
        } catch (ParseException e) {
            painelGeral.setText("<html><span style='color:red;'>Erro de sintaxe:<br>" 
                                + e.getMessage() + "</span></html>");
        } catch (TokenMgrError e) {
            painelGeral.setText("<html><span style='color:red;'>Erro léxico:<br>" 
                                + e.getMessage() + "</span></html>");
        } catch (Exception e) {
            painelGeral.setText("<html><span style='color:red;'>Erro inesperado:<br>" 
                                + e.getMessage() + "</span></html>");
        }
    }

    public static void main(String[] args) {
        new InterfaceCompilador();
    }
}


// painel de numeração de linhas
class LineNumberPanel extends JPanel implements DocumentListener {

    private final JTextArea textArea;
    private final Font font = new Font("Consolas", Font.PLAIN, 14);

    public LineNumberPanel(JTextArea textArea) {
        this.textArea = textArea;
        textArea.getDocument().addDocumentListener(this);
        setPreferredSize(new Dimension(40, Integer.MAX_VALUE));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();

        int lineHeight = fm.getHeight();
        int start = textArea.getInsets().top;
        int lineCount = textArea.getLineCount();

        for (int i = 0; i < lineCount; i++) {
            int y = start + i * lineHeight + fm.getAscent();
            g.drawString(String.valueOf(i + 1), 5, y);
        }
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        repaint();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        repaint();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        repaint();
    }
}
