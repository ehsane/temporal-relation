package rainbownlp.i2b2.sharedtask2012.loader;
//EVENT="The 1 cm cyst" 30:1 30:3||TIMEX3="10/04/1993" 4:0 4:0||type="BEFORE"
public class I2b2TLink {
//	<TLINK id="TL106" fromID="E119" fromText="brisk diuresis" toID="E86" toText="diuresed" type="OVERLAP" />
	public String linkAltId;
	public String fromContent;
	//can be entity ot timex
	public String fromType;
	public String fromAltId;
	public int fromStartLineOffset;
	public int fromStartWordOffset;
	public int fromEndLineOffset;
	public int fromEndWordOffset;
	
	public String toContent;
	public String toAltId;
	
	//can be entity ot timex
	public String toType;
	public int toStartLineOffset;
	public int toStartWordOffset;
	public int toEndLineOffset;
	public int toEndWordOffset;
	
	public String linkType;
	
	public I2b2TLink(){
		
	}

}
