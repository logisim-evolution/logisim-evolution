/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import static com.cburch.logisim.util.Strings.S;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.swing.ProgressMonitor;

/**
 * GIFEncoder is a class which takes an image and saves it to a stream using the GIF file format (<A
 * HREF="http://www.dcs.ed.ac.uk/%7Emxr/gfx/">Graphics Interchange Format</A>). A GIFEncoder is
 * constructed with either an AWT Image (which must be fully loaded) or a set of RGB arrays. The
 * image can be written out with a call to <CODE>Write</CODE>.
 *
 * <p>Three caveats:
 *
 * <UL>
 *   <LI>GIFEncoder will convert the image to indexed color upon construction. This will take some
 *       time, depending on the size of the image. Also, actually writing the image out (Write) will
 *       take time.
 *   <LI>The image cannot have more than 256 colors, since GIF is an 8 bit format. For a 24 bit to 8
 *       bit quantization algorithm, see Graphics Gems II III.2 by Xialoin Wu. Or check out his <A
 *       HREF="http://www.csd.uwo.ca/faculty/wu/cq.c">C source</A>.
 *   <LI>Since the image must be completely loaded into memory, GIFEncoder may have problems with
 *       large images. Attempting to encode an image which will not fit into memory will probably
 *       result in the following exception:
 *       <CODE>java.awt.AWTException: Grabber returned false: 192</CODE>
 * </UL>
 *
 * <p>GIFEncoder is based upon gifsave.c, which was written and released by:
 *
 * <p>Sverre H. Huseby<br>
 * Bjoelsengt. 17<br>
 * N-0468 Oslo<br>
 * Norway
 *
 * <p>Phone: +47 2 230539<br>
 * sverrehu@ifi.uio.no
 *
 * @version 0.90 21 Apr 1996
 * @author <A HREF="http://www.cs.brown.edu/people/amd/">Adam Doppelt</A>
 */
public class GifEncoder {
  private static class BitFile {
    final OutputStream outputStream;
    final byte[] buffer;
    int index;
    int bitsLeft;

    BitFile(OutputStream output) {
      outputStream = output;
      buffer = new byte[256];
      index = 0;
      bitsLeft = 8;
    }

    void flush() throws IOException {
      int numBytes = index + (bitsLeft == 8 ? 0 : 1);
      if (numBytes > 0) {
        outputStream.write(numBytes);
        outputStream.write(buffer, 0, numBytes);
        buffer[0] = 0;
        index = 0;
        bitsLeft = 8;
      }
    }

    void writeBits(int bits, int numbits) throws IOException {
      // int bitsWritten = 0;
      int numBytes = 255;
      do {
        if ((index == 254 && bitsLeft == 0) || index > 254) {
          outputStream.write(numBytes);
          outputStream.write(buffer, 0, numBytes);

          buffer[0] = 0;
          index = 0;
          bitsLeft = 8;
        }

        if (numbits <= bitsLeft) {
          buffer[index] |= (bits & ((1 << numbits) - 1)) << (8 - bitsLeft);
          // bitsWritten += numbits;
          bitsLeft -= numbits;
          numbits = 0;
        } else {
          buffer[index] |= (bits & ((1 << bitsLeft) - 1)) << (8 - bitsLeft);
          // bitsWritten += bitsLeft_;
          bits >>= bitsLeft;
          numbits -= bitsLeft;
          buffer[++index] = 0;
          bitsLeft = 8;
        }
      } while (numbits != 0);
    }
  }

  private static class BitUtils {
    static byte bitsNeeded(int n) {
      byte ret = 1;

      if (n-- == 0) return 0;

      while ((n >>= 1) != 0) ++ret;

      return ret;
    }

    static void writeString(OutputStream output, String string) throws IOException {
      for (int loop = 0; loop < string.length(); ++loop) output.write((byte) (string.charAt(loop)));
    }

    static void writeWord(OutputStream output, short w) throws IOException {
      output.write(w & 0xFF);
      output.write((w >> 8) & 0xFF);
    }
  }

  private static class ImageDescriptor {
    final byte separator;
    final short leftPosition;
    final short topPosition;
    final short width;
    final short height;
    private byte byteVal;

    ImageDescriptor(short width, short height, char separator) {
      this.separator = (byte) separator;
      leftPosition = 0;
      topPosition = 0;
      this.width = width;
      this.height = height;
      setLocalColorTableSize((byte) 0);
      setReserved((byte) 0);
      setSortFlag((byte) 0);
      setInterlaceFlag((byte) 0);
      setLocalColorTableFlag((byte) 0);
    }

    void setInterlaceFlag(byte num) {
      byteVal |= (num & 1) << 6;
    }

    void setLocalColorTableFlag(byte num) {
      byteVal |= (num & 1) << 7;
    }

    void setLocalColorTableSize(byte num) {
      byteVal |= (num & 7);
    }

    void setReserved(byte num) {
      byteVal |= (num & 3) << 3;
    }

    void setSortFlag(byte num) {
      byteVal |= (num & 1) << 5;
    }

    void write(OutputStream output) throws IOException {
      output.write(separator);
      BitUtils.writeWord(output, leftPosition);
      BitUtils.writeWord(output, topPosition);
      BitUtils.writeWord(output, width);
      BitUtils.writeWord(output, height);
      output.write(byteVal);
    }
  }

  private static class LZWCompressor {
    static void lzwCompress(OutputStream output, int codesize, byte[] toCompress)
        throws IOException {
      byte c;
      short index;
      int clearcode;
      int endofinfo;
      int numbits;

      short prefix = (short) 0xFFFF;

      clearcode = 1 << codesize;
      endofinfo = clearcode + 1;

      numbits = codesize + 1;
      int limit = (1 << numbits) - 1;

      var strings = new LZWStringTable();
      strings.clearTable(codesize);

      final var bitFile = new BitFile(output);
      bitFile.writeBits(clearcode, numbits);

      for (byte compress : toCompress) {
        c = compress;
        if ((index = strings.findCharString(prefix, c)) != -1) prefix = index;
        else {
          bitFile.writeBits(prefix, numbits);
          if (strings.addCharString(prefix, c) > limit) {
            if (++numbits > 12) {
              bitFile.writeBits(clearcode, numbits - 1);
              strings.clearTable(codesize);
              numbits = codesize + 1;
            }
            limit = (1 << numbits) - 1;
          }

          prefix = (short) ((short) c & 0xFF);
        }
      }

      if (prefix != -1) bitFile.writeBits(prefix, numbits);

      bitFile.writeBits(endofinfo, numbits);
      bitFile.flush();
    }
  }

  private static class LZWStringTable {
    static int hash(short index, byte lastbyte) {
      return (((short) (lastbyte << 8) ^ index) & 0xFFFF) % HASHSIZE;
    }

    private static final int RES_CODES = 2;
    private static final short HASH_FREE = (short) 0xFFFF;
    private static final short NEXT_FIRST = (short) 0xFFFF;
    private static final int MAXBITS = 12;
    private static final int MAXSTR = (1 << MAXBITS);
    private static final short HASHSIZE = 9973;

    private static final short HASHSTEP = 2039;
    final byte[] strChr;
    final short[] strNext;
    final short[] strHash;

    short numStrings;

    LZWStringTable() {
      strChr = new byte[MAXSTR];
      strNext = new short[MAXSTR];
      strHash = new short[HASHSIZE];
    }

    int addCharString(short index, byte b) {
      int hshidx;

      if (numStrings >= MAXSTR) return 0xFFFF;

      hshidx = hash(index, b);
      while (strHash[hshidx] != HASH_FREE) hshidx = (hshidx + HASHSTEP) % HASHSIZE;

      strHash[hshidx] = numStrings;
      strChr[numStrings] = b;
      strNext[numStrings] = (index != HASH_FREE) ? index : NEXT_FIRST;

      return numStrings++;
    }

    void clearTable(int codesize) {
      numStrings = 0;

      for (int q = 0; q < HASHSIZE; q++) {
        strHash[q] = HASH_FREE;
      }

      int w = (1 << codesize) + RES_CODES;
      for (int q = 0; q < w; q++) addCharString((short) 0xFFFF, (byte) q);
    }

    short findCharString(short index, byte b) {
      int hashIndex;
      int nextIndex;

      if (index == HASH_FREE) return b;

      hashIndex = hash(index, b);
      while ((nextIndex = strHash[hashIndex]) != HASH_FREE) {
        if (strNext[nextIndex] == index && strChr[nextIndex] == b) return (short) nextIndex;
        hashIndex = (hashIndex + HASHSTEP) % HASHSIZE;
      }

      return (short) 0xFFFF;
    }
  }

  private static class MyGrabber extends PixelGrabber {
    final ProgressMonitor monitor;
    int progress;
    final int goal;

    MyGrabber(
        ProgressMonitor monitor,
        Image image,
        int x,
        int y,
        int width,
        int height,
        int[] values,
        int start,
        int scan) {
      super(image, x, y, width, height, values, start, scan);
      this.monitor = monitor;
      progress = 0;
      goal = width * height;
      monitor.setMinimum(0);
      monitor.setMaximum(goal * 21 / 20);
    }

    @Override
    public void setPixels(
        int srcX,
        int srcY,
        int srcW,
        int srcH,
        ColorModel model,
        int[] pixels,
        int srcOff,
        int srcScan) {
      progress += srcW * srcH;
      monitor.setProgress(progress);
      if (monitor.isCanceled()) {
        abortGrabbing();
      } else {
        super.setPixels(srcX, srcY, srcW, srcH, model, pixels, srcOff, srcScan);
      }
    }
  }

  private static class ScreenDescriptor {
    final short localScreenWidth;
    final short localScreenHeight;
    private byte byteVal;
    final byte backgroundColorIndex;
    final byte pixelAspectRatio;

    ScreenDescriptor(short width, short height, int numColors) {
      localScreenWidth = width;
      localScreenHeight = height;
      setGlobalColorTableSize((byte) (BitUtils.bitsNeeded(numColors) - 1));
      setGlobalColorTableFlag((byte) 1);
      setSortFlag((byte) 0);
      setColorResolution((byte) 7);
      backgroundColorIndex = 0;
      pixelAspectRatio = 0;
    }

    void setColorResolution(byte num) {
      byteVal |= (num & 7) << 4;
    }

    void setGlobalColorTableFlag(byte num) {
      byteVal |= (num & 1) << 7;
    }

    void setGlobalColorTableSize(byte num) {
      byteVal |= (num & 7);
    }

    void setSortFlag(byte num) {
      byteVal |= (num & 1) << 3;
    }

    void write(OutputStream output) throws IOException {
      BitUtils.writeWord(output, localScreenWidth);
      BitUtils.writeWord(output, localScreenHeight);
      output.write(byteVal);
      output.write(backgroundColorIndex);
      output.write(pixelAspectRatio);
    }
  }

  public static void toFile(Image img, File file) throws IOException, AWTException {
    toFile(img, file, null);
  }

  public static void toFile(Image img, File file, ProgressMonitor monitor)
      throws IOException, AWTException {
    FileOutputStream out = new FileOutputStream(file);
    new GifEncoder(img, monitor).write(out);
    out.close();
  }

  public static void toFile(Image img, String filename) throws IOException, AWTException {
    toFile(img, filename, null);
  }

  public static void toFile(Image img, String filename, ProgressMonitor monitor)
      throws IOException, AWTException {
    FileOutputStream out = new FileOutputStream(filename);
    new GifEncoder(img, monitor).write(out);
    out.close();
  }

  private final short width;
  private final short height;

  private int numColors;

  private byte[] pixels;
  private byte[] colors;

  /**
   * Construct a GifEncoder. The constructor will convert the image to an indexed color array.
   * <B>This may take some time.</B>
   *
   * <p>Each array stores intensity values for the image. In other words, r[x][y] refers to the red
   * intensity of the pixel at column x, row y.
   *
   * @param r An array containing the red intensity values.
   * @param g An array containing the green intensity values.
   * @param b An array containing the blue intensity values.
   * @exception AWTException Will be thrown if the image contains more than 256 colors.
   */
  public GifEncoder(byte[][] r, byte[][] g, byte[][] b) throws AWTException {
    width = (short) (r.length);
    height = (short) (r[0].length);

    toIndexedColor(r, g, b);
  }

  /**
   * Construct a GIFEncoder. The constructor will convert the image to an indexed color array.
   * <B>This may take some time.</B>
   *
   * @param image The image to encode. The image <B>must</B> be completely loaded.
   * @exception AWTException Will be thrown if the pixel grab fails. This can happen if Java runs
   *     out of memory. It may also indicate that the image contains more than 256 colors.
   */
  public GifEncoder(Image image, ProgressMonitor monitor) throws AWTException {
    width = (short) image.getWidth(null);
    height = (short) image.getHeight(null);

    int[] values = new int[width * height];
    PixelGrabber grabber;
    if (monitor != null) {
      grabber = new MyGrabber(monitor, image, 0, 0, width, height, values, 0, width);
    } else {
      grabber = new PixelGrabber(image, 0, 0, width, height, values, 0, width);
    }

    try {
      if (!grabber.grabPixels())
        throw new AWTException(S.get("grabberError") + ": " + grabber.status());
    } catch (InterruptedException ignored) {
    }

    byte[][] r = new byte[width][height];
    byte[][] g = new byte[width][height];
    byte[][] b = new byte[width][height];
    int index = 0;
    for (int y = 0; y < height; ++y)
      for (int x = 0; x < width; ++x) {
        r[x][y] = (byte) ((values[index] >> 16) & 0xFF);
        g[x][y] = (byte) ((values[index] >> 8) & 0xFF);
        b[x][y] = (byte) ((values[index]) & 0xFF);
        ++index;
      }
    toIndexedColor(r, g, b);
  }

  void toIndexedColor(byte[][] r, byte[][] g, byte[][] b) throws AWTException {
    pixels = new byte[width * height];
    colors = new byte[256 * 3];
    int colornum = 0;
    for (int x = 0; x < width; ++x) {
      for (int y = 0; y < height; ++y) {
        int search;
        for (search = 0; search < colornum; ++search)
          if (colors[search * 3] == r[x][y]
              && colors[search * 3 + 1] == g[x][y]
              && colors[search * 3 + 2] == b[x][y]) break;

        if (search > 255) throw new AWTException(S.get("manyColorError"));

        pixels[y * width + x] = (byte) search;

        if (search == colornum) {
          colors[search * 3] = r[x][y];
          colors[search * 3 + 1] = g[x][y];
          colors[search * 3 + 2] = b[x][y];
          ++colornum;
        }
      }
    }
    numColors = 1 << BitUtils.bitsNeeded(colornum);
    byte[] copy = new byte[numColors * 3];
    System.arraycopy(colors, 0, copy, 0, numColors * 3);
    colors = copy;
  }

  /**
   * Writes the image out to a stream in the GIF file format. This will be a single GIF87a image,
   * non-interlaced, with no background color. <B>This may take some time.</B>
   *
   * @param output The stream to output to. This should probably be a buffered stream.
   * @exception IOException Will be thrown if a write operation fails.
   */
  public void write(OutputStream output) throws IOException {
    BitUtils.writeString(output, "GIF87a");

    ScreenDescriptor sd = new ScreenDescriptor(width, height, numColors);
    sd.write(output);

    output.write(colors, 0, colors.length);

    ImageDescriptor id = new ImageDescriptor(width, height, ',');
    id.write(output);

    byte codesize = BitUtils.bitsNeeded(numColors);
    if (codesize == 1) ++codesize;
    output.write(codesize);

    LZWCompressor.lzwCompress(output, codesize, pixels);
    output.write(0);

    id = new ImageDescriptor((byte) 0, (byte) 0, ';');
    id.write(output);
    output.flush();
  }
}
