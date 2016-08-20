package org.giiwa.core.base;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Random;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;
import org.giiwa.core.cache.Cache;
import org.giiwa.core.cache.DefaultCachable;

public class Captcha {

  static Log log = LogFactory.getLog(Captcha.class);

  public static enum Result {
    badcode, expired, ok
  };

  private static final String VERIFY_CODES = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
  private static Random       random       = new Random();

  /**
   * output the "code image" to the file
   * 
   * @param sid
   *          the session id
   * @param expired
   *          the expired timestamp
   * @param w
   *          the width of the image
   * @param h
   *          the height of the image
   * @param outputFile
   *          the output file
   * @param len
   *          the length of the code
   * @return String of the code
   * @throws IOException
   *           throw exception when write image to the file
   */
  public static boolean create(String sid, long expired, int w, int h, File outputFile, int len) throws IOException {
    if (outputFile == null) {
      return false;
    }
    File dir = outputFile.getParentFile();
    if (!dir.exists()) {
      dir.mkdirs();
    }
    try {
      String code = UID.random(len, VERIFY_CODES).toLowerCase();
      outputFile.createNewFile();
      FileOutputStream fos = new FileOutputStream(outputFile);
      outputImage(w, h, fos, code.toUpperCase());
      fos.close();

      Cache.set("//captcha/" + sid, Code.create(code, expired));
      return true;
    } catch (IOException e) {
      throw e;
    }
  }

  /**
   * verify the code associated
   * 
   * @param sid
   *          the session id
   * @param code
   *          the code
   * @return Result <br>
   */
  public static Result verify(String sid, String code) {
    String id = "//captcha/" + sid;
    Code c = (Code) Cache.get(id);
    if (c == null) {
      log.warn("no code in cache, sid=" + sid);
      return Result.badcode;
    } else if (!X.isSame(code, c.code)) {
      log.warn("is not same, code.server=" + c.code + ", code.client=" + code);
      return Result.badcode;
    } else if (c.expired < System.currentTimeMillis()) {
      log.warn("expired, expired=" + c.expired);
      return Result.expired;
    }
    return Result.ok;
  }

  /**
   * remove the captcha code for sid
   * 
   * @param sid
   *          the session id
   */
  public static void remove(String sid) {
    String id = "//captcha/" + sid;
    Cache.remove(id);
  }

  /**
   * 
   * @param w
   * @param h
   * @param os
   * @param code
   * @throws IOException
   */
  private static void outputImage(int w, int h, OutputStream os, String code) throws IOException {
    int verifySize = code.length();
    BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    Random rand = new Random();
    Graphics2D g2 = image.createGraphics();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    Color[] colors = new Color[5];
    Color[] colorSpaces = new Color[] { Color.WHITE, Color.CYAN, Color.GRAY, Color.LIGHT_GRAY, Color.MAGENTA,
        Color.ORANGE, Color.PINK, Color.YELLOW };
    float[] fractions = new float[colors.length];
    for (int i = 0; i < colors.length; i++) {
      colors[i] = colorSpaces[rand.nextInt(colorSpaces.length)];
      fractions[i] = rand.nextFloat();
    }
    Arrays.sort(fractions);

    g2.setColor(Color.GRAY);
    g2.fillRect(0, 0, w, h);

    Color c = getRandColor(200, 250);
    g2.setColor(c);
    g2.fillRect(0, 2, w, h - 4);

    // Random random = new Random();
    g2.setColor(getRandColor(160, 200));
    for (int i = 0; i < 20; i++) {
      int x = random.nextInt(w - 1);
      int y = random.nextInt(h - 1);
      int xl = random.nextInt(6) + 1;
      int yl = random.nextInt(12) + 1;
      g2.drawLine(x, y, x + xl + 40, y + yl + 20);
    }

    float yawpRate = 0.05f;
    int area = (int) (yawpRate * w * h);
    for (int i = 0; i < area; i++) {
      int x = random.nextInt(w);
      int y = random.nextInt(h);
      int rgb = getRandomIntColor();
      image.setRGB(x, y, rgb);
    }

    shear(g2, w, h, c);

    g2.setColor(getRandColor(100, 160));
    int fontSize = h - 4;
    Font font = new Font("Algerian", Font.ITALIC, fontSize);
    g2.setFont(font);
    char[] chars = code.toCharArray();
    for (int i = 0; i < verifySize; i++) {
      AffineTransform affine = new AffineTransform();
      affine.setToRotation(Math.PI / 4 * rand.nextDouble() * (rand.nextBoolean() ? 1 : -1),
          (w / verifySize) * i + fontSize / 2, h / 2);
      g2.setTransform(affine);
      g2.drawChars(chars, i, 1, ((w - 10) / verifySize) * i + 5, h / 2 + fontSize / 2 - 10);
    }

    g2.dispose();
    ImageIO.write(image, "jpg", os);
  }

  private static Color getRandColor(int fc, int bc) {
    if (fc > 255)
      fc = 255;
    if (bc > 255)
      bc = 255;
    int r = fc + random.nextInt(bc - fc);
    int g = fc + random.nextInt(bc - fc);
    int b = fc + random.nextInt(bc - fc);
    return new Color(r, g, b);
  }

  private static int getRandomIntColor() {
    int[] rgb = getRandomRgb();
    int color = 0;
    for (int c : rgb) {
      color = color << 8;
      color = color | c;
    }
    return color;
  }

  private static int[] getRandomRgb() {
    int[] rgb = new int[3];
    for (int i = 0; i < 3; i++) {
      rgb[i] = random.nextInt(255);
    }
    return rgb;
  }

  private static void shear(Graphics g, int w1, int h1, Color color) {
    shearX(g, w1, h1, color);
    shearY(g, w1, h1, color);
  }

  private static void shearX(Graphics g, int w1, int h1, Color color) {

    int period = random.nextInt(2);

    boolean borderGap = true;
    int frames = 1;
    int phase = random.nextInt(2);

    for (int i = 0; i < h1; i++) {
      double d = (double) (period >> 1)
          * Math.sin((double) i / (double) period + (6.2831853071795862D * (double) phase) / (double) frames);
      g.copyArea(0, i, w1, 1, (int) d, 0);
      if (borderGap) {
        g.setColor(color);
        g.drawLine((int) d, i, 0, i);
        g.drawLine((int) d + w1, i, w1, i);
      }
    }

  }

  private static void shearY(Graphics g, int w1, int h1, Color color) {

    int period = random.nextInt(40) + 10; // 50;

    boolean borderGap = true;
    int frames = 20;
    int phase = 7;
    for (int i = 0; i < w1; i++) {
      double d = (double) (period >> 1)
          * Math.sin((double) i / (double) period + (6.2831853071795862D * (double) phase) / (double) frames);
      g.copyArea(i, 0, 1, h1, 0, (int) d);
      if (borderGap) {
        g.setColor(color);
        g.drawLine(i, (int) d, i, 0);
        g.drawLine(i, (int) d + h1, i, h1);
      }

    }

  }

  public static void main(String[] args) throws IOException {
    File dir = new File("/tmp/verifies");
    int w = 200, h = 80;
    for (int i = 0; i < 50; i++) {
      File file = new File(dir, i + ".jpg");
      create("1", System.currentTimeMillis() + 6 * X.AMINUTE, w, h, file, 4);
    }
  }

  public static class Code extends DefaultCachable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    String                    code;
    long                      expired;

    static Code create(String code, long expired) {
      Code c = new Code();
      c.code = code;
      c.expired = expired;
      return c;
    }
  }
}
