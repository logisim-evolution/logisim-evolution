/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import java.awt.Toolkit;

import java.awt.datatransfer.DataFlavor;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageUtil {
  private static final Logger logger = LoggerFactory.getLogger(ImageUtil.class);

  private static final Map<String, SoftReference<BufferedImage>> IMAGE_CACHE = Collections
      .synchronizedMap(new WeakHashMap<>());

  private static final String[] BYTE_UNITS = new String[] {"B", "KB", "MB", "GB", "TB"};

  public static String formatBytes(long bytes) {
    if (bytes <= 0) return "0 B";
    int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
    digitGroups = Math.min(digitGroups, BYTE_UNITS.length - 1);
    if (digitGroups == 0) {
      return bytes + " B";
    }
    return String.format(java.util.Locale.US, "%.1f %s", bytes / Math.pow(1024, digitGroups), BYTE_UNITS[digitGroups]);
  }

  public static String formatDataSize(String base64Source) {
    if (base64Source == null || base64Source.isEmpty()) {
      return formatBytes(0);
    }
    final var bytes = base64Source.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
    return formatBytes(bytes);
  }

  private ImageUtil() {
    // static
  }

  /**
   * Reads an image file and converts it into a Base64 data URI string
   * ("data:image/png;base64,...").
   */
  public static String fileToBase64(File file) throws IOException {
    if (file == null || !file.exists()) {
      return "";
    }
    byte[] fileContent = Files.readAllBytes(file.toPath());
    String mimeType = Files.probeContentType(file.toPath());
    if (mimeType == null || !mimeType.startsWith("image/")) {
      String name = file.getName().toLowerCase();
      if (name.endsWith(".png"))
        mimeType = "image/png";
      else if (name.endsWith(".jpg") || name.endsWith(".jpeg"))
        mimeType = "image/jpeg";
      else if (name.endsWith(".gif"))
        mimeType = "image/gif";
      else
        mimeType = "image/png";
    }
    String encoded = Base64.getEncoder().encodeToString(fileContent);
    return "data:" + mimeType + ";base64," + encoded;
  }

  /**
   * Decodes a Base64 data URI or reads a file path, returning a BufferedImage.
   */
  public static BufferedImage loadBufferedImage(String source) {
    if (source == null || source.isBlank()) {
      return null;
    }
    final var ref = IMAGE_CACHE.get(source);
    if (ref != null) {
      final var cached = ref.get();
      if (cached != null) {
        return cached;
      }
    }
    final var img = loadBufferedImageInternal(source);
    if (img != null) {
      IMAGE_CACHE.put(source, new SoftReference<>(img));
    }
    return img;
  }

  public static final int MAX_IMAGE_DIMENSION = 2048;

  public static BufferedImage enforceMaxDimensions(BufferedImage img) {
    if (img == null) return null;
    final var width = img.getWidth();
    final var height = img.getHeight();
    if (width <= MAX_IMAGE_DIMENSION && height <= MAX_IMAGE_DIMENSION) {
      return img;
    }
    final var scale = Math.min((double) MAX_IMAGE_DIMENSION / width, (double) MAX_IMAGE_DIMENSION / height);
    final var targetWidth = Math.max(1, (int) (width * scale));
    final var targetHeight = Math.max(1, (int) (height * scale));
    final var imageType = img.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : img.getType();
    final var scaled = new BufferedImage(targetWidth, targetHeight, imageType);
    final var g2 = scaled.createGraphics();
    g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g2.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
    g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
    g2.drawImage(img, 0, 0, targetWidth, targetHeight, null);
    g2.dispose();
    logger.info("Downscaled large image from {}x{} to {}x{}", width, height, targetWidth, targetHeight);
    return scaled;
  }

  public static int[] getOptimizedTargetDimensions(BufferedImage img, int frameW, int frameH, String scaleOpt) {
    if (img == null || frameW <= 0 || frameH <= 0) return new int[] {0, 0};
    final var w = img.getWidth();
    final var h = img.getHeight();
    if ("stretch".equalsIgnoreCase(scaleOpt) || "cover".equalsIgnoreCase(scaleOpt)) {
      return new int[] {frameW, frameH};
    }
    final var scale = Math.min((double) frameW / w, (double) frameH / h);
    if (scale >= 1.0) {
      return new int[] {w, h};
    }
    final var targetW = Math.max(1, (int) (w * scale));
    final var targetH = Math.max(1, (int) (h * scale));
    return new int[] {targetW, targetH};
  }

  public static BufferedImage optimizeImage(BufferedImage img, int frameW, int frameH, String scaleOpt) {
    if (img == null || frameW <= 0 || frameH <= 0) return img;
    final var targetDims = getOptimizedTargetDimensions(img, frameW, frameH, scaleOpt);
    final var targetW = targetDims[0];
    final var targetH = targetDims[1];
    if (targetW <= 0 || targetH <= 0) return img;
    if (img.getWidth() == targetW && img.getHeight() == targetH) return img;

    final var imageType = img.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : img.getType();
    final var scaled = new BufferedImage(targetW, targetH, imageType);
    final var g2 = scaled.createGraphics();
    g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g2.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
    g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

    if ("cover".equalsIgnoreCase(scaleOpt)) {
      final var imgW = img.getWidth();
      final var imgH = img.getHeight();
      final var scaleFactor = Math.max((double) targetW / imgW, (double) targetH / imgH);
      final var coveredW = Math.max(1, (int) (imgW * scaleFactor));
      final var coveredH = Math.max(1, (int) (imgH * scaleFactor));
      final var coveredX = (targetW - coveredW) / 2;
      final var coveredY = (targetH - coveredH) / 2;
      g2.drawImage(img, coveredX, coveredY, coveredW, coveredH, null);
    } else {
      g2.drawImage(img, 0, 0, targetW, targetH, null);
    }
    g2.dispose();
    logger.info("Optimized image (mode={}) from {}x{} to {}x{}", scaleOpt, img.getWidth(), img.getHeight(), targetW, targetH);
    return scaled;
  }

  private static BufferedImage loadBufferedImageInternal(String source) {
    try {
      BufferedImage img = null;
      if (source.startsWith("data:image/")) {
        final var commaIdx = source.indexOf(',');
        if (commaIdx >= 0) {
          final var base64Data = source.substring(commaIdx + 1).replaceAll("\\s+", "");
          final var imageBytes = Base64.getDecoder().decode(base64Data);
          img = ImageIO.read(new ByteArrayInputStream(imageBytes));
        }
      } else {
        final var file = new File(source);
        if (file.exists() && file.isFile()) {
          img = ImageIO.read(file);
        }
      }
      return enforceMaxDimensions(img);
    } catch (Exception e) {
      logger.error(
          "Failed to load image from source: {}",
          source.length() > 50 ? source.substring(0, 50) + "..." : source,
          e);
    }
    return null;
  }

  public static String bufferedImageToBase64(BufferedImage img) {
    if (img == null) return "";
    try (final var baos = new java.io.ByteArrayOutputStream()) {
      ImageIO.write(img, "png", baos);
      final var bytes = baos.toByteArray();
      final var encoded = Base64.getEncoder().encodeToString(bytes);
      return "data:image/png;base64," + encoded;
    } catch (Exception e) {
      logger.error("Failed to convert BufferedImage to Base64", e);
      return "";
    }
  }

  // Flood fill from image borders to make outer white background transparent
  public static BufferedImage makeWhiteTransparent(BufferedImage src) {
    if (src == null) return null;
    final var w = src.getWidth();
    final var h = src.getHeight();
    final var out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    final var visited = new boolean[w][h];
    final var queue = new java.util.ArrayDeque<int[]>();

    for (var y = 0; y < h; y++) {
      for (var x = 0; x < w; x++) {
        out.setRGB(x, y, src.getRGB(x, y));
      }
    }

    java.util.function.BiPredicate<Integer, Integer> isWhite = (x, y) -> {
      final var rgb = src.getRGB(x, y);
      final var a = (rgb >> 24) & 0xFF;
      final var r = (rgb >> 16) & 0xFF;
      final var g = (rgb >> 8) & 0xFF;
      final var b = rgb & 0xFF;
      return a > 0 && r > 235 && g > 235 && b > 235;
    };

    // Start flood fill from all four outer borders
    for (var x = 0; x < w; x++) {
      if (isWhite.test(x, 0)) {
        visited[x][0] = true;
        queue.add(new int[]{x, 0});
      }
      if (isWhite.test(x, h - 1)) {
        visited[x][h - 1] = true;
        queue.add(new int[]{x, h - 1});
      }
    }
    for (var y = 0; y < h; y++) {
      if (isWhite.test(0, y) && !visited[0][y]) {
        visited[0][y] = true;
        queue.add(new int[]{0, y});
      }
      if (isWhite.test(w - 1, y) && !visited[w - 1][y]) {
        visited[w - 1][y] = true;
        queue.add(new int[]{w - 1, y});
      }
    }

    final int[] dx = {0, 0, 1, -1};
    final int[] dy = {1, -1, 0, 0};

    while (!queue.isEmpty()) {
      final var curr = queue.poll();
      final var cx = curr[0];
      final var cy = curr[1];

      final var rgb = src.getRGB(cx, cy);
      out.setRGB(cx, cy, 0x00FFFFFF & rgb);

      for (var i = 0; i < 4; i++) {
        final var nx = cx + dx[i];
        final var ny = cy + dy[i];
        if (nx >= 0 && nx < w && ny >= 0 && ny < h && !visited[nx][ny]) {
          if (isWhite.test(nx, ny)) {
            visited[nx][ny] = true;
            queue.add(new int[]{nx, ny});
          }
        }
      }
    }

    return out;
  }

  public static BufferedImage toGrayscale(BufferedImage src) {
    if (src == null) return null;
    final var w = src.getWidth();
    final var h = src.getHeight();
    final var out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    for (var y = 0; y < h; y++) {
      for (var x = 0; x < w; x++) {
        final var rgb = src.getRGB(x, y);
        final var a = (rgb >> 24) & 0xFF;
        final var r = (rgb >> 16) & 0xFF;
        final var g = (rgb >> 8) & 0xFF;
        final var b = rgb & 0xFF;
        final var gray = (int) (0.2126 * r + 0.7152 * g + 0.0722 * b);
        final var newRgb = (a << 24) | (gray << 16) | (gray << 8) | gray;
        out.setRGB(x, y, newRgb);
      }
    }
    return out;
  }

  public static BufferedImage toBufferedImage(java.awt.Image img) {
    if (img == null) return null;
    if (img instanceof BufferedImage b) return enforceMaxDimensions(b);
    final var w = img.getWidth(null);
    final var h = img.getHeight(null);
    if (w <= 0 || h <= 0) return null;
    final var bimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    final var g2 = bimg.createGraphics();
    g2.drawImage(img, 0, 0, null);
    g2.dispose();
    return enforceMaxDimensions(bimg);
  }

  public static BufferedImage getSystemClipboardImage() {
    try {
      final var sysClip = Toolkit.getDefaultToolkit().getSystemClipboard();
      if (sysClip == null) return null;

      // Prefer javaFileListFlavor first to get actual image files copied in file explorer
      if (sysClip.isDataFlavorAvailable(DataFlavor.javaFileListFlavor)) {
        @SuppressWarnings("unchecked")
        final var files = (List<File>) sysClip.getData(DataFlavor.javaFileListFlavor);
        if (files != null) {
          for (final var file : files) {
            final var name = file.getName().toLowerCase();
            if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")
                || name.endsWith(".gif") || name.endsWith(".bmp") || name.endsWith(".wbmp")) {
              final var buf = loadBufferedImageInternal(file.getAbsolutePath());
              if (buf != null) {
                return buf;
              }
            }
          }
        }
      }

      if (sysClip.isDataFlavorAvailable(DataFlavor.imageFlavor)) {
        final var obj = sysClip.getData(DataFlavor.imageFlavor);
        if (obj instanceof java.awt.Image img) {
          final var buf = toBufferedImage(img);
          if (buf != null) {
            return buf;
          }
        }
      }

      final var flavors = sysClip.getAvailableDataFlavors();
      if (flavors != null) {
        for (final var flavor : flavors) {
          try {
            if (flavor.isMimeTypeEqual("image/png")
                || flavor.isMimeTypeEqual("image/jpeg")
                || flavor.isMimeTypeEqual("image/gif")
                || flavor.isMimeTypeEqual("image/bmp")
                || (flavor.getPrimaryType() != null && flavor.getPrimaryType().equalsIgnoreCase("image"))) {
              final var obj = sysClip.getData(flavor);
              if (obj instanceof java.awt.Image img) {
                final var buf = toBufferedImage(img);
                if (buf != null) return buf;
              } else if (obj instanceof java.io.InputStream stream) {
                final var buf = ImageIO.read(stream);
                if (buf != null) return buf;
              } else if (obj instanceof byte[] bytes) {
                final var buf = ImageIO.read(new ByteArrayInputStream(bytes));
                if (buf != null) return buf;
              }
            }
          } catch (Exception ignored) {
          }
        }
      }
    } catch (Exception e) {
      logger.debug("Failed to get image from system clipboard", e);
    }
    return null;
  }

  public static boolean hasSystemClipboardImage() {
    return getSystemClipboardImage() != null;
  }
}
