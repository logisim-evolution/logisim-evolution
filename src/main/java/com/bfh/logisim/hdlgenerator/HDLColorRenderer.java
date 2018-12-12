package com.bfh.logisim.hdlgenerator;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;


@SuppressWarnings("serial")
public class HDLColorRenderer extends JLabel
							  implements TableCellRenderer{
    public final static String VHDLSupportString = "VHDL_SUPPORTED";
    public final static String VERILOGSupportString = "VERILOG_SUPPORTED";
    public final static String NoSupportString = "HDL_NOT_SUPPORTED";
    public final static String UnKnownString = "HDL_UNKNOWN";
    public final static String RequiredFieldString = ">_HDL_REQUIRED_FIELD_<";
    private final static ArrayList<String> CorrectStrings = new ArrayList<String>();
	
	Border border = null;
	
	public HDLColorRenderer() {
		setOpaque(true);
		CorrectStrings.clear();
		CorrectStrings.add(VERILOGSupportString);
		CorrectStrings.add(NoSupportString);
		CorrectStrings.add(UnKnownString);
		CorrectStrings.add(VHDLSupportString);
	}
	
	public Component getTableCellRendererComponent (
			JTable table, Object Info, boolean isSelected,
			boolean hasFocus, int row, int column) {
		/* we have a difference between the first row and the rest */
		if (row==0) {
			String value = (String)Info;
			boolean passive = value.equals(NoSupportString);
			Color newColor = (passive) ? Color.red :
										 Color.green;
			if (value.equals(UnKnownString)) 
				newColor = table.getGridColor();
			setBackground(newColor);
			setForeground(Color.black);
			setText(CorrectStrings.contains(value)?column==0 ? HDLGeneratorFactory.VHDL : HDLGeneratorFactory.VERILOG : value);
			setHorizontalAlignment(JLabel.CENTER);
			if (border==null)
				border = BorderFactory.createMatteBorder(2,5,2,5,
						table.getGridColor());
			setBorder(border);
		} else {
			String myInfo = (String) Info;
			if (myInfo != null && myInfo.equals(RequiredFieldString)) {
				setBackground(Color.YELLOW);
				setForeground(Color.BLUE);
				setText("HDL Required");
				setHorizontalAlignment(JLabel.CENTER);
				setBorder(null);
			} else if (myInfo != null && myInfo.contains("#")&& myInfo.indexOf('#')==0&&
					   (myInfo.length() == 7 || myInfo.length() == 9)) {
				int red,green,blue,alpha;				
				red = Integer.valueOf(myInfo.substring(1, 3), 16);
				green = Integer.valueOf(myInfo.substring(3, 5), 16);
				blue = Integer.valueOf(myInfo.substring(5, 7), 16);
				alpha = myInfo.length() == 7 ? 255 : Integer.valueOf(myInfo.substring(7, 9), 16);
				setBackground(new Color(red,green,blue,alpha));
				setText("");
				setBorder(null);
			} else {
				Color newColor = isSelected ? Color.lightGray :
											  Color.white;
				setBackground(newColor);
				setForeground(Color.black);
				setText((String)Info);
				setHorizontalAlignment(JLabel.LEFT);
				setBorder(null);
			}
		}
		return this;
	}

}
