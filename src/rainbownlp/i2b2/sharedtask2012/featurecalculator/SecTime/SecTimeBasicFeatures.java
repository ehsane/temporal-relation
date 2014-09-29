/**
 * 
 */
package rainbownlp.i2b2.sharedtask2012.featurecalculator.SecTime;

import java.util.List;

import rainbownlp.core.FeatureValuePair;
import rainbownlp.core.Phrase;
import rainbownlp.core.PhraseLink;
import rainbownlp.core.FeatureValuePair.FeatureName;
import rainbownlp.i2b2.sharedtask2012.ClinicalEvent;
import rainbownlp.i2b2.sharedtask2012.SecTime;
import rainbownlp.i2b2.sharedtask2012.SecTimeEventExampleBuilder;
import rainbownlp.i2b2.sharedtask2012.TimexPhrase;
import rainbownlp.i2b2.sharedtask2012.ruleengines.SecTimeEventUtils;
import rainbownlp.machinelearning.IFeatureCalculator;
import rainbownlp.machinelearning.MLExample;
import rainbownlp.machinelearning.MLExampleFeature;

/**
 * @author Azadeh
 * 
 */
public class SecTimeBasicFeatures implements IFeatureCalculator {
	
	
	public static void main (String[] args) throws Exception
	{
//		String experimentgroup = "BinaryLinkClassifier";
		String experimentgroup = SecTimeEventExampleBuilder.ExperimentGroupSecTimeEvent;
		List<MLExample> trainExamples = 
			MLExample.getAllExamples(experimentgroup, true);
		for (MLExample example:trainExamples)
		{
			SecTimeBasicFeatures lbf = new SecTimeBasicFeatures();
			lbf.calculateFeatures(example);
		}
		
	}
	@Override
	public void calculateFeatures(MLExample exampleToProcess) throws Exception {
			
			PhraseLink phraseLink = exampleToProcess.getRelatedPhraseLink();
			Phrase phrase1 = phraseLink.getFromPhrase();
			Phrase phrase2 = phraseLink.getToPhrase();
			
			String phrase1_type = phrase1.getPhraseEntityType();
			String phrase2_type = phrase2.getPhraseEntityType();
			

			if(phrase1_type.equals("EVENT"))
			{
				ClinicalEvent phrase1_event = ClinicalEvent.getRelatedEventFromPhrase(phrase1);
				
				//findout if it is hospital or history section
				
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
				
				ClinicalEvent.EventType p1_event_type = phrase1_event.getEventType();
				// just incluse content if it has these condition
				if (p1_event_type.equals(ClinicalEvent.EventType.EVIDENTIAL) 
						|| p1_event_type.equals(ClinicalEvent.EventType.OCCURRENCE) )
				{
					// The content of the args
					FeatureValuePair fromPhraseContentFeature = FeatureValuePair.getInstance(
							FeatureName.FromPhraseContent, 
							phrase1.getPhraseContent(), "1");
					
					MLExampleFeature.setFeatureExample(exampleToProcess, fromPhraseContentFeature);
					
					// POS of the phrases
				
					FeatureValuePair fromPhrasePOSFeature = FeatureValuePair.getInstance(
							FeatureName.FromPhrasePOS, 
							phrase1.getPOS(), "1");
					MLExampleFeature.setFeatureExample(exampleToProcess, fromPhrasePOSFeature);
				}
				//I want to know if it is hospital or history section
				String related_sec = SecTimeEventUtils.getEventRelatedSection
					(phrase1.getStartArtifact().getParentArtifact().getParentArtifact(),phrase1 );
				
				FeatureValuePair eventSecFeature= FeatureValuePair.getInstance(
						FeatureName.relatedSectionInDoc, 
						related_sec, "1");
				
				MLExampleFeature.setFeatureExample(exampleToProcess, eventSecFeature);
				
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
						
			if(phrase2_type.equals("TIMEX3"))
			{
				//find related phrase in Phrase table
				String related_sectime_type = getSecTimeType(phrase2);
				if (related_sectime_type != null)
				{
				
					FeatureValuePair secTimeTypeFeature = FeatureValuePair.getInstance(
							FeatureName.AdmissionOrDischarge, 
							related_sectime_type, "1");
					
					MLExampleFeature.setFeatureExample(exampleToProcess, secTimeTypeFeature);
					
				}
			}
				
	}
	public static String getSecTimeType(Phrase pPhrase)
	{
		String sec_time_type = null;
		if(pPhrase.getPhraseEntityType().equals("TIMEX3"))
		{
			Phrase related_sectime_phrase = Phrase.findInstance(pPhrase.getStartArtifact(),
					pPhrase.getEndArtifact(), "SECTIME");
			if (related_sectime_phrase != null)
			{
				SecTime rel_sectime = SecTime.findInstance(related_sectime_phrase);
				
				if (rel_sectime != null )
				{
					sec_time_type =rel_sectime.getSecTimeType();
				}
			}
			else
			{
				String prev_sent = pPhrase.getStartArtifact().getParentArtifact().getPreviousArtifact().getContent().toLowerCase();
				if (prev_sent.startsWith("discharge"))
				{
					sec_time_type ="DISCHARGE";
				}
				else if (prev_sent.startsWith("admission"))
				{
					sec_time_type ="ADMISSION";
				}
					
			}
			
		}
		else if(pPhrase.getPhraseEntityType().equals("MADEUP"))
		{
			sec_time_type ="DISCHARGE";
		}
		
	
		return sec_time_type;
		
	}
	
}
