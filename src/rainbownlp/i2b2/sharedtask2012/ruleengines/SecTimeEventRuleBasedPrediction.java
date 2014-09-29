package rainbownlp.i2b2.sharedtask2012.ruleengines;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import rainbownlp.core.Artifact;
import rainbownlp.core.Phrase;
import rainbownlp.core.PhraseLink;
import rainbownlp.i2b2.sharedtask2012.TimexPhrase;
import rainbownlp.i2b2.sharedtask2012.loader.I2b2DocumentDetails;
import rainbownlp.machinelearning.MLExample;
import rainbownlp.util.HibernateUtil;

public class SecTimeEventRuleBasedPrediction implements IRuleBasedPrediction{

	public  HashMap<Artifact, HashMap<Integer, TimexPhrase>> sentPhraseDateMap =
		new HashMap<Artifact, HashMap<Integer,TimexPhrase>>();
	
	public  HashMap<Integer, I2b2DocumentDetails> docDetailsMap =
		new HashMap<Integer, I2b2DocumentDetails>();
	@Override
	public List<MLExample> predictByRules(List<MLExample> examples) throws Exception {
		
		List<MLExample> not_decided_examples = new ArrayList<MLExample>();
		
		for (MLExample example:examples)
		{
			int predicted = predictByRules(example);
			if (predicted == -1)
			{
				not_decided_examples.add(example);
			}
			else
			{
				example.setPredictedClass(predicted);
				String savePredictedQuery = "update MLExample set predictedClass ="+example.getPredictedClass()+"" +
				" where exampleId="+example.getExampleId();
				HibernateUtil.executeNonReader(savePredictedQuery);
			}
		}
		return not_decided_examples;
	}

	@Override
	public int predictByRules(MLExample example) throws Exception {
		
		PhraseLink phrase_link = example.getRelatedPhraseLink();
		Phrase from = phrase_link.getFromPhrase();
		Phrase to = phrase_link.getToPhrase();
		Artifact relatedSentence = from.getStartArtifact().getParentArtifact();
		
		
		Artifact doc = relatedSentence.getParentArtifact();
		
		HashMap<Integer, TimexPhrase> phrase_date_map = sentPhraseDateMap.get(relatedSentence);
		I2b2DocumentDetails doc_details_map = docDetailsMap.get(doc);
		
		if (phrase_date_map == null)
		{
			phrase_date_map = RuleEngineUtils.setRelatedSentPhraseDatesByDependency(example);
			sentPhraseDateMap.put(relatedSentence, phrase_date_map);
		}
		if (doc_details_map == null)
		{
			doc_details_map = SecTimeEventUtils.setDocumentRelatedDetails(doc);
			docDetailsMap.put(doc.getArtifactId(), doc_details_map);
		}
		
		//Rule1 set doc admission and discharge overlap
		int predicted = SecTimeEventUtils.checkDocTargetEventTimes(example,doc_details_map);
		
		//this is rule that checks mentions of admission and discharge
//		if (predicted == -1)
//		{
//			predicted = SecTimeEventUtils.reasonBeaseOnAdmissionDischargeMentions(example,phrase_date_map,doc);
//			if (predicted != -1)
//			{
//				example.setPredictedClass(predicted);
//				HibernateUtil.save(example);
//			}
//		}
		//this is rule that checks the time of the phrase with related
		if (predicted == -1)
		{
			predicted = SecTimeEventUtils.reasonBeaseOnPhraseTime(example,phrase_date_map,doc);
		}
		return predicted;
	}


}
