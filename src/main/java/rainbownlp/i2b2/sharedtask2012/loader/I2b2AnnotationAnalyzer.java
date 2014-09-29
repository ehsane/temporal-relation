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


public class I2b2AnnotationAnalyzer{

	public static void main(String[] args) throws Exception
	{
		String trainingRoot = args[0];
		Setting.TrainingMode = true;
		if(args.length>1 && args[1].equals("test"))
			Setting.TrainingMode = false;
//		Util.log("process documents and convert them to words and calculate attributes", Level.INFO);
		I2b2AnnotationAnalyzer ann_proc = new I2b2AnnotationAnalyzer();
		ann_proc.processAnnotationFiles(trainingRoot);
	}
	private void processAnnotationFiles(String filesRoot) throws Exception {
		File f = new File(filesRoot);
		List<String> loaded_ = new ArrayList<String>();
		if (f.exists() && f.isFile()) {
			//Util.log("Loading document :"+filesRoot, Level.INFO);
			//Util.generateParseFilesIfnotExist(filesRoot);
			try {
				loadAnnotations(f);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
			List<File> files = 
				FileUtil.getFilesInDirectory(filesRoot,"extent");
			
			//Util.log("Loading documents from :"+filesRoot, Level.INFO);
			int counter = 0;
			for(File file: files) {
				
//				if(!file_path.contains("0330.txt")) continue;
				//Util.generateParseFilesIfnotExist(file_path);
				try{
					loadAnnotations(file);
				}catch (IOException e){
					e.printStackTrace();
				}
				counter++;
			}
			
		}
//		this.documents = loaded_documents;
		// findTriggersBagOfWords();

	}
	private void loadAnnotations(File f) throws Exception {
		// read each line
		//get content, start and end
		List<String> lines = FileUtil.loadLineByLine(f);
	    for (String line : lines) 
	    	parseAnnotationLine(line,f);
	}
	private void parseAnnotationLine(String line, File f) throws Exception {
		//EVENT="ADMISSION" 0:0 0:0||type="OCCURRENCE"||modality="FACTUAL"||polarity="POS"||sec_time_rel="OVERLAP"
		//getting the line and token indexes
		String text_file_path = getTextFilePath(f.getAbsolutePath());
		String text_file_partial_path = XMLLoader.getFileName(text_file_path.replace(".txt", ""));
		I2b2Annotation annotation = new I2b2Annotation();
		setAnnotationGeneralDetails(annotation,line);
//		TODO: remove line 37 file 357
//		if (annotation.content.matches("0%.*") || annotation.content.matches(".*(#|:|%|/).*") ||
//			
//				annotation.content.matches("falling out\\..*")
//				|| annotation.content.matches(".*\\.$"))
//		{
//			return;
//		}
		if (annotation.content.matches(".*(#|:|%|/).*") ||
				
					annotation.content.matches("falling out\\..*")
					|| annotation.content.matches(".*\\.$"))
			{
				return;
			}
		String ann_type = annotation.type;
		//get start and end artifacts
		Artifact startArtifact = Artifact.findInstance
			(Artifact.Type.Word, text_file_partial_path, annotation.startTokenIndex,annotation.startLineOffset);
		
//		String annotation_content_lower = annotation.content.toLowerCase();
		String annotation_content = annotation.content.toLowerCase();
		if (annotation_content.matches(".*&apos;s.*") || 
				annotation_content.matches(".*&apos;d.*") )
		{
			annotation_content = annotation_content.replaceAll("&apos;", "'");
		}
		annotation_content = annotation_content.replaceAll("&quot;", "\"");
		
		if (startArtifact ==null || !annotation_content.startsWith(startArtifact.getContent().toLowerCase())){
			
			//get the firts word
			String[] words = annotation_content.split("\\s+");
			String first_word = words[0].replaceAll("\\W+$", "");
			
		
			String second_word; 
			
			if (words.length >1)
			{
				second_word = words[1].replaceAll("\\W+$", "");
				startArtifact = Artifact.findInstance
				(Type.Word, text_file_partial_path, annotation.startLineOffset, annotation.startTokenIndex, first_word,second_word);
			}
			else
			{
				startArtifact = Artifact.findInstance
				(Type.Word, text_file_partial_path, annotation.startLineOffset, annotation.startTokenIndex, first_word,null);
			}
			
			
			if (!annotation_content.startsWith(startArtifact.getContent().toLowerCase())){
				throw new Exception("content incorrect" );
			}
		}
		
		String art_content = startArtifact.getContent().toLowerCase();
		Artifact curr_artifact = startArtifact;
		Artifact endArtifact = curr_artifact;
		

		String annotation_content_compressed = 
			annotation_content.replaceAll("\\s+", " ").replaceAll(" ", "").toLowerCase();
		
		while (!art_content.equals(annotation_content_compressed))
		{
			
			curr_artifact =curr_artifact.getNextArtifact();
			if (art_content.endsWith(".") && curr_artifact.getContent().equals("."))
				continue;
			
			art_content += curr_artifact.getContent().toLowerCase();
		}
		endArtifact = curr_artifact;
		
		
//		Artifact endArtifact = Artifact.findInstance(Artifact.Type.Word, text_file_partial_path, annotation.endTokenIndex,annotation.endLineOffset);
		
		
		if (!annotation_content.endsWith(endArtifact.getContent().toLowerCase())){
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
			Phrase.getInstance(annotation.content, startArtifact, endArtifact,ann_type);
		rel_phrase.setAltLineIndex(annotation.startLineOffset);
		rel_phrase.setAltStartWordIndex(annotation.startTokenIndex);
		rel_phrase.setAltEndWordIndex(annotation.endTokenIndex);
		
		HibernateUtil.save(rel_phrase);
		
//		// check the correctness of the offsets, it can be removed
//		if (annotation.startLineOffset != rel_phrase.getStartArtifact().getLineIndex()){
//			throw new Exception("startLineOffset incorrect" );
//		}
//		if (annotation.startTokenIndex != rel_phrase.getStartArtifact().getWordIndex()){
//			throw new Exception("startTokenIndex incorrect" );
//		}
//		if (annotation.endLineOffset != rel_phrase.getEndArtifact().getLineIndex()){
//			throw new Exception("endLineOffset incorrect" );
//		}
//		if (annotation.endTokenIndex != rel_phrase.getEndArtifact().getWordIndex()){
//			throw new Exception("endTokenIndex incorrect" );
//		}
		//create event or timex accordingly
		if (ann_type.equals("EVENT"))
		{
			setAnnotationEventDetails(annotation,line);
//			rel_phrase.setPhraseEntityType("EVENT");
			
			ClinicalEvent.getInstance(rel_phrase, 
					EventType.valueOf(annotation.eventType), 
			Modality.valueOf(annotation.modality),Polarity.valueOf(annotation.polarity));
			
		}
		else if (ann_type.equals("TIMEX3"))
		{
			setAnnotationTimexDetails(annotation,line);
			
			TimexPhrase.getInstance(rel_phrase,TimexType.getEnum(annotation.timexType),
					TimexMod.getEnum(annotation.mod), annotation.normalizedValue);
//			rel_phrase.setPhraseEntityType("TIMEX3");

		
		}
		else if (ann_type.equals("SECTIME"))
		{
			setAnnotationSecTimeDetails(annotation,line);
			 SecTime.getInstance(rel_phrase, annotation.secTimeType, annotation.normalizedValue);

		}
		else
		{
			throw (new Exception("The annotation type is unknown"));
		}
	}
	private void setAnnotationSecTimeDetails(I2b2Annotation annotation,
			String line) throws Exception {
//		SECTIME="2012-03-13" 4:0 4:0||type="DISCHARGE"||dvalue="2012-03-13"
		Pattern p = Pattern.compile("SECTIME=.*\\|\\|type=\"(\\w+)\"\\|\\|dvalue=\"(.*)\"");
		Matcher m = p.matcher(line);
		if (m.matches())
		{
			annotation.secTimeType = m.group(1);
			annotation.normalizedValue = m.group(2);
		}
		else
		{
			throw new Exception("wrong timex tag...");
		}
		
	}
	private void setAnnotationTimexDetails(I2b2Annotation annotation,
			String line) throws Exception {
//		TIMEX3="the day of discharge" 19:11 19:14||type="DATE"||val="2012-03-13"||mod="NA"
		Pattern p = Pattern.compile("TIMEX3=.*\\|\\|type=\"(\\w+)\"\\|\\|val=\"(.*)\"\\|\\|mod=\"(\\w+)\"");
		Matcher m = p.matcher(line);
		if (m.matches())
		{
			annotation.timexType = m.group(1);
			annotation.normalizedValue = m.group(2);
			annotation.mod = m.group(3);
		}
		else
		{
			throw new Exception("wrong timex tag...");
		}
		
		
	}
	private void setAnnotationEventDetails(I2b2Annotation annotation,
			String line) throws Exception {
//		EVENT="her dry weight" 19:22 19:24||type="TEST"||modality="FACTUAL"||polarity="POS"
		Pattern p = Pattern.compile("EVENT=.*\\|\\|type=\"(\\w*)\"\\|\\|modality=\"(\\w+)\"\\|\\|polarity=\"(\\w+)\"");
		Matcher m = p.matcher(line);
		if (m.matches())
		{
			annotation.eventType = m.group(1);
			if (annotation.eventType.equals(""))
			{
				annotation.eventType="UNKNOWN";
			}
			annotation.modality = m.group(2);
			annotation.polarity = m.group(3);
		}
		else
		{
			throw new Exception("wrong event tag...");
		}
		
	}
	private void setAnnotationGeneralDetails(I2b2Annotation annotation, String line) throws Exception {
		
		String start_line_index;
		String end_line_index;
		String start_token_index;
	
		String end_token_index;
		String content;
		
		Pattern p = Pattern.compile("(EVENT|TIMEX3|SECTIME)=\"(.*)\"\\s(\\d+):(\\d+)\\s(\\d+):(\\d+)\\|\\|.*");
		Matcher m = p.matcher(line);
		if (m.matches())
		{
			annotation.type = m.group(1);
			
			content = m.group(2);
			annotation.content = content;
			
			start_line_index = m.group(3);
			annotation.startLineOffset = Integer.parseInt(start_line_index);
			
			start_token_index =  m.group(4);
			annotation.startTokenIndex = Integer.parseInt(start_token_index);
			
			end_line_index =  m.group(5);
			annotation.endLineOffset = Integer.parseInt(end_line_index);
			
			end_token_index =  m.group(6);
			annotation.endTokenIndex = Integer.parseInt(end_token_index);
			
	
		}
		else
		{
			throw new Exception("the annotation line is not correct.... ");
		}
		
	}

	public static String getTextFilePath(String extentFilePath)
	{
//		/home/azadeh/Documents/i2b22012/trainingData/2012-06-18.release/1.xml.extent
		String text_file="";
		String file_name="";
		Pattern p = Pattern.compile("(.*/)(\\d+)\\.xml\\.extent$");
		Matcher m = p.matcher(extentFilePath);
		if (m.matches())
		{
			file_name = m.group(2);
			text_file = m.group(1)+file_name+".xml.txt";
		}
		return text_file;
	}
}
