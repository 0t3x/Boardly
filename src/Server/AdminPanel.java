package Server;

import java.awt.*;
import java.net.InetAddress;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class AdminPanel extends JFrame {

    private ServerMain server;
    private DefaultTableModel userModel;
    private DefaultTableModel logModel;

    public AdminPanel(ServerMain server) {
        this.server = server;

        setTitle("Server Admin");
        setSize(500, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(5, 5));

        JPanel info = new JPanel(new GridLayout(2, 1));
        info.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            info.add(new JLabel("Host IP: " + ip));
        } catch (Exception e) {
            info.add(new JLabel("Host IP: localhost")); //--
        }
        info.add(new JLabel("Port: 12345"));

        userModel = new DefaultTableModel(new String[] { "Users" }, 0); //def
        JTable userTable = new JTable(userModel);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane userScroll = new JScrollPane(userTable);

        userScroll.setBorder(
            BorderFactory.createTitledBorder("Connected Users")
        );
        userScroll.setPreferredSize(new Dimension(0, 120));

        logModel = new DefaultTableModel(new String[] { "Logs" }, 0);
        JTable logTable = new JTable(logModel);
        JScrollPane logScroll = new JScrollPane(logTable);
        logScroll.setBorder(BorderFactory.createTitledBorder("Activity"));

        // the split
        JSplitPane split = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT,
            userScroll,
            logScroll
        );
        split.setResizeWeight(0.4);

        JPanel buttons = new JPanel(new FlowLayout());
        JButton kick = new JButton("Kick");
        JButton ban = new JButton("Ban");

        //--

        kick.addActionListener(e -> {
            int row = userTable.getSelectedRow();
            if (row >= 0) {
                String user = (String) userModel.getValueAt(row, 0);
                server.kick(user);
                log("you just kicked: " + user);
            }
        });
        //ban
        ban.addActionListener(e -> {
            int row = userTable.getSelectedRow();
            if (row >= 0) {
                String user = (String) userModel.getValueAt(row, 0);
                if (
                    JOptionPane.showConfirmDialog(
                        this,
                        "really Ban " + user + "?"
                    ) ==
                    0
                ) {
                    server.ban(user);
                    log("you banned: " + user);
                }
            }
        });

        buttons.add(kick);
        buttons.add(ban);

        add(info, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        new Timer(2000, e -> updateUsers()).start();

        log("server started");
        log("use IP AND PORT ON OTHER DEVICE TO ENTER");
        setLocationRelativeTo(null);
    }

    public void updateUsers() {
        userModel.setRowCount(0);
        for (String user : server.getUsers()) {
            userModel.addRow(new Object[] { user });
        }
    }

    public void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logModel.insertRow(0, new Object[] { msg });
            if (logModel.getRowCount() > 50) {
                logModel.removeRow(50);
            }
        });
    }
}
