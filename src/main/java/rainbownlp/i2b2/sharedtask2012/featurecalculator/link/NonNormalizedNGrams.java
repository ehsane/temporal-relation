/**
 * 
 */
package rainbownlp.i2b2.sharedtask2012.featurecalculator.link;

import java.util.List;

import rainbownlp.core.Artifact;
import rainbownlp.core.Phrase;
import rainbownlp.core.PhraseLink;
import rainbownlp.machinelearning.IFeatureCalculator;
import rainbownlp.machinelearning.MLExample;


/**
 * @author ehsan
 * 
 */
public class NonNormalizedNGrams implements IFeatureCalculator {
	
	public static void main(String[] args) throws Exception
	{
		String experimentgroup = "BinaryLinkClassifier";
//		List<MLExample> trainExamples = 
//			MLExample.getAllExamples(experimentgroup, true);

		List<MLExample> trainExamples = 
			MLExample.getAllExamples(experimentgroup, true);
		
		for ( MLExample example_to_process: trainExamples )
		{
			NonNormalizedNGrams n_grams =  new NonNormalizedNGrams();
			
			n_grams.calculateFeatures(example_to_process);
		}
		
		
		
	}
	public void calculateFeatures(MLExample exampleToProcess) {
			
			PhraseLink phraseLink = exampleToProcess.getRelatedPhraseLink();
			Phrase phrase1 = phraseLink.getFromPhrase();
			Phrase phrase2 = phraseLink.getToPhrase();
			
//			String[] word_text = 
//				StringUtil.getTermByTermWordnet(phrase1.getStartArtifact().getParentArtifact().getContent().toLowerCase()).split(" ");
			int window = 2;
			Artifact curArtifact1 = phrase1.getStartArtifact();
			Artifact curArtifact2 = phrase2.getStartArtifact();
			String content1 = phrase1.getPhraseContent();
			String content2 = phrase2.getPhraseContent();
			//backward
			for(int i=0;i<window;i++)
			{
				if(curArtifact1.getPreviousArtifact()!=null)
				{
					curArtifact1 = curArtifact1.getPreviousArtifact();
					content1 = curArtifact1.getContent()+" "+content1;
				}
				if(curArtifact2.getPreviousArtifact()!=null)
				{
					curArtifact2 = curArtifact2.getPreviousArtifact();
					content2 = curArtifact2.getContent()+" "+content2;
				}
			}
			curArtifact1 = phrase1.getEndArtifact();
			curArtifact2 = phrase2.getEndArtifact();
			//forward
			for(int i=0;i<window;i++)
			{
				if(curArtifact1.getNextArtifact()!=null)
				{
					curArtifact1 = curArtifact1.getNextArtifact();
					content1 = content1+" "+curArtifact1.getContent();
				}
				if(curArtifact2.getNextArtifact()!=null)
				{
					curArtifact2 = curArtifact2.getNextArtifact();
					content2 = content2+" "+curArtifact2.getContent();
				}
			}
			
			NormalizedNGrams.calculateNgrams(exampleToProcess, content1, "NonNormalizedNGramFrom");
			NormalizedNGrams.calculateNgrams(exampleToProcess, content1, "NonNormalizedNGramTo");
			
//			for(int i=0;i<word_text.length;i++)
//			{
//				String cur_content = word_text[i].trim();
//				for(int n=2;n<4;n++)
//				{
//					int new_part_index = i+n-1;
//					if(new_part_index<word_text.length && !word_text[new_part_index].trim().equals(""))
//					{
//						cur_content = 
//							cur_content.concat("_"+word_text[new_part_index].trim());
//						FeatureValuePair value_pair = FeatureValuePair.getInstance(
//								"NonNormalizedNGram"+n, cur_content, "1");
//						MLExampleFeature.setFeatureExample(exampleToProcess,value_pair);
//				
//					}
//				}
//			}

				
	}
		
		
		

	
}
