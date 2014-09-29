package rainbownlp.i2b2.sharedtask2012.loader;

import rainbownlp.core.Setting;
import rainbownlp.i2b2.sharedtask2012.patternminer.GeneralizedSentence;
import rainbownlp.parser.ParseHandler;


public class I2b22012MainLoader{

	public static void main(String[] args) throws Exception
	{
		String trainingRoot = args[0];
		Setting.TrainingMode = true;
		if(args.length>1 && args[1].equals("test"))
			Setting.TrainingMode = false;
//		// 1. Load documents
		I2b2ClinicalDocumentAnalyzer doc_proc = new I2b2ClinicalDocumentAnalyzer();
		doc_proc.processDocuments(trainingRoot);
////		
		System.out.println("Loading events...\n");
		//2. Load events
		I2b2AnnotationAnalyzerFromXML ann_proc = new I2b2AnnotationAnalyzerFromXML();
		ann_proc.processAnnotationFiles(trainingRoot);
		
		System.out.println("Parsing...\n");
//		//3. POS and normalization
		GeneralizedSentence.calculateStanfordParseAndNormalize(false);
		
		System.out.println("Loading Tlinks...\n");
		//3. Load TLinks
		if (Setting.TrainingMode == false)
		{
			I2b2TLinkAnnotationAnalyzer link_proc = new I2b2TLinkAnnotationAnalyzer();
			link_proc.processTLinkFiles(trainingRoot);
		}
		
	}
	

	
	
}
