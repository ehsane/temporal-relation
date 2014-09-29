/**
 * 
 */
package rainbownlp.i2b2.sharedtask2012.featurecalculator.link;

import java.util.List;

import rainbownlp.core.FeatureValuePair;
import rainbownlp.core.Phrase;
import rainbownlp.core.PhraseLink;
import rainbownlp.core.FeatureValuePair.FeatureName;
import rainbownlp.i2b2.sharedtask2012.ClinicalEvent;
import rainbownlp.i2b2.sharedtask2012.TimexPhrase;
import rainbownlp.machinelearning.IFeatureCalculator;
import rainbownlp.machinelearning.MLExample;
import rainbownlp.machinelearning.MLExampleFeature;

/**
 * @author ehsan-Azadeh
 * 
 */
public class LinkArgumentBasicFeatures implements IFeatureCalculator {
	
	public static void main (String[] args) throws Exception
	{
		String experimentgroup = "BinaryLinkClassifier";
		List<MLExample> trainExamples = 
			MLExample.getAllExamples(experimentgroup, true,50);
		for (MLExample example:trainExamples)
		{
			LinkArgumentBasicFeatures lbf = new LinkArgumentBasicFeatures();
			lbf.calculateFeatures(example);
		}
		
	}
	public void calculateFeatures(MLExample exampleToProcess) throws Exception {
			
			PhraseLink phraseLink = exampleToProcess.getRelatedPhraseLink();
			Phrase phrase1 = phraseLink.getFromPhrase();
			Phrase phrase2 = phraseLink.getToPhrase();
			
			String phrase1_type = phrase1.getPhraseEntityType();
			String phrase2_type = phrase2.getPhraseEntityType();
			
			// timex=0 and event=1
			Integer linke_type = 0;
			if(phrase1_type.equals("EVENT"))
				linke_type = 10;
			if(phrase2_type.equals("EVENT"))
				linke_type += 1;
			
			FeatureValuePair argumentTypeFeature = FeatureValuePair.getInstance(
						FeatureName.LinkArgumentType, 
						linke_type.toString(), "1");
				
			MLExampleFeature.setFeatureExample(exampleToProcess, argumentTypeFeature);
			
			if(phrase1_type.equals("EVENT"))
			{
				ClinicalEvent phrase1_event = ClinicalEvent.getRelatedEventFromPhrase(phrase1);
				
				FeatureValuePair eventTypeFeature = FeatureValuePair.getInstance(
						FeatureName.LinkFromPhraseType, 
						phrase1_event.getEventType().toString(), "1");
				
				MLExampleFeature.setFeatureExample(exampleToProcess, eventTypeFeature);
				
				FeatureValuePair eventModalityFeature = FeatureValuePair.getInstance(
						FeatureName.LinkFromPhraseModality, 
						phrase1_event.getModality().toString(), "1");
				
				MLExampleFeature.setFeatureExample(exampleToProcess, eventModalityFeature);
				
				FeatureValuePair eventPolarityFeature = FeatureValuePair.getInstance(
						FeatureName.LinkFromPhrasePolarity, 
						phrase1_event.getPolarity().toString(), "1");
				
				MLExampleFeature.setFeatureExample(exampleToProcess, eventPolarityFeature);
			}else
			{
				TimexPhrase phrase1_timex = TimexPhrase.getRelatedTimexFromPhrase(phrase1);
				

				FeatureValuePair timexModFeature = FeatureValuePair.getInstance(
						FeatureName.LinkFromPhraseTimexMod, 
						phrase1_timex.getTimexMod().toString(), "1");
				
				MLExampleFeature.setFeatureExample(exampleToProcess, timexModFeature);
				
				
				FeatureValuePair timexTypeFeature = FeatureValuePair.getInstance(
						FeatureName.LinkFromPhraseType, 
						phrase1_timex.getTimexType().toString(), "1");
				
				MLExampleFeature.setFeatureExample(exampleToProcess, timexTypeFeature);
			}
			
			if(phrase2_type.equals("EVENT"))
			{
				ClinicalEvent phrase2_event =
						ClinicalEvent.getRelatedEventFromPhrase(phrase2);
				
				FeatureValuePair eventTypeFeature = FeatureValuePair.getInstance(
						FeatureName.LinkToPhraseType, 
						phrase2_event.getEventType().toString(), "1");
				
				MLExampleFeature.setFeatureExample(exampleToProcess, eventTypeFeature);
				
				FeatureValuePair eventModalityFeature = FeatureValuePair.getInstance(
						FeatureName.LinkToPhraseModality, 
						phrase2_event.getModality().toString(), "1");
				
				MLExampleFeature.setFeatureExample(exampleToProcess, eventModalityFeature);
				
				FeatureValuePair eventPolarityFeature = FeatureValuePair.getInstance(
						FeatureName.LinkToPhrasePolarity, 
						phrase2_event.getPolarity().toString(), "1");
				
				MLExampleFeature.setFeatureExample(exampleToProcess, eventPolarityFeature);
					
			}else
			{
				TimexPhrase phrase2_timex = TimexPhrase.getRelatedTimexFromPhrase(phrase2);
				
				FeatureValuePair timexModFeature = FeatureValuePair.getInstance(
						FeatureName.LinkToPhraseTimexMod, 
						phrase2_timex.getTimexMod().toString(), "1");
				
				MLExampleFeature.setFeatureExample(exampleToProcess, timexModFeature);
				
				
				FeatureValuePair timexTypeFeature = FeatureValuePair.getInstance(
						FeatureName.LinkToPhraseType, 
						phrase2_timex.getTimexType().toString(), "1");
				
				MLExampleFeature.setFeatureExample(exampleToProcess, timexTypeFeature);
			}
						
			
				
	}

	
}
