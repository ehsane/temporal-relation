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
import rainbownlp.machinelearning.MLExample;
import rainbownlp.util.HibernateUtil;

public class TimexEventRuleBasedPrediction implements IRuleBasedPrediction{

	public static HashMap<String,Integer> patternToClass = new HashMap<String, Integer>();
	public  HashMap<Artifact, HashMap<Integer, TimexPhrase>> sentPhraseDateMap =
		new HashMap<Artifact, HashMap<Integer,TimexPhrase>>();
	
	public  HashMap<Artifact, I2b2DocumentDetails> docDetailsMap =
		new HashMap<Artifact, I2b2DocumentDetails>();
	public HashMap<Integer, CustomDependencyGraph> cache = new HashMap<Integer, CustomDependencyGraph>();
	@Override
	public List<MLExample> predictByRules(List<MLExample> examples) throws Exception {
		
		List<MLExample> not_decided_examples = new ArrayList<MLExample>();
		
		for (MLExample example:examples)
		{
			int predicted = predictByRules(example);
			if (predicted == -1)
			{
				not_decided_examples.add(example);
			}else
			{
				example.setPredictedClass(predicted);
//				HibernateUtil.save(example, MLExample.hibernateSession);
		
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
		
		PhraseLink phrase_link = example.getRelatedPhraseLink();
		Phrase from_p = phrase_link.getFromPhrase();
		Phrase to_p = phrase_link.getToPhrase();
		Artifact relatedSentence = from_p.getStartArtifact().getParentArtifact();
		
		String content = relatedSentence.getContent().toLowerCase();
		
		String between = phrase_link.getBetweenContent().toLowerCase();
		int predicted=-1;
//		TODO: probably not needed just delete
//		if((between.matches("(of)|(ready for)") &&
//				phrase_link.getFirstPhrase().getPhraseId() == 
//				from_p.getPhraseId() ) ||
//				(between.matches("(in( approximately)?)|(on)|(at)|(during)|(for)") &&
//						phrase_link.getFirstPhrase().getPhraseId() != 
//						from_p.getPhraseId() ) ||
//						between.equals(""))
//		{
//			return LinkType.OVERLAP.ordinal();
//		}
////		
//		if(
////				(between.matches("(until)") &&
////				phrase_link.getFirstPhrase().getPhraseId() == 
////				from_p.getPhraseId() ) ||
//				(between.matches("(\\w+ )?until") &&
//						phrase_link.getFirstPhrase().getPhraseId() != 
//						from_p.getPhraseId() ) 
////						||
////						between.equals("")
//						)
//		{
//			return LinkType.AFTER.ordinal();
//		}
//		/////////////////**************pattens***********////////////////////////
//		int predicted = -1;
	
//		GeneralizedSentence generalized_sent =
//			GeneralizedSentence.findInstance(from_p,to_p, relatedSentence,GeneralizationModels.TTO);
//		
//		predicted  = PatternBasedPredictionTLink.predictByHighConfPatterns
//		(from_p,to_p,generalized_sent,LinkExampleBuilder.ExperimentGroupTimexEvent);
//				
//		if (predicted != -1)
//		{
//			return predicted;
//		}
////		/////////////////////////////////////////////////////////////////////
//		GeneralizedSentence gs = GeneralizedSentence.getInstance
//		(from_p, to_p, relatedSentence,
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
		//////////////////////////////////GRAPH//////////////////
		//rULE2

		CustomDependencyGraph cgraph = 
			cache.get(relatedSentence.getArtifactId());
		if(cgraph == null)
		{
			cgraph = new CustomDependencyGraph(relatedSentence, example.getForTrain());
			cache.put(relatedSentence.getArtifactId(), cgraph);
		}
		predicted = cgraph.reasonBasenOnPathBetween(from_p,to_p);
		if (predicted != -1)
		{
			return predicted;
		}
	
		
		return predicted;
	}
	public void getTimeLine(List<MLExample> examples) throws Exception
	{
		//get all the sentences involved in test run
		List<Artifact> involved_sents = new ArrayList<Artifact>();
		for(MLExample example:examples)
		{
			//get sent and if nor in list add it
			Artifact related_sent = example.getRelatedPhraseLink().getFromPhrase().getStartArtifact().getParentArtifact();
			if (!involved_sents.contains(related_sent))
				involved_sents.add(related_sent);
		}
		for (Artifact involved_sent:involved_sents)
		{
			HashMap<Integer, TimexPhrase> phrase_date_map = sentPhraseDateMap.get(involved_sent);
			if (phrase_date_map == null)
			{
//				phrase_date_map = RuleEngineUtils.setRelatedSentPhraseDates(example);
				phrase_date_map = RuleEngineUtils.setSentPhraseDatesByDependency(involved_sent);
				
				sentPhraseDateMap.put(involved_sent, phrase_date_map);
			}
			List<TimexPhrase> sent_time_line = RuleEngineUtils.getSentenceDateTimeline(involved_sent);
			HashMap<TimexPhrase, List<Integer>> date_phrase_map = new HashMap<TimexPhrase, List<Integer>>();
			for (TimexPhrase sorted_timex : sent_time_line)
			{
				List<Integer> overlapping_phrases = date_phrase_map.get(sorted_timex);
				if (overlapping_phrases ==null) overlapping_phrases =new ArrayList<Integer>();
				for (Integer phrase_id : phrase_date_map.keySet())
				{
					TimexPhrase phrase_timex = phrase_date_map.get(phrase_id);
					if (phrase_timex.equals(sorted_timex))
					{
						overlapping_phrases.add(phrase_id);
					}
				}
				date_phrase_map.put(sorted_timex, overlapping_phrases);
			}
			
			setPredictedForRelatedExamples(date_phrase_map);
		}
		

		
		
	}

	private List<MLExample> setPredictedForRelatedExamples(
			HashMap<TimexPhrase, List<Integer>> date_phrase_map) {
		List<MLExample> finalizedExamples = new ArrayList<MLExample>();
		
		for(TimexPhrase timex: date_phrase_map.keySet())
		{
			List<Integer> phrase_ids = date_phrase_map.get(timex);
			for (Integer phrase_id:phrase_ids )
			{
				
				//find the related exaple
				PhraseLink phrase_link =PhraseLink.findPhraseLink(timex.getRelatedPhrase(), Phrase.getInstance(phrase_id));
				MLExample relatedExample = MLExample.findInstance(phrase_link,LinkExampleBuilder.ExperimentGroupTimexEvent);
				relatedExample.setPredictedClass(LinkType.OVERLAP.ordinal());
				HibernateUtil.save(relatedExample);
				finalizedExamples.add(relatedExample);
				//set predicted to overlap
			}
		}
		return finalizedExamples;
	}


}
