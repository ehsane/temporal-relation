package rainbownlp.i2b2.sharedtask2012;

import java.util.ArrayList;
import java.util.List;

import rainbownlp.core.Artifact;
import rainbownlp.core.Phrase;
import rainbownlp.core.PhraseLink;
import rainbownlp.machinelearning.IFeatureCalculator;
import rainbownlp.machinelearning.MLExample;
import rainbownlp.machinelearning.featurecalculator.sentence.SentenceNGram;
import rainbownlp.machinelearning.featurecalculator.sentence.SentenceSyntax;
import rainbownlp.util.FileUtil;
import rainbownlp.util.HibernateUtil;

public class SentenceExampleBuilder {
	final static String ExperimentGroup = "SentenceLinkBinaryClassification";
	public static void main(String[] args) throws Exception
	{
		List<Artifact> sentences = 
				Artifact.listByType(Artifact.Type.Sentence);
		List<IFeatureCalculator> featureCalculators = 
				new ArrayList<IFeatureCalculator>();
		
		featureCalculators.add(new SentenceSyntax());
		featureCalculators.add(new SentenceNGram());
		int counter = 0;
		for(int i=0;i<sentences.size()-1;i++)
		{
			Artifact sentence1 = sentences.get(i);
			Artifact sentence2 = sentence1;
			for(int j=0;j<2;j++)
			{
				sentence2 = sentence2.getNextArtifact();
				if(sentence2==null)
					break;
				
				if(Phrase.getPhrasesInSentence(sentence1).size()==0 || 
						Phrase.getPhrasesInSentence(sentence2).size()==0)
					continue;
				
				PhraseLink sentence_link = 
						PhraseLink.getInstance(sentence1, 
									sentence2);
				
				boolean sentenceLinked = PhraseLink.sentencesLinked(sentence1, sentence2);
				MLExample sentence_example = 
						MLExample.getInstanceForLink(sentence_link, ExperimentGroup);
				
				sentence_example.setExpectedClass(sentenceLinked?1:0);
				sentence_example.setPredictedClass(-1);
				if(sentence1.getAssociatedFilePath().contains("/train/"))
					sentence_example.setForTrain(true);
				else
					sentence_example.setForTrain(false);
				
				HibernateUtil.save(sentence_example);
				
				
				sentence_example.calculateFeatures(featureCalculators);
			}
			
			counter++;
			FileUtil.logLine(null, "Sentence processed: "+counter);
			HibernateUtil.clearLoaderSession();
			   
		}
	}
}
