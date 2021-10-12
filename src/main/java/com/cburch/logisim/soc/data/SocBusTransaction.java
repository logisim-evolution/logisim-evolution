/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.data;

import static com.cburch.logisim.soc.Strings.S;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics2D;

public class SocBusTransaction {

  public static final int BLOCK_SKIP = 2;
  public static final int BLOCK_MARKER = 14;
  public static final int BLOCK_HEX = 78;

  public static final int READ_TRANSACTION = 1;
  public static final int WRITE_TRANSACTION = 2;
  public static final int ATOMIC_TRANSACTION = 4;

  public static final int NO_ERROR = 0;
  public static final int NO_RESPONS_ERROR = 1;
  public static final int NO_SLAVES_ERROR = 2;
  public static final int MULTIPLE_SLAVES_ERROR = 3;
  public static final int NONE_ATOMIC_READ_WRITE_ERROR = 4;
  public static final int NO_SOC_BUS_CONNECTED_ERROR = 5;
  public static final int MISALIGNED_ADDRESS_ERROR = 6;
  public static final int ACCESS_TYPE_NOT_SUPPORTED_ERROR = 7;
  public static final int READ_ONLY_ACCESS_ERROR = 8;
  public static final int WRITE_ONLY_ACCESS_ERROR = 9;
  public static final int REGISTER_DOES_NOT_EXIST_ERROR = 10;


  public static final int BYTE_ACCESS = 1;
  public static final int HALF_WORD_ACCESS = 2;
  public static final int WORD_ACCESS = 3;

  private final int address;
  private final int writeData;
  private int readData;
  private final int type;
  private final int access;
  private final Object master;
  private Component slave;
  private int error;
  private boolean hidden;

  public SocBusTransaction(int type, int addr, int value, int access, Object master) {
    this.type = type;
    this.address = addr;
    this.writeData = value;
    this.access = access;
    this.master = master;
    slave = null;
    readData = 0;
    error = NO_ERROR;
    hidden = false;
  }

  public void setAsHiddenTransaction() {
    hidden = true;
  }

  public boolean isHidden() {
    return hidden;
  }

  public int getAccessType() {
    return access;
  }

  public String getErrorMessage() {
    return switch (error) {
      case NO_ERROR -> S.get("SocTransactionSuccessfull");
      case NO_RESPONS_ERROR -> S.get("SocTransactionNoRespons");
      case NO_SLAVES_ERROR -> S.get("SocTransactionNoSlavesAttached");
      case MULTIPLE_SLAVES_ERROR -> S.get("SocTransactionMultipleSlaveAnswers");
      case NONE_ATOMIC_READ_WRITE_ERROR -> S.get("SocTransactionNoneAtomicRW");
      case NO_SOC_BUS_CONNECTED_ERROR -> S.get("SocTransactionNoBusConnected");
      case MISALIGNED_ADDRESS_ERROR -> S.get("SocTransactionMisalignedAddress");
      case ACCESS_TYPE_NOT_SUPPORTED_ERROR -> switch (access) {
        case BYTE_ACCESS -> S.get("SocTransactionByteAccesNoSupport");
        case HALF_WORD_ACCESS -> S.get("SocTransactionHalfWordAccesNoSupport");
        default -> S.get("SocTransactionWordAccesNoSupport");
      };
      case READ_ONLY_ACCESS_ERROR -> S.get("SocTransactionReadOnlyAccessError");
      case WRITE_ONLY_ACCESS_ERROR -> S.get("SocTransactionWriteOnlyAccessError");
      case REGISTER_DOES_NOT_EXIST_ERROR -> S.get("SocTransactionRegisterDoesNotExist");
      default -> S.get("SocTransactionUnknownError");
    };
  }

  public String getShortErrorMessage() {
    return switch (error) {
      case NO_ERROR -> S.get("SocTransactionSuccessfullShort");
      case NO_RESPONS_ERROR -> S.get("SocTransactionNoResponsShort");
      case NO_SLAVES_ERROR -> S.get("SocTransactionNoSlavesAttachedShort");
      case MULTIPLE_SLAVES_ERROR -> S.get("SocTransactionMultipleSlaveAnswersShort");
      case NONE_ATOMIC_READ_WRITE_ERROR -> S.get("SocTransactionNoneAtomicRWShort");
      case NO_SOC_BUS_CONNECTED_ERROR -> S.get("SocTransactionNoBusConnectedShort");
      case MISALIGNED_ADDRESS_ERROR -> S.get("SocTransactionMisalignedAddressShort");
      case ACCESS_TYPE_NOT_SUPPORTED_ERROR -> switch (access) {
        case BYTE_ACCESS -> S.get("SocTransactionByteAccesNoSupportShort");
        case HALF_WORD_ACCESS -> S.get("SocTransactionHalfWordAccesNoSupportShort");
        default -> S.get("SocTransactionWordAccesNoSupportShort");
      };
      case READ_ONLY_ACCESS_ERROR -> S.get("SocTransactionReadOnlyAccessErrorShort");
      case WRITE_ONLY_ACCESS_ERROR -> S.get("SocTransactionWriteOnlyAccessErrorShort");
      case REGISTER_DOES_NOT_EXIST_ERROR -> S.get("SocTransactionRegisterDoesNotExistShort");
      default -> S.get("SocTransactionUnknownErrorShort");
    };
  }

  public int getType() {
    return type;
  }

  public void setError(int value) {
    error = value;
  }

  public boolean hasError() {
    return error != NO_ERROR;
  }

  public boolean isReadTransaction() {
    return (type & READ_TRANSACTION) != 0;
  }

  public boolean isWriteTransaction() {
    return (type & WRITE_TRANSACTION) != 0;
  }

  public boolean isAtomicTransaction() {
    return (type & ATOMIC_TRANSACTION) != 0;
  }

  public int getAddress() {
    return address;
  }

  public int getReadData() {
    return readData;
  }

  public int getWriteData() {
    return writeData;
  }

  public void setReadData(int value) {
    readData = value;
  }

  private String getTransactionInitiatorName() {
    if (master instanceof String str) return str;
    if (master instanceof Component comp) return SocSupport.getComponentName(comp);
    return "BUG";
  }

  public Object getTransactionInitiator() {
    return master;
  }

  private String getTransactionResponderName() {
    return SocSupport.getComponentName(slave);
  }

  public Component getTransactionResponder() {
    return slave;
  }

  public void setTransactionResponder(Component comp) {
    slave = comp;
  }

  private void paintTraceInfo(Graphics2D g2, BoxInfo boxInfo, boolean isRequest, boolean scale) {
    final var gfx = (Graphics2D) g2.create();
    Bounds bds;
    if (!scale) {
      gfx.setColor(Color.BLACK);
      gfx.drawLine(0, 0, 0, SocBusStateInfo.TRACE_HEIGHT - 2);
    }
    if (hasError() && !isRequest) {
      gfx.setColor(Color.RED);
      gfx.setFont(StdAttr.DEFAULT_LABEL_FONT);
      bds = getScaled(boxInfo.blockWidth / 2, (SocBusStateInfo.TRACE_HEIGHT - 2) >> 1, 0, 0, scale);
      GraphicsUtil.drawCenteredText(gfx, getShortErrorMessage(), bds.getX(), bds.getY());
      gfx.dispose();
      return;
    }
    final var title = isRequest
            ? S.get("SocBusStateMaster") + getTransactionInitiatorName()
            : S.get("SocBusStateSlave") + getTransactionResponderName();
    bds = getScaled(boxInfo.blockWidth / 2, (SocBusStateInfo.TRACE_HEIGHT - 2) >> 2, 0, 0, scale);
    GraphicsUtil.drawCenteredText(gfx, title, bds.getX(), bds.getY());
    bds = getScaled(boxInfo.skip, ((SocBusStateInfo.TRACE_HEIGHT - 2) >> 1) + 1,
            boxInfo.mark + boxInfo.hex, (SocBusStateInfo.TRACE_HEIGHT - 2) >> 1, scale);
    gfx.drawRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    bds = getScaled(boxInfo.skip + boxInfo.mark, ((SocBusStateInfo.TRACE_HEIGHT - 2) >> 1) + 1,
            0, SocBusStateInfo.TRACE_HEIGHT - 2, scale);
    gfx.drawLine(bds.getX(), bds.getY(), bds.getX(), bds.getHeight());
    bds = getScaled(boxInfo.skip + boxInfo.mark / 2, (3 * (SocBusStateInfo.TRACE_HEIGHT - 2)) / 4, 0, 0, scale);
    GraphicsUtil.drawCenteredText(gfx, "A", bds.getX(), bds.getY());
    var addrStr = String.format("0x%08X", getAddress());
    bds = getScaled(boxInfo.skip + boxInfo.mark + boxInfo.hex / 2, (3 * (SocBusStateInfo.TRACE_HEIGHT - 2)) / 4, 0, 0, scale);
    GraphicsUtil.drawCenteredText(gfx, addrStr, bds.getX(), bds.getY());
    bds = getScaled(boxInfo.skip + boxInfo.mark + boxInfo.hex, 0, 0, 0, scale);
    gfx.translate(bds.getX(), 0);
    bds = getScaled(boxInfo.skip, ((SocBusStateInfo.TRACE_HEIGHT - 2) >> 1) + 1, boxInfo.mark + boxInfo.hex,
            (SocBusStateInfo.TRACE_HEIGHT - 2) >> 1, scale);
    gfx.drawRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    bds = getScaled(boxInfo.skip + boxInfo.mark, ((SocBusStateInfo.TRACE_HEIGHT - 2) >> 1) + 1, 0,
            (SocBusStateInfo.TRACE_HEIGHT - 2), scale);
    gfx.drawLine(bds.getX(), bds.getY(), bds.getX(), bds.getHeight());
    bds = getScaled(boxInfo.skip + boxInfo.mark / 2, (3 * (SocBusStateInfo.TRACE_HEIGHT - 2)) / 4, 0, 0, scale);
    GraphicsUtil.drawCenteredText(gfx, "D", bds.getX(), bds.getY());
    if ((isRequest && isWriteTransaction()) || (!isRequest && isReadTransaction())) {
      final var format = switch (getAccessType()) {
        case SocBusTransaction.HALF_WORD_ACCESS -> "0x%04X";
        case SocBusTransaction.BYTE_ACCESS -> "0x%02X";
        default -> "0x%08X";
      };
      addrStr = String.format(format, isRequest ? getWriteData() : getReadData());
    } else
      addrStr = S.get("SocBusStateNoDataMax10chars");
    bds = getScaled(boxInfo.skip + boxInfo.mark + boxInfo.hex / 2, (3 * (SocBusStateInfo.TRACE_HEIGHT - 2)) / 4, 0, 0, scale);
    GraphicsUtil.drawCenteredText(gfx, addrStr, bds.getX(), bds.getY());
    if (!isRequest) {
      gfx.dispose();
      return;
    }
    bds = getScaled(boxInfo.skip + boxInfo.mark + boxInfo.hex, 0, 0, 0, scale);
    gfx.translate(bds.getX(), 0);
    if (isAtomicTransaction()) {
      gfx.setColor(Color.yellow);
      bds = getScaled(0, ((SocBusStateInfo.TRACE_HEIGHT - 2) >> 1) + 1, boxInfo.mark, (SocBusStateInfo.TRACE_HEIGHT - 2) >> 1, scale);
      gfx.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
      gfx.setColor(Color.BLUE);
      bds = getScaled(boxInfo.mark / 2, (3 * (SocBusStateInfo.TRACE_HEIGHT - 2)) / 4, 0, 0, scale);
      GraphicsUtil.drawCenteredText(gfx, "A", bds.getX(), bds.getY());
      gfx.setColor(Color.BLACK);
    }
    bds = getScaled(boxInfo.skip + boxInfo.mark, 0, 0, 0, scale);
    gfx.translate(bds.getX(), 0);
    if (isWriteTransaction()) {
      bds = getScaled(0, ((SocBusStateInfo.TRACE_HEIGHT - 2) >> 1) + 1, boxInfo.mark, (SocBusStateInfo.TRACE_HEIGHT - 2) >> 1, scale);
      gfx.setColor(Color.MAGENTA);
      gfx.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
      gfx.setColor(Color.BLACK);
      bds = getScaled(boxInfo.mark / 2, (3 * (SocBusStateInfo.TRACE_HEIGHT - 2)) / 4, 0, 0, scale);
      GraphicsUtil.drawCenteredText(gfx, "W", bds.getX(), bds.getY());
    }
    bds = getScaled(boxInfo.skip + boxInfo.mark, 0, 0, 0, scale);
    gfx.translate(bds.getX(), 0);
    if (isReadTransaction()) {
      bds = getScaled(0, ((SocBusStateInfo.TRACE_HEIGHT - 2) >> 1) + 1, boxInfo.mark, (SocBusStateInfo.TRACE_HEIGHT - 2) >> 1, scale);
      gfx.setColor(Color.CYAN);
      gfx.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
      bds = getScaled(boxInfo.mark / 2, (3 * (SocBusStateInfo.TRACE_HEIGHT - 2)) / 4, 0, 0, scale);
      gfx.setColor(Color.BLACK);
      GraphicsUtil.drawCenteredText(gfx, "R", bds.getX(), bds.getY());
    }
    gfx.dispose();
  }

  public void paint(int x, int y, Graphics2D g2, Long index) {
    final var realWidth = getRealBlockWidth(g2, false);
    final var gfx = (Graphics2D) g2.create();
    gfx.translate(x, y);
    gfx.setColor(Color.WHITE);
    gfx.fillRect(0, 0, SocBusStateInfo.TRACE_WIDTH - 2, SocBusStateInfo.TRACE_HEIGHT - 1);
    gfx.setColor(Color.BLACK);
    gfx.drawRect(0, 0, SocBusStateInfo.TRACE_WIDTH - 2, SocBusStateInfo.TRACE_HEIGHT - 1);
    GraphicsUtil.drawCenteredText(gfx, S.get("SocBusStateTraceIndex"), 79, (SocBusStateInfo.TRACE_HEIGHT - 2) / 4);
    GraphicsUtil.drawCenteredText(gfx, index.toString(), 79, (3 * (SocBusStateInfo.TRACE_HEIGHT - 2) / 4));
    gfx.translate(158, 0);
    paintTraceInfo(gfx, realWidth, true, false);
    gfx.translate(235, 0);
    paintTraceInfo(gfx, realWidth, false, false);
    gfx.dispose();
  }

  private BoxInfo getRealBlockWidth(Graphics2D gfx, boolean scale) {
    final var boxInfo = new BoxInfo();
    if (scale) {
      boxInfo.skip = AppPreferences.getScaled(BLOCK_SKIP);
      double prefferedMark = AppPreferences.getScaled(BLOCK_MARKER);
      double prefferedHex = AppPreferences.getScaled(BLOCK_HEX);
      final var fntMetrics = gfx.getFontMetrics();
      double realHex = fntMetrics.getStringBounds("0x00000000", gfx).getWidth();
      double corFactor = realHex <= prefferedHex ? 1.0 : realHex / prefferedHex;
      boxInfo.mark = AppPreferences.getDownScaled((int) Math.round(corFactor * prefferedMark));
      boxInfo.hex = AppPreferences.getDownScaled((int) Math.round(corFactor * prefferedHex));
      boxInfo.blockWidth = 6 * boxInfo.skip + 5 * boxInfo.mark + 2 * boxInfo.hex;
    } else {
      boxInfo.skip = BLOCK_SKIP;
      boxInfo.mark = BLOCK_MARKER;
      boxInfo.hex = BLOCK_HEX;
      boxInfo.blockWidth = SocBusStateInfo.BLOCK_WIDTH;
    }
    return boxInfo;
  }

  private static class BoxInfo {
    private int skip;
    private int mark;
    private int hex;
    private int blockWidth;
  }

  private Bounds getScaled(int x, int y, int width, int height, boolean scale) {
    return scale
           ? Bounds.create(AppPreferences.getScaled(x), AppPreferences.getScaled(y),
                AppPreferences.getScaled(width), AppPreferences.getScaled(height))
           : Bounds.create(x, y, width, height);
  }

  public int paint(Graphics2D g2, Long index, int width) {
    final var realWidth = getRealBlockWidth(g2, true);
    final var usedWidth = Math.max(realWidth.blockWidth, width);
    var bds = getScaled(usedWidth / 2, (SocBusStateInfo.TRACE_HEIGHT - 2) / 4, usedWidth, SocBusStateInfo.TRACE_HEIGHT >> 1, true);
    g2.setColor(Color.LIGHT_GRAY);
    g2.fillRect(0, 0, bds.getWidth(), bds.getHeight() - 1);
    g2.setColor(Color.black);
    g2.drawRect(0, 0, bds.getWidth(), bds.getHeight() - 1);
    GraphicsUtil.drawCenteredText(g2, S.get("SocBusStateTraceIndex") + " " + index.toString(), bds.getX(), bds.getY());
    g2.translate(0, bds.getHeight());
    bds = getScaled(0, 0, usedWidth, SocBusStateInfo.TRACE_HEIGHT, true);
    g2.drawLine(0, -1, bds.getWidth(), -1);
    g2.setColor(Color.WHITE);
    g2.fillRect(0, 1, bds.getWidth() - 2, bds.getHeight() - 1);
    g2.setColor(Color.BLACK);
    g2.drawRect(0, 1, bds.getWidth() - 2, bds.getHeight() - 1);
    paintTraceInfo(g2, realWidth, true, true);
    bds = getScaled(0, 0, usedWidth, SocBusStateInfo.TRACE_HEIGHT, true);
    g2.translate(0, bds.getHeight());
    g2.setColor(Color.YELLOW);
    g2.fillRect(0, 0, bds.getWidth(), bds.getHeight());
    g2.setColor(Color.BLACK);
    g2.drawLine(0, 0, bds.getWidth(), 0);
    paintTraceInfo(g2, realWidth, false, true);
    return usedWidth;
  }

}
