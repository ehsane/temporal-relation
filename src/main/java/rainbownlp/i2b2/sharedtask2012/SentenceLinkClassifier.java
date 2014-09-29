package rainbownlp.i2b2.sharedtask2012;

import java.util.List;

import rainbownlp.analyzer.evaluation.classification.CrossValidation;
import rainbownlp.machinelearning.LearnerEngine;
import rainbownlp.machinelearning.MLExample;
import rainbownlp.machinelearning.SVMMultiClass;

public class SentenceLinkClassifier  extends LearnerEngine  {
	public static final String experimentgroup = "BinaryLinkClassifier";
	
	
	public static void main(String[] args) throws Exception
	{
		SentenceLinkClassifier blc = new SentenceLinkClassifier();
		CrossValidation cv = new CrossValidation(blc);
		
		List<MLExample> trainExamples = 
			MLExample.getAllExamples(SentenceExampleBuilder.ExperimentGroup, true, 10);
		
		cv.crossValidation(trainExamples, 2).printResult();
	}
	SVMMultiClass svm = (SVMMultiClass) SVMMultiClass.getLearnerEngine(experimentgroup);
	
	 @Override
	public void train(List<MLExample> exampleForTrain) throws Exception
	 {
		 if(exampleForTrain.size()==0) return;
		 
		svm.train(exampleForTrain);
	 }

	@Override
	public void test(List<MLExample> pTestExamples) throws Exception {
		if(pTestExamples.size()==0) return;
		 
		
		svm.test(pTestExamples);
		
	}
}
