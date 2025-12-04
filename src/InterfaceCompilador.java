import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.text.DefaultHighlighter;

public class InterfaceCompilador extends JFrame {

    private JTextArea editorArea;
    private JTextPane consoleArea;
    private JTextArea arvoreArea;
    private JTextArea tokensArea;
    private JTextArea lineNumbers;
    
    private No raizArvore = null;

    public InterfaceCompilador() {
        super("Compilador Russo - IDE");
        setSize(1100, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        // --- BOTOES ---
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        
        JButton btnAbrir = new JButton("Abrir");
        btnAbrir.addActionListener(e -> abrirArquivo());
        
        JButton btnCompilar = new JButton("Compilar e Executar");
        btnCompilar.setBackground(new Color(200, 255, 200));
        btnCompilar.addActionListener(e -> compilarCodigo());
        
        JButton btnGerarC = new JButton("Gerar C");
        btnGerarC.addActionListener(e -> gerarCodigoC());

        toolBar.add(btnAbrir);
        toolBar.addSeparator();
        toolBar.add(btnCompilar);
        toolBar.add(btnGerarC);
        add(toolBar, BorderLayout.NORTH);

        // --- EDITOR ---
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

        // --- ABAS INFERIORES ---
        JTabbedPane tabbedPane = new JTabbedPane();

        consoleArea = new JTextPane();
        consoleArea.setContentType("text/html");
        consoleArea.setEditable(false);
        tabbedPane.addTab("Terminal / Saida", new JScrollPane(consoleArea));

        arvoreArea = new JTextArea();
        arvoreArea.setEditable(false);
        arvoreArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        tabbedPane.addTab("Arvore Sintatica", new JScrollPane(arvoreArea));

        tokensArea = new JTextArea();
        tokensArea.setEditable(false);
        tokensArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        tabbedPane.addTab("Tokens", new JScrollPane(tokensArea));

        JSplitPane splitPrincipal = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollEditor, tabbedPane);
        splitPrincipal.setDividerLocation(450);
        
        add(splitPrincipal, BorderLayout.CENTER);
        setVisible(true);
    }

    private void atualizarLinhas() {
        int lines = editorArea.getLineCount();
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= lines; i++) { sb.append(i).append("\n"); }
        lineNumbers.setText(sb.toString());
    }

    private void abrirArquivo() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Arquivos de Texto", "txt", "rus"));
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                editorArea.setText(Files.readString(fileChooser.getSelectedFile().toPath()));
                atualizarLinhas();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage());
            }
        }
    }

    private void compilarCodigo() {
        String codigo = editorArea.getText();
        limparInterface();
        this.raizArvore = null;

        PrintStream originalOut = System.out;
        ByteArrayOutputStream baosArvore = new ByteArrayOutputStream();
        ByteArrayOutputStream baosExecucao = new ByteArrayOutputStream();

        StringBuilder htmlFinal = new StringBuilder("<html><body style='font-family:Consolas;'>");

        try {
            listarTokens(codigo);

            InputStream is = new ByteArrayInputStream(codigo.getBytes());
            RusskiyCompiler parser = new RusskiyCompiler(is);
            
            System.setOut(new PrintStream(baosArvore)); 
            this.raizArvore = parser.Programa(); 
            
            arvoreArea.setText(baosArvore.toString());

            htmlFinal.append("<h2 style='color:green;'>✔ Compilado com Sucesso!</h2>");
            htmlFinal.append("<hr><b>Saida do Programa:</b><br><br>");

            // --- EXECUCAO ---
            if (this.raizArvore != null) {
                System.setOut(new PrintStream(baosExecucao)); // Captura System.out.println
                
                // Mapa de memória para variáveis
                this.raizArvore.executar(new HashMap<>());
                
                String saidaTexto = baosExecucao.toString();
                if(saidaTexto.isEmpty()) saidaTexto = "(Nenhuma saida de dados)";
                
                htmlFinal.append("<span style='color:blue;'>" + saidaTexto.replace("\n", "<br>") + "</span>");
            }

        } catch (ParseException e) {
            tratarErro(e.getMessage(), true, htmlFinal);
        } catch (TokenMgrError e) {
            tratarErro(e.getMessage(), false, htmlFinal);
        } catch (Exception e) {
            htmlFinal.append("<h3 style='color:red;'>Erro de Execucao:</h3>");
            htmlFinal.append("<p>" + e.getMessage() + "</p>");
        } finally {
            System.setOut(originalOut); 
            htmlFinal.append("</body></html>");
            consoleArea.setText(htmlFinal.toString());
        }
    }

    private void listarTokens(String codigo) {
        StringBuilder sb = new StringBuilder();
        InputStream is = new ByteArrayInputStream(codigo.getBytes());
        RusskiyCompilerTokenManager tm = new RusskiyCompilerTokenManager(new SimpleCharStream(is));
        Token t = tm.getNextToken();
        while (t.kind != RusskiyCompilerConstants.EOF) {
            String nomeToken = RusskiyCompilerConstants.tokenImage[t.kind].replace("\"", "");
            sb.append(String.format("L:%d | %s -> %s\n", t.beginLine, nomeToken, t.image));
            t = tm.getNextToken();
        }
        tokensArea.setText(sb.toString());
    }

    private void tratarErro(String msgOriginal, boolean isSintatico, StringBuilder html) {
        // Regex poderosa para achar a linha em qualquer formato de erro
        // Procura por "line X" ou "linha X" (case insensitive)
        int linhaErro = -1;
        Pattern p = Pattern.compile("(line|linha)\\s*:?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(msgOriginal);
        if (m.find()) {
            linhaErro = Integer.parseInt(m.group(2));
            realcarLinhaErro(linhaErro);
        }

        String msgTraduzida = msgOriginal
                .replace("Encountered", "Encontrado")
                .replace("at line", "na linha")
                .replace("column", "coluna")
                .replace("Expected:", "Esperado:")
                .replace("Lexical error", "Erro Lexico")
                .replace("Was expecting one of:", "Esperava um destes:");

        String titulo = isSintatico ? "❌ Erro Sintatico / Semantico" : "❌ Erro Lexico";
        html.append("<h3 style='color:red;'>" + titulo + "</h3>");
        html.append("<p>" + msgTraduzida + "</p>");
        
        if (linhaErro != -1) {
            html.append("<p><b>Erro detectado na linha: " + linhaErro + "</b></p>");
        }
    }

    private void realcarLinhaErro(int linha) {
        try {
            // Swing conta linhas a partir do 0, JavaCC a partir do 1
            int start = editorArea.getLineStartOffset(linha - 1);
            int end = editorArea.getLineEndOffset(linha - 1);
            editorArea.getHighlighter().addHighlight(start, end, 
                new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 200, 200)));
        } catch (Exception e) {
            // Se a linha não existir (ex: erro no EOF), ignora o highlight
        }
    }
    
    private void gerarCodigoC() {
        if (this.raizArvore == null) {
            JOptionPane.showMessageDialog(this, "Voce precisa compilar com sucesso primeiro!");
            return;
        }
        try {
            String codigoC = this.raizArvore.gerarC();
            File arquivoSaida = new File("saida.c");
            Files.writeString(arquivoSaida.toPath(), codigoC);
            JOptionPane.showMessageDialog(this, "Arquivo 'saida.c' gerado com sucesso!");
            
            String htmlC = "<html><h3 style='color:blue'>Codigo C Gerado:</h3><pre>" + codigoC + "</pre></html>";
            consoleArea.setText(htmlC);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar arquivo: " + e.getMessage());
        }
    }

    private void limparInterface() {
        consoleArea.setText("");
        arvoreArea.setText("");
        tokensArea.setText("");
        editorArea.getHighlighter().removeAllHighlights();
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
        SwingUtilities.invokeLater(InterfaceCompilador::new);
    }
}