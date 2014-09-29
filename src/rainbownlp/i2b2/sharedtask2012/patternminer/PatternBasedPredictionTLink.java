package rainbownlp.i2b2.sharedtask2012.patternminer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rainbownlp.core.Artifact;
import rainbownlp.core.Phrase;
import rainbownlp.core.PhraseLink;
import rainbownlp.core.PhraseLink.LinkType;
import rainbownlp.core.Setting;
import rainbownlp.i2b2.sharedtask2012.ClinicalEvent;
import rainbownlp.i2b2.sharedtask2012.ClinicalEvent.Modality;
import rainbownlp.i2b2.sharedtask2012.SharedConstants;
import rainbownlp.i2b2.sharedtask2012.patternminer.GeneralizedSentence.GeneralizationModels;
import rainbownlp.i2b2.sharedtask2012.ruleengines.RuleEngineUtils;
import rainbownlp.machinelearning.MLExample;
import rainbownlp.util.FileUtil;

public class PatternBasedPredictionTLink {
	
	private static String file_path = "/home/azadeh/projects/rainbowNLP/data/patterns/binaryLinkPatterns";
	private static String root_path = Setting.getValue("ProjectRoot");
	
	static ArrayList<String> overlap_freq_between_patterns_ee = new ArrayList<String>(
		    Arrays.asList("test evidential", "problem with problem", "problem or problem",
		    		"treatment , and treatment","problem , and problem",
		    		"treatment with treatment"));
	
	ArrayList<String> overlap_freq_between_patterns_te = new ArrayList<String>(
		    Arrays.asList("date of occurrence"));
	public static void predictByPattern(MLExample example ) throws Exception
	{
		int predicted = -1;
		
		PhraseLink phrase_link =  example.getRelatedPhraseLink();

		Phrase fromPhrase = phrase_link.getFromPhrase();
		Phrase toPhrase = phrase_link.getToPhrase();
		Artifact sentence = fromPhrase.getStartArtifact().getParentArtifact();
		
		GeneralizedSentence gs = 
			GeneralizedSentence.getInstance(fromPhrase, toPhrase, sentence, 
					GeneralizationModels.TTO, phrase_link.getLinkType(),example.getForTrain());
		String example_between_chunk = gs.getBetweenMentionsChunk();
		
		//check overlap, 
		
		List<String> lines =rainbownlp.util.FileUtil.loadLineByLine(file_path);
		for (String pattern: lines)
		{
			Pattern p = Pattern.compile(pattern);
			Matcher m = p.matcher(example_between_chunk);
			if (m.matches())
			{
				predicted=2;
				break;
			}
				
		}
		if (predicted==-1)
			predicted=1;
		example.setPredictedClass(predicted);
	}
	public static boolean areSeqOfSameTypeEvents(Phrase p1, Phrase p2 ) throws Exception
	{
		boolean is_signal = false;
		Artifact sent = p1.getStartArtifact().getParentArtifact();

		
		List<Artifact> words_between= RuleEngineUtils.getWordArtifactsBetweenPhrases(p1,p2);	
	
		if (p1.getPhraseEntityType().equals("EVENT") && 
				p2.getPhraseEntityType().equals("EVENT"))
		{
			ClinicalEvent phrase1_event  = ClinicalEvent.getRelatedEventFromPhrase(p1);
			ClinicalEvent phrase2_event = ClinicalEvent.getRelatedEventFromPhrase(p2);
			
			if ((phrase1_event.getModality().equals(Modality.FACTUAL)
					 && phrase2_event.getModality().equals(Modality.FACTUAL)) &&
					phrase1_event.getEventType().equals(phrase2_event.getEventType()) &&
					((words_between.size()==1 && words_between.get(0).getContent().equals(",")) ||
							words_between.size()==0))
			{
				is_signal = true;
			}
		}
		
		return is_signal;
	}
	//test evidential
	public static boolean isMatchingBetweenMentionPattern(Phrase p1, Phrase p2,String pattern, GeneralizedSentence gs) throws Exception
	{
		boolean is_matching = false;
		
		if (gs!= null)
		{
			String example_between_chunk = gs.getBetweenMentionsChunk();
		 	
			Pattern p = Pattern.compile(pattern);
			Matcher m = p.matcher(example_between_chunk);
			if (m.matches())
			{
				is_matching = true;
			}
		}
		
		return is_matching;
	}
	public static boolean doesOverlapPatternsMatchBetween(Phrase p1, Phrase p2) throws Exception
	{
		boolean overlap_matches= false;
		GeneralizedSentence gs = 
			GeneralizedSentence.findInstance(p1, p2, p1.getStartArtifact().getParentArtifact(), 
					GeneralizationModels.TTO);
		for (String pattern : overlap_freq_between_patterns_ee)
		{
			if (isMatchingBetweenMentionPattern(p1,p2,pattern,gs))
			{
				overlap_matches = true;
				break;
			}
		}
		return overlap_matches;
	}
	private static String ee_path = root_path+"/data/patterns/highConf/eventEvent/";
	private static String te_path = root_path+"/data/patterns/highConf/timexEvent/";
	
	static ArrayList<LinkType> likTypePatterns = new ArrayList<LinkType>(
		    Arrays.asList(LinkType.OVERLAP,
		    		LinkType.BEFORE,
		    		LinkType.AFTER));
	public static Integer predictByHighConfPatterns(
			Phrase p1,Phrase p2,GeneralizedSentence generalized_sent,String experimentgroup) throws Exception 
	{
		int predicted = -1;
		if(generalized_sent==null)
			return predicted;
		String between_mention = generalized_sent.getBetweenMentionsChunk();
		//String between_mention_excl = generalized_sent.getBetweenMentionsChunkExclusive();
	//	if (between_mention_excl.matches("during") || between_mention_excl.matches("on"))
		//{
			//predicted=LinkType.OVERLAP.ordinal();
		//	return predicted;
	//	}
		if (experimentgroup.equals(SharedConstants.EXPERIMENT_GROUP_EVENTEVENT))
		{
			file_path = ee_path;
		}
		else
		{
			file_path = te_path;
		}
		FileUtil.logLine(null,"predictByHighConfPatterns------------------------------- link_type");
		for (LinkType link_type: likTypePatterns)
		{
			
			//create path
			file_path = file_path+link_type.name()+".txt";
			List<String> lines =rainbownlp.util.FileUtil.loadLineByLineAndTrim(file_path);
			if (lines.contains(between_mention))
			{
				predicted = link_type.ordinal();
				return predicted;
			}
//			for (String pattern:lines)
//			{
//				
//				if (between_mention.equals(pattern.trim()))
//				{
//					predicted = link_type.ordinal();
//					return predicted;
//				}
//			}
				
		}		
		
		return predicted;
	}
}
