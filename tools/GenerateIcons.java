// Vygeneruje ikonu aplikace a banner pro Android TV (roubená chaloupka s kouřícím komínem).
// Spuštění z kořene projektu:  java tools/GenerateIcons.java
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class GenerateIcons {

    static final Color SKY_TOP = new Color(0xF7B548);
    static final Color SKY_BOTTOM = new Color(0xD8691F);
    static final Color HILL = new Color(0xB4501A);
    static final Color HILL2 = new Color(0x8E3B14);
    static final Color GROUND = new Color(0x5A2E12);
    static final Color LOG_A = new Color(0x8B4A22);
    static final Color LOG_B = new Color(0x74391A);
    static final Color LOG_END = new Color(0xD39552);
    static final Color LOG_RING = new Color(0x9A5A2A);
    static final Color ROOF = new Color(0x3E1D0B);
    static final Color ROOF_EDGE = new Color(0x6A3414);
    static final Color STONE = new Color(0x7E6252);
    static final Color STONE_DARK = new Color(0x5E4638);
    static final Color WINDOW = new Color(0xFFD34D);
    static final Color DOOR = new Color(0x4E260E);
    static final Color CREAM = new Color(0xFFF1D6);

    public static void main(String[] args) throws Exception {
        String res = "app/src/main/res/";
        int[] sizes = {48, 72, 96, 144, 192};
        String[] dirs = {"mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"};
        for (int i = 0; i < sizes.length; i++) {
            write(icon(sizes[i]), res + "mipmap-" + dirs[i] + "/ic_launcher.png");
        }
        write(banner(), res + "drawable-xhdpi/banner.png");
        write(icon(512), "tools/ikona-512.png");
    }

    static void write(BufferedImage img, String path) throws Exception {
        File f = new File(path);
        f.getParentFile().mkdirs();
        ImageIO.write(img, "png", f);
        System.out.println("zapsáno " + path);
    }

    static Graphics2D gfx(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g;
    }

    static BufferedImage icon(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = gfx(img);
        double s = size / 100.0;
        Shape clip = new RoundRectangle2D.Double(2 * s, 2 * s, 96 * s, 96 * s, 22 * s, 22 * s);
        g.setClip(clip);
        background(g, 0, 0, size, size);
        g.scale(s, s);
        landscape(g, 100, 100);
        cabin(g, 0, 0, 1);
        g.setClip(null);
        g.dispose();
        return img;
    }

    static BufferedImage banner() {
        int w = 320, h = 180;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = gfx(img);
        background(g, 0, 0, w, h);
        AffineTransform base = g.getTransform();
        // Krajina přes celou šířku, chaloupka vlevo.
        g.scale(h / 100.0, h / 100.0);
        landscape(g, w * 100.0 / h, 100);
        cabin(g, -4, 2, 0.98);
        g.setTransform(base);

        // Text vpravo.
        String l1 = "Kvízy";
        String l2 = "na dovolených";
        int textLeft = 168, textRight = w - 10;
        Font f2 = fit(g, l2, new Font("Segoe UI", Font.BOLD, 30), textRight - textLeft);
        Font f1 = f2.deriveFont(f2.getSize2D() * 1.9f);
        FontMetrics m1 = g.getFontMetrics(f1);
        FontMetrics m2 = g.getFontMetrics(f2);
        int y1 = 76, y2 = y1 + m2.getAscent() + 6;
        int x1 = textLeft + (textRight - textLeft - m1.stringWidth(l1)) / 2;
        int x2 = textLeft + (textRight - textLeft - m2.stringWidth(l2)) / 2;
        shadowText(g, l1, f1, x1, y1);
        shadowText(g, l2, f2, x2, y2);
        g.dispose();
        return img;
    }

    static Font fit(Graphics2D g, String text, Font f, int maxWidth) {
        while (g.getFontMetrics(f).stringWidth(text) > maxWidth && f.getSize() > 8) {
            f = f.deriveFont(f.getSize2D() - 0.5f);
        }
        return f;
    }

    static void shadowText(Graphics2D g, String t, Font f, int x, int y) {
        g.setFont(f);
        g.setColor(new Color(0x2B, 0x14, 0x06, 170));
        g.drawString(t, x + 2, y + 2);
        g.setColor(CREAM);
        g.drawString(t, x, y);
    }

    static void background(Graphics2D g, int x, int y, int w, int h) {
        g.setPaint(new GradientPaint(0, y, SKY_TOP, 0, y + h, SKY_BOTTOM));
        g.fillRect(x, y, w, h);
        // Podzimní slunce
        g.setPaint(new RadialGradientPaint(new Point2D.Double(x + w * 0.2, y + h * 0.18), (float) (h * 0.35),
                new float[]{0f, 1f}, new Color[]{new Color(255, 236, 150, 170), new Color(255, 236, 150, 0)}));
        g.fillRect(x, y, w, h);
    }

    /** Kopce, louka a padající listí; souřadnice v jednotkách, výška 100. */
    static void landscape(Graphics2D g, double w, double h) {
        g.setColor(HILL);
        g.fill(new Ellipse2D.Double(-30, 62, w * 0.9, 50));
        g.setColor(HILL2);
        g.fill(new Ellipse2D.Double(w * 0.35, 66, w * 0.95, 50));
        g.setColor(GROUND);
        g.fill(new Rectangle2D.Double(0, 84, w, 20));
        // Stromky v pozadí (podzimní koruny)
        tree(g, 9, 84, new Color(0xE8A21E));
        tree(g, 91, 84, new Color(0xC2441A));
        if (w > 120) {
            tree(g, w - 14, 84, new Color(0xE8A21E));
            tree(g, w - 40, 86, new Color(0xC2441A));
        }
        // Listí
        double[][] leaves = {{14, 30, 0.6}, {86, 40, -0.5}, {80, 70, 0.9}, {6, 64, -0.3}, {26, 18, 1.2}};
        Color[] lc = {new Color(0xFFD34D), new Color(0xE0641C), new Color(0xB8321A)};
        for (int i = 0; i < leaves.length; i++) {
            leaf(g, leaves[i][0], leaves[i][1], leaves[i][2], lc[i % lc.length]);
        }
    }

    static void tree(Graphics2D g, double x, double baseY, Color crown) {
        g.setColor(new Color(0x4A240C));
        g.fill(new Rectangle2D.Double(x - 1.2, baseY - 12, 2.4, 12));
        g.setColor(crown);
        g.fill(new Ellipse2D.Double(x - 7, baseY - 26, 14, 16));
        g.setColor(crown.darker());
        g.fill(new Ellipse2D.Double(x - 2, baseY - 20, 8, 9));
    }

    static void leaf(Graphics2D g, double x, double y, double rot, Color c) {
        AffineTransform t = g.getTransform();
        g.translate(x, y);
        g.rotate(rot);
        g.setColor(c);
        g.fill(new Ellipse2D.Double(-2.6, -1.3, 5.2, 2.6));
        g.setColor(c.darker());
        g.setStroke(new BasicStroke(0.35f));
        g.draw(new Line2D.Double(-2.6, 0, 2.6, 0));
        g.setTransform(t);
    }

    /** Roubená chaloupka v boxu 100×100 jednotek. */
    static void cabin(Graphics2D g, double ox, double oy, double sc) {
        AffineTransform t = g.getTransform();
        g.translate(ox, oy);
        g.translate(50, 50);
        g.scale(sc, sc);
        g.translate(-50, -50);

        // Kouř z komína
        Color[] smoke = {new Color(255, 248, 235, 215), new Color(255, 248, 235, 175), new Color(255, 248, 235, 130)};
        g.setColor(smoke[0]);
        g.fill(new Ellipse2D.Double(63, 19, 8, 7));
        g.setColor(smoke[1]);
        g.fill(new Ellipse2D.Double(66, 11, 10, 8.5));
        g.setColor(smoke[2]);
        g.fill(new Ellipse2D.Double(62, 3, 12, 9.5));

        // Komín (kamenný)
        g.setColor(STONE);
        g.fill(new Rectangle2D.Double(62, 25, 10, 22));
        g.setColor(STONE_DARK);
        g.fill(new Rectangle2D.Double(60.5, 24, 13, 3.5));
        g.setStroke(new BasicStroke(0.6f));
        for (double yy = 31; yy < 46; yy += 5) {
            g.draw(new Line2D.Double(62, yy, 72, yy));
        }

        // Stěny z trámů
        double wl = 22, wr = 78, top = 53, rows = 5, rowH = 6.2;
        for (int i = 0; i < rows; i++) {
            double y = top + i * rowH;
            g.setColor(i % 2 == 0 ? LOG_A : LOG_B);
            g.fill(new RoundRectangle2D.Double(wl, y, wr - wl, rowH, rowH, rowH));
            g.setColor(new Color(0x3A1A08));
            g.setStroke(new BasicStroke(0.5f));
            g.draw(new Line2D.Double(wl + 2, y + rowH, wr - 2, y + rowH));
            // Čela trámů na rozích
            for (double ex : new double[]{wl - 2.5, wr - 3.7}) {
                g.setColor(LOG_END);
                g.fill(new Ellipse2D.Double(ex, y + 0.1, 6.2, 6.0));
                g.setColor(LOG_RING);
                g.setStroke(new BasicStroke(0.55f));
                g.draw(new Ellipse2D.Double(ex + 1.4, y + 1.4, 3.4, 3.2));
            }
        }

        // Okno se světlem
        g.setPaint(new RadialGradientPaint(new Point2D.Double(36, 66), 16f, new float[]{0f, 1f},
                new Color[]{new Color(255, 214, 90, 120), new Color(255, 214, 90, 0)}));
        g.fill(new Ellipse2D.Double(20, 50, 32, 32));
        g.setColor(WINDOW);
        g.fill(new Rectangle2D.Double(29, 59, 13, 12));
        g.setColor(DOOR);
        g.setStroke(new BasicStroke(1.4f));
        g.draw(new Rectangle2D.Double(29, 59, 13, 12));
        g.draw(new Line2D.Double(35.5, 59, 35.5, 71));
        g.draw(new Line2D.Double(29, 65, 42, 65));

        // Dveře
        g.setColor(DOOR);
        g.fill(new RoundRectangle2D.Double(53, 62, 13, 22, 3, 3));
        g.setColor(new Color(0x6B3616));
        g.setStroke(new BasicStroke(0.6f));
        g.draw(new Line2D.Double(57.3, 63, 57.3, 84));
        g.draw(new Line2D.Double(61.6, 63, 61.6, 84));
        g.setColor(WINDOW);
        g.fill(new Ellipse2D.Double(63, 72, 2, 2));

        // Střecha
        Path2D roof = new Path2D.Double();
        roof.moveTo(13, 56);
        roof.lineTo(50, 27);
        roof.lineTo(87, 56);
        roof.lineTo(81, 56);
        roof.lineTo(50, 33);
        roof.lineTo(19, 56);
        roof.closePath();
        Path2D gable = new Path2D.Double();
        gable.moveTo(19, 56);
        gable.lineTo(50, 33);
        gable.lineTo(81, 56);
        gable.closePath();
        // Štít z prken
        g.setColor(LOG_B);
        g.fill(gable);
        Shape oldClip = g.getClip();
        g.clip(gable);
        g.setColor(new Color(0x5A2C10));
        g.setStroke(new BasicStroke(0.5f));
        for (double yy = 39; yy < 56; yy += 4.2) {
            g.draw(new Line2D.Double(10, yy, 90, yy));
        }
        g.setClip(oldClip);
        g.setColor(WINDOW);
        g.fill(new Ellipse2D.Double(47, 43, 6, 6));
        g.setColor(ROOF);
        g.fill(roof);
        g.setColor(ROOF_EDGE);
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(13, 56, 50, 27));
        g.draw(new Line2D.Double(50, 27, 87, 56));
        g.setTransform(t);
    }
}
