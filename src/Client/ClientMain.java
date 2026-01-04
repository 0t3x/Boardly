package Client;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class ClientMain extends JFrame {

    private DrawCanvas canvas;
    private PrintWriter out;
    private Socket socket;
    private String clientId;
    private Map<JButton, String> buttonMap = new HashMap<>();

    private JButton freeBtn, lineBtn, rectBtn, circleBtn, textBtn, eraserBtn;
    private JButton colorBtn, fillBtn;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientMain());
    }

    public ClientMain() {
        String username = JOptionPane.showInputDialog("Enter username:");
        if (username == null || username.trim().isEmpty()) {
            username = "User" + (int) (Math.random() * 999);
        }

        clientId = username + "_" + System.currentTimeMillis();

        if (!connect(username)) {
            System.exit(0);
        }

        createUI();
    }

    //--
    private boolean connect(String username) {
        try {
            socket = new Socket("localhost", 12345); //on client change local host with host ip for demo
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println("JOIN|" + username);

            new Thread(() -> listen()).start();
            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Cannot connect to server!");
            return false;
        }
    }

    //--
    private void listen() {
        try {
            BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
            );
            String msg;
            while ((msg = in.readLine()) != null) {
                if (msg.equals("BANNED")) {
                    JOptionPane.showMessageDialog(this, "You are banned!");
                    System.exit(0);
                } else if (msg.equals("KICKED")) {
                    JOptionPane.showMessageDialog(this, "You were kicked!");
                    socket.close();
                } else if (msg.startsWith("CANVASLIST|")) {
                    showLoadDialog(msg.substring(11));
                } else {
                    canvas.receive(msg);
                }
            }
        } catch (Exception e) {}
    }

    private void createUI() {

        setTitle("📝|Boardly - " + clientId.split("_")[0]);
        setSize(1100, 750);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        canvas = new DrawCanvas(out, clientId);
        JScrollPane scroll = new JScrollPane(canvas);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 3));
        toolbar.setBackground(new Color(50, 50, 50));

        //button
        freeBtn = createButton("Free");
        lineBtn = createButton("Line");
        rectBtn = createButton("Rect");
        circleBtn = createButton("Circle");
        textBtn = createButton("Text");
        eraserBtn = createButton("Eraser");
        colorBtn = createButton("Color");
        fillBtn = createButton("Fill: OFF");

        //shape button
        toolbar.add(freeBtn);
        toolbar.add(lineBtn);
        toolbar.add(rectBtn);
        toolbar.add(circleBtn);
        toolbar.add(textBtn);
        toolbar.add(separator());

        //tool button
        toolbar.add(colorBtn);
        toolbar.add(eraserBtn);
        toolbar.add(fillBtn);

        //stroke  3 for now
        JLabel strokeLabel = new JLabel("Stroke:");
        strokeLabel.setForeground(Color.WHITE);
        toolbar.add(strokeLabel);

        JComboBox<Integer> strokeBox = new JComboBox<>(
            new Integer[] { 1, 2, 3, 5, 7, 9, 12, 15, 18, 20 }
        );
        strokeBox.setSelectedItem(3);
        strokeBox.addActionListener(e ->
            canvas.stroke = (Integer) strokeBox.getSelectedItem()
        );
        toolbar.add(strokeBox);
        toolbar.add(separator());

        //zoom buttons added 11/29
        toolbar.add(createButton("Zoom+"));
        toolbar.add(createButton("Zoom-"));
        toolbar.add(createButton("Reset"));
        toolbar.add(createButton("Clear My")); //onyl clients work clear
        toolbar.add(createButton("Undo"));
        toolbar.add(separator());

        //file button
        toolbar.add(createButton("Save"));
        toolbar.add(createButton("Open"));

        //connect button click
        freeBtn.addActionListener(e -> selectShape("free", freeBtn));
        lineBtn.addActionListener(e -> selectShape("line", lineBtn));
        rectBtn.addActionListener(e -> selectShape("rect", rectBtn));
        circleBtn.addActionListener(e -> selectShape("circle", circleBtn));

        textBtn.addActionListener(e -> selectShape("text", textBtn)); //

        eraserBtn.addActionListener(e -> selectEraser());

        colorBtn.setBackground(Color.BLACK);
        colorBtn.addActionListener(e -> selectColor());

        fillBtn.addActionListener(e -> toggleFill());

        add(toolbar, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        //keyboard keys  shortcut
        getRootPane()
            .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(
                KeyStroke.getKeyStroke(
                    KeyEvent.VK_Z,
                    InputEvent.CTRL_DOWN_MASK
                ),
                "undo"
            );
        getRootPane()
            .getActionMap()
            .put(
                "undo",
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        canvas.undo();
                    }
                }
            );

        selectShape("free", freeBtn);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    //text button

    private JButton createButton(String text) {
        JButton btn = new JButton(text);
        btn.setFocusable(false);
        btn.setBackground(new Color(80, 80, 80));
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.PLAIN, 11));
        btn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        if (text.equals("Zoom+")) btn.addActionListener(e -> canvas.zoomIn());
        else if (text.equals("Zoom-")) btn.addActionListener(e ->
            canvas.zoomOut()
        );
        else if (text.equals("Reset")) btn.addActionListener(e ->
            canvas.resetZoom()
        );
        else if (text.equals("Clear My")) btn.addActionListener(e -> {
            if (
                JOptionPane.showConfirmDialog(
                    this,
                    "clear all your drawings?",
                    "Confirm",
                    JOptionPane.YES_NO_OPTION
                ) ==
                0
            ) {
                canvas.clearMy();
            }
        });
        else if (text.equals("Undo")) btn.addActionListener(e -> canvas.undo());
        else if (text.equals("Save")) btn.addActionListener(e -> saveFile());
        else if (text.equals("Open")) btn.addActionListener(e -> openFile());

        return btn;
    }

    private void selectShape(String shapeName, JButton btn) {
        canvas.shape = shapeName;
        canvas.setEraser(false);
        updateButtonStates(btn);
    }

    private void selectEraser() {
        canvas.setEraser(true);
        updateButtonStates(eraserBtn);
    }

    private void selectColor() {
        Color c = JColorChooser.showDialog(this, "pick color", canvas.color);

        if (c != null) {
            canvas.color = c;
            colorBtn.setBackground(c); //fixed
        }
    }

    private void toggleFill() {
        canvas.fill = !canvas.fill;
        fillBtn.setText(canvas.fill ? "Fill: ON" : "Fill: OFF"); //toggle
        fillBtn.setBackground(
            canvas.fill ? new Color(0, 150, 0) : new Color(80, 80, 80)
        );
    }

    private void updateButtonStates(JButton selected) {
        JButton[] shapeButtons = {
            freeBtn,
            lineBtn,
            rectBtn,
            circleBtn,
            textBtn,
            eraserBtn,
        };
        for (JButton btn : shapeButtons) {
            if (btn == selected) {
                btn.setBackground(new Color(0, 120, 215));
            } else {
                btn.setBackground(new Color(80, 80, 80));
            }
        }
    }

    private Component separator() {
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(2, 30));
        return sep;
    }

    private void saveFile() {
        //file sysmade 11/9
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(
            new FileNameExtensionFilter("Boardly Files (*.boardly)", "boardly")
        ); //.boardly

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = chooser.getSelectedFile();
                if (!file.getName().endsWith(".boardly")) {
                    file = new File(file.getAbsolutePath() + ".boardly");
                }

                PrintWriter writer = new PrintWriter(new FileWriter(file));
                for (String drawing : canvas.getAllDrawings()) {
                    writer.println(drawing);
                }
                writer.close();

                JOptionPane.showMessageDialog(this, "Saved!");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }

    private void openFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(
            new FileNameExtensionFilter("Boardly Files (*.boardly)", "boardly")
        ); //.boardly

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                BufferedReader reader = new BufferedReader(
                    new FileReader(chooser.getSelectedFile())
                );
                java.util.List<String> drawings = new java.util.ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    drawings.add(line);
                }
                reader.close();

                canvas.loadDrawings(drawings);

                for (String drawing : drawings) {
                    out.println(drawing);
                }

                JOptionPane.showMessageDialog(
                    this,
                    "File content loaded Successfully"
                );
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }

    private void showLoadDialog(String list) {
        //load
        String[] names = list.split(",");
        if (names.length == 0 || names[0].isEmpty()) {
            JOptionPane.showMessageDialog(this, "wheres canvas?");
            return;
        }

        String selected = (String) JOptionPane.showInputDialog(
            this,
            "Select canvas:",
            "Load",
            JOptionPane.QUESTION_MESSAGE,
            null,
            names,
            names[0]
        );

        if (selected != null) {
            out.println("LOAD|" + selected);
        }
    }
}
