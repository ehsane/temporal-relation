package rainbownlp.i2b2.sharedtask2012;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rainbownlp.analyzer.evaluation.classification.CrossValidation;
import rainbownlp.core.PhraseLink;
import rainbownlp.machinelearning.LearnerEngine;
import rainbownlp.machinelearning.MLExample;
import rainbownlp.machinelearning.MLExampleFeature;
import rainbownlp.machinelearning.SVMMultiClass;
import rainbownlp.util.HibernateUtil;

public class BinaryLinkClassifier  extends LearnerEngine  {
	public static final String experimentgroup = "BinaryLinkClassifier";
	
	
	public static void main(String[] args) throws Exception
	{
		BinaryLinkClassifier blc = new BinaryLinkClassifier();
		CrossValidation cv = new CrossValidation(blc);
		
		List<MLExample> trainExamples = 
			MLExample.getAllExamples(experimentgroup, true);
		
//		HibernateUtil.startTransaction();
		int counter = 0;
		List<MLExample> binary_examples = new ArrayList<MLExample>();
		 for(MLExample example : trainExamples)
		 {
			 MLExample binary_example = 
				 MLExample.getInstanceForLink(example.getRelatedPhraseLink(), experimentgroup);
			 int expected_class_binary = 0;
			 if(example.getExpectedClass()!=PhraseLink.LinkType.UNKNOWN.ordinal())
				 expected_class_binary = 1;
			 
			 binary_example.setExpectedClass(expected_class_binary);
			 HibernateUtil.save(binary_example);
			 
			 List<MLExampleFeature> features =
					 example.getExampleFeatures();
			 for(MLExampleFeature feature : features)
			 {
				 MLExampleFeature.setFeatureExample(binary_example, 
						 feature.getFeatureValuePair());
			 }
			 binary_examples.add(binary_example);
			 
			 counter++;
//			 if(counter%100==0)
//				 HibernateUtil.flushTransaction();
			 
			 HibernateUtil.clearLoaderSession();
		 }
		
//		HibernateUtil.endTransaction();
		
		trainExamples = null;
		System.gc();
		
//		List<MLExample> binary_examples = 
//			MLExample.getAllExamples(experimentgroup, true);
		
		cv.crossValidation(binary_examples, 2).printResult();
		
//		blc.train(binary_examples);
//		blc.test(binary_examples);
		
		
//		
//		Evaluator.getEvaluationResult(binary_examples).printResult();
	}
	SVMMultiClass svm = (SVMMultiClass) SVMMultiClass.getLearnerEngine(experimentgroup);
	
	 @Override
	public void train(List<MLExample> exampleForTrain) throws Exception
	 {
		 if(exampleForTrain.size()==0) return;
		 
		 if(exampleForTrain.get(0).getCorpusName().equals(experimentgroup))
		 {//input is binary example
			svm.train(exampleForTrain);
		 }else
		 {
		 	 List<MLExample> binary_examples = new ArrayList<MLExample>();
			 for(MLExample example : exampleForTrain)
			 {
				 MLExample binary_example = 
					 MLExample.getInstanceForLink(example.getRelatedPhraseLink(), experimentgroup);
				 int expected_class_binary = 0;
				 if(example.getExpectedClass()!=PhraseLink.LinkType.UNKNOWN.ordinal())
					 expected_class_binary = 1;
				 
				 binary_example.setExpectedClass(expected_class_binary);
				 HibernateUtil.save(binary_example);
				 
				 binary_examples.add(binary_example);
			 }
			 
			 svm.train(binary_examples);
		 }
	 }

	@Override
	public void test(List<MLExample> pTestExamples) throws Exception {
		if(pTestExamples.size()==0) return;
		 
		 if(pTestExamples.get(0).getCorpusName().equals(experimentgroup))
		 {//input is binary example
			svm.test(pTestExamples);
		 }else
		 {
			List<MLExample> binary_examples = new ArrayList<MLExample>();
			for(int i=0;i< pTestExamples.size();i++)
				{
				 MLExample original_example =
					 pTestExamples.get(i);
				 MLExample binary_example = 
					 MLExample.getInstanceForLink(original_example.getRelatedPhraseLink(), experimentgroup);
				 
				 binary_example.setPredictedClass(-1);
				 HibernateUtil.save(binary_example);
				 
				 binary_examples.add(binary_example);
			 }
			 
			 svm.test(binary_examples);
			 
			 for(int i=0;i< pTestExamples.size();i++)
			 {
				 MLExample binary_example = 
					binary_examples.get(i);
				 MLExample original_example =
					 pTestExamples.get(i);
				 
				 if(binary_example.getPredictedClass() == 0)
				 {
					 original_example.setPredictedClass(PhraseLink.LinkType.UNKNOWN.ordinal());
				     HibernateUtil.save(original_example);
				 }
			 }
		 }
	}
}
