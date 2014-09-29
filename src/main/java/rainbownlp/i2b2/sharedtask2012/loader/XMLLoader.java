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
import rainbownlp.util.FileUtil;
import rainbownlp.util.HibernateUtil;
import edu.umass.cs.mallet.base.pipe.iterator.FileIterator;


public class XMLLoader{

	public static void main(String[] args) throws Exception
	{
		String trainingRoot = args[0];
		Setting.TrainingMode = true;
		if(args.length>1 && args[1].equals("test"))
			Setting.TrainingMode = false;
//		Util.log("process documents and convert them to words and calculate attributes", Level.INFO);
		XMLLoader ann_proc = new XMLLoader();
		ann_proc.processXMLAnnotationFiles(trainingRoot);
	}
	private void processXMLAnnotationFiles(String filesRoot) throws Exception {
		List<File> files = 
				FileUtil.getFilesInDirectory(filesRoot,"xml");

		//Util.log("Loading documents from :"+filesRoot, Level.INFO);
		int counter = 0;
		for(File file: files) {
			try{
				loadXMLAnnotations(file.getAbsolutePath());
			}catch (IOException e){
				e.printStackTrace();
			}
			counter++;
		}
	}
	private void loadXMLAnnotations(String filePath) throws Exception {
		// read each line
		//get content, start and end
		List<String> lines = FileUtil.loadLineByLine(filePath);
	    for (String line : lines) 
	    	parseXMLAnnotationLine(line,filePath);
	}
	
	
	public static List<I2b2Annotation> loadXMLMentions(String filePath) throws Exception {
		List<I2b2Annotation> annotations = new ArrayList<I2b2Annotation>();
		List<String> lines = FileUtil.loadLineByLine(filePath.replaceAll("ehsan", "azadeh"));
	    for (String line : lines)
	    {
	    	I2b2Annotation curI2b2Annotation = 
	    		getXMLLineI2b2Annotation(line);
	    	if(curI2b2Annotation!=null)
	    		annotations.add(curI2b2Annotation);
	    }
	    
	    return annotations;
	}
	
	
	private static I2b2Annotation getXMLLineI2b2Annotation(String line) throws Exception {
		I2b2Annotation annotation = null;
		if (line.matches("<(EVENT|TIMEX3|SECTIME).*") )
		{
			annotation = new I2b2Annotation();
			setXMLAnnotationGeneralDetails(annotation,line);
		}
		
		return annotation;
	}
	
	
	private void parseXMLAnnotationLine(String line, String filePath) throws Exception {
//		<EVENT id="E0" start="1" end="10" text="Admission" modality="FACTUAL" polarity="POS" type="OCCURRENCE" />
		//getting the line and token indexes
		
		if (line.matches("<(EVENT|TIMEX3|SECTIME).*") )
		{
			String text_file_partial_path = getFileName(filePath);
			I2b2Annotation annotation = new I2b2Annotation();
			setXMLAnnotationGeneralDetails(annotation,line);
				
			String ann_type = annotation.type;
			//get start and end artifacts
			// the start index of the Artifact table is different from the xml i2b2 file
			//so the xml_start_index = artifact.startIndex+artifact.LineIndex+1
			Artifact startArtifact = Artifact.findInstanceByStartIndex( text_file_partial_path, annotation.startChar, Type.Word);
			if ((startArtifact == null && (ann_type.equals("SECTIME") || ann_type.equals("TIMEX3"))) 
					||
					!annotation.content.toLowerCase().startsWith(startArtifact.getContent().toLowerCase()))
			{
				startArtifact = Artifact.findInstanceByStartIndex( text_file_partial_path, annotation.startChar+1, Type.Word);
			}
			String art_content = startArtifact.getContent().toLowerCase();
			Artifact curr_artifact = startArtifact;
			Artifact end_artifact = curr_artifact;
			
			String annotation_content = annotation.content;
			if (annotation.content.matches(".*&apos;s.*") || annotation.content.matches(".*&apos;d.*"))
			{
				annotation_content = annotation.content.replaceAll("&apos;", "'");
			}
			annotation_content = annotation_content.replaceAll("&quot;", "\"");
			String annotation_content_compressed = 
				annotation_content.replaceAll("\\s+", " ").replaceAll(" ", "").toLowerCase();
			
			while (!art_content.equals(annotation_content_compressed))
			{
				
				curr_artifact =curr_artifact.getNextArtifact();
				if (art_content.endsWith(".") && curr_artifact.getContent().startsWith("."))
					continue;
				
				art_content += curr_artifact.getContent().toLowerCase();
			}
			end_artifact = curr_artifact;
			
			
			assert (startArtifact!=null);

			if(startArtifact==null)
			{
				throw new Exception ("ERROR: Artifact not found for the phrase! Text file: "+
						text_file_partial_path+" annotation: start char"+annotation.startChar);
				
			}
			Phrase rel_phrase = Phrase.getInstance(annotation.content, startArtifact, end_artifact);
			// create phrase
//			Phrase rel_phrase = 
//				Phrase.findInstance(startArtifact,annotation.content);
			if (rel_phrase==null)
			{
				System.out.println("(((((((((((((((");
			}
			rel_phrase.setAltID(annotation.altId);
			
			HibernateUtil.save(rel_phrase);
		}
		
	}

	public static I2b2Annotation setXMLAnnotationGeneralDetails(I2b2Annotation annotation, String line) throws Exception {
		
		String start_char_offest;
		String end_char_offset;
	
		String content;
//		<EVENT id="E1" start="127" end="141" text="GASTRIC BYPASS" modality="FACTUAL" polarity="POS" type="TREATMENT" />
		Pattern p = Pattern.compile("<(EVENT|TIMEX3|SECTIME)\\s+id=\"(.*)\"\\s+start=\"(\\d+)\"" +
				"\\s+end=\"(\\d+)\"\\s+text=\"([^\"]*)\"\\s+(type|modality)=.*$");
		Matcher m = p.matcher(line);
		if (line.matches("<(EVENT|TIMEX3|SECTIME).*") )
		{
			if ( m.matches())
			{
				annotation.type = m.group(1);
				
				annotation.altId = m.group(2);
				
				start_char_offest = m.group(3);
				end_char_offset =m.group(4);
				
				annotation.startChar = Integer.parseInt(start_char_offest);
			
				annotation.endChar = Integer.parseInt(end_char_offset);

				content =  m.group(5);
				annotation.content = content;
		
			}
			else
			{
				throw new Exception("the xml annotation line is not correct.... ");
			}
		}
		
		return annotation;
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
	public static String getFileName(String FilePath)
	{
		String text_file="";
		String file_name="";
		Pattern p = Pattern.compile("(.*/)(\\d+)\\.xml$");
		Matcher m = p.matcher(FilePath);
		if (m.matches())
		{
			file_name = m.group(2);
			text_file = "/"+file_name+".xml.txt";
		}
		return text_file;
	}
	public static String getFileNameWithoutPath(String FilePath)
	{
		String file_name="";
		Pattern p = Pattern.compile("(.*/)(\\d+)\\.xml.?txt?$");
		Matcher m = p.matcher(FilePath);
		if (m.matches())
		{
			file_name = m.group(2);
		}
		return file_name;
	}

}
