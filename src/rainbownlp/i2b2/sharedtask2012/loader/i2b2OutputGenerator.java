package rainbownlp.i2b2.sharedtask2012.loader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rainbownlp.core.Phrase;
import rainbownlp.core.PhraseLink;
import rainbownlp.core.PhraseLink.LinkType;
import rainbownlp.i2b2.sharedtask2012.LinkExampleBuilder;
import rainbownlp.machinelearning.MLExample;
import rainbownlp.util.FileUtil;
// gets  a set of test MLExamples and the input file directory that keeps all the gold statndards and an optional output directory that saves the 
// gold and system Folders that will be used by the evaluation script
public class i2b2OutputGenerator {
	
	// for each file in the directory creat the map key and put the current lines
	// then for each example find the related  list and add to it
// gets the input and output directory as the arguments 	
	public static String root = "/host/ubnutustuff/projects/i2b2Temporal/i2b22012";
	public static void main(String[] args) throws Exception
	{
//		String input_directory = args[0];
//		String outputDirectory = args[1];
//
//		List<String> experimentGroups = Arrays.asList(new String[]{SecTimeEventExampleBuilder.ExperimentGroupSecTimeEvent});
//		
//		for (String experiment_group: experimentGroups)
//		{
//			//create a new directory just for testing, it is not used for real test
////			outputDirectory = outputDirectory+"/"+experiment_group;
//			
//			List<MLExample> ml_examples = MLExample.getLastExamples(experiment_group, true,200);
//			
//			generateOutPutFiles(ml_examples, input_directory, outputDirectory);	
//			
//		
//		}
		
		
		List<MLExample> testExamples = 
			MLExample.getLastExamples(LinkExampleBuilder.ExperimentGroupEventEvent, true,500);
		
		i2b2OutputGenerator.generateOutPutFiles(testExamples,
				root+"/train/2012-07-06.release-fix", 
				root+"/output");
	
		
	}
	
	public static void generateOutPutFiles(List<MLExample> testExamples, String inputDirectory, String outPutDirectory) throws Exception
	{
		//create new gold and systemm directories
		String system_output_directory = FileUtil.deleteAndCreateFolder(outPutDirectory+"/system");
		String gold_directory = FileUtil.deleteAndCreateFolder(outPutDirectory+"/gold");
		
		ArrayList<String> involved_xml_files_in_test = new ArrayList<String>();
		HashMap<String, List<String>> fileNameToLinesMap =  new HashMap<String, List<String>>();
		//this is just to make it fast
		HashMap<MLExample,String> mlExampleIdTofilePath =  new HashMap<MLExample, String>();
		// for all MLexamples get the related file and put in gold
		for (MLExample ml_example: testExamples)
		{
			// get the related file
			String related_file_path = ml_example.getAssociatedFilePath();
			mlExampleIdTofilePath.put(ml_example, related_file_path);
			related_file_path = related_file_path.replaceAll("\\.txt", "");
			String absolute_file_name = inputDirectory+"/"+getFileNameFromPath(related_file_path,"xml")+".xml";
			
			
			if (!involved_xml_files_in_test.contains(absolute_file_name))
				involved_xml_files_in_test.add(absolute_file_name);
		}
		for (String file_path: involved_xml_files_in_test)
		{
			String file_name = getFileNameFromPath(file_path,"xml");
			//put the file in gold folder
			FileUtil.CopyFileToDirectory(file_name+".xml", inputDirectory, gold_directory);
			
			//create a list of all the lines before tags
			List<String> xml_lines = readLinesBeforeEndTags(file_path);

			fileNameToLinesMap.put(file_name, xml_lines);
		}
		int counter = 0;
		
		for (MLExample testExample: mlExampleIdTofilePath.keySet())
		{
			
			String related_file_path = mlExampleIdTofilePath.get(testExample);
			String file_name = getFileNameFromPath(related_file_path,"xml\\.txt");
			List<String> related_file_lines = fileNameToLinesMap.get(file_name);
			if (related_file_lines ==null)
			{
				throw new Exception ("the associated file path does not exist in the input file "+related_file_path);
			}
			
			if (testExample.getPredictedClass() >0)
			{
				MLExample.hibernateSession.update(testExample);
				String xml_link_line = convertMLExampleToTLinkTag(testExample, counter);
				counter++;
				related_file_lines.add(xml_link_line);
//					TODO: is this line necessary?
				fileNameToLinesMap.put(file_name, related_file_lines);
			}	
		}
		
		for (String file_name:fileNameToLinesMap.keySet())
		{
//			get the list
			List<String> list_of_lines= fileNameToLinesMap.get(file_name);
//			add the closing tags
			list_of_lines.add("</TAGS>");
			list_of_lines.add("</ClinicalNarrativeTemporalAnnotation>");
//			update the map
			fileNameToLinesMap.put(file_name, list_of_lines);
			
//			write to file
			FileUtil.createFile(FileUtil.getFilePathInDirectory(system_output_directory,file_name+".xml"), list_of_lines);
		}
	}

private static List<String> readLinesBeforeEndTags(String file_path) {
		List<String> xml_lines =  new ArrayList<String>();
		List<String> all_lines = FileUtil.loadLineByLine(file_path);
		
		for (String line: all_lines)
		{
			if (!line.startsWith("</") && !line.startsWith("<TLINK") )
			{
				xml_lines.add(line);
			}
		}
		return xml_lines;
	}
public static String convertMLExampleToTLinkTag(MLExample ml_example, int counter)
{
//	<TLINK id="TL0" fromID="E12" fromText="admission" toID="E0" toText="ADMISSION" type="OVERLAP" />
	
	PhraseLink phrase_link = ml_example.getRelatedPhraseLink();
	Phrase from_phrase = Phrase.getInstance(phrase_link.getFromPhrase().getPhraseId());
	Phrase to_phrase = Phrase.getInstance(phrase_link.getToPhrase().getPhraseId());
	
	String from_text = from_phrase.getPhraseContent();
	String to_text =  to_phrase.getPhraseContent();
	
//	TODO: complete phrase to have it
	String from_id = from_phrase.getAltID();
	String to_id = to_phrase.getAltID();
//	String link_alt_id = phrase_link.getAltLinkID();
	String tlink_tag="";
	//TODO: convert to value
	int type_number = ml_example.getPredictedClass().intValue();
	if (type_number >0)
	{
		String type = LinkType.getEnum(ml_example.getPredictedClass().intValue()).name();
		
//		<TLINK id="TL0" fromID="E12" fromText="admission" toID="E0" toText="ADMISSION" type="OVERLAP" />
		tlink_tag = "<TLINK id=\"TL"+counter+"\" fromID=\""+from_id+"\"" +
				" fromText=\""+from_text+"\" toID=\""+to_id+"\" toText=\""+to_text+"\" " +
				"type=\""+type+"\" />";
	}
	
	return tlink_tag;
}
public static String getFileNameFromPath(String file_path,String extention)
{
//	/home/azadeh/i2b22012/train/2012-07-06.release-fix/2.xml
	String file_name="";
	Pattern p = Pattern.compile(".*/(\\w+)\\."+extention+"$");
	Matcher m = p.matcher(file_path);
	if (m.matches())
	{
		file_name = m.group(1);
	}
	return file_name;
}
}
