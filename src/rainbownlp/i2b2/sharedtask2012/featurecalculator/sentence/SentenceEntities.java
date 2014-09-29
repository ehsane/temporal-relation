package rainbownlp.i2b2.sharedtask2012.featurecalculator.sentence;

import java.util.List;

import rainbownlp.core.Artifact;
import rainbownlp.core.FeatureValuePair;
import rainbownlp.core.FeatureValuePair.FeatureName;
import rainbownlp.i2b2.sharedtask2012.ClinicalEvent;
import rainbownlp.i2b2.sharedtask2012.TimexPhrase;
import rainbownlp.machinelearning.IFeatureCalculator;
import rainbownlp.machinelearning.MLExample;
import rainbownlp.machinelearning.MLExampleFeature;

public class SentenceEntities implements IFeatureCalculator {

	@Override
	public void calculateFeatures(MLExample exampleToProcess) {
		Artifact sentence = exampleToProcess.getRelatedPhrase().getStartArtifact();
		List<TimexPhrase> timexs = TimexPhrase.findTimexInSentence(sentence);
		
		FeatureValuePair timexCountFeature = FeatureValuePair.getInstance(FeatureName.TimexCount, 
				((Integer)timexs.size()).toString());
		
		MLExampleFeature.setFeatureExample(exampleToProcess, timexCountFeature);
		
		
		List<ClinicalEvent> events = ClinicalEvent.findEventsInSentence(sentence);
		
		FeatureValuePair eventsCountFeature = FeatureValuePair.getInstance(FeatureName.ClinicalEventsCount, 
				((Integer)events.size()).toString());
		
		MLExampleFeature.setFeatureExample(exampleToProcess, eventsCountFeature);
		
		
	}

}
