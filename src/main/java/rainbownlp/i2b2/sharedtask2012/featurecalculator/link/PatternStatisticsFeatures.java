/**
 * 
 */
package rainbownlp.i2b2.sharedtask2012.featurecalculator.link;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;

import rainbownlp.core.FeatureValuePair;
import rainbownlp.core.FeatureValuePair.FeatureName;
import rainbownlp.core.Phrase;
import rainbownlp.core.PhraseLink;
import rainbownlp.core.PhraseLink.LinkType;
import rainbownlp.i2b2.sharedtask2012.LinkExampleBuilder;
import rainbownlp.i2b2.sharedtask2012.patternminer.GeneralizedSentence;
import rainbownlp.i2b2.sharedtask2012.patternminer.GeneralizedSentence.GeneralizationModels;
import rainbownlp.machinelearning.IFeatureCalculator;
import rainbownlp.machinelearning.MLExample;
import rainbownlp.machinelearning.MLExampleFeature;
import rainbownlp.machinelearning.featurecalculator.link.LinkGeneralFeatures;
import rainbownlp.util.FileUtil;
import rainbownlp.util.HibernateUtil;

/**
 * @author Azadeh
 * 
 */
public class PatternStatisticsFeatures implements IFeatureCalculator {
	public static HashMap<String,Double[]> cache = new HashMap<String, Double[]>();
	public static HashMap<String,Double[]> cache_TTO = new HashMap<String, Double[]>();
	public HashMap<String,Integer> patternToClass = new HashMap<String, Integer>();
	
	public static void main (String[] args) throws Exception
	{
		String experimentgroup = LinkExampleBuilder.ExperimentGroupEventEvent;
		List<MLExample> trainExamples = 
			MLExample.getAllExamples(experimentgroup, false);
		int counter = 0;
		for (MLExample example:trainExamples)
		{
			PatternStatisticsFeatures lbf = new PatternStatisticsFeatures();
			lbf.calculateFeatures(example);
			counter++;
			FileUtil.logLine(null, "Processed : "+counter +"/"+trainExamples.size());
		}
		
	}
	public void calculateFeatures(MLExample exampleToProcess) throws Exception {
		
		
		PhraseLink phraseLink = exampleToProcess.getRelatedPhraseLink();
		Phrase phrase1 = phraseLink.getFromPhrase();
		Phrase phrase2 = phraseLink.getToPhrase();
		//this feature has meaning for within sentence links
		if (LinkGeneralFeatures.getInterMentionLocationType(phrase1, phrase2).equals("betweenSent"))
			return;
		
		GeneralizedSentence gs = GeneralizedSentence.getInstance
			(phrase1, phrase2, phrase1.getStartArtifact().getParentArtifact(),
					GeneralizationModels.TTPP, phraseLink.getLinkType(),exampleToProcess.getForTrain());
		
		String between_pattern = gs.getBetweenMentionsChunk();
		
		FeatureValuePair between = FeatureValuePair.getInstance(
				FeatureName.betweenChunck, 
				between_pattern,"1");
		MLExampleFeature.setFeatureExample(exampleToProcess, between);
		
		
//		*************************
//		***************
//		***************	
		
//		//get the prob of different classes given the pattern
//		Double[] linkTypeProbabilities =cache.get(between_pattern);
//		if(linkTypeProbabilities==null)
//		{
//			linkTypeProbabilities = 
//					getConditionalProbs(GeneralizationModels.TTPP, between_pattern);
//			cache.put(between_pattern, linkTypeProbabilities);
//		}
//		Integer p = getLinkTypeWithMaxProb(GeneralizationModels.TTPP, between_pattern);
//
//		Integer max_prob_class =patternToClass.get(between_pattern);
//		if(max_prob_class==null)
//		{
//			max_prob_class = 
//				getLinkTypeWithMaxProb(GeneralizationModels.TTPP, between_pattern);
//			patternToClass.put(between_pattern, max_prob_class);
//		}
//		if (max_prob_class != -1)
//		{
//			FeatureValuePair max_prob_classFeature = FeatureValuePair.getInstance(
//					FeatureName.maxProbClassByPattern, max_prob_class.toString());
//			MLExampleFeature.setFeatureExample(exampleToProcess, max_prob_classFeature);
//		}
////		FeatureValuePair feasibleLinkFeature = FeatureValuePair.getInstance(
////				FeatureName.hasFeasibleLink, 
////				isLinkFeasibleByPattern(GeneralizationModels.TTPP,between_pattern,LinkType.UNKNOWN));
////		MLExampleFeature.setFeatureExample(exampleToProcess, feasibleLinkFeature);
//
//			
//
//		
//		FeatureValuePair pOverlapgivenpattern = FeatureValuePair.getInstance(
//				FeatureName.POverlapGivenPattern,
//				linkTypeProbabilities[LinkType.OVERLAP.ordinal()].toString());
//		MLExampleFeature.setFeatureExample(exampleToProcess, pOverlapgivenpattern);
//		
//		FeatureValuePair pbeforegivenpattern = FeatureValuePair.getInstance(
//				FeatureName.PBeforeGivenPattern, 
//				linkTypeProbabilities[LinkType.BEFORE.ordinal()].toString());
//		MLExampleFeature.setFeatureExample(exampleToProcess, pbeforegivenpattern);
//		
//		FeatureValuePair pAfterivenpattern = FeatureValuePair.getInstance(
//				FeatureName.PAfterGivenPattern, 
//				linkTypeProbabilities[LinkType.AFTER.ordinal()].toString());
//		MLExampleFeature.setFeatureExample(exampleToProcess, pAfterivenpattern);
//		
//		FeatureValuePair punknowngivenpattern = FeatureValuePair.getInstance(
//				FeatureName.PNoLinkGivenPattern,  
//				linkTypeProbabilities[LinkType.UNKNOWN.ordinal()].toString());
//		MLExampleFeature.setFeatureExample(exampleToProcess, punknowngivenpattern);
//		*************************
//		***************
//		***************
//		
//		
//			
//		GeneralizedSentence gs_TTO = GeneralizedSentence.getInstance
//		(phrase1, phrase2, phrase1.getStartArtifact().getParentArtifact(),
//				GeneralizationModels.TTO, phraseLink.getLinkType(),exampleToProcess.getForTrain());
//		
//		String between_pattern_TTO = gs_TTO.getBetweenMentionsChunk();
//		
//			
//		//get the prob of different classes given the pattern
//		Double[] linkTypeProbabilities_TTO =cache_TTO.get(between_pattern_TTO);
//		if(linkTypeProbabilities_TTO==null)
//		{
//			linkTypeProbabilities_TTO = 
//					getConditionalProbs(GeneralizationModels.TTO, between_pattern_TTO);
//			cache.put(between_pattern_TTO, linkTypeProbabilities_TTO);
//		}
//		
//		FeatureValuePair pOverlapgivenpattern_TTO = FeatureValuePair.getInstance(
//				FeatureName.POverlapGivenPatternTTO,
//				linkTypeProbabilities_TTO[LinkType.OVERLAP.ordinal()].toString());
//		MLExampleFeature.setFeatureExample(exampleToProcess, pOverlapgivenpattern_TTO);
//		
//		FeatureValuePair pbeforegivenpattern_TTO = FeatureValuePair.getInstance(
//				FeatureName.PBeforeGivenPatternTTO, 
//				linkTypeProbabilities_TTO[LinkType.BEFORE.ordinal()].toString());
//		MLExampleFeature.setFeatureExample(exampleToProcess, pbeforegivenpattern_TTO);
//		
//		FeatureValuePair pAfterivenpattern_TTO = FeatureValuePair.getInstance(
//				FeatureName.PAfterGivenPatternTTO, 
//				linkTypeProbabilities_TTO[LinkType.AFTER.ordinal()].toString());
//		MLExampleFeature.setFeatureExample(exampleToProcess, pAfterivenpattern_TTO);
//		
//		FeatureValuePair punknowngivenpattern_TTO = FeatureValuePair.getInstance(
//				FeatureName.PNoLinkGivenPatternTTO,  
//				linkTypeProbabilities_TTO[LinkType.UNKNOWN.ordinal()].toString());
//		MLExampleFeature.setFeatureExample(exampleToProcess, punknowngivenpattern_TTO);
//			
//		
								
	}

	public static String isLinkFeasibleByPattern(GeneralizationModels pModel,
			String pbetweenChunkInclusive, LinkType pExcludedLinkType )
	{
		String is_feasible = "0";
		String hql = "select count(*),betweenMentionsChunk  from TLinkPattern" +
				" where  generalizationModel=:model  and linkType<> :linkType and " +
				"betweenMentionsChunk=:pPattern ";

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("model", pModel);
		params.put("linkType", pExcludedLinkType);
		params.put("pPattern", pbetweenChunkInclusive);
		
		List<GeneralizedSentence> list = 
		(List<GeneralizedSentence>) HibernateUtil.executeReader(hql, params);
		
		if(list.size() > 0)
			is_feasible= "1";
		return is_feasible;
	}
	static Double[] getConditionalProbs(GeneralizationModels model,
						String between_mention_chunck)
	{
		String sql= "select count(*) from TLinkPattern where " +
				"linkType = :linkType and generalizationModel = :model and " +
				"betweenMentionsChunk= :pPattern and forTrain =1";
		Double[] linkTypeProbabilities = new Double[]{0.0,0.0,0.0,0.0};
		Integer[] linkTypeCounts = new Integer[]{0,0,0,0};
		int sum = 0;
		for(int i=0;i<4;i++)
		{
			HashMap<String, Object> params = new HashMap<String, Object>();
			params.put("model", model.name());
			params.put("pPattern", between_mention_chunck);
			params.put("linkType", i);
			linkTypeCounts[i] = 
					((BigInteger)HibernateUtil.executeGetOneValue(sql, params)).intValue();
			if(linkTypeCounts[i]!=null)
				sum+= linkTypeCounts[i];
		}
		for(int i=0;i<4;i++)
		{
			if(linkTypeCounts[i]!=null && sum>2.00 )
			{
				linkTypeProbabilities[i] = ((double)linkTypeCounts[i]/(double)sum);
			}
			else
			{
				linkTypeProbabilities[i] = 0.00;
			}
		}
		return linkTypeProbabilities;
	}
	public static Integer getLinkTypeWithMaxProb(GeneralizationModels model,
						String between_mention_chunck)
	{
		Integer predicted = -1;
		String sql= "select count(*) from TLinkPattern where " +
		"linkType = :linkType and generalizationModel = :model and " +
		"betweenMentionsChunk= :pPattern and forTrain =1";
		Double[] linkTypeProbabilities = new Double[]{0.0,0.0,0.0,0.0};
		Integer[] linkTypeCounts = new Integer[]{0,0,0,0};
		int sum = 0;
		for(int i=0;i<4;i++)
		{
			HashMap<String, Object> params = new HashMap<String, Object>();
			params.put("model", model.name());
			params.put("pPattern", between_mention_chunck);
			params.put("linkType", i);
			linkTypeCounts[i] = 
					((BigInteger)HibernateUtil.executeGetOneValue(sql, params)).intValue();
			if(linkTypeCounts[i]!=null)
				sum+= linkTypeCounts[i];
		}
		for(int i=0;i<4;i++)
		{
			if(linkTypeCounts[i]!=null && sum>2.00 )
			{
				linkTypeProbabilities[i] = ((double)linkTypeCounts[i]/(double)sum)*100;
	
			}
			else
			{
				linkTypeProbabilities[i] = 0.00;
			}
		}
		if(linkTypeProbabilities!=null )
		{
			for(int i=1;i<4;i++)
			{
				if (linkTypeProbabilities[i]>50.0)
				{
					predicted = i;
					return predicted;
				}
			}

		}
		return predicted;
	}
		
}

