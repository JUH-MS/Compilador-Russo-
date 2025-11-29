import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class InterfaceCompilador extends JFrame {

    private JTextArea editorArea;
    private JTextPane painelGeral; 
    private JTextArea arvoreArea;  
    private JTextArea lineNumbers; 

    public InterfaceCompilador() {
        super("Compilador Russo - Interface");

        setSize(950, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null); 

        // --- 1. Botões (Topo) ---
        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton btnAbrir = new JButton("Abrir Arquivo");
        btnAbrir.addActionListener(e -> abrirArquivo());

        JButton btnCompilar = new JButton("Compilar");
        btnCompilar.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnCompilar.setBackground(new Color(220, 255, 220)); 
        btnCompilar.addActionListener(e -> compilarCodigo());

        painelBotoes.add(btnAbrir);
        painelBotoes.add(btnCompilar);

        add(painelBotoes, BorderLayout.NORTH);

        // --- 2. Área de Código (Centro) ---
        editorArea = new JTextArea();
        editorArea.setFont(new Font("Consolas", Font.PLAIN, 16));

        lineNumbers = new JTextArea("1");
        lineNumbers.setBackground(Color.LIGHT_GRAY);
        lineNumbers.setEditable(false);
        lineNumbers.setFont(new Font("Consolas", Font.PLAIN, 16));
        lineNumbers.setBorder(new EmptyBorder(0, 5, 0, 5));

        editorArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { atualizarLinhas(); }
            public void removeUpdate(DocumentEvent e) { atualizarLinhas(); }
            public void changedUpdate(DocumentEvent e) { atualizarLinhas(); }
        });

        JScrollPane scrollEditor = new JScrollPane(editorArea);
        scrollEditor.setRowHeaderView(lineNumbers);
        
        add(scrollEditor, BorderLayout.CENTER);

        // --- 3. Painéis Inferiores (Sul) ---
        
        // Painel de Status (Esquerda)
        painelGeral = new JTextPane();
        painelGeral.setContentType("text/html");
        painelGeral.setEditable(false);
        JScrollPane scrollGeral = new JScrollPane(painelGeral);
        scrollGeral.setBorder(BorderFactory.createTitledBorder("Console / Status"));

        // Painel da Árvore (Direita)
        arvoreArea = new JTextArea();
        arvoreArea.setEditable(false);
        arvoreArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        JScrollPane scrollArvore = new JScrollPane(arvoreArea);
        scrollArvore.setBorder(BorderFactory.createTitledBorder("Arvore Sintatica / Saida"));

        // Divisão Horizontal
        JSplitPane painelInferior = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                scrollGeral,
                scrollArvore
        );
        painelInferior.setDividerLocation(450);
        painelInferior.setResizeWeight(0.5);
        painelInferior.setPreferredSize(new Dimension(getWidth(), 200));

        add(painelInferior, BorderLayout.SOUTH);

        setVisible(true);
    }


    private void atualizarLinhas() {
        int linhas = editorArea.getLineCount();
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= linhas; i++) {
            sb.append(i).append(System.lineSeparator());
        }
        lineNumbers.setText(sb.toString());
    }

    private void abrirArquivo() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);

        if(result == JFileChooser.APPROVE_OPTION){
            try {
                String texto = Files.readString(chooser.getSelectedFile().toPath());
                editorArea.setText(texto);
                atualizarLinhas();
            } catch (Exception ex) {
                painelGeral.setText("<html><span style='color:red;'>Erro ao abrir arquivo:<br>" 
                                    + ex.getMessage() + "</span></html>");
            }
        }
    }

    private void compilarCodigo() {
        String codigo = editorArea.getText();

        painelGeral.setText("");
        arvoreArea.setText("");

        PrintStream originalOut = System.out;

        try {
            System.setOut(new PrintStream(new OutputStream() {
                @Override
                public void write(int b) {
                    arvoreArea.append(String.valueOf((char) b));
                }
            }));

            InputStream is = new ByteArrayInputStream(codigo.getBytes());

            RusskiyCompiler parser = new RusskiyCompiler(is);

            parser.Programa();

            painelGeral.setText("<html><h3 style='color:green; font-family:sans-serif;'> ✔ Código aceito com sucesso!</h3>" +
                                "<p>Arvore/Saida gerada no painel a direita.</p></html>");
    
        } catch (ParseException e) {
            painelGeral.setText("<html><h3 style='color:red; font-family:sans-serif;'>Erro Sintatico:</h3>" 
                                + "<p>" + e.getMessage() + "</p></html>");
        } catch (TokenMgrError e) {
            painelGeral.setText("<html><h3 style='color:red; font-family:sans-serif;'>Erro LLexico:</h3>" 
                                + "<p>" + e.getMessage() + "</p></html>");
        } catch (Exception e) {
            painelGeral.setText("<html><span style='color:red;'>Erro Inesperado:<br>" 
                                + e.getMessage() + "</span></html>");
            e.printStackTrace(); 
        } finally {
            System.setOut(originalOut);
        }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
        SwingUtilities.invokeLater(() -> new InterfaceCompilador());
    }
}