/**
 * 
 */
package rainbownlp.i2b2.sharedtask2012.featurecalculator.link;

import java.util.List;


import rainbownlp.core.FeatureValuePair;
import rainbownlp.core.Phrase;
import rainbownlp.core.PhraseLink;
import rainbownlp.i2b2.sharedtask2012.patternminer.GeneralizedSentence;
import rainbownlp.i2b2.sharedtask2012.patternminer.GeneralizedSentence.GeneralizationModels;
import rainbownlp.machinelearning.IFeatureCalculator;
import rainbownlp.machinelearning.MLExample;
import rainbownlp.machinelearning.MLExampleFeature;
import rainbownlp.util.StringUtil;


/**
 * @author Azadeh
 * 
 */
public class NormalizedNGrams implements IFeatureCalculator {
	
	public static void main(String[] args) throws Exception
	{
		String experimentgroup = "BinaryLinkClassifier";
		List<MLExample> trainExamples = 
			MLExample.getAllExamples(experimentgroup, true,5);
		
		for ( MLExample example_to_process: trainExamples )
		{
			
			NormalizedNGrams n_grams =  new NormalizedNGrams();
			
			n_grams.calculateFeatures(example_to_process);
			
		}
		
		
		
	}
		@Override
	public void calculateFeatures(MLExample exampleToProcess) throws Exception {
			
			PhraseLink phraseLink = exampleToProcess.getRelatedPhraseLink();
			Phrase phrase1 = phraseLink.getFromPhrase();
			Phrase phrase2 = phraseLink.getToPhrase();
			
			//get related generalized sentence, TTO mean targets=type, Mentions=Tyepe and nonmention=Otiginal
			GeneralizedSentence generalized_sent =
				GeneralizedSentence.getInstance(phrase1, phrase2, phrase1.getStartArtifact().getParentArtifact(),
						GeneralizationModels.TTO, phraseLink.getLinkType(),exampleToProcess.getForTrain());		
			//here it calculates the ngrams based on original non mentions and also includes the target args also 
			calculateNgrams(exampleToProcess, generalized_sent.getBetweenMentionsChunk(), "NorBetweenNGram");
				
	}
	
	public static void calculateNgrams(MLExample exampleToProcess, String content, String featureName)	
	{
		String[] word_text = 
			StringUtil.getTermByTermWordnet(content.toLowerCase()).split(" ");
		
		for(int i=0;i<word_text.length;i++)
		{
			String cur_content = word_text[i].trim();
			for(int n=2;n<3;n++)
			{
				int new_part_index = i+n-1;
				if(new_part_index<word_text.length && !word_text[new_part_index].trim().equals(""))
				{
					cur_content = 
						cur_content.concat("_"+word_text[new_part_index].trim());
					FeatureValuePair value_pair = FeatureValuePair.getInstance(
							featureName+n, cur_content, "1");
					MLExampleFeature.setFeatureExample(exampleToProcess,value_pair);
			
				}
			}
		}
	}

	
}
