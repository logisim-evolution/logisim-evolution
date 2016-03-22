package com.cburch.logisim.std.hdl;

import java.util.ArrayList;
import java.util.List;

import com.cburch.logisim.instance.Port;

public class VhdlSimulatorTestPatternMapping {
	List<PortInfo> DList=new ArrayList<PortInfo>();
	int PatternLengthIn=0;
	int PatternLengthOut=0;
	private String current_component="";
	private int index=0;

	public VhdlSimulatorTestPatternMapping(){
		clear();
	}

	public void clear(){
		DList.clear();
		PatternLengthIn=0;
		PatternLengthOut=0;
	}

	public void add(String EntityName, String PinName, int PinWidth, int PortNumber,int Direction){
		if(Direction==0){
			DList.add(new PortInfo(EntityName,PinName,PinWidth,PatternLengthIn,PortNumber,Direction));
			PatternLengthIn+=PinWidth;
			PatternLengthOut+=PinWidth;
		} else if(Direction==1){
			DList.add(new PortInfo(EntityName,PinName,PinWidth,PatternLengthIn,PortNumber,Direction));
			PatternLengthIn+=PinWidth;
		} else if(Direction==2){
			DList.add(new PortInfo(EntityName,PinName,PinWidth,PatternLengthOut,PortNumber,Direction));
			PatternLengthOut+=PinWidth;
		}
	}
	
	public int getTestPatternSize(int Direction){
		if(Direction==1){
			return PatternLengthIn;
		} else if(Direction==2){
			return PatternLengthOut;			
		} else{
			return -1;
		}
	}
	
	public int getTestPatternPos(String EntityName, String PinName,int Direction){
		for (PortInfo pInfo : DList) {
			if((pInfo.EntityName==EntityName)&&(pInfo.PinName==PinName)&&(pInfo.Direction==Direction)){
				return pInfo.TestPatternPos;
			}
		}
		return -1;
	}
	

	public void setValue(String EntityNameAnPin,String Value){
		for (PortInfo pInfo : DList) {
			if(EntityNameAnPin.equals(pInfo.EntityName+"_"+pInfo.PinName)){
				pInfo.Value=Value.toCharArray();
				current_component=pInfo.EntityName;
				break;
			}
		}
	}	

	public void setValue(String EntityName, String PinName,String Value){
		for (PortInfo pInfo : DList) {
			if((pInfo.EntityName==EntityName)&&(pInfo.PinName==PinName)){
				pInfo.Value=Value.toCharArray();
				current_component=EntityName;
				break;
			}
		}
	}

	public String getValue(String EntityName, String PinName){
		for (PortInfo pInfo : DList) {
			if((pInfo.EntityName==EntityName)&&(pInfo.PinName==PinName)){
				return new String(pInfo.Value);
			}
		}
		return "";
	}

	public void setOutputsFromPattern(String pattern){
		if(pattern.length()<PatternLengthOut) return;
		char[] cpattern=pattern.toCharArray();
		for (PortInfo pInfo : DList) {
			if(pInfo.Direction!=1)
				for(int i=0;i<pInfo.PinWidth;i++){
					pInfo.Value[i]=cpattern[pInfo.TestPatternPos+i];
				}
		}
	}

	public void resetEnumerate(){
		index=0;
	}

	public String getNextOutput(){
		while(index<DList.size()){
			PortInfo pInfo=DList.get(index);
			index++;
			if((pInfo.Value!=null)&&(pInfo.EntityName.equals(current_component)&&(pInfo.Direction!=1))){
				return pInfo.EntityName+"_"+pInfo.PinName+":"+new String(pInfo.Value)+":"+pInfo.PortNumber;	
			}
		}
		index=0;
		return null;
	}

	public String getInputPattern(){
		char [] result= new char[PatternLengthIn];
		for(int i=0;i<PatternLengthIn;i++){
			result[i]='U';
		}
		for (PortInfo pInfo : DList) {
			if((pInfo.Value!=null)&&(pInfo.Direction==1))
				for(int i=0;i<pInfo.PinWidth;i++){
					result[pInfo.TestPatternPos+i]=pInfo.Value[i];
				}
		}
		return new String(result);
	}
}


class PortInfo{
	public String EntityName;
	public String PinName;
	public int PinWidth;
	public int TestPatternPos;
	public int PortNumber;
	public int Direction;
	public char[] Value;

	PortInfo(String EntityName, String PinName, int PinWidth, int TestPatternPos, int PortNumber,int Direction){
		this.EntityName=EntityName;
		this.PinName=PinName;
		this.PinWidth=PinWidth;
		this.TestPatternPos=TestPatternPos;
		this.Value=new char[PinWidth];
		for(int i=0;i<PinWidth;i++){
			Value[i]='U';
		}		
		this.PortNumber=PortNumber;
		this.Direction=Direction;
	}





}


