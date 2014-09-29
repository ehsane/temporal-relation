package rainbownlp.i2b2.sharedtask2012.loader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rainbownlp.core.Artifact;
import rainbownlp.core.Artifact.Type;
import rainbownlp.core.Phrase;
import rainbownlp.core.Setting;
import rainbownlp.i2b2.sharedtask2012.ClinicalEvent;
import rainbownlp.i2b2.sharedtask2012.ClinicalEvent.EventType;
import rainbownlp.i2b2.sharedtask2012.ClinicalEvent.Modality;
import rainbownlp.i2b2.sharedtask2012.ClinicalEvent.Polarity;
import rainbownlp.i2b2.sharedtask2012.SecTime;
import rainbownlp.i2b2.sharedtask2012.TimexPhrase;
import rainbownlp.i2b2.sharedtask2012.TimexPhrase.TimexMod;
import rainbownlp.i2b2.sharedtask2012.TimexPhrase.TimexType;
import rainbownlp.util.FileUtil;
import rainbownlp.util.HibernateUtil;
import edu.umass.cs.mallet.base.pipe.iterator.FileIterator;


public class I2b2AnnotationAnalyzerFromXML{

	public static void main(String[] args) throws Exception
	{
		String trainingRoot = args[0];
		Setting.TrainingMode = true;
		if(args.length>1 && args[1].equals("test"))
			Setting.TrainingMode = false;
//		Util.log("process documents and convert them to words and calculate attributes", Level.INFO);
		I2b2AnnotationAnalyzerFromXML ann_proc = new I2b2AnnotationAnalyzerFromXML();
		ann_proc.processAnnotationFiles(trainingRoot);
	}
	public void processAnnotationFiles(String filesRoot) throws Exception {
		File f = new File(filesRoot);
		List<String> loaded_ = new ArrayList<String>();
		if (f.exists() && f.isFile()) {
			//Util.log("Loading document :"+filesRoot, Level.INFO);
			//Util.generateParseFilesIfnotExist(filesRoot);
			try {
				loadAnnotations(filesRoot);
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
			FileIterator file_iterator = 
				FileUtil.getFilesInDirectory(filesRoot,"xml");
			
			//Util.log("Loading documents from :"+filesRoot, Level.INFO);
			int counter = 0;
			while (file_iterator.hasNext()) {
				String file_path = file_iterator.nextFile().getPath();
//				if(!file_path.contains("0330.txt")) continue;
				//Util.generateParseFilesIfnotExist(file_path);
				try{
				
					loadAnnotations(file_path);
				}catch (IOException e){
					e.printStackTrace();
				}
				counter++;
				FileUtil.logLine(null, "processAnnotationFiles................... files processed:"+counter);
			}
			
		}
//		this.documents = loaded_documents;
		// findTriggersBagOfWords();

	}
	private void loadAnnotations(String filePath) throws Exception {
		// read each line
		//get content, start and end
		List<String> lines = FileUtil.loadLineByLine(filePath);
	    for (String line : lines) 
	    	parseAnnotationLine(line,filePath);
	}
	private void parseAnnotationLine(String line, String filePath) throws Exception {
		
		//getting the line and token indexes
		String text_file_path = getTextFilePath(filePath);
		String text_file_partial_path = XMLLoader.getFileName(text_file_path.replace(".txt", ""));
		
		//here we just handle mention annotations
		if (!line.matches("<(EVENT|TIMEX3|SECTIME).*") )
		{
			return;
		}
		I2b2Annotation annotation = new I2b2Annotation();
		
		annotation = XMLLoader.setXMLAnnotationGeneralDetails(annotation,line);

		String ann_type = annotation.type;

		//get start and end artifacts
		Artifact startArtifact = Artifact.findInstanceByStartIndex
			( text_file_partial_path, annotation.startChar);
		
//		String annotation_content_lower = annotation.content.toLowerCase();
		String annotation_content = annotation.content.replaceAll("<|\\[|\\{", "\\(");
		annotation_content = annotation_content.replaceAll("\\>|\\]|\\}", "\\)").toLowerCase();
		annotation_content = annotation_content.replaceAll("&apos;", "'");
//		if (annotation_content.matches(".*&apos;s.*") || 
//				annotation_content.matches(".*&apos;d.*") )
//		{
//			annotation_content = annotation_content.replaceAll("&apos;", "'");
//		}
		annotation_content = annotation_content.replaceAll("&quot;", "\"");
		annotation_content = annotation_content.replaceAll("`", "'");
		
		if (startArtifact ==null || !annotation_content.startsWith(startArtifact.getContent().toLowerCase().replaceAll(" ", " "))){
			
			//get the firts word
			String[] words = annotation_content.split("\\s+");
			String first_word = words[0];
			
		
			String second_word; 
			
			if (words.length >1)
			{
				second_word = words[1];
				startArtifact = Artifact.findInstance(Type.Word, text_file_partial_path, 
					 annotation.startChar, first_word,second_word);
			}
			else
			{
				startArtifact = Artifact.findInstance(Type.Word, text_file_partial_path, 
						 annotation.startChar, first_word,null);
			}
			
			
			if (!annotation_content.startsWith(startArtifact.getContent().toLowerCase())){
				throw new Exception("content incorrect" );
			}
		}
		
		String art_content = startArtifact.getContent().replaceAll(" ", " ").replaceAll(" ", "").toLowerCase();
		Artifact curr_artifact = startArtifact;
		Artifact endArtifact = curr_artifact;
		

		String annotation_content_compressed = 
			annotation_content.replaceAll(" ", " ").replaceAll("\\s+", " ").replaceAll(" ", "").toLowerCase();
		//handling for the rare case 78.xml
		if (filePath.endsWith("78.xml") && startArtifact.getStartIndex()== 2109)
		{
			curr_artifact = curr_artifact.getNextArtifact();
			while (curr_artifact != null && curr_artifact.getStartIndex() !=2132)
			{
				curr_artifact = curr_artifact.getNextArtifact();
			}
			endArtifact = curr_artifact;
		}
			
		while (!art_content.equals(annotation_content_compressed))
		{
			if (startArtifact.getStartIndex()== 2109 && filePath.endsWith("78.xml"))
				break;
			curr_artifact =curr_artifact.getNextArtifact();
			if (art_content.endsWith(".") && curr_artifact.getContent().equals("."))
				continue;
			
			art_content += unifyTags(curr_artifact.getContent().toLowerCase());
		}
		endArtifact = curr_artifact;
		
		
		if (!annotation_content.endsWith(unifyTags(endArtifact.getContent().toLowerCase()))){
			throw new Exception("content incorrect" );
		}
		
		assert (startArtifact!=null);
		assert (endArtifact!=null);
		if(startArtifact==null || endArtifact==null)
		{
			System.out.println("ERROR: Artifact not found for the phrase! Text file: "+
					text_file_path+" annotation: "+annotation.startLineOffset+":"+annotation.startTokenIndex
					 +" "+annotation.endLineOffset+":"+annotation.endTokenIndex);
			return;
		}
		// create phrase
		Phrase rel_phrase = 
			Phrase.getInstance(annotation.content, startArtifact, endArtifact,ann_type,
					annotation.altId);
		rel_phrase.setAltID(annotation.altId);
		rel_phrase.setStartCharOffset(annotation.startChar);
		rel_phrase.setEndCharOffset(annotation.endChar);
		
		
		HibernateUtil.save(rel_phrase);
	
		//create event or timex accordingly
		if (ann_type.equals("EVENT"))
		{
			setXMLAnnotationEventDetails(annotation,line);
//			rel_phrase.setPhraseEntityType("EVENT");
			
			ClinicalEvent.getInstance(rel_phrase, 
					EventType.valueOf(annotation.eventType), 
			Modality.valueOf(annotation.modality),Polarity.valueOf(annotation.polarity));
			
		}
		else if (ann_type.equals("TIMEX3"))
		{
			setXMLAnnotationTimexDetails(annotation,line);
			
			TimexPhrase.getInstance(rel_phrase,TimexType.getEnum(annotation.timexType),
					TimexMod.getEnum(annotation.mod), annotation.normalizedValue);
		
		}
		else if (ann_type.equals("SECTIME"))
		{
			setXMLAnnotationSecTimeDetails(annotation,line);
			 SecTime.getInstance(rel_phrase, annotation.secTimeType, annotation.normalizedValue);

		}
		else
		{
			throw (new Exception("The annotation type is unknown"));
		}
	}
	private void setXMLAnnotationSecTimeDetails(I2b2Annotation annotation,
			String line) throws Exception {
//		<SECTIME id="S0" start="18" end="28" text="03/30/1999" type="ADMISSION" dvalue="1999-03-30" />
		Pattern p = Pattern.compile("<SECTIME\\s+id=\"(.*)\"\\s+start=\"\\d+\"" +
		"\\s+end=\"\\d+\"\\s+text=\".*\"\\s+type=\"(\\w+)\"\\s+dvalue=\"(.*)\"\\s+/>");
		
		Matcher m = p.matcher(line);
		if (m.matches())
		{
			annotation.secTimeType = m.group(2);
			annotation.normalizedValue = m.group(3);
		}
		else
		{
			throw new Exception("wrong sectime tag...");
		}
		
	}
	private void setXMLAnnotationTimexDetails(I2b2Annotation annotation,
			String line) throws Exception {
//		<TIMEX3 id="T10" start="1146" end="1153" text="the day" type="DATE" val="1999-03-30" mod="END" />
		
		Pattern p = Pattern.compile("<TIMEX3\\s+id=\"(.*)\"\\s+start=\"\\d+\"" +
		"\\s+end=\"\\d+\"\\s+text=\".*\"\\s+type=\"(\\w+)\"\\s+val=\"(.*)\"\\s+mod=\"(\\w+)\"\\s+/>");
		
		Matcher m = p.matcher(line);
		if (m.matches())
		{
			annotation.timexType = m.group(2);
			annotation.normalizedValue = m.group(3);
			annotation.mod = m.group(4);
		}
		else
		{
			throw new Exception("wrong timex tag...");
		}
		
		
	}
	private void setXMLAnnotationEventDetails(I2b2Annotation annotation,
			String line) throws Exception {
//		<EVENT id="E150" start="5129" end="5143" text="a nursing home" modality="PROPOSED" polarity="NEG" type="OCCURRENCE" />
		
		Pattern p = Pattern.compile("<EVENT\\s+id=\"(.*)\"\\s+start=\"(\\d+)\"" +
		"\\s+end=\"(\\d+)\"\\s+text=\"(.*)\"\\s+modality=\"(\\w+)\"\\s+polarity=\"(\\w+)\"\\s+type=\"(.*)\"\\s+/>");
		Matcher m = p.matcher(line);
		if (m.matches())
		{
			annotation.altId = m.group(1);
			
			annotation.modality = m.group(5);
			annotation.polarity = m.group(6);
			annotation.eventType = m.group(7);
			if (annotation.eventType.equals(""))
			{
				annotation.eventType="UNKNOWN";
			}
		}
		else
		{
			FileUtil.logLine("/tmp/WrongEventTags.txt", line);
//			throw new Exception("wrong event tag...");
		}
		
	}
	
	public static String getTextFilePath(String extentFilePath)
	{
//		/home/azadeh/Documents/i2b22012/trainingData/2012-06-18.release/1.xml.extent
		String text_file="";
		String file_name="";
		Pattern p = Pattern.compile("(.*/)(\\d+)\\.xml$");
		Matcher m = p.matcher(extentFilePath);
		if (m.matches())
		{
			file_name = m.group(2);
			text_file = m.group(1)+file_name+".xml.txt";
		}
		return text_file;
	}
	public static String unifyTags(String text)
	{
		String cleaned = text;
		cleaned = cleaned.replaceAll("<|\\[|\\{", "\\(");
		cleaned = cleaned.replaceAll("\\>|\\]|\\}", "\\)").toLowerCase();
		cleaned = cleaned.replaceAll("&apos;", "'");

		cleaned = cleaned.replaceAll("&quot;", "\"");
		cleaned = cleaned.replaceAll("`", "'");
		return cleaned;
	}
}
