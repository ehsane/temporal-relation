package rainbownlp.i2b2.sharedtask2012.loader;

import rainbownlp.core.Phrase;

public class I2b2Annotation {
	public String content;
	public int startTokenIndex;
	public int endTokenIndex;
	public int startLineOffset;
	public int endLineOffset;
	public String type;
	
	//these attributes are for XML data
	public String altId;
	public int startChar;
	public int endChar;
	
	//Event attributes
	public String eventType;
	public String modality;
	public String polarity;
	
	//Timex attributes
	public String timexType;
	
	public String normalizedValue;
	
	public String mod;	
	
	public String secTimeType;
	
	@Override
	public boolean equals(Object objectToCompare)
	{
		if(objectToCompare instanceof Phrase)
		{
			Phrase phrase = (Phrase) objectToCompare;
			int phraseStart = phrase.getStartArtifact().getStartIndex() + 
				phrase.getStartArtifact().getLineIndex();
			if(phrase.getPhraseContent().trim().equals(content.trim()) 
					&&
				phraseStart ==  startChar)
				return true;
		}
		return false;
	}
	public I2b2Annotation(){
		
	}
	
	@Override
	public String toString()
	{
		return "altId="+altId+"/Content="+
		content;
	}
}
