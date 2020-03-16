/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.giiwa.misc;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.logging.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.giiwa.dao.X;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

/**
 * The Class GImage is image utility, convert image, create QRCode image
 */
@SuppressWarnings("deprecation")
public class GImage {

	/** The log. */
	static Log log = LogFactory.getLog(GImage.class);

	/** The client. */
	static HttpClient client = new DefaultHttpClient();

	/**
	 * create QRCode image, and ouput to the file.
	 *
	 * @param out    the output image file
	 * @param text   the content of the qrcode
	 * @param width  width of image
	 * @param height height of image
	 * @throws Exception the exception
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void QRCode(File out, String text, int width, int height) throws Exception {
		int BLACK = 0xFF000000;
		int WHITE = 0xFFFFFFFF;

		String format = "jpg";
		Hashtable hints = new Hashtable();

		// 内容所使用编码
		hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
		BitMatrix matrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height, hints);

		// 生成二维码
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				image.setRGB(x, y, matrix.get(x, y) ? BLACK : WHITE);
			}
		}

		if (!ImageIO.write(image, format, out)) {
			throw new IOException("Could not write an image of format " + format + " to " + out);
		}
	}

	/**
	 * Scale the source image file to destination image file.
	 * 
	 * @param source the source image file
	 * @param x      the x of source image
	 * @param y      the y of the source image
	 * @param w0     the width of the source image
	 * @param h0     the height of the source image
	 * @param file   the destination image file
	 * @param pw     the width of the destination image
	 * @param ph     the height of the destination image
	 * @param w      the width of the destination image
	 * @param h      the height of the destination image
	 * @deprecated
	 */
	public static void scale(String source, int x, int y, int w0, int h0, String file, int pw, int ph, int w, int h) {
		try {
			BufferedImage img = ImageIO.read(new File(source));
			if (img == null)
				return;

			BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);// TYPE_4BYTE_BGR);
			Graphics g = out.getGraphics();
			g.setColor(Color.white);
			g.fillRect(0, 0, w, h);

			int gh = img.getHeight();
			int gw = img.getWidth();

			int lx = (x * gw) / pw;
			int ly = (y * gh) / ph;

			int lw = (w0 * gw) / pw;
			int lh = (h0 * gh) / ph;

			int dx1 = 0, dy1 = 0, dx2 = w, dy2 = h, sx1 = lx, sy1 = ly, sx2 = lx + lw, sy2 = ly + lh;

			g.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
			ImageIO.write(out, "jpg", new File(file));

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Scale the source image to destination image file.
	 * 
	 * @deprecated
	 * @param source the source image file
	 * @param x      the x of source image
	 * @param y      the y of source image
	 * @param w0     the width of source image
	 * @param h0     the height of source image
	 * @param w1     the width of source image
	 * @param h1     the height of source image
	 * @param file   the destination image file
	 * @param w      the width of destination image
	 * @param h      the height of destination image
	 */
	public static void scale(String source, int x, int y, int w0, int h0, int w1, int h1, String file, int w, int h) {
		try {

			BufferedImage img = ImageIO.read(new File(source));
			if (img == null || w < 0 || h < 0)
				return;

			int w2 = img.getWidth();
			int h2 = img.getHeight();

			if (w > w1 || h > h1)
				return;

			x = x * w2 / w1;
			y = y * h2 / h1;
			w0 = w0 * w2 / w1;
			h0 = h0 * h2 / h1;

			BufferedImage out = new BufferedImage(w0, h0, BufferedImage.TYPE_3BYTE_BGR);// TYPE_4BYTE_BGR);
			Graphics g = out.getGraphics();
			g.drawImage(img, 0, 0, w0, h0, x, y, x + w0, y + h0, null);

			Image tmp = out.getScaledInstance(w, h, Image.SCALE_SMOOTH);
			out = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);// TYPE_4BYTE_BGR);
			g = out.getGraphics();
			g.drawImage(tmp, 0, 0, w, h, null);
			ImageIO.write(out, "jpg", new File(file));

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * 以最大（高/宽）缩放， 不填充，输出w/h 不一定与参数一致
	 *
	 * @param source the source
	 * @param file   the file
	 * @param w      the w
	 * @param h      the h
	 * @throws IOException
	 */
	public static void scale2(InputStream src, OutputStream dest, int w, int h) throws IOException {

		try {
			BufferedImage img = ImageIO.read(src);
			if (img == null || w < 0 || h < 0)
				throw new IOException("bad [src, w, h]");

			int h1 = img.getHeight();
			int w1 = img.getWidth();

			if (w > w1 || h > h1)
				throw new IOException("bad [src, w, h]");

			if (h <= 0)
				h = h1;
			if (w <= 0)
				w = w1;

			float fh = ((float) h1) / h;
			float fw = ((float) w1) / w;

			if (fh > fw) {
				int w2 = (int) (w1 / fh);
				w = w2;
			} else {
				int h2 = (int) (h1 / fw);
				h = h2;
			}

			BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);// .TYPE_3BYTE_BGR);//
																						// TYPE_4BYTE_BGR);
			Graphics g = out.getGraphics();

			Image tmp = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
			g.drawImage(tmp, 0, 0, w, h, null);
			ImageIO.write(out, "png", dest);
		} finally {
			X.close(src, dest);
		}

	}

	/**
	 * get the size of image in inputstream.
	 *
	 * @param in the inputstream of image
	 * @return Point of size
	 */
	public static Point size(InputStream in) {
		Point p = new Point();
		try {
			in = new BufferedInputStream(in);
			in.mark(Integer.MAX_VALUE);
			BufferedImage img = ImageIO.read(in);

			if (img != null) {
				p.y = img.getHeight();
				p.x = img.getWidth();

				in.reset();
				Metadata metadata = JpegMetadataReader.readMetadata(in);
				if (metadata != null && metadata.getDirectories() != null) {
					Iterator<Directory> it = metadata.getDirectories().iterator();
					while (it.hasNext()) {
						Directory exif = it.next();
						if (exif != null && exif.getTags() != null) {
							Iterator<Tag> tags = exif.getTags().iterator();
							while (tags.hasNext()) {
								Tag tag = (Tag) tags.next();
								if ("Orientation".equals(tag.getTagName())) {
									String desc = tag.getDescription();
									if (desc.indexOf("Rotate 90") > 0 || desc.indexOf("Rotate 270") > 0) {
										int x = p.x;
										p.x = p.y;
										p.y = x;
										return p;
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
		}
		return p;
	}

	/**
	 * get the size of image by url.
	 *
	 * @param url the url
	 * @return Point of size
	 */
	public static Point size(URL url) {
		if (url == null)
			return null;

		try {
			return size(url.openStream());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * 以最小（高/宽）缩放， 然后裁剪，输出w/h=输入参数
	 *
	 * @param source the source image
	 * @param file   the destination image file
	 * @param w      the width of the destination image
	 * @param h      the height of the destination image
	 * @throws IOException
	 */
	public static void scale3(InputStream src, OutputStream dest, final int w, final int h) throws IOException {

		try {
			BufferedImage img = ImageIO.read(src);
			if (img == null || w < 0 || h < 0)
				throw new IOException("bad [src, w, h]");

			int h1 = img.getHeight();
			int w1 = img.getWidth();

			// if (w > w1 || h > h1)
			// return -1;

			int w0 = w;
			int h0 = h;

			if (h <= 0)
				h0 = h1;
			if (w <= 0)
				w0 = w1;

			float fh = ((float) h1) / h;
			float fw = ((float) w1) / w;

			if (fh < fw) {
				int w2 = (int) (w1 / fh);
				w0 = w2;
			} else {
				int h2 = (int) (h1 / fw);
				h0 = h2;
			}

			BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);// .TYPE_3BYTE_BGR);//
																						// TYPE_4BYTE_BGR);
			Graphics g = out.getGraphics();

			Image tmp = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);

			int ox = (w - w0) / 2;
			int oy = (h - h0) / 2;

			g.drawImage(tmp, ox, oy, w0, h0, null);

			ImageIO.write(out, "png", dest);
		} finally {
			X.close(src, dest);
		}

	}

	/**
	 * 以最小（高/宽）， 缩放收裁剪， 输出w/h=输入参数
	 * 
	 * @param source the InputStream
	 * @param file   the file object
	 * @param w      the width
	 * @param h      the height
	 * @throws IOException
	 */
	public static void scale(InputStream src, OutputStream dest, final int w, final int h) throws IOException {

		try {
			BufferedImage img = ImageIO.read(src);
			if (img == null || w < 0 || h < 0)
				throw new IOException("bad [src, w, h]");

			int h1 = img.getHeight();
			int w1 = img.getWidth();

			// if (w > w1 || h > h1)
			// return -1;

			int w0 = w;
			int h0 = h;

			if (h <= 0)
				h0 = h1;
			if (w <= 0)
				w0 = w1;

			float fh = ((float) h1) / h;
			float fw = ((float) w1) / w;

			if (fh < fw) {
				int w2 = (int) (w1 / fh);
				w0 = w2;
			} else {
				int h2 = (int) (h1 / fw);
				h0 = h2;
			}

			BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);// .TYPE_3BYTE_BGR);//
																						// TYPE_4BYTE_BGR);
			Graphics g = out.getGraphics();

			Image tmp = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);

			int ox = (w - w0) / 2;
			int oy = (h - h0) / 2;

			g.drawImage(tmp, ox, oy, w0, h0, null);
			ImageIO.write(out, "jpg", dest);
		} finally {
			X.close(src, dest);
		}
	}

	/**
	 * 以最大（高/宽）缩放， 透明填充， 输出w/h=输入参数
	 *
	 * @param source the source image file
	 * @param file   the destination image file
	 * @param w      the width of destination image
	 * @param h      the height of destination image
	 * @return the int
	 * @throws IOException
	 */
	public static void scale1(InputStream src, OutputStream dest, int w, int h) throws IOException {

		try {
			BufferedImage img = ImageIO.read(src);
			if (img == null)
				throw new IOException("bad [src, w, h]");

			// BufferedImage out = Scalr.resize(img, Scalr.Method.ULTRA_QUALITY,
			// w, h);// , Scalr.OP_ANTIALIAS);

			BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);// .TYPE_3BYTE_BGR);//
																						// TYPE_4BYTE_BGR);
			Graphics g = out.getGraphics();
			// g.setColor(Color.white);
			// g.fillRect(0, 0, w, h);

			int h1 = img.getHeight();
			int w1 = img.getWidth();
			if (h <= 0)
				h = h1;
			if (w <= 0)
				w = w1;

			float fh = ((float) h1) / h;
			float fw = ((float) w1) / w;
			int oh = 0;
			int ow = 0;

			if (fh > fw) {
				int w2 = (int) (w1 / fh);
				ow = (w - w2) / 2;
				w = w2;
			} else {
				int h2 = (int) (h1 / fw);
				oh = (h - h2) / 2;
				h = h2;
			}

			Image tmp = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
			g.drawImage(tmp, ow, oh, w, h, null);
			ImageIO.write(out, "png", dest);
		} finally {
			X.close(src, dest);
		}

	}

	/**
	 * Scale the source image in "url" with "referer" to destination image.
	 * 
	 * @deprecated
	 * @param url     the source image url
	 * @param referer the referer which maybe required when "get" the source image
	 * @param file    the destination image file
	 * @param w       the width of destination image
	 * @param h       the height of destination image
	 */
	public static void scale(String url, String referer, String file, int w, int h) {
		HttpGet get = null;
		InputStream in = null;
		try {

			get = new HttpGet(formatUrl(url, "utf-8"));
			if (referer != null && referer.length() > 0) {
				get.addHeader("Referer", referer);
			}

			HttpResponse agent = client.execute(get);

			in = agent.getEntity().getContent();
			BufferedImage img = ImageIO.read(in);
			if (img == null)
				return;

			BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);// TYPE_3BYTE_BGR);//
																						// TYPE_4BYTE_BGR);
			Graphics g = out.getGraphics();
			// g.setColor(Color.white);
			// g.fillRect(0, 0, w, h);

			int w0 = img.getWidth();
			int h0 = img.getHeight();
			int sx1 = 0, sx2 = 0 + w0, sy1 = 0, sy2 = 0 + h0, dx1 = 0, dx2 = w, dy1 = 0, dy2 = h;

			float fh = ((float) h0) / h;
			float fw = ((float) w0) / w;

			if (fh > fw) {
				int w2 = (int) (w0 / fh);
				dx1 = (w - w2) / 2;
				dx2 = dx1 + w2;
			} else {
				int h2 = (int) (h0 / fw);
				dy1 = (h - h2) / 2;
				dy2 = dy1 + h2;
			}

			g.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
			ImageIO.write(out, "png", new File(file));

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				}
			}
			if (get != null) {
				get.abort();
			}
		}
	}

	/**
	 * Format url.
	 * 
	 * @param uri     the uri
	 * @param charset the charset
	 * @return the string
	 * @throws UnsupportedEncodingException the unsupported encoding exception
	 */
	private static String formatUrl(String uri, String charset) throws UnsupportedEncodingException {

		int i = uri.indexOf(".");
		if (i > -1) {
			uri = uri.substring(0, i);
		}
		uri = uri.replace(" ", "%20");

		uri = charset != null ? URLEncoder.encode(uri, charset) : URLEncoder.encode(uri, "utf-8");

		uri = uri.replaceAll("%2F", "/");
		uri = uri.replaceAll("%3F", "?");
		uri = uri.replaceAll("%3D", "=");
		uri = uri.replaceAll("%26", "&");
		uri = uri.replaceAll("%3A", ":");
		uri = uri.replaceAll("%25", "%");

		return uri;

	}

	/**
	 * make a Cover by source images.
	 *
	 * @param dest    the destination cover image file
	 * @param w       the width of destination image
	 * @param h       the height of destination image
	 * @param sources the source image files, should be 4, or 9
	 * @throws Exception the exception
	 * @deprecated
	 */
	public static void cover(String dest, int w, int h, List<String> sources) throws Exception {
		if (sources == null) {
			throw new Exception("no source!");
		}

		int len = sources.size();
		if (len == 4 || len == 9) {

			BufferedImage src[] = new BufferedImage[len];
			for (int i = 0; i < len; i++) {
				src[i] = ImageIO.read(new File(sources.get(i)));
			}

			BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);// TYPE_4BYTE_BGR);
			Graphics g = out.getGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, w, h);
			g.setColor(new Color(0xCCD4D0));

			int size = len == 4 ? 2 : 3;
			int space = len == 4 ? 4 : 2;

			int w1 = (w - 1) / size;
			int h1 = (h - 1) / size;
			float fd = ((float) w1) / h1;

			for (int i = 0; i < len; i++) {
				// scale the source to new w1/h1
				BufferedImage img = src[i];
				int w2 = img.getWidth();
				int h2 = img.getHeight();

				float fs = ((float) w2) / h2;
				if (fs > fd) {
					w2 = w1;
					h2 = (int) (w2 / fs);
				} else if (fs < fd) {
					h2 = h1;
					w2 = (int) (h2 * fs);
				} else {
					w2 = w1;
					h2 = h1;
				}

				Image tmp = img.getScaledInstance(w2, h2, Image.SCALE_SMOOTH);

				int x = (i % size) * w1;
				int y = (i / size) * h1;
				g.drawImage(tmp, x + space + (w1 - w2) / 2, y + space + (h1 - h2) / 2, w2 - 2 * space, h2 - 2 * space,
						null);
				g.drawRect(x, y, w1, h1);
			}

			ImageIO.write(out, "jpg", new File(dest));
		} else {
			throw new Exception("sources MUST is 4 or 9 picutures!" + len);
		}
	}

	/**
	 * make a cover by source images.
	 *
	 * @param dest    the destination of cover image
	 * @param type    the type of cover image
	 * @param w       the width of cover image
	 * @param h       the height of cover image
	 * @param sources the source images files
	 * @return true, if successful
	 * @throws Exception the exception
	 */
	public static boolean cover(String dest, int type, int w, int h, List<String> sources) throws Exception {
		if (sources == null) {
			throw new Exception("no source!");
		}

		switch (type) {
		case 1:
			return _cover1(dest, w, h, sources);
		case 2:
			return _cover2(dest, w, h, sources);
		case 3:
			return _cover3(dest, w, h, sources);
		case 4:
			return _cover4(dest, w, h, sources);
		case 5:
			return _cover5(dest, w, h, sources);
		default:
			throw new Exception("the TYPE Must be 1-5");
		}
	}

	/**
	 * +---+---+ <br>
	 * | 1 | 2 | <br>
	 * +---+---+ <br>
	 * | 3 | 4 | <br>
	 * +---+---+ <br>
	 * .
	 * 
	 * @param dest    the dest
	 * @param w       the w
	 * @param h       the h
	 * @param sources the sources
	 * @return true, if successful
	 * @throws Exception the exception
	 */
	private static boolean _cover1(String dest, int w, int h, List<String> sources) throws Exception {

		int len = sources.size();
		if (len == 4) {

			BufferedImage src[] = new BufferedImage[len];
			for (int i = 0; i < len; i++) {
				String s = sources.get(i);
				if (!EMPTY.equals(s)) {
					src[i] = ImageIO.read(new File(sources.get(i)));
				}
			}

			BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);// TYPE_4BYTE_BGR);
			Graphics2D g = (Graphics2D) out.getGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, w, h);

			int size = 2;
			int space = 4;

			int w1 = (w - 1) / size;
			int h1 = (h - 1) / size;
			float fd = ((float) w1) / h1;

			for (int i = 0; i < len; i++) {
				// scale the source to new w1/h1
				int x = (i % size) * w1;
				int y = (i / size) * h1;

				BufferedImage img = src[i];
				if (img != null) {
					int w2 = img.getWidth();
					int h2 = img.getHeight();

					float fs = ((float) w2) / h2;
					if (fs > fd) {
						w2 = w1;
						h2 = (int) (w2 / fs);
					} else if (fs < fd) {
						h2 = h1;
						w2 = (int) (h2 * fs);
					} else {
						w2 = w1;
						h2 = h1;
					}

					Image tmp = img.getScaledInstance(w2, h2, Image.SCALE_SMOOTH);

					g.drawImage(tmp, x + space + (w1 - w2) / 2, y + space + (h1 - h2) / 2, w2 - 2 * space,
							h2 - 2 * space, null);
				}
				drawRect(g, x, y, w1, h1);
			}

			// log.info(out + "==>" + dest);
			ImageIO.write(out, "jpg", new File(dest));

			g.dispose();

			return true;
		} else {
			throw new Exception("sources MUST is 4 picutures!" + len);
		}
	}

	/**
	 * +---+---+ <br>
	 * | | 2 | <br>
	 * | |---+ <br>
	 * |1 | 3 | <br>
	 * | |---+ <br>
	 * | |4 | <br>
	 * +---+---+ <br>
	 * .
	 * 
	 * @param dest    the dest
	 * @param w       the w
	 * @param h       the h
	 * @param sources the sources
	 * @return true, if successful
	 * @throws Exception the exception
	 */
	private static boolean _cover2(String dest, int w, int h, List<String> sources) throws Exception {

		int len = sources.size();
		if (len == 4) {

			BufferedImage src[] = new BufferedImage[len];
			for (int i = 0; i < len; i++) {
				String s = sources.get(i);
				if (!EMPTY.equals(s)) {
					src[i] = ImageIO.read(new File(sources.get(i)));
				}
			}

			BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);// TYPE_4BYTE_BGR);
			Graphics2D g = (Graphics2D) out.getGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, w, h);

			int space = 4;

			int w1 = (w - 1) * 2 / 3;
			int h1 = (h - 1);
			float fd = ((float) w1) / h1;

			// scale the source to new w1/h1
			int x = 0;
			int y = 0;
			BufferedImage img = src[0];
			if (img != null) {
				int w2 = img.getWidth();
				int h2 = img.getHeight();

				float fs = ((float) w2) / h2;
				if (fs > fd) {
					w2 = w1;
					h2 = (int) (w2 / fs);
				} else if (fs < fd) {
					h2 = h1;
					w2 = (int) (h2 * fs);
				} else {
					w2 = w1;
					h2 = h1;
				}

				Image tmp = img.getScaledInstance(w2, h2, Image.SCALE_SMOOTH);

				g.drawImage(tmp, x + space + (w1 - w2) / 2, y + space + (h1 - h2) / 2, w2 - 2 * space, h2 - 2 * space,
						null);
			}
			drawRect(g, x, y, w1, h1);

			int size = 3;

			w1 = (w - 1) / size;
			h1 = (h - 1) / size;
			fd = ((float) w1) / h1;

			for (int i = 1; i < len; i++) {
				// scale the source to new w1/h1
				x = w1 * 2;
				y = (i - 1) * h1;
				img = src[i];
				if (img != null) {
					int w2 = img.getWidth();
					int h2 = img.getHeight();

					float fs = ((float) w2) / h2;
					if (fs > fd) {
						w2 = w1;
						h2 = (int) (w2 / fs);
					} else if (fs < fd) {
						h2 = h1;
						w2 = (int) (h2 * fs);
					} else {
						w2 = w1;
						h2 = h1;
					}

					Image tmp = img.getScaledInstance(w2, h2, Image.SCALE_SMOOTH);

					g.drawImage(tmp, x + space + (w1 - w2) / 2, y + space + (h1 - h2) / 2, w2 - 2 * space,
							h2 - 2 * space, null);
				}
				drawRect(g, x, y, w1, h1);
			}

			ImageIO.write(out, "jpg", new File(dest));

			g.dispose();

			return true;
		} else {
			throw new Exception("sources MUST is 4 picutures!" + len);
		}
	}

	/**
	 * +-----+ <br>
	 * | 1 | <br>
	 * +-+-+-+ <br>
	 * |2|3|4| <br>
	 * +-+-+-+ <br>
	 * .
	 * 
	 * @param dest    the dest
	 * @param w       the w
	 * @param h       the h
	 * @param sources the sources
	 * @return true, if successful
	 * @throws Exception the exception
	 */
	private static boolean _cover3(String dest, int w, int h, List<String> sources) throws Exception {

		int len = sources.size();
		if (len == 4) {

			BufferedImage src[] = new BufferedImage[len];
			for (int i = 0; i < len; i++) {
				String s = sources.get(i);
				if (!EMPTY.equals(s)) {
					src[i] = ImageIO.read(new File(sources.get(i)));
				}
			}
			BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);// TYPE_4BYTE_BGR);
			Graphics2D g = (Graphics2D) out.getGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, w, h);

			int space = 4;

			int w1 = (w - 1);
			int h1 = (h - 1) * 2 / 3;
			float fd = ((float) w1) / h1;

			// scale the source to new w1/h1
			int x = 0;
			int y = 0;
			BufferedImage img = src[0];
			if (img != null) {
				int w2 = img.getWidth();
				int h2 = img.getHeight();

				float fs = ((float) w2) / h2;
				if (fs > fd) {
					w2 = w1;
					h2 = (int) (w2 / fs);
				} else if (fs < fd) {
					h2 = h1;
					w2 = (int) (h2 * fs);
				} else {
					w2 = w1;
					h2 = h1;
				}

				Image tmp = img.getScaledInstance(w2, h2, Image.SCALE_SMOOTH);

				g.drawImage(tmp, x + space + (w1 - w2) / 2, y + space + (h1 - h2) / 2, w2 - 2 * space, h2 - 2 * space,
						null);
			}
			drawRect(g, x, y, w1, h1);

			int size = 3;
			w1 = (w - 1) / size;
			h1 = (h - 1) / size;
			fd = ((float) w1) / h1;

			for (int i = 1; i < len; i++) {
				// scale the source to new w1/h1
				x = (i - 1) * w1;
				y = h1 * 2;
				img = src[i];
				if (img != null) {
					int w2 = img.getWidth();
					int h2 = img.getHeight();

					float fs = ((float) w2) / h2;
					if (fs > fd) {
						w2 = w1;
						h2 = (int) (w2 / fs);
					} else if (fs < fd) {
						h2 = h1;
						w2 = (int) (h2 * fs);
					} else {
						w2 = w1;
						h2 = h1;
					}

					Image tmp = img.getScaledInstance(w2, h2, Image.SCALE_SMOOTH);

					g.drawImage(tmp, x + space + (w1 - w2) / 2, y + space + (h1 - h2) / 2, w2 - 2 * space,
							h2 - 2 * space, null);
				}
				drawRect(g, x, y, w1, h1);
			}

			ImageIO.write(out, "jpg", new File(dest));

			g.dispose();

			return true;
		} else {
			throw new Exception("sources MUST is 4 picutures!" + len);
		}
	}

	/**
	 * +---+-+ <br>
	 * | |2| <br>
	 * + 1 +-+ <br>
	 * | |3| <br>
	 * +-+-+-+ <br>
	 * |4|5|6| <br>
	 * +-+-+-+ <br>
	 * .
	 * 
	 * @param dest    the dest
	 * @param w       the w
	 * @param h       the h
	 * @param sources the sources
	 * @return true, if successful
	 * @throws Exception the exception
	 */
	private static boolean _cover4(String dest, int w, int h, List<String> sources) throws Exception {

		int len = sources.size();
		if (len == 6) {

			BufferedImage src[] = new BufferedImage[len];
			for (int i = 0; i < len; i++) {
				String s = sources.get(i);
				if (!EMPTY.equals(s)) {
					src[i] = ImageIO.read(new File(sources.get(i)));
				}
			}
			BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
			Graphics2D g = (Graphics2D) out.getGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, w, h);

			int space = 4;

			int w1 = (w - 1) * 2 / 3;
			int h1 = (h - 1) * 2 / 3;
			float fd = ((float) w1) / h1;

			// scale the source to new w1/h1
			int x = 0;
			int y = 0;
			BufferedImage img = src[0];
			if (img != null) {
				int w2 = img.getWidth();
				int h2 = img.getHeight();

				float fs = ((float) w2) / h2;
				if (fs > fd) {
					w2 = w1;
					h2 = (int) (w2 / fs);
				} else if (fs < fd) {
					h2 = h1;
					w2 = (int) (h2 * fs);
				} else {
					w2 = w1;
					h2 = h1;
				}

				Image tmp = img.getScaledInstance(w2, h2, Image.SCALE_SMOOTH);

				g.drawImage(tmp, x + space + (w1 - w2) / 2, y + space + (h1 - h2) / 2, w2 - 2 * space, h2 - 2 * space,
						null);
			}
			drawRect(g, x, y, w1, h1);

			int size = 3;
			w1 = (w - 1) / size;
			h1 = (h - 1) / size;
			fd = ((float) w1) / h1;

			for (int i = 1; i < len; i++) {
				// scale the source to new w1/h1
				if (i >= size) {
					x = (i % size) * w1;
					y = 2 * h1;
				} else {
					x = 2 * w1;
					y = (i - 1) * h1;
				}
				img = src[i];
				if (img != null) {
					int w2 = img.getWidth();
					int h2 = img.getHeight();

					float fs = ((float) w2) / h2;
					if (fs > fd) {
						w2 = w1;
						h2 = (int) (w2 / fs);
					} else if (fs < fd) {
						h2 = h1;
						w2 = (int) (h2 * fs);
					} else {
						w2 = w1;
						h2 = h1;
					}

					Image tmp = img.getScaledInstance(w2, h2, Image.SCALE_SMOOTH);

					g.drawImage(tmp, x + space + (w1 - w2) / 2, y + space + (h1 - h2) / 2, w2 - 2 * space,
							h2 - 2 * space, null);
				}
				drawRect(g, x, y, w1, h1);
			}

			ImageIO.write(out, "jpg", new File(dest));

			g.dispose();
			return true;
		} else {
			throw new Exception("sources MUST is 6 picutures!" + len);
		}
	}

	/**
	 * +-+-+-+ <br>
	 * |1|2|3| <br>
	 * +-+-+-+ <br>
	 * |4|5|6| <br>
	 * +-+-+-+ <br>
	 * |7|8|9| <br>
	 * +-+-+-+ <br>
	 * .
	 * 
	 * @param dest    the dest
	 * @param w       the w
	 * @param h       the h
	 * @param sources the sources
	 * @return true, if successful
	 * @throws Exception the exception
	 */
	private static boolean _cover5(String dest, int w, int h, List<String> sources) throws Exception {

		w *= 2;
		h *= 2;
		int len = sources.size();
		if (len == 9) {

			BufferedImage src[] = new BufferedImage[len];
			for (int i = 0; i < len; i++) {
				String s = sources.get(i);
				if (!EMPTY.equals(s)) {
					src[i] = ImageIO.read(new File(s));
				}
			}

			BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);// TYPE_4BYTE_BGR);
			Graphics2D g = (Graphics2D) out.getGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, w, h);

			int size = 3;
			int space = 2;

			int w1 = (w - 1) / size;
			int h1 = (h - 1) / size;
			float fd = ((float) w1) / h1;

			for (int i = 0; i < len; i++) {
				// scale the source to new w1/h1
				int x = (i % size) * w1;
				int y = (i / size) * h1;

				BufferedImage img = src[i];
				if (img != null) {
					int w2 = img.getWidth();
					int h2 = img.getHeight();

					float fs = ((float) w2) / h2;
					if (fs > fd) {
						w2 = w1;
						h2 = (int) (w2 / fs);
					} else if (fs < fd) {
						h2 = h1;
						w2 = (int) (h2 * fs);
					} else {
						w2 = w1;
						h2 = h1;
					}

					Image tmp = img.getScaledInstance(w2, h2, Image.SCALE_SMOOTH);

					g.drawImage(tmp, x + space + (w1 - w2) / 2, y + space + (h1 - h2) / 2, w2 - 2 * space,
							h2 - 2 * space, null);
				}

				drawRect(g, x, y, w1, h1);
			}

			ImageIO.write(out, "jpg", new File(dest));

			g.dispose();
			return true;
		} else {
			throw new Exception("sources MUST is 9 picutures!" + len);
		}
	}

	/**
	 * Draw rect.
	 * 
	 * @param g  the g
	 * @param x  the x
	 * @param y  the y
	 * @param w1 the w1
	 * @param h1 the h1
	 */
	private static void drawRect(Graphics2D g, int x, int y, int w1, int h1) {
		g.setColor(linecolor);
		g.setStroke(stroke1f);
		if (x == 0) {
			g.drawLine(x, y, x, y + h1);
		}
		if (y == 0) {
			g.drawLine(x, y, x + w1, y);
		}

		g.drawLine(x + w1, y, x + w1, y + h1);
		g.drawLine(x, y + h1, x + w1, y + h1);
	}

	/** The Constant stroke1f. */
	static final BasicStroke stroke1f = new BasicStroke(1f);

	/** The Constant linecolor. */
	static final Color linecolor = new Color(0xeeeeee);

	/** The Constant EMPTY. */
	static final String EMPTY = "";

	public static void cut(String source, int w, int h, String target) {
		try {

			BufferedImage img = ImageIO.read(new File(source));
			if (img == null || w < 0 || h < 0)
				return;

			int h1 = img.getHeight();
			int w1 = img.getWidth();

			if (w > w1 || h > h1)
				return;

			for (int x = 0; x < w1; x += w) {
				for (int y = 0; y < h1; y += h) {
					BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
					Graphics g = out.getGraphics();
					g.drawImage(img, 0, 0, w, h, x, y, x + w, y + h, null);
					ImageIO.write(out, "png", new File(target + "/" + x + "_" + y + ".png"));
				}
			}

		} catch (Exception e) {
			log.error(source, e);
		}
	}

	public static void cover(int w, String s, Color color, OutputStream output) {

		try {
			BufferedImage out = new BufferedImage(w, w, BufferedImage.TYPE_3BYTE_BGR);// TYPE_4BYTE_BGR);

			Graphics2D g = (Graphics2D) out.getGraphics();
			g.setColor(color);
			g.fillRect(0, 0, w, w);

			int n = s.length() + 1;

			g.setFont(new Font("宋体", Font.BOLD, w / n));
			int x = (w - g.getFontMetrics().stringWidth(s)) / 2;

			int h = g.getFontMetrics().stringWidth("A");

			g.setColor(Color.WHITE);

			g.drawString(s, x, (w + h) / 2);

			ImageIO.write(out, "png", output);

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			X.close(output);
		}

	}

	public static void main(String[] args) {
//		String s1 = "/Users/joe/Downloads/aaa.jpg";
		String s2 = "/Users/joe/Downloads/t2.png";

		try {
			OutputStream out = new FileOutputStream(s2);

//			scale1(new FileInputStream(s1), out, 300, 300);

			X.Image.cover(100, "测试",
					new Color((int) (128 * Math.random()), (int) (156 * Math.random()), (int) (156 * Math.random())),
					out);

		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("done");
	}

}
