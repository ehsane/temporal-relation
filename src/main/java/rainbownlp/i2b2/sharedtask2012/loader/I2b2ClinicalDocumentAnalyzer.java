package rainbownlp.i2b2.sharedtask2012.loader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import rainbownlp.core.Artifact;
import rainbownlp.core.Setting;
import rainbownlp.preprocess.DocumentAnalyzer;
import rainbownlp.preprocess.Tokenizer;
import rainbownlp.util.FileUtil;
import rainbownlp.util.HibernateUtil;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.umass.cs.mallet.base.pipe.iterator.FileIterator;


public class I2b2ClinicalDocumentAnalyzer extends DocumentAnalyzer{

	public static void main(String[] args)
	{
		String trainingRoot = args[0];
		Setting.TrainingMode = true;
		if(args.length>1 && args[1].equals("test"))
			Setting.TrainingMode = false;
//		Util.log("process documents and convert them to words and calculate attributes", Level.INFO);
		I2b2ClinicalDocumentAnalyzer doc_proc = new I2b2ClinicalDocumentAnalyzer();
		doc_proc.processDocuments(trainingRoot);
	}
	
	private List<Artifact> documents;

	private void loadDocuments(String filesRoot) {
		File f = new File(filesRoot);
		List<Artifact> loaded_documents = new ArrayList<Artifact>();
		if (f.exists() && f.isFile()) {
			//Util.log("Loading document :"+filesRoot, Level.INFO);
			//Util.generateParseFilesIfnotExist(filesRoot);
	
			Artifact new_doc = Artifact.getInstance(Artifact.Type.Document, filesRoot, 0);
			try {
				loadSentences(new_doc);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			loaded_documents.add(new_doc);

		} else {
			List<File> files = 
				FileUtil.getFilesInDirectory(filesRoot,"");
				
			//Util.log("Loading documents from :"+filesRoot, Level.INFO);
			int counter = 0;
			for(File file: files) {
//				if(!file_path.contains("0330.txt")) continue;
				//Util.generateParseFilesIfnotExist(file_path);
				
				Artifact new_doc = 
					Artifact.getInstance(Artifact.Type.Document, file.getAbsolutePath(), 0);
				
				loaded_documents.add(new_doc);
				counter++;
			}
			
			for(Artifact doc:loaded_documents){
				System.out.print("\nLoading document: " + doc.getAssociatedFilePath());
				try {
					loadSentences(doc);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		this.documents = loaded_documents;
		// findTriggersBagOfWords();

	}


	private void loadSentences(Artifact parentDoc) throws IOException {
		Tokenizer docTokenizer = new Tokenizer(parentDoc.getAssociatedFilePath());
		HashMap<Integer, String> setences = docTokenizer.getSentences();
		List<Artifact> setencesArtifacts = new ArrayList<Artifact>();
		Artifact previous_sentence = null;
		
		for(int curSentenceIndex=0;
			curSentenceIndex<setences.keySet().size();curSentenceIndex++){
//			System.out.print("\r Loading sentences for: "+parentDoc.get_associatedFilePath()+ " "+
//					curSentenceIndex + "/ " + setences.size()+longspace);
			
			String tokenizedSentence = setences.get(curSentenceIndex);
			List<Integer> tokens_starts = 
				docTokenizer.sentences_tokens_indexes.get(curSentenceIndex);
			List<Word> tokens = 
				docTokenizer.sentences_tokens.get(curSentenceIndex);
		
			if(tokens.size()==0)
				continue;

			Artifact new_sentence = Artifact.getInstance(Artifact.Type.Sentence,
					parentDoc.getAssociatedFilePath(), tokens_starts.get(0));//line number start from 1
			new_sentence.setParentArtifact(parentDoc);
			new_sentence.setLineIndex(curSentenceIndex+1);
			new_sentence.setContent(tokenizedSentence);
			if (previous_sentence != null) {
				new_sentence.setPreviousArtifact(previous_sentence);
				previous_sentence.setNextArtifact(new_sentence);
				HibernateUtil.save(previous_sentence);
			}
			
			HibernateUtil.save(new_sentence);
			
			loadWords(new_sentence,tokens, tokens_starts, curSentenceIndex);

			setencesArtifacts.add(new_sentence);
			
			//Pattern.insert(Util.getSentencePOSsPattern(curSentence));
			previous_sentence = new_sentence;
			HibernateUtil.clearLoaderSession();
		}
//		parentDoc.setChildsArtifact(setencesArtifacts);

	}

	private void loadWords(Artifact parentSentence,  List<Word> tokens, 
			List<Integer> starts, int parentOffset) {
		
		List<Artifact> tokensArtifacts = new ArrayList<Artifact>();
		Artifact previous_word = null;
		int tokens_count = tokens.size();
		//save POS
		String textContent = "";
		Artifact new_word = null;
//		Util.log(""+tokens_count, 1);
		for(int curTokenIndex=0;
			curTokenIndex<tokens_count;curTokenIndex++){
			textContent = 
				PTBTokenizer.ptbToken2Text(tokens.get(curTokenIndex).value());
			new_word = Artifact.getInstance(
					Artifact.Type.Word, 
					parentSentence.getAssociatedFilePath(), starts.get(curTokenIndex));
			new_word.setContent(textContent);
			new_word.setParentArtifact(parentSentence);
			new_word.setLineIndex(parentOffset+1);
			new_word.setWordIndex(curTokenIndex);

			if (previous_word != null) {
				new_word.setPreviousArtifact(previous_word);
				previous_word.setNextArtifact(new_word);
				HibernateUtil.save(previous_word);
			}
			
			HibernateUtil.save(new_word);
				
			tokensArtifacts.add(new_word);
			previous_word = new_word;
			
		}
//		parentSentence.setChildsArtifact(tokensArtifacts);
	}

	public List<Artifact> processDocuments(String rootPath){
		int numberOfInstances = 0;
		
		loadDocuments(rootPath);
		
//		Tokenizer.fixDashSplitted();
		
		return documents;
	}
}
