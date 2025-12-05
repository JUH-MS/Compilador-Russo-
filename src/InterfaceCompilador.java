import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder; // Para a borda bonita
import javax.swing.event.*;
import javax.swing.text.DefaultHighlighter;

public class InterfaceCompilador extends JFrame {

    private JTextArea editorArea;
    private JTextArea saidaArea;    // Terminal (apenas leitura)
    private JTextField campoEntrada; // Campo onde você digita
    private JTextArea arvoreArea;
    private JTextArea tokensArea;
    private JTextArea lineNumbers;
    private File arquivoAtual = null;
    
    private No raizArvore = null;
    
    // Controle de Thread
    private static String bufferEntrada = null;
    private static final Object lock = new Object();

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
        
        JButton btnSalvar = new JButton("Salvar");
        btnSalvar.addActionListener(e -> salvarArquivo());

        JButton btnGerarC = new JButton("Gerar C");
        btnGerarC.addActionListener(e -> gerarCodigoC());

        toolBar.add(btnAbrir);
        toolBar.addSeparator();
        toolBar.add(btnCompilar);
        toolBar.add(btnSalvar);
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

        // 1. ABA TERMINAL
        JPanel painelTerminal = new JPanel(new BorderLayout());
        
        // Área de Saída (Onde aparece o texto)
        saidaArea = new JTextArea();
        saidaArea.setEditable(false); // O usuário NÃO pode digitar aqui
        saidaArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        saidaArea.setBackground(Color.WHITE); 
        saidaArea.setForeground(Color.BLACK);
        
        // Campo de Entrada (Onde o usuário DEVE digitar)
        campoEntrada = new JTextField();
        campoEntrada.setFont(new Font("Consolas", Font.BOLD, 14));
        campoEntrada.setBackground(new Color(230, 230, 230)); // Cinza quando desativado
        campoEntrada.setForeground(Color.BLACK);
        campoEntrada.setEnabled(false); // Começa travado
        campoEntrada.setBorder(new TitledBorder("Entrada de Dados (Digite aqui quando solicitado)"));
        
        campoEntrada.addActionListener(e -> enviarEntrada()); // Funciona ao dar Enter

        painelTerminal.add(new JScrollPane(saidaArea), BorderLayout.CENTER);
        painelTerminal.add(campoEntrada, BorderLayout.SOUTH);
        
        tabbedPane.addTab("Terminal / Execucao", painelTerminal);

        // 2. ABA ARVORE
        arvoreArea = new JTextArea();
        arvoreArea.setEditable(false);
        arvoreArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        tabbedPane.addTab("Arvore Sintatica", new JScrollPane(arvoreArea));

        // 3. ABA TOKENS
        tokensArea = new JTextArea();
        tokensArea.setEditable(false);
        tokensArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        tabbedPane.addTab("Tokens", new JScrollPane(tokensArea));

        JSplitPane splitPrincipal = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollEditor, tabbedPane);
        splitPrincipal.setDividerLocation(450);
        
        add(splitPrincipal, BorderLayout.CENTER);
        setVisible(true);
    }

    // --- LÓGICA DE INPUT ---
    private void enviarEntrada() {
        synchronized (lock) {
            bufferEntrada = campoEntrada.getText();
            
            // Mostra o que você digitou na tela de cima para manter histórico
            saidaArea.append(bufferEntrada + "\n");
            saidaArea.setCaretPosition(saidaArea.getDocument().getLength());

            campoEntrada.setText("");
            campoEntrada.setEnabled(false); // Trava de novo
            campoEntrada.setBackground(new Color(230, 230, 230)); // Volta pra cinza
            
            lock.notify(); // Destrava o interpretador
        }
    }

    public static String lerEntradaUsuario() {
        // Usa invokeLater para mexer na interface com segurança
        SwingUtilities.invokeLater(() -> {
            JFrame frame = (JFrame) Frame.getFrames()[0];
            if(frame instanceof InterfaceCompilador) {
                InterfaceCompilador gui = (InterfaceCompilador) frame;
                
                // Ativa o campo e muda a cor para AMARELO
                gui.campoEntrada.setEnabled(true);
                gui.campoEntrada.setBackground(new Color(255, 255, 200)); // Amarelo claro
                gui.campoEntrada.requestFocus(); // Joga o foco lá
            }
        });

        // Pausa a thread do interpretador até o usuário dar Enter
        synchronized (lock) {
            try { lock.wait(); } catch (InterruptedException e) {}
        }
        return bufferEntrada;
    }

    // --- COMPILAÇÃO E EXECUÇÃO ---
    private void compilarCodigo() {
        String codigo = editorArea.getText();
        limparInterface();
        this.raizArvore = null;

        if (codigo.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Digite algum codigo para compilar.");
            return;
        }

        // Thread separada para a execução (Isso evita que a interface congele)
        new Thread(() -> {
            ByteArrayOutputStream baosArvore = new ByteArrayOutputStream();
            try {
                // 1. Tokens
                listarTokens(codigo);

                // 2. Parser
                InputStream is = new ByteArrayInputStream(codigo.getBytes());
                RusskiyCompiler parser = new RusskiyCompiler(is);
                
                // Captura a árvore
                PrintStream oldOut = System.out;
                System.setOut(new PrintStream(baosArvore));
                this.raizArvore = parser.Programa();
                System.setOut(oldOut);
                
                // Atualiza a árvore na aba correta (Thread-safe)
                SwingUtilities.invokeLater(() -> arvoreArea.setText(baosArvore.toString()));

                // 3. Execução
                if (this.raizArvore != null) {
                    safeAppend("--- INICIO DA EXECUCAO ---\n");
                    
                    // Redireciona o System.out do interpretador para o JTextArea de forma SEGURA
                    PrintStream printTerminal = new PrintStream(new OutputStream() {
                        @Override
                        public void write(int b) throws IOException {
                            // Atualiza a interface na thread correta
                            SwingUtilities.invokeLater(() -> {
                                saidaArea.append(String.valueOf((char) b));
                                saidaArea.setCaretPosition(saidaArea.getDocument().getLength());
                            });
                        }
                    });
                    System.setOut(printTerminal);

                    this.raizArvore.executar(new HashMap<>());
                    
                    System.setOut(oldOut); // Restaura o console
                    safeAppend("\n--- SUCESSO ---\n");
                }

            } catch (ParseException e) {
                tratarErro(e.getMessage(), true);
            } catch (TokenMgrError e) {
                tratarErro(e.getMessage(), false);
            } catch (Exception e) {
                safeAppend("\n[ERRO]: " + e.getMessage() + "\n");
            }
        }).start();
    }

    // Método auxiliar para escrever no terminal sem travar
    private void safeAppend(String text) {
        SwingUtilities.invokeLater(() -> {
            saidaArea.append(text);
            saidaArea.setCaretPosition(saidaArea.getDocument().getLength());
        });
    }

    private void salvarArquivo() {
        try {
            if (arquivoAtual == null) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Arquivos de Texto", "txt", "rus"));
                if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    arquivoAtual = fileChooser.getSelectedFile();
                    if (!arquivoAtual.getName().contains(".")) 
                        arquivoAtual = new File(arquivoAtual.getAbsolutePath() + ".rus");
                } else return;
            }
            Files.writeString(arquivoAtual.toPath(), editorArea.getText());
            JOptionPane.showMessageDialog(this, "Salvo com sucesso!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar: " + e.getMessage());
        }
    }

    private void listarTokens(String codigo) {
        // Executa na thread principal pois é rápido
        SwingUtilities.invokeLater(() -> {
            try {
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
            } catch(Exception e) {}
        });
    }

    private void tratarErro(String msgOriginal, boolean isSintatico) {
        SwingUtilities.invokeLater(() -> {
            String msgTraduzida = msgOriginal
                    .replace("Encountered", "Encontrado")
                    .replace("at line", "na linha")
                    .replace("column", "coluna")
                    .replace("Expected:", "Esperado:")
                    .replace("Lexical error", "Erro Lexico")
                    .replace("Was expecting one of:", "Esperava um destes:");

            int linhaErro = 1;
            Pattern p = Pattern.compile("linha (\\d+)", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(msgTraduzida);
            if (m.find()) {
                linhaErro = Integer.parseInt(m.group(1));
                realcarLinhaErro(linhaErro);
            }
            
            saidaArea.setForeground(Color.RED);
            saidaArea.append("\n[ERRO]: " + msgTraduzida + "\n");
        });
    }

    private void realcarLinhaErro(int linha) {
        try {
            int start = editorArea.getLineStartOffset(linha - 1);
            int end = editorArea.getLineEndOffset(linha - 1);
            editorArea.getHighlighter().addHighlight(start, end, 
                new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 200, 200)));
        } catch (Exception e) {}
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
            
            // Mostra o código gerado no terminal também
            safeAppend("\n--- CODIGO C GERADO ---\n" + codigoC + "\n-----------------------\n");
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar arquivo: " + e.getMessage());
        }
    }

    private void atualizarLinhas() {
        int lines = editorArea.getLineCount();
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= lines; i++) { sb.append(i).append("\n"); }
        lineNumbers.setText(sb.toString());
    }

    private void abrirArquivo() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                arquivoAtual = fileChooser.getSelectedFile();
                editorArea.setText(Files.readString(arquivoAtual.toPath()));
            } catch (Exception ex) {}
        }
    }

    private void limparInterface() {
        saidaArea.setText("");
        saidaArea.setForeground(Color.BLACK); // Volta para preto (pois o fundo é branco)
        arvoreArea.setText("");
        tokensArea.setText("");
        editorArea.getHighlighter().removeAllHighlights();
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
        SwingUtilities.invokeLater(InterfaceCompilador::new);
    }
}