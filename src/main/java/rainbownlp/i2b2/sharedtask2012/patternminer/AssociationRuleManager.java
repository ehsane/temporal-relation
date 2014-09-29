package rainbownlp.i2b2.sharedtask2012.patternminer;



public class AssociationRuleManager {
	static String rootPath = "/host/ubnutustuff/SentimentFinder/sentiment_finder_misc/";
//	static String rootPath = "/home/azadeh/SentimentFinder/sentiment_finder_misc/";
	public static void main(String[] args) throws Exception
	{
//		generateDepTxns();
//		generateFreqPatterns(rootPath+"patterns/dependencies/");
		
		//this will use regex and nell categories
//		generateNormalizedDepTxns();
//		generateFreqPatterns(rootPath+"patterns/dependencies/normalized");
		
//		generateNormalizedTxns();
//		generateFreqseqPatterns(rootPath+"patterns/normalized","nor");
//		generateFreqPatterns(rootPath+"patterns/normalized");
		
		
		
		
//		generatePOSSentTxns();
//		generateFreqseqPatterns(rootPath+"patterns/just-pos","just-pos");
		
//		generateSentTxns();
//		generatePreprocessedSentTxns();
//		generateFreqPatterns(rootPath+"patterns/preprocessed","preprocess");
		
		
		
//		generatePartialPOSSentTxns();
//		generateFreqseqPatterns(rootPath+"patterns/pos","pos");
//		generateFreqPatterns(rootPath+"patterns/preprocessed","preprocess");
		
//		generatePolarDepTxns();
//		generateFreqPatterns(rootPath+"patterns/dependencies/polar");
	}
	

//
//
//public static void generateSentGenTxns(TLinkPattern.targetMentionNonMentionType pGenMethod )
//		throws Exception
//{
//	// TODO add a switch case to call the related methods
//	// get all transactions
//	ArrayList<GeneralizedSentence> gen_sentences = GeneralizedSentence.getListByType(GeneralizedSentence.targetMentionNonMentionType.TTO);
////	String[] word_text = 
////		Utils.getTermByTermWordnet(Utils.replaceNameEntities(sentenceToProcess.getContent().replaceAll("[^A-Za-z0-9\\s]", "").toLowerCase())).split(" ");
//	Convertor.ExampleTypes[] exampleTypes = Convertor.ExampleTypes.values();
//	for (Convertor.ExampleTypes emotion:exampleTypes)
//	{
//		File file = new File(rootPath+"/patterns/txn-"+emotion);
//		BufferedWriter output = new BufferedWriter(new FileWriter(file));
//		
//		ResultSet rs = SentenceExampleTable.getAllTrainingSentenceExamplesByCategory(emotion.toString());
//		while (rs.next())
//		{
//			String txn_line="";
//			int sent_example_id = rs.getInt("id");
//			SentenceExample curExampleObj = new SentenceExample(sent_example_id);
//			 
//			Sentence sentToProcess = curExampleObj.relatedSentence;
//			txn_line = 
//				Utils.getTermByTermWordnet(Utils.replaceNameEntities(sentToProcess.getContent().replaceAll("[^A-Za-z0-9\\s]", "").toLowerCase()));
//			output.write(txn_line+"\n");
//		}
//		output.flush();
//		output.close();
//		
//	}
//}

//
//
//public static String getPOSTxn(String pos_tags) throws Exception
//{
//
//	String[] words_tags = pos_tags.split(" ");
//	String txn_line="";
//
//	for(int i=0;i<words_tags.length;i++){
//		String word = words_tags[i].split("/")[0];
//		String pos =  words_tags[i].split("/")[1];
////				ArrayList<String> stopWords = new ArrayList<String>(Arrays.asList("and"));
////				if (stopWords.contains(word))
////				{
////					continue;
////				}
//		txn_line = txn_line.concat(pos+" ");
//
//	}
//
//	return txn_line;	
//}
////*******************************
//static void generateFreqPatterns(String txnFilePath,String param) throws Exception
//{
//	Convertor.IncludedExampleTypes[] exampleTypes = Convertor.IncludedExampleTypes.values();
//	String extractor_engine = "/home/azadeh/Apps/apriori ";
//	
//	for (Convertor.IncludedExampleTypes emotion:exampleTypes)
//	{
//		
//		String input_file = txnFilePath+"/txn-"+emotion;
//		String output_file =  txnFilePath+"/freq-items-"+emotion;
////		String engine_params = " -ts -m2 -n4 -s1 -c70 ";
//        String engine_params = " -ts -m2 -n2 -s2 -c70 ";
//		if(param.equals("preprocess"))
//		{
//			engine_params = " -ts -m2 -n3 -s3 -c70 ";
//		}
//		String myShellScript = extractor_engine+engine_params+ input_file+" "+output_file;
//		
//		Utils.runShellCommand(myShellScript);
//
//
//	}
//}
//static void generateFreqseqPatterns(String txnFilePath,String featureType) throws Exception
//{
//	Convertor.ExampleTypes[] exampleTypes = Convertor.ExampleTypes.values();
//	String extractor_engine = "/home/azadeh/Apps/seqwog ";
//	
//	for (ExampleTypes emotion:exampleTypes)
//	{
//		String input_file = txnFilePath+"/txn-"+emotion;
//		String output_file =  txnFilePath+"/freq-seq-items-"+emotion;
//		String engine_params = " -tc -m2 -n4 -s1 ";
//		//for the patterns
//		if (featureType.equals("just-pos"))
//		{
//			engine_params = " -ts -m3  -s3 ";
//		}
//		if (featureType.equals("pos"))
//		{
//			engine_params = " -ts -m3  -s4 ";
//		}
//		if (featureType.equals("nor"))
//		{
//			engine_params = " -tc -m3 -n3 -s1 ";
//		}
//		
//		String myShellScript = extractor_engine+engine_params+ input_file+" "+output_file;
//		
//		Utils.runShellCommand(myShellScript);
//
//	}
//}





}
