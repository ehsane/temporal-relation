package rainbownlp.i2b2.sharedtask2012.ruleengines;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import rainbownlp.core.Artifact;
import rainbownlp.core.Phrase;
import rainbownlp.core.PhraseLink;
import rainbownlp.core.PhraseLink.LinkType;
import rainbownlp.i2b2.sharedtask2012.CustomDependencyGraph;
import rainbownlp.i2b2.sharedtask2012.LinkExampleBuilder;
import rainbownlp.i2b2.sharedtask2012.TimexPhrase;
import rainbownlp.i2b2.sharedtask2012.loader.I2b2DocumentDetails;
import rainbownlp.i2b2.sharedtask2012.patternminer.GeneralizedSentence;
import rainbownlp.i2b2.sharedtask2012.patternminer.GeneralizedSentence.GeneralizationModels;
import rainbownlp.i2b2.sharedtask2012.patternminer.PatternBasedPredictionTLink;
import rainbownlp.machinelearning.MLExample;
import rainbownlp.util.HibernateUtil;

public class EventEventRuleBasedPrediction implements IRuleBasedPrediction{

	public HashMap<Integer, CustomDependencyGraph> cache = new HashMap<Integer, CustomDependencyGraph>();
	public static HashMap<String,Integer> patternToClass = new HashMap<String, Integer>();
	
	private  HashMap<Artifact, HashMap<Phrase, TimexPhrase>> sentPhraseDateMap =
		new HashMap<Artifact, HashMap<Phrase,TimexPhrase>>();
	
	public  HashMap<Artifact, I2b2DocumentDetails> docDetailsMap =
		new HashMap<Artifact, I2b2DocumentDetails>();
//	public EventEventRuleBasedPrediction(HashMap<Artifact, HashMap<Phrase, TimexPhrase>> phrase_date_map)
//	{
//		setSentPhraseDateMap(phrase_date_map);
//	}
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
//				HibernateUtil.save(example);
				String savePredictedQuery = "update MLExample set predictedClass ="+example.getPredictedClass()+"" +
				" where exampleId="+example.getExampleId();
				HibernateUtil.executeNonReader(savePredictedQuery);
			}
		}
		patternToClass.clear();
		return not_decided_examples;
	}

	@Override
	public int predictByRules(MLExample example) throws Exception {
		
		int predicted = -1;
		PhraseLink phrase_link = example.getRelatedPhraseLink();
		Phrase from = phrase_link.getFromPhrase();
		Phrase to = phrase_link.getToPhrase();
		Artifact relatedSentence = from.getStartArtifact().getParentArtifact();
		
	
		
		////////////////////// Related to extracting the time of the event and then sortingthat
		Artifact doc = relatedSentence.getParentArtifact();
		
		HashMap<Phrase, TimexPhrase> phrase_date_map = sentPhraseDateMap.get(relatedSentence);
		I2b2DocumentDetails doc_details_map = docDetailsMap.get(doc);
		
		if (phrase_date_map == null)
		{
			phrase_date_map = RuleEngineUtils.setRelatedSentPhraseDates(example);
			sentPhraseDateMap.put(relatedSentence, phrase_date_map);
		}
		if (doc_details_map == null)
		{
			doc_details_map = SecTimeEventUtils.setDocumentRelatedDetails(doc);
			docDetailsMap.put(doc, doc_details_map);
		}
		///////////////////Max prob patterns//////////////////////
		///////////////////////////////////////////////////
//		GeneralizedSentence gs = GeneralizedSentence.getInstance
//		(from, to, relatedSentence,
//				GeneralizationModels.TTPP, phrase_link.getLinkType(),example.getForTrain());
//	
//		String between_pattern = gs.getBetweenMentionsChunk();
//		Integer max_prob_class =patternToClass.get(between_pattern);
//		if(max_prob_class==null)
//		{
//			max_prob_class = 
//				PatternStatisticsFeatures.getLinkTypeWithMaxProb(GeneralizationModels.TTPP, between_pattern);
//			patternToClass.put(between_pattern, max_prob_class);
//		}
//		if (max_prob_class != -1)
//		{
//			predicted = max_prob_class;
//			return predicted;
//		}
////		//////////////////high precision patterns/////////////////////////////
//////		Pattern based prediction
//		ClinicalEvent event1 = ClinicalEvent.getRelatedEventFromPhrase(from);
//		ClinicalEvent event2 = ClinicalEvent.getRelatedEventFromPhrase(to);
//		boolean is_negative = false;
//		
//		if ((event1!= null && event1.getPolarity()==Polarity.NEG) ||
//				(event2!= null && event2.getPolarity()==Polarity.NEG))
//		{
//			is_negative = true;
//		}
//		if(is_negative==false)
//			predicted = predictByCommonPatterns(from,to);
//		if (predicted != -1)
//		{
//			return predicted;
//		}

//		////////////////////////////////////////////
//		//Rule1 
//		
		CustomDependencyGraph cgraph = 
			cache.get(relatedSentence.getArtifactId());
		if(cgraph == null)
		{
			cgraph = new CustomDependencyGraph(relatedSentence,example.getForTrain());
			cache.put(relatedSentence.getArtifactId(), cgraph);
		}
		predicted = cgraph.reasonBasenOnPathBetween(from,to);
		if (predicted != -1)
		{
			return predicted;
		}
	
		
		return predicted;
	}

	public void setSentPhraseDateMap(HashMap<Artifact, HashMap<Phrase, TimexPhrase>> sentPhraseDateMap) {
		this.sentPhraseDateMap = sentPhraseDateMap;
	}

	public HashMap<Artifact, HashMap<Phrase, TimexPhrase>> getSentPhraseDateMap() {
		return sentPhraseDateMap;
	}
	public static Integer predictByCommonPatterns(Phrase p1, Phrase p2 ) throws Exception
	{
		Integer link_type = -1;
		
		Artifact sent = p1.getStartArtifact().getParentArtifact();
		
		if (PatternBasedPredictionTLink.areSeqOfSameTypeEvents(p1, p2))
		{
			link_type = LinkType.OVERLAP.ordinal();
			return link_type;
		}
		
		GeneralizedSentence generalized_sent =
			GeneralizedSentence.findInstance(p1, p2, sent,GeneralizationModels.TTO);
		
	
		//checks with patterns with high conf
		link_type = PatternBasedPredictionTLink.predictByHighConfPatterns
			(p1,p2,generalized_sent,LinkExampleBuilder.ExperimentGroupEventEvent);
		if (link_type ==-1)
		{
			//Add a list of especial handling for other patterns
		}
		return link_type;
	}
	

	


}
