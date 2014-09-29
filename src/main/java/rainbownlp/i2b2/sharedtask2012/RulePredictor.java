package rainbownlp.i2b2.sharedtask2012;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rainbownlp.analyzer.evaluation.classification.CrossValidation;
import rainbownlp.analyzer.evaluation.classification.Evaluator;
import rainbownlp.core.Artifact;
import rainbownlp.core.Phrase;
import rainbownlp.core.PhraseLink;
import rainbownlp.i2b2.sharedtask2012.featurecalculator.link.NormalizedDependencyFeatures;
import rainbownlp.i2b2.sharedtask2012.patternminer.BinaryLinkPattern;
import rainbownlp.machinelearning.LearnerEngine;
import rainbownlp.machinelearning.MLExample;
import rainbownlp.machinelearning.featurecalculator.link.LinkGeneralFeatures;
import rainbownlp.util.HibernateUtil;

public class RulePredictor extends LearnerEngine	{
	public static void main(String[] args) throws Exception
	{
		RulePredictor rp = new RulePredictor();
		CrossValidation cv = new CrossValidation(rp);
		
//		MLExample.resetExamplesPredicted(LinkExampleBuilder.ExperimentGroupEventEvent, true);
		MLExample.resetExamplesPredicted(BinaryLinkClassifier.experimentgroup, true);
		
		List<MLExample> trainExamples = 
			MLExample.getAllExamples(BinaryLinkClassifier.experimentgroup, true,1000);
		
		List<MLExample> withinSentExamples = new ArrayList<MLExample>();
		for(MLExample example: trainExamples)
		{
			PhraseLink phrase_link = example.getRelatedPhraseLink();
			if (LinkGeneralFeatures.	getInterMentionLocationType
					(phrase_link.getFromPhrase(), phrase_link.getToPhrase()).equals("withinSent"))
			{
				withinSentExamples.add(example);
			}
		}
		for(MLExample example : trainExamples)
		{
			example.setPredictedClass(-1);
			HibernateUtil.save(example);
		}
		
		rp.test(withinSentExamples);
		Evaluator.getEvaluationResult(withinSentExamples).printResult();
		
//		cv.crossValidation(trainExamples, 2).printResult();
	}
	static final List<String> beforeTriggers = Arrays.asList(new String[]{"before", "getting", "was", "evidential", "to have", "prior to"});
	static final List<String> afterTriggers = Arrays.asList(new String[]{"after", "to", "to the", "to dt"});
	static final List<String> overlapTriggers = Arrays.asList(new String[]{",", "cc", "and", "with", "in dt"});

	
	@Override
	public void train(List<MLExample> pTrainExamples) throws Exception {
		// TODO Auto-generated method stub
		
	}

	
	@Override
	//develop rules somewhere else like sentiment analyser and then apply them here
	public void test(List<MLExample> pTestExamples) throws Exception {
		
		
		
//		pTestExamples.get(counter).setPredictedClass(classNum);
//		HibernateUtil.save(pTestExamples.get(counter));
		for (MLExample test_example:pTestExamples)
		{
			//TODO: operation <TRIGGER> condition => condition before operation
			//TODO: condition .* comparative events (e.g. worse) => condition before the comparative event
		
			BinaryLinkPattern.patternBasedPrediction(test_example);
			
			//get related phraseLink
			PhraseLink phrase_link =  test_example.getRelatedPhraseLink();
			Phrase phrase1 = phrase_link.getFirstPhrase();
			Phrase phrase2 = phrase_link.getToPhrase();
			
			NormalizedDependencyFeatures ndf = new NormalizedDependencyFeatures();
//			
//			if(!phrase_link.linkedWithAnotherPhraseInBetween())
//			{
//				triggerBasedPrediction(test_example);
//			}
//			
//
			Phrase fromPhrase = phrase_link.getFromPhrase();
			Artifact sentence = fromPhrase.getStartArtifact().getParentArtifact();
//			
//			List<TimexPhrase> timexs = TimexPhrase.findTimexInSentence(sentence);
//			List<ClinicalEvent> events = ClinicalEvent.findEventsInSentence(sentence);
//				
//			if(!LinkExampleBuilder.possiblyHasLink(sentence, timexs, events)) 
//				test_example.setPredictedClass(0);
				
			//check if they are in the same sentence or not
			//TODO: find solutions if they are not in the same sentence
			if (LinkGeneralFeatures.getInterMentionLocationType(phrase1, phrase2).equals("betweenSent"))
			{
				test_example.setPredictedClass(0);
			}
			//now we know that they are in the same sentence
			else
			{
				boolean are_conj_and = ndf.areConjunctedBy(phrase_link, "and");
				if (are_conj_and)
				{
					test_example.setPredictedClass(1);
				}
				else
				{
					//identify have relation or not
//					Boolean are_directly_connected =
//						ndf.haveDirectRelation(phrase_link);
//					if (are_directly_connected == true)
//						test_example.setPredictedClass(1);
//					else 
//					if (ndf.haveCommonGoverners(phrase1,phrase2))
//						test_example.setPredictedClass(1);
//					else 
////					BAD rule
//					if (ndf.areGovernersDirectlyConnected(phrase_link) == true)
//						test_example.setPredictedClass(1);
//					
				}
			
			//save predicated
			HibernateUtil.save(test_example);
		}
	}
}

	private void triggerBasedPrediction(MLExample test_example) {
		PhraseLink phrase_link =  test_example.getRelatedPhraseLink();
		Phrase fromPhrase = phrase_link.getFromPhrase();
		Phrase firstPhrase = phrase_link.getFirstPhrase();
		Phrase secondPhrase = phrase_link.getSecondPhrase();
		
		Artifact curArtifact = firstPhrase.getEndArtifact().getNextArtifact();
		Artifact endArtifact = secondPhrase.getStartArtifact();
		int counter = 0;
		int expected = test_example.getExpectedClass().intValue();
		int predicted = -1;
		while(curArtifact!=null && !curArtifact.equals(endArtifact))
		{
			if(counter==2) break;
			counter++;
			String artContent= curArtifact.getContent().toLowerCase();
			if(beforeTriggers.contains(artContent))
			{
				predicted = 1;
				break;
			}
			else if(afterTriggers.contains(artContent))
			{
				predicted = 2;
				break;
			}
			else if(overlapTriggers.contains(artContent))
			{
				predicted = 3;
				break;
			}
						
			curArtifact = curArtifact.getNextArtifact();
		}
		
		//reverse if the link is RTL
		if(predicted!=-1)
		{
			if(fromPhrase.getPhraseId()!=firstPhrase.getPhraseId())
			{
				if(predicted==2)
					predicted=1;
				else if(predicted==1)
					predicted=2;
			}
			
			test_example.setPredictedClass(predicted);
		}
	}
	
	private void eliminativePrediction(MLExample test_example) {
		
		
//		int predicted=-1;
//		if (PatternStatisticsFeatures.
//				isLinkFeasibleByPattern(GeneralizationModels.TTPP, pbetweenChunkInclusive, pExcludedLinkType))
//		{
//			
//		}
//		//check normalizedSentence condition
//		else 
//		//reverse if the link is RTL
//		if(predicted!=-1)
//		{
//			
//			test_example.setPredictedClass(predicted);
//		}
	}
	
 
}
