package rainbownlp.i2b2.sharedtask2012.loader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rainbownlp.core.Phrase;
import rainbownlp.core.PhraseLink;
import rainbownlp.core.PhraseLink.LinkType;
import rainbownlp.core.Setting;
import rainbownlp.util.FileUtil;
import rainbownlp.util.HibernateUtil;


public class TLinkUpdateManager{

	public static void main(String[] args) throws Exception
	{
		String trainingRoot = args[0];
		Setting.TrainingMode = true;
		if(args.length>1 && args[1].equals("test"))
			Setting.TrainingMode = false;
//		Util.log("process documents and convert them to words and calculate attributes", Level.INFO);
		TLinkUpdateManager link_proc = new TLinkUpdateManager();
		link_proc.processTLinkFiles(trainingRoot);
	}
	public void processTLinkFiles(String filesRoot) throws Exception {
		File f = new File(filesRoot);

		if (f.exists() && f.isFile()) {
			//Util.log("Loading document :"+filesRoot, Level.INFO);
			//Util.generateParseFilesIfnotExist(filesRoot);
			try {
				updatePhraseLink(filesRoot);
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
//			FileIterator file_iterator = 
//				FileUtil.getFilesInDirectory(filesRoot,"tlink");
			List<File> files = 
					FileUtil.getFilesInDirectory(filesRoot,"xml");

			//Util.log("Loading documents from :"+filesRoot, Level.INFO);
			int counter = 0;
			for(File file: files) {
				try{
					updatePhraseLink(file.getAbsolutePath());
				}catch (IOException e){
					e.printStackTrace();
				}
				counter++;
			}
			
		}

	}
	private void updatePhraseLink(String filePath) throws Exception {
		
		String text_file_path = XMLLoader.getTextFilePath(filePath);
		String text_file_partial_path = XMLLoader.getFileName(text_file_path.replace(".txt", ""));

		List<I2b2TLink> tagged_tlinks = loadTLinksFromXML(filePath);
		for (I2b2TLink tagged_link: tagged_tlinks)
		{
			
			Phrase fromPhrase = Phrase.findInstance(text_file_partial_path,
					tagged_link.fromAltId);
			Phrase toPhrase = Phrase.findInstance(text_file_partial_path,tagged_link.toAltId);
//			TODO: handle this
			if (toPhrase==null && tagged_link.toAltId.equals("Discharge"))
			{
//				FileUtil.logLine("/home/azadeh/projects/rainbownlp/data/log files/LinkToALtIdIsDischarge.txt",
//						filePath+" "+tagged_link);
				continue;
//				toPhrase =Phrase.getInstance("Discharge", "Discharge","MADEUP");
			}
			
			if (!(fromPhrase.getPhraseContent().equals(tagged_link.fromContent) 
					&& toPhrase.getPhraseContent().equals(tagged_link.toContent)))
			{
				throw new Exception("ERROR: Phrase not found for the link! Text file: "+
				text_file_path+" tLink: "+tagged_link.fromAltId + tagged_link.toAltId);
			}
			if(fromPhrase==null || toPhrase==null )
			{
				
				throw new Exception("ERROR: Phrase not found for the link! Text file: "+
						text_file_path+" tLink: "+tagged_link.fromAltId + tagged_link.toAltId);
				
			}
			
			LinkType link_type;
			
			PhraseLink phrase_link = 
				PhraseLink.createInstance(fromPhrase,toPhrase);
			
			link_type = LinkType.valueOf(tagged_link.linkType);
			String altId = tagged_link.linkAltId;
			if (link_type== null || altId == null || altId.isEmpty())
			{
				throw new Exception("Empty");
			}
			
			phrase_link.setLinkType(link_type);
			phrase_link.setAltLinkID(altId);
			
//			if (phrase_link !=null)
//			{
//				link_type = LinkType.valueOf(tagged_link.linkType);
//			}
//			else
//			{
//				PhraseLink phrase_link_reverse = 
//					PhraseLink.findPhraseLink(toPhrase,fromPhrase);
//				if (phrase_link_reverse != null)
//				{
//					if (tagged_link.linkType.equals("BEFORE"))
//					{
//						link_type = LinkType.valueOf("AFTER");
//					}
//					else if (tagged_link.linkType.equals("AFTER"))
//					{
//						link_type = LinkType.valueOf("BEFORE");
//					}
//					else
//					{
//						link_type = LinkType.valueOf(tagged_link.linkType);
//					}
//					phrase_link = phrase_link_reverse;
//				}
//				else
//				{
//					phrase_link = 
//						PhraseLink.getInstance(fromPhrase,toPhrase);
//					link_type = LinkType.valueOf(tagged_link.linkType);
//				}
//			}
		
			
			//This was for updating the forTrain Field
			String updateQuery = "update PhraseLink set linkType = "+link_type.ordinal() +
			" , forTrain = false, altLinkId ='"+tagged_link.linkAltId+"'  where PhraseLinkId="+phrase_link.getPhraseLinkId();
			
			HibernateUtil.executeNonReader(updateQuery);
			
			//This was for updating the forTrain Field
//			String savePredictedQuery = "update PhraseLink set forTrain = false" +
//			" where PhraseLinkId="+phrase_link.getPhraseLinkId();
//			HibernateUtil.executeNonReader(savePredictedQuery);
			
			
		}
	}
	//This was used for t
	private void loadAnnotations(String filePath) throws Exception {
		// read each line
		//get content, start and end
		List<String> lines = FileUtil.loadLineByLine(filePath);
	    for (String line : lines) 
	    	parseTLinkLine(line,filePath);
	}
	
	private void parseTLinkLine(String line, String filePath) throws Exception {
//		EVENT="hepatomegaly" 29:15 29:15||TIMEX3="10/04/1993" 4:0 4:0||type="BEFORE"
		//getting the line and token indexes
		String text_file_path = getTextFilePath(filePath);
		
		I2b2TLink tLink = getTLinkDetails(line);
		
		//get start and end phrases
		
		Phrase fromPhrase = Phrase.findInstance(text_file_path,
				tLink.fromStartLineOffset,tLink.fromStartWordOffset,
				tLink.fromEndLineOffset,tLink.fromEndWordOffset);
		Phrase toPhrase = Phrase.findInstance(text_file_path,
				tLink.toStartLineOffset,tLink.toStartWordOffset,
				tLink.toEndLineOffset,tLink.toEndWordOffset);
		
		assert (fromPhrase!=null);
		assert (toPhrase!=null);
		if(fromPhrase==null || toPhrase==null || tLink.linkType.equals(""))
		{
			System.out.println("ERROR: Phrase not found for the link! Text file: "+
					text_file_path+" tLink: "+line);
			return;
		}
		// create phraseLink
		PhraseLink phrase_link = 
			PhraseLink.getInstance(fromPhrase,toPhrase);
		
		phrase_link.setLinkType(LinkType.valueOf(tLink.linkType));
		
		HibernateUtil.save(phrase_link);
	}
	private I2b2TLink getTLinkDetails(String line) throws Exception {
		
		I2b2TLink tLink = new I2b2TLink();
//		EVENT="The 1 cm cyst" 30:1 30:3||TIMEX3="10/04/1993" 4:0 4:0||type="BEFORE"
		Pattern p = Pattern.compile("(EVENT|TIMEX3|SECTIME)=\"(.*)\"\\s+(\\d+):(\\d+)\\s+(\\d+):(\\d+)\\|\\|" +
				"(EVENT|TIMEX3|SECTIME)=\"(.*)\"\\s+(\\d+):(\\d+)\\s+(\\d+):(\\d+)\\|\\|type=\"(.*)\"");
		Matcher m = p.matcher(line);
		if (m.matches())
		{
			tLink.fromType = m.group(1);
			tLink.fromContent = m.group(2);
			tLink.fromStartLineOffset = Integer.parseInt(m.group(3));
			tLink.fromStartWordOffset = Integer.parseInt(m.group(4));
			tLink.fromEndLineOffset = Integer.parseInt(m.group(5));
			tLink.fromEndWordOffset = Integer.parseInt(m.group(6));
			
			tLink.toType = m.group(7);
			tLink.toContent = m.group(8);
			tLink.toStartLineOffset = Integer.parseInt(m.group(9));
			tLink.toStartWordOffset = Integer.parseInt(m.group(10));
			tLink.toEndLineOffset = Integer.parseInt(m.group(11));
			tLink.toEndWordOffset = Integer.parseInt(m.group(12));
			
			
			tLink.linkType = m.group(13);
		}
		else
		{
			throw new Exception("something wring with the input tlink line");
		}
		return tLink;
	}

	public static String getTextFilePath(String extentFilePath)
	{
//		/home/azadeh/Documents/i2b22012/trainingData/2012-06-18.release/1.xml.extent
		String text_file="";
		String file_name="";
		Pattern p = Pattern.compile("(.*/)(\\d+)\\.xml\\.tlink$");
		Matcher m = p.matcher(extentFilePath);
		if (m.matches())
		{
			file_name = m.group(2);
			text_file = m.group(1)+file_name+".xml.txt";
		}
		return text_file;
	}
	public static List<I2b2TLink> loadTLinksFromXML(String filePath) throws Exception
	{
		
		List<I2b2TLink> tagged_tlinks = new ArrayList<I2b2TLink>();
		
		List<String> lines = FileUtil.loadLineByLine(filePath.replaceAll("ehsan", "azadeh"));
		
	    for (String line : lines)
	    {
	    	if (!line.matches("<TLINK id=.*") )
	    		continue;
	    	I2b2TLink curI2b2TLink = 
	    		getI2b2TLINKFromXMLLine(line);
	    	if(curI2b2TLink!=null)
	    		tagged_tlinks.add(curI2b2TLink);
	    }
	    
	    return tagged_tlinks;

	}
	private static I2b2TLink getI2b2TLINKFromXMLLine(String line) throws Exception {
		I2b2TLink tlink = null;
		if (line.matches("<TLINK id=.*") )
		{
			tlink = new I2b2TLink();
			
			
			tlink = getTLinkDetailsFromXML(line);
			
		}
		
		return tlink;
	}
private static I2b2TLink getTLinkDetailsFromXML(String line) throws Exception {
		
		I2b2TLink tLink = new I2b2TLink();
		
//		<TLINK id="TL105" fromID="E88" fromText="responded" toID="E86" toText="diuresed" type="OVERLAP" />
		Pattern p = Pattern.compile("<TLINK\\s+id=\"(.*)\"\\s+fromID=\"(\\w+)\"" +
		"\\s+fromText=\"(.*)\"\\s+toID=\"(.*)\"\\s+toText=\"(.*)\"\\s+type=\"(.*)\"\\s+/>");
		
		Matcher m = p.matcher(line);
		if (m.matches())
		{
			tLink.linkAltId =  m.group(1);
			tLink.fromAltId = m.group(2);
			tLink.fromContent = m.group(3);
			tLink.toAltId = m.group(4);
			tLink.toContent = m.group(5);
			tLink.linkType = m.group(6);
			if (tLink.linkType.equals(""))
			{
				tLink.linkType = PhraseLink.LinkType.UNKNOWN.name();
			}
		}
		else
		{
			throw new Exception("wrong TLINK tag..."+line);
		}

		return tLink;
	}
}
