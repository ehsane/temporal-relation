package rainbownlp.i2b2.sharedtask2012;

import rainbownlp.i2b2.sharedtask2012.patternminer.GeneralizedSentence;
import rainbownlp.i2b2.sharedtask2012.patternminer.GeneralizedSentence.GeneralizationModels;

public class MainExampleBuilder {

	
	public static void main(String[] args) throws Exception
	{	
		boolean is_training_mode = false;
		// 1. create between sent and in sent examples
//		LinkExampleBuilder.createBetweenSentenceLinkExamples(is_training_mode);
		LinkExampleBuilder.createInSentenceLinkExamples(is_training_mode);
//		
////		//2. create sectime examples
//		SecTimeEventExampleBuilder.buildSectimeExamples(is_training_mode);
//		SecTimeEventExampleBuilder.calculateSecTimeEventFeatures
//			(SecTimeEventExampleBuilder.ExperimentGroupSecTimeEvent,is_training_mode);
//		
//		GeneralizedSentence.calculateGeneralizedSentences(GeneralizationModels.TTO,is_training_mode);
//		GeneralizedSentence.calculateGeneralizedSentences(GeneralizationModels.TTPP,is_training_mode);
//		
////		//3. Calculate eventEvent and timeEvent
//////		TODO: convert to false for test data
//		
//		LinkExampleBuilder.calculateEventAndTimexEventFeatures(is_training_mode);
//		is_training_mode = false;
////		GeneralizedSentence.calculateGeneralizedSentences(GeneralizationModels.TTPP,is_training_mode);
//		LinkExampleBuilder.calculateEventAndTimexEventFeatures(is_training_mode);
//		
//		
//		
//		LinkExampleBuilder.calculateEventAndTimexEventRiskyFeatures(is_training_mode);
	}
}
