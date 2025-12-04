import java.awt.*;
import java.io.*;
import java.nio.file.Files;
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
        setSize(1000, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        // --- BOTOES ---
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        
        JButton btnAbrir = new JButton("Abrir");
        btnAbrir.addActionListener(e -> abrirArquivo());
        
        JButton btnCompilar = new JButton("Compilar");
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

        // --- ABAS ---
        JTabbedPane tabbedPane = new JTabbedPane();

        consoleArea = new JTextPane();
        consoleArea.setContentType("text/html");
        consoleArea.setEditable(false);
        tabbedPane.addTab("Console / Erros", new JScrollPane(consoleArea));

        arvoreArea = new JTextArea();
        arvoreArea.setEditable(false);
        arvoreArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        tabbedPane.addTab("Arvore Sintatica", new JScrollPane(arvoreArea));

        tokensArea = new JTextArea();
        tokensArea.setEditable(false);
        tokensArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        tabbedPane.addTab("Lista de Tokens", new JScrollPane(tokensArea));

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
                File selectedFile = fileChooser.getSelectedFile();
                String content = Files.readString(selectedFile.toPath());
                editorArea.setText(content);
                atualizarLinhas();
                consoleArea.setText("<html><font color='blue'>Arquivo carregado: " + selectedFile.getName() + "</font></html>");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro ao ler arquivo: " + ex.getMessage());
            }
        }
    }

    private void compilarCodigo() {
        String codigo = editorArea.getText();
        limparInterface();
        this.raizArvore = null; 
        PrintStream originalOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));

        try {

            listarTokens(codigo);

            InputStream is = new ByteArrayInputStream(codigo.getBytes());
            RusskiyCompiler parser = new RusskiyCompiler(is);
            
            
            this.raizArvore = parser.Programa(); 

            
            arvoreArea.setText(baos.toString()); 
            consoleArea.setText("<html><h3 style='color:green'>Compilacao com Sucesso!</h3></html>");

        } catch (ParseException e) {
            tratarErro(e.getMessage(), true);
        } catch (TokenMgrError e) {
            tratarErro(e.getMessage(), false);
        } catch (Exception e) {
            consoleArea.setText("<html><font color='red'>Erro Fatal: " + e.getMessage() + "</font></html>");
            e.printStackTrace();
        } finally {
            System.setOut(originalOut); 
        }
    }

    private void listarTokens(String codigo) {
        StringBuilder sb = new StringBuilder();
        
        InputStream is = new ByteArrayInputStream(codigo.getBytes());
        RusskiyCompilerTokenManager tm = new RusskiyCompilerTokenManager(new SimpleCharStream(is));
        
        Token t = tm.getNextToken();
        while (t.kind != RusskiyCompilerConstants.EOF) {
            String nomeToken = RusskiyCompilerConstants.tokenImage[t.kind];
            nomeToken = nomeToken.replace("\"", ""); 
            
            sb.append(String.format("L:%d | %s -> %s\n", t.beginLine, nomeToken, t.image));
            t = tm.getNextToken();
        }
        tokensArea.setText(sb.toString());
    }

    private void tratarErro(String msgOriginal, boolean isSintatico) {
        
        String msgTraduzida = msgOriginal
                .replace("Encountered", "Encontrado")
                .replace("at line", "na linha")
                .replace("column", "coluna")
                .replace("Expected:", "Esperado:")
                .replace("Lexical error", "Erro Lexico")
                .replace("Was expecting one of:", "Era esperado um destes:");

       
        int linhaErro = 1;
        Pattern p = Pattern.compile("linha (\\d+)");
        Matcher m = p.matcher(msgTraduzida);
        if (m.find()) {
            linhaErro = Integer.parseInt(m.group(1));
        }

        realcarLinhaErro(linhaErro);

        String tipo = isSintatico ? "Erro Sintatico" : "Erro Lexico";
        consoleArea.setText("<html><h3 style='color:red'>" + tipo + "</h3><p>" + msgTraduzida + "</p></html>");
    }

    private void realcarLinhaErro(int linha) {
        try {
            int start = editorArea.getLineStartOffset(linha - 1);
            int end = editorArea.getLineEndOffset(linha - 1);
            DefaultHighlighter.DefaultHighlightPainter painter = 
                new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 200, 200));
            editorArea.getHighlighter().addHighlight(start, end, painter);
        } catch (Exception e) {
            
        }
    }
    
    private void gerarCodigoC() {
        if (this.raizArvore == null) {
            JOptionPane.showMessageDialog(this, "Voce precisa compilar o codigo com sucesso primeiro!");
            return;
        }
        try {
            
            String codigoC = this.raizArvore.gerarC();

           
            File arquivoSaida = new File("saida.c");
            Files.writeString(arquivoSaida.toPath(), codigoC);

            JOptionPane.showMessageDialog(this, "Arquivo 'saida.c' gerado com sucesso na pasta do projeto!");
            
            
            consoleArea.setText("<html><h3 style='color:blue'>Codigo C Gerado com Sucesso!</h3><pre>" + codigoC + "</pre></html>");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar arquivo C: " + e.getMessage());
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