import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class InterfaceCompilador extends JFrame {

    private JTextArea editorArea;
    private JTextPane painelGeral; // Para mensagens de Sucesso/Erro
    private JTextArea arvoreArea;  // Para a Árvore Sintática / Saída
    private JTextArea lineNumbers; // Numeração das linhas

    public InterfaceCompilador() {
        super("Compilador Russo - Interface");

        setSize(950, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null); // Centraliza na tela

        // --- 1. Botões (Topo) ---
        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton btnAbrir = new JButton("Abrir Arquivo");
        btnAbrir.addActionListener(e -> abrirArquivo());

        JButton btnCompilar = new JButton("Compilar");
        btnCompilar.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnCompilar.setBackground(new Color(220, 255, 220)); // Verde claro
        btnCompilar.addActionListener(e -> compilarCodigo());

        painelBotoes.add(btnAbrir);
        painelBotoes.add(btnCompilar);

        add(painelBotoes, BorderLayout.NORTH);

        // --- 2. Área de Código (Centro) ---
        editorArea = new JTextArea();
        editorArea.setFont(new Font("Consolas", Font.PLAIN, 16));
        
        // Configuração da numeração de linhas
        lineNumbers = new JTextArea("1");
        lineNumbers.setBackground(Color.LIGHT_GRAY);
        lineNumbers.setEditable(false);
        lineNumbers.setFont(new Font("Consolas", Font.PLAIN, 16));
        lineNumbers.setBorder(new EmptyBorder(0, 5, 0, 5));
        
        // Atualiza linhas ao digitar
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
        scrollArvore.setBorder(BorderFactory.createTitledBorder("Árvore Sintática / Saída"));

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

    // --- MÉTODOS QUE ESTAVAM FALTANDO ---

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
        
        // Limpa áreas anteriores
        painelGeral.setText("");
        arvoreArea.setText("");

        // Salva a saída original do sistema
        PrintStream originalOut = System.out;

        try {
            // 1. Redireciona System.out para a área da árvore
            System.setOut(new PrintStream(new OutputStream() {
                @Override
                public void write(int b) {
                    arvoreArea.append(String.valueOf((char) b));
                }
            }));

            // 2. Prepara o parser com o código da tela
            InputStream is = new ByteArrayInputStream(codigo.getBytes());
            
            // Instancia o parser
            RusskiyCompiler parser = new RusskiyCompiler(is);
            
            // 3. Executa a regra inicial
            parser.Programa();
    
            // 4. Se não deu erro, exibe sucesso no painel esquerdo
            painelGeral.setText("<html><h3 style='color:green; font-family:sans-serif;'>✔ Código aceito com sucesso!</h3>" +
                                "<p>Árvore/Saída gerada no painel à direita.</p></html>");
    
        } catch (ParseException e) {
            painelGeral.setText("<html><h3 style='color:red; font-family:sans-serif;'>Erro Sintático:</h3>" 
                                + "<p>" + e.getMessage() + "</p></html>");
        } catch (TokenMgrError e) {
            painelGeral.setText("<html><h3 style='color:red; font-family:sans-serif;'>Erro Léxico:</h3>" 
                                + "<p>" + e.getMessage() + "</p></html>");
        } catch (Exception e) {
            painelGeral.setText("<html><span style='color:red;'>Erro Inesperado:<br>" 
                                + e.getMessage() + "</span></html>");
            e.printStackTrace(); 
        } finally {
            // Restaura o System.out para não travar o IDE
            System.setOut(originalOut);
        }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
        SwingUtilities.invokeLater(() -> new InterfaceCompilador());
    }
}