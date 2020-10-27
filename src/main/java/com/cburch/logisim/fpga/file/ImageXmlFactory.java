/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.fpga.file;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageXmlFactory {

  static final Logger logger = LoggerFactory.getLogger(ImageXmlFactory.class);

  private String[] CodeTable;
  private StringBuffer AsciiStream;
  private String[] InitialCodeTable = {
    "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s",
    "t", "u", "v", "w", "x", "y", "z", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L",
    "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "0", "1", "2", "3", "4",
    "5", "6", "7", "8", "9", "(", ")", "+a", "+b", "+c", "+d", "+e", "+f", "+g", "+h", "+i", "+j",
    "+k", "+l", "+m", "+n", "+o", "+p", "+q", "+r", "+s", "+t", "+u", "+v", "+w", "+x", "+y", "+z",
    "+A", "+B", "+C", "+D", "+E", "+F", "+G", "+H", "+I", "+J", "+K", "+L", "+M", "+N", "+O", "+P",
    "+Q", "+R", "+S", "+T", "+U", "+V", "+W", "+X", "+Y", "+Z", "+0", "+1", "+2", "+3", "+4", "+5",
    "+6", "+7", "+8", "+9", "+(", "+)", "-a", "-b", "-c", "-d", "-e", "-f", "-g", "-h", "-i", "-j",
    "-k", "-l", "-m", "-n", "-o", "-p", "-q", "-r", "-s", "-t", "-u", "-v", "-w", "-x", "-y", "-z",
    "-A", "-B", "-C", "-D", "-E", "-F", "-G", "-H", "-I", "-J", "-K", "-L", "-M", "-N", "-O", "-P",
    "-Q", "-R", "-S", "-T", "-U", "-V", "-W", "-X", "-Y", "-Z", "-0", "-1", "-2", "-3", "-4", "-5",
    "-6", "-7", "-8", "-9", "-(", "-)", "=a", "=b", "=c", "=d", "=e", "=f", "=g", "=h", "=i", "=j",
    "=k", "=l", "=m", "=n", "=o", "=p", "=q", "=r", "=s", "=t", "=u", "=v", "=w", "=x", "=y", "=z",
    "=A", "=B", "=C", "=D", "=E", "=F", "=G", "=H", "=I", "=J", "=K", "=L", "=M", "=N", "=O", "=P",
    "=Q", "=R", "=S", "=T", "=U", "=V", "=W", "=X", "=Y", "=Z", "=0", "=1", "=2", "=3", "=4", "=5",
    "=6", "=7", "=8", "=9", "=(", "=)"
  };
  private char V2_Identifier = '@';

  private String[] CreateCodeTable(byte stream[]) {
    String[] result = new String[256];
    Long[] ocurances = new Long[256];
    int[] index = new int[256];
    for (int i = 0; i < 256; i++) {
      ocurances[i] = (long) 0;
      index[i] = i;
    }
    for (int i = 0; i < stream.length; i++) {
      ocurances[stream[i] + 128]++;
    }
    boolean swapped = true;
    while (swapped) {
      swapped = false;
      for (int i = 0; i < 255; i++) {
        if (ocurances[i] < ocurances[i + 1]) {
          swapped = true;
          int temp = index[i];
          index[i] = index[i + 1];
          index[i + 1] = temp;
          long swap = ocurances[i];
          ocurances[i] = ocurances[i + 1];
          ocurances[i + 1] = swap;
        }
      }
    }
    for (int i = 0; i < 256; i++) {
      result[index[i]] = InitialCodeTable[i];
    }
    return result;
  }

  public void CreateStream(Image BoardPicture) {
    BufferedImage result = new BufferedImage(740, 400, BufferedImage.TYPE_3BYTE_BGR);
    Graphics2D g2 = result.createGraphics();
    int width = BoardPicture.getWidth(null);
    int hight = BoardPicture.getHeight(null);
    PixelGrabber pixelGrabber = new PixelGrabber(BoardPicture, 0, 0, width, hight, false);
    try {
      pixelGrabber.grabPixels();
    } catch (Exception e) {
      /* TODO: handle exceptions */
      logger.error("PixelGrabber exception: {}", e.getMessage());
    }
    ColorModel color_model = pixelGrabber.getColorModel();
    if (pixelGrabber.getPixels() instanceof byte[]) {
      byte[] the_pixels = (byte[]) pixelGrabber.getPixels();
      int index = 0;
      for (int y = 0; y < hight; y++) {
        for (int x = 0; x < width; x++) {
          Color PixCol =
              new Color(
                  color_model.getRed(the_pixels[index]),
                  color_model.getGreen(the_pixels[index]),
                  color_model.getBlue(the_pixels[index++]));
          g2.setColor(PixCol);
          g2.fillRect(x, y, 1, 1);
        }
      }
    } else {
      int[] the_pixels = (int[]) pixelGrabber.getPixels();
      int index = 0;
      for (int y = 0; y < hight; y++) {
        for (int x = 0; x < width; x++) {
          Color PixCol =
              new Color(
                  color_model.getRed(the_pixels[index]),
                  color_model.getGreen(the_pixels[index]),
                  color_model.getBlue(the_pixels[index++]));
          g2.setColor(PixCol);
          g2.fillRect(x, y, 1, 1);
        }
      }
    }
    ByteArrayOutputStream blaat = new ByteArrayOutputStream();
    try {
      ImageIO.write(result, "jpg", blaat);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      logger.error("JPEG Writer exception: {}", e.getMessage());
    }
    byte data[] = blaat.toByteArray();
    CodeTable = CreateCodeTable(data);
    AsciiStream = new StringBuffer();
    AsciiStream.append(V2_Identifier);
    for (int i = 0; i < data.length; i++) {
      String code = CodeTable[data[i] + 128];
      AsciiStream.append(code);
    }
  }

  public String GetCodeTable() {
    StringBuffer result = new StringBuffer();
    for (int i = 0; i < CodeTable.length; i++) {
      if (i != 0) {
        result.append(" ");
      }
      result.append(CodeTable[i]);
    }
    return result.toString();
  }

  public String GetCompressedString() {
    return AsciiStream.toString();
  }

  public BufferedImage GetPicture(int width, int height) {
    if (AsciiStream == null) return null;
    if (CodeTable == null) return null;
    if (CodeTable.length != 256) return null;
    BufferedImage result = null;
    Map<String, Integer> CodeLookupTable = new HashMap<String, Integer>();
    for (int i = 0; i < CodeTable.length; i++) CodeLookupTable.put(CodeTable[i], i);
    int index = 0;
    Set<String> TwoCodes = new HashSet<String>();
    TwoCodes.add("-");
    TwoCodes.add("+");
    TwoCodes.add("=");
    boolean jpegCompressed = AsciiStream.charAt(0) == V2_Identifier;
    if (jpegCompressed) {
      index++;
      ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
      while (index < AsciiStream.length()) {
        if (TwoCodes.contains(AsciiStream.substring(index, index + 1))) {
          bytestream.write(
              (byte) (CodeLookupTable.get(AsciiStream.substring(index, index + 2)) - 128));
          index += 2;
        } else {
          bytestream.write(
              (byte) (CodeLookupTable.get(AsciiStream.substring(index, index + 1)) - 128));
          index++;
        }
      }
      try {
        bytestream.flush();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      ByteArrayInputStream instream = new ByteArrayInputStream(bytestream.toByteArray());
      try {
        result = ImageIO.read(instream);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    } else {
      result = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
      Graphics2D g2 = result.createGraphics();
      g2.setBackground(Color.BLACK);
      String CurRedComp, CurGreenComp, CurBlueComp;
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          if (TwoCodes.contains(AsciiStream.substring(index, index + 1))) {
            CurRedComp = AsciiStream.substring(index, index + 2);
            index += 2;
          } else {
            CurRedComp = AsciiStream.substring(index, index + 1);
            index++;
          }
          if (TwoCodes.contains(AsciiStream.substring(index, index + 1))) {
            CurGreenComp = AsciiStream.substring(index, index + 2);
            index += 2;
          } else {
            CurGreenComp = AsciiStream.substring(index, index + 1);
            index++;
          }
          if (TwoCodes.contains(AsciiStream.substring(index, index + 1))) {
            CurBlueComp = AsciiStream.substring(index, index + 2);
            index += 2;
          } else {
            CurBlueComp = AsciiStream.substring(index, index + 1);
            index++;
          }
          if (!CodeLookupTable.containsKey(CurRedComp)
              || !CodeLookupTable.containsKey(CurGreenComp)
              || !CodeLookupTable.containsKey(CurBlueComp)) {
            return null;
          }
          Color PixCol =
              new Color(
                  CodeLookupTable.get(CurRedComp),
                  CodeLookupTable.get(CurGreenComp),
                  CodeLookupTable.get(CurBlueComp));
          g2.setColor(PixCol);
          g2.fillRect(x, y, 1, 1);
        }
      }
    }
    return result;
  }

  public void SetCodeTable(String[] Table) {
    CodeTable = Table.clone();
  }

  public void SetCompressedString(String stream) {
    AsciiStream = new StringBuffer();
    AsciiStream.append(stream);
  }
}
