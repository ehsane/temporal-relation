package rainbownlp.i2b2.sharedtask2012;

import java.util.ArrayList;
import java.util.List;

import rainbownlp.analyzer.evaluation.classification.Evaluator;
import rainbownlp.i2b2.sharedtask2012.featurecalculator.SecTime.SecTimeBasicFeatures;
import rainbownlp.i2b2.sharedtask2012.featurecalculator.SecTime.SecTimeParseDependencyFeatures;
import rainbownlp.i2b2.sharedtask2012.loader.i2b2OutputGenerator;
import rainbownlp.i2b2.sharedtask2012.ruleengines.SecTimeEventRuleBasedPrediction;
import rainbownlp.machinelearning.IFeatureCalculator;
import rainbownlp.machinelearning.MLExample;
import rainbownlp.machinelearning.SVMMultiClass;

public class SecTimeEventClassifier {
	public static void main(String[] args) throws Exception
	{
		List<MLExample> allExamples = 
		MLExample.getAllExamples(SecTimeEventExampleBuilder.ExperimentGroupSecTimeEvent, true);
//		calculateFeaturesSecTimeEvent(allExamples);
		
		//before classification apply some rules 
		List<MLExample> classifierInputExamples = new ArrayList<MLExample>();

		List<MLExample> trainExamples = 
			MLExample.getExamplesByDocument(SecTimeEventExampleBuilder.ExperimentGroupSecTimeEvent, true,
					2);
		
		//TODO: calculate all features 
//		calculateFeaturesSecTimeEvent(trainExamples);
		
		
		//get the engine
		SVMMultiClass svm = (SVMMultiClass) 
		SVMMultiClass.getLearnerEngine(SecTimeEventExampleBuilder.ExperimentGroupSecTimeEvent);
		
//		List<MLExample> testExamples = 
//			MLExample.getLastExamples(SecTimeEventExampleBuilder.ExperimentGroupSecTimeEvent, true,1000);
		List<MLExample> testExamples = 
			MLExample.getLastExamplesByDocument(SecTimeEventExampleBuilder.ExperimentGroupSecTimeEvent, true,
					100);
//		calculateFeaturesSecTimeEvent(testExamples);
		
//		 TODO: apply the rules here
//		for(MLExample example : testExamples)
//		{
//			LinkType predicted_class = SecTimeEventUtils.
//				reasonBeaseOnAdmissionDischargeMentions(example.getRelatedPhraseLink());
//			if (predicted_class.equals(LinkType.UNKNOWN))
//			{
//				classifierInputExamples.add(example);
//			}
//			else
//			{
//				example.setPredictedClass(predicted_class.ordinal());
//			}
//		}
		SecTimeEventRuleBasedPrediction sectime_rule_engine = new SecTimeEventRuleBasedPrediction();
		classifierInputExamples = sectime_rule_engine.predictByRules(testExamples);

		svm.train(trainExamples);
		svm.test(classifierInputExamples);
//		CrossValidation cv = new CrossValidation(svm);
//		cv.crossValidation(trainExamples, 2).printResult();
		//TODO: check if it is correct
		Evaluator.getEvaluationResult(testExamples).printResult();
		
		i2b2OutputGenerator.generateOutPutFiles(testExamples, "/home/azadeh/Documents/i2b22012/train/2012-07-06.release-fix", "/home/azadeh/Documents/i2b22012/output");

	}

	
	public static void calculateFeaturesSecTimeEvent(List<MLExample> examples ) throws Exception
	{
		List<IFeatureCalculator> SecTimeEventFeatureCalculators = 
			new ArrayList<IFeatureCalculator>();
	
		SecTimeEventFeatureCalculators.add(new SecTimeBasicFeatures());
		SecTimeEventFeatureCalculators.add(new SecTimeParseDependencyFeatures());
//		SecTimeEventFeatureCalculators.add(new NonNormalizedNGrams());
//		SecTimeEventFeatureCalculators.add(new ConceptsBetweenWords());
//		SecTimeEventFeatureCalculators.add(new NormalizedNGrams());
//		SecTimeEventFeatureCalculators.add(new ParseDependencyFeatures());
		
		
		for(MLExample example : examples)
		{
			example.calculateFeatures(SecTimeEventFeatureCalculators);
		}
	}
}
