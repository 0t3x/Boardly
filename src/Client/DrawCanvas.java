package Client;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.*;

public class DrawCanvas extends JPanel {

    private PrintWriter out;
    private List<DrawAction> actions = new CopyOnWriteArrayList<>();
    private DrawAction current = null;

    public Color color = Color.BLACK;
    public int stroke = 3;
    public boolean fill = false;
    public String shape = "free";
    private boolean isEraser = false;

    private String clientId;
    private Point startPoint;
    private Point lastPoint;
    private double zoom = 1.0;
    private Point offset = new Point(0, 0);

    private Timer smoothTimer; //--

    public DrawCanvas(PrintWriter out, String clientId) {
        this.out = out;
        this.clientId = clientId;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(1200, 800));

        // inp
        smoothTimer = new Timer(50, e -> {
            if (current != null && shape.equals("free")) {
                send(current.serialize());
            }
        });

        MouseAdapter adapter = new MouseAdapter() {
            //adapter
            private Point dragStart;

            public void mousePressed(MouseEvent e) {
                if (
                    e.isControlDown() || SwingUtilities.isMiddleMouseButton(e)
                ) {
                    dragStart = e.getPoint();
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    return;
                }

                startPoint = screenToCanvas(e.getPoint());
                lastPoint = startPoint;

                if (shape.equals("text")) {
                    String input = JOptionPane.showInputDialog("Enter text:"); //default dialogue 1/3
                    if (input != null && !input.trim().isEmpty()) {
                        current = new DrawAction(
                            color,
                            stroke,
                            fill,
                            "text",
                            clientId
                        ); //options
                        current.text = input;
                        current.addPoint(startPoint);
                        actions.add(current);
                        send(current.serialize());
                        current = null;
                        repaint();
                    }
                    return;
                }

                Color drawColor = isEraser ? Color.WHITE : color;
                current = new DrawAction(
                    drawColor,
                    stroke,
                    fill,
                    shape,
                    clientId
                );
                if (shape.equals("free")) {
                    current.addPoint(startPoint);
                    smoothTimer.start();
                }
                repaint();
            }

            public void mouseDragged(MouseEvent e) {
                if (dragStart != null) {
                    Point now = e.getPoint();
                    offset.x += now.x - dragStart.x;
                    offset.y += now.y - dragStart.y;
                    dragStart = now;
                    repaint();
                    return;
                }

                if (current == null) return;

                Point p = screenToCanvas(e.getPoint());

                if (shape.equals("free")) {
                    double dist = p.distance(lastPoint);
                    if (dist > 2) {
                        current.addPoint(p);
                        lastPoint = p;
                    }
                } else {
                    current.points.clear();
                    current.addPoint(startPoint);
                    current.addPoint(p);
                }
                repaint();
            }

            public void mouseReleased(MouseEvent e) {
                if (dragStart != null) {
                    dragStart = null;
                    setCursor(Cursor.getDefaultCursor());
                    return;
                }

                if (current == null) return;

                smoothTimer.stop();

                Point p = screenToCanvas(e.getPoint());

                if (!shape.equals("free")) {
                    current.points.clear();
                    current.addPoint(startPoint);
                    current.addPoint(p);
                }

                actions.add(current);
                send(current.serialize());
                current = null;
                repaint();
            }

            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.isControlDown()) {
                    double oldZoom = zoom;
                    Point mouse = e.getPoint();

                    if (e.getWheelRotation() < 0) {
                        zoom = Math.min(3.0, zoom * 1.1);
                    } else {
                        zoom = Math.max(0.3, zoom / 1.1);
                    }

                    offset.x = (int) (mouse.x -
                        ((mouse.x - offset.x) * zoom) / oldZoom);
                    offset.y = (int) (mouse.y -
                        ((mouse.y - offset.y) * zoom) / oldZoom);

                    repaint();
                    e.consume();
                }
            }
        };

        addMouseListener(adapter);
        addMouseMotionListener(adapter);
        addMouseWheelListener(adapter);
    }

    private Point screenToCanvas(Point screen) {
        int x = (int) ((screen.x - offset.x) / zoom);
        int y = (int) ((screen.y - offset.y) / zoom);
        return new Point(x, y);
    }

    private void send(String msg) {
        if (out != null) {
            out.println(msg);
            out.flush();
        }
    }

    public void receive(String msg) {
        SwingUtilities.invokeLater(() -> {
            if (msg.equals("CLEAR")) {
                actions.clear();
            } else if (msg.startsWith("CLEARMY|")) {
                String id = msg.substring(8);
                actions.removeIf(a -> a.clientId.equals(id));
            } else {
                DrawAction action = DrawAction.deserialize(msg);
                if (action != null) {
                    actions.add(action);
                }
            }
            repaint();
        });
    }

    public void setEraser(boolean e) {
        isEraser = e;
        if (e) shape = "free";
    }

    public void zoomIn() {
        zoom = Math.min(3.0, zoom * 1.2);
        repaint();
    }

    public void zoomOut() {
        zoom = Math.max(0.3, zoom / 1.2);
        repaint();
    }

    public void resetZoom() {
        zoom = 1.0;
        offset = new Point(0, 0);
        repaint();
    }

    public void clearMy() {
        SwingUtilities.invokeLater(() -> {
            actions.removeIf(a -> a.clientId.equals(clientId));
            send("CLEARMY|" + clientId);
            repaint();
        });
    }

    public void undo() {
        SwingUtilities.invokeLater(() -> {
            for (int i = actions.size() - 1; i >= 0; i--) {
                if (actions.get(i).clientId.equals(clientId)) {
                    actions.remove(i);
                    break;
                }
            }
            repaint();
        });
    }

    public List<String> getAllDrawings() {
        java.util.List<String> drawings = new java.util.ArrayList<>();
        for (DrawAction a : actions) {
            drawings.add(a.serialize());
        }
        return drawings;
    }

    public void loadDrawings(List<String> drawings) {
        SwingUtilities.invokeLater(() -> {
            actions.clear();
            for (String d : drawings) {
                DrawAction a = DrawAction.deserialize(d);
                if (a != null) actions.add(a);
            }
            repaint();
        });
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );
        g2.setRenderingHint(
            RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_QUALITY
        );

        AffineTransform original = g2.getTransform();
        g2.translate(offset.x, offset.y);
        g2.scale(zoom, zoom);

        for (DrawAction a : actions) {
            a.draw(g2);
        }

        if (current != null) {
            current.draw(g2);
        }

        g2.setTransform(original);

        //--
        Graphics2D hud = (Graphics2D) g2.create(); //rewored
        hud.setColor(Color.GRAY);
        hud.setFont(getFont().deriveFont(Font.PLAIN, 12f));
        hud.drawString(String.format("Zoom: %.0f%%", zoom * 100), 10, 20);
        hud.dispose();
    }

    //--
    //
    //
    //
}
