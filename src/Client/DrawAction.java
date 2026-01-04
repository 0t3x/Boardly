package Client;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DrawAction {

    public Color color;
    public int stroke;
    public boolean fill;
    public String shape;
    public List<Point> points = new ArrayList<>();
    public String text = "";
    public String clientId = "";

    public DrawAction(Color c, int s, boolean f, String sh, String id) {
        color = c;
        stroke = s;
        fill = f;
        shape = sh;
        clientId = id;
    }

    public void addPoint(Point p) {
        points.add(p);
    }

    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(color.getRGB()).append("|");
        sb.append(stroke).append("|");
        sb.append(fill).append("|");
        sb.append(shape).append("|");
        sb.append(text.replace("|", "")).append("|");
        sb.append(clientId).append("|");
        for (Point p : points) {
            sb.append(p.x).append(",").append(p.y).append(";");
        }
        return sb.toString();
    }

    public static DrawAction deserialize(String data) {
        try {
            String[] parts = data.split("\\|");
            Color c = new Color(Integer.parseInt(parts[0]));
            int s = Integer.parseInt(parts[1]);
            boolean f = Boolean.parseBoolean(parts[2]);
            String shape = parts[3];

            DrawAction action = new DrawAction(c, s, f, shape, "");
            action.text = parts[4];
            action.clientId = parts.length > 5 ? parts[5] : "";

            if (parts.length > 6) {
                String[] coords = parts[6].split(";");
                for (String coord : coords) {
                    if (!coord.isEmpty()) {
                        String[] xy = coord.split(",");
                        action.addPoint(
                            new Point(
                                Integer.parseInt(xy[0]),
                                Integer.parseInt(xy[1])
                            )
                        );
                    }
                }
            }
            return action;
        } catch (Exception e) {
            return null;
        }
    }

    public void draw(Graphics2D g) {
        g.setColor(color);
        g.setStroke(
            new BasicStroke(
                stroke,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND
            )
        );

        if (points.isEmpty()) return;

        switch (shape) {
            case "free":
                for (int i = 0; i < points.size() - 1; i++) {
                    g.drawLine(
                        points.get(i).x,
                        points.get(i).y,
                        points.get(i + 1).x,
                        points.get(i + 1).y
                    );
                }
                break;
            case "line":
                if (points.size() >= 2) {
                    Point p1 = points.get(0);
                    Point p2 = points.get(points.size() - 1);
                    g.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
                break;
            case "rect":
                if (points.size() >= 2) {
                    Point p1 = points.get(0);
                    Point p2 = points.get(points.size() - 1);
                    int x = Math.min(p1.x, p2.x);
                    int y = Math.min(p1.y, p2.y);
                    int w = Math.abs(p2.x - p1.x);
                    int h = Math.abs(p2.y - p1.y);
                    if (fill) g.fillRect(x, y, w, h);
                    else g.drawRect(x, y, w, h);
                }
                break;
            case "circle":
                if (points.size() >= 2) {
                    Point p1 = points.get(0);
                    Point p2 = points.get(points.size() - 1);
                    int x = Math.min(p1.x, p2.x);
                    int y = Math.min(p1.y, p2.y);
                    int w = Math.abs(p2.x - p1.x);
                    int h = Math.abs(p2.y - p1.y);
                    if (fill) g.fillOval(x, y, w, h);
                    else g.drawOval(x, y, w, h);
                }
                break;
            case "text":
                if (!points.isEmpty() && !text.isEmpty()) {
                    Font font = new Font("Arial", Font.PLAIN, stroke * 3);
                    g.setFont(font);
                    g.drawString(text, points.get(0).x, points.get(0).y);
                }
                break;
        }
    }
}
