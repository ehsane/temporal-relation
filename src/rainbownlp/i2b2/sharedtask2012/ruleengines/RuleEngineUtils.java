package rainbownlp.i2b2.sharedtask2012.ruleengines;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rainbownlp.core.Artifact;
import rainbownlp.core.Phrase;
import rainbownlp.core.PhraseLink;
import rainbownlp.core.PhraseLink.LinkType;
import rainbownlp.i2b2.sharedtask2012.TimexPhrase;
import rainbownlp.i2b2.sharedtask2012.analyzer.NormalizationMethod;
import rainbownlp.i2b2.sharedtask2012.analyzer.NormalizedSentence;
import rainbownlp.i2b2.sharedtask2012.patternminer.GeneralizedSentence;
import rainbownlp.i2b2.sharedtask2012.patternminer.GeneralizedSentence.GeneralizationModels;
import rainbownlp.machinelearning.MLExample;
import rainbownlp.i2b2.sharedtask2012.featurecalculator.link.NormalizedDependencyFeatures;
import rainbownlp.parser.DependencyLine;
import rainbownlp.util.StanfordDependencyUtil;

public class RuleEngineUtils {
	
	//there are two approaches in extracting the date
	//1. based on clause,
//		a. based on normalized heads
//	    b. based on original dependencies
	//2. based on pattern, based on the patterns in the TLinkPattern table
	//we start with just a simple clause based approach
	public static Date extractRelatedDate(Phrase pPhrase,HashMap<Phrase, TimexPhrase> phrase_date_map) throws Exception
	{
		Date date = null;
		Artifact relatedSentence = pPhrase.getStartArtifact().getParentArtifact();
		if (phrase_date_map !=null)
		{
			TimexPhrase related_date_timex = phrase_date_map.get(pPhrase);
			if (related_date_timex != null)
			{
				//get the value
				String timex_val = related_date_timex.getNormalizedValue();
				
				DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
				date = df.parse(timex_val);
			}
		}
	
	
		return date;
	}

	public static Date extractRelatedDate(TimexPhrase related_date_timex) throws Exception
	{
		Date date = null;
		String format ="yyyy-MM-dd";
		
		if (related_date_timex != null)
		{
			//get the value
			String timex_val = related_date_timex.getNormalizedValue();
//			if (timex_val.matches("\\d{4}-\\d\\d-\\d\\dT\\d\\d:\\d\\d")){
//				format = "yyyy-MM-dd";
////				timex_val =timex_val.replaceAll("T", "'T'");
//			}
			if (timex_val.matches("\\d{4}-\\d\\d")){
				timex_val = timex_val+"-01";
			}
			else if (timex_val.matches("\\d{4}")){
				timex_val = timex_val+"-01-01";
			}
			if (!(timex_val.matches("\\d{4}-\\d\\d-\\d\\d") || timex_val.matches("\\d{4}-\\d\\d-\\d\\dT\\d\\d:\\d\\d")))
			{
//				throw new Exception("unexpected normal timex val: "+timex_val);
				return null;
			}
	
			DateFormat df = new SimpleDateFormat(format);
			date = df.parse(timex_val);
		}
		return date;
	}
	// 
	public static LinkType compareTimexes(TimexPhrase fromTimex,TimexPhrase toTimex) throws Exception
	{
		LinkType result =null;
		Date date1 = extractRelatedDate(fromTimex);
		Date date2 = extractRelatedDate(toTimex);
		if(date1==null || date2 ==null)
		{
			return result;
		}
		if(date1.compareTo(date2)>0){
			result = LinkType.AFTER;
		}else if(date1.compareTo(date2)<0){
			result = LinkType.BEFORE;
		}else if(date1.compareTo(date2)==0){
			result = LinkType.OVERLAP;
		}else{
			System.out.println("How to get here?");
		}
		return result;
	}
	public static HashMap<Phrase, TimexPhrase> setRelatedSentPhraseDates(MLExample example)
		throws Exception
	{
		PhraseLink phrase_link = example.getRelatedPhraseLink();
		
		Phrase from = phrase_link.getFromPhrase();
		Phrase to = phrase_link.getToPhrase();
		Artifact relatedSentence = from.getStartArtifact().getParentArtifact();

		HashMap<Phrase, TimexPhrase> phrase_date_map = new HashMap<Phrase, TimexPhrase>();
		
//		TODO: generate automatically
		List<String> overlap_patterns = 
			Arrays.asList(new String[]{"\\[(event_\\d+)\\] in \\[(date_\\d+)\\]",
					"in \\[(date_\\d+)\\] , \\[(event_\\d+)\\]",
					"\\[(date_\\d+)\\] in \\[(event_\\d+)\\]",
					"in \\[(date_\\d+)\\] \\[(event_\\d+)\\]",
					"\\[(event_\\d+)\\] \\[(date_\\d+)\\]",
					"in \\[(date_\\d+)\\ in \\[(event_\\d+)\\]"});
		
		List<TimexPhrase> timexs = TimexPhrase.findDatesAndTimesInSentence(relatedSentence);
		
		if (timexs.size() !=0)
		{
			ArrayList<Phrase> phrase_list = (ArrayList<Phrase>)Phrase.getPhrasesInSentence(relatedSentence);
			
			String generalized_sent  = GeneralizedSentence.getRelatedGeneralizedSentence
			 (from, to, relatedSentence, GeneralizationModels.NNOID);
			for (String overlap_pattern:overlap_patterns)
			{
				if (phrase_list.isEmpty())
					break;
				Pattern p = Pattern.compile(overlap_pattern);
				Matcher m = p.matcher(generalized_sent);
				while (m.find())
				{
					//get related phrase and date
					String g1 = m.group(1);
					String g2 = m.group(2);
					Integer event_id= null;
					Integer timex_id = null;
					Pattern event_id_pattern = Pattern.compile("event_(\\d+)");
					Matcher m2 = event_id_pattern.matcher(g1);
					
					Pattern date_id_pattern = Pattern.compile("date_(\\d+)");
					Matcher m3 = date_id_pattern.matcher(g1);
					
//						first one is event
					if (m2.matches())
					{
						event_id  = Integer.parseInt(m2.group(1));
						Matcher g2_matcher = date_id_pattern.matcher(g2);
						if (g2_matcher.matches())
						{
							timex_id  = Integer.parseInt(g2_matcher.group(1));
						}

					}
					else if (m3.matches())
					{
						timex_id  = Integer.parseInt(m3.group(1));
						Matcher g2_matcher = event_id_pattern.matcher(g2);
						if (g2_matcher.matches())
						{
							event_id  = Integer.parseInt(g2_matcher.group(1));
						}
					}
					Phrase rel_phrase = Phrase.getInstance(event_id);
					TimexPhrase date = TimexPhrase.getInstance(timex_id);
					
					phrase_date_map.put(rel_phrase, date);
					phrase_list.remove(rel_phrase);
					
				}
					
			}
		}
		return phrase_date_map;
	}

	public static HashMap<Integer, TimexPhrase> setRelatedSentPhraseDatesByDependency(MLExample example)
									throws Exception
	{
		PhraseLink phrase_link = example.getRelatedPhraseLink();
		
		Phrase from = phrase_link.getFromPhrase();
		Phrase to = phrase_link.getToPhrase();
		Artifact relatedSentence = from.getStartArtifact().getParentArtifact();
	
		HashMap<Integer, TimexPhrase> phrase_date_map = setSentPhraseDatesByDependency(relatedSentence);
	
		return phrase_date_map;
	}

	public static HashMap<Integer, TimexPhrase> setSentPhraseDatesByDependency(Artifact relatedSentence)
									throws Exception
	{
		HashMap<Integer, TimexPhrase> phrase_date_map = new HashMap<Integer, TimexPhrase>();
	
		List<TimexPhrase> timexs = TimexPhrase.findDatesAndTimesInSentence(relatedSentence);
	
		if (timexs.size() !=0)
		{
			ArrayList<Phrase> phrase_list = (ArrayList<Phrase>)Phrase.getPhrasesInSentence(relatedSentence);
			
			NormalizedSentence normalized_sent_obj = NormalizedSentence.getInstance
				(relatedSentence, NormalizationMethod.MethodType.MentionToHead);
			for (Phrase p:phrase_list)
			{
				normalized_sent_obj.getPhraseNewOffsetMap().put(p.getPhraseId(),p.getNormalOffset());
			}
			for (TimexPhrase date: timexs)
			{
				if (phrase_list.size()==0)
					break;
				List<Phrase> related_phrases = getRelatedPhraseToDate(date,normalized_sent_obj);
				for(Phrase rel_phrase: related_phrases)
				{
					phrase_date_map.put(rel_phrase.getPhraseId(), date);
					phrase_list.remove(rel_phrase);
				}
				
			}

		}
		return phrase_date_map;
	}
	public static List<Phrase> getRelatedPhraseToDate(TimexPhrase date,NormalizedSentence nor_sent_obj) throws SQLException
	{
		List<Phrase> related_phrases = new ArrayList<Phrase>();
		// get the new offset of the timex and get the head
		String mention_head = date.getRelatedPhrase().getNormalizedHead();
 		if (mention_head == null)
		{
			mention_head = NormalizedSentence.getPhraseHead(nor_sent_obj.getRelatedSentence(), date.getRelatedPhrase(), "TIMEX3");
		}
		Integer nor_offset = date.getRelatedPhrase().getNormalOffset();
		// get govs in prep
		List<DependencyLine> time_rel_govs = StanfordDependencyUtil.getTimeRelatedGovs
			(mention_head, nor_offset+1, nor_sent_obj.getNormalizedDependencies());
		//get first parts and add to 
		for (DependencyLine gov:time_rel_govs)
		{
			Phrase rel_phrase = NormalizedDependencyFeatures.getRelatedPhraseFromNorDepLine
						(gov.firstPart,gov.firstOffset,nor_sent_obj.getPhraseNewOffsetMap());
			if (rel_phrase != null)
			{
				related_phrases.add(rel_phrase);
			}
			
			List<DependencyLine> deps_of_gov = StanfordDependencyUtil.getRelatedDependentsInPrep
			( gov.firstPart, gov.firstOffset,nor_sent_obj.getNormalizedDependencies());
			List<String> excluded_relations = new ArrayList<String>();
			excluded_relations.add("prep_for");
			excluded_relations.add("prep_because_of");
			for (DependencyLine dep_of_gov:deps_of_gov)
			{
				if(excluded_relations.contains(dep_of_gov.relationName)) continue;
				Phrase rel_p = NormalizedDependencyFeatures.getRelatedPhraseFromNorDepLine(dep_of_gov.secondPart,dep_of_gov.secondOffset,
							nor_sent_obj.getPhraseNewOffsetMap());
				if (rel_p != null && !related_phrases.contains(rel_p))
				{
					related_phrases.add(rel_p);
				}
			}
		}
		List<DependencyLine> time_rel_dependents = StanfordDependencyUtil.getTimeRelatedDependents
			(mention_head, nor_offset+1, nor_sent_obj.getNormalizedDependencies());
		//get first parts and add to 
		for (DependencyLine dep_prep:time_rel_dependents)
		{
			Phrase rel_phrase = NormalizedDependencyFeatures.getRelatedPhraseFromNorDepLine(dep_prep.secondPart,dep_prep.secondOffset,nor_sent_obj.getPhraseNewOffsetMap());
			if (rel_phrase != null)
			{
				related_phrases.add(rel_phrase);
			}
//			HashMap<Integer, String> related_words = StanfordDependencyUtil.getRelatedWords(dep_prep.secondPart, dep_prep.secondOffset, nor_sent_obj.getNormalizedDependencies());
//			for (Integer offset:related_words.keySet())
//			{
//				Phrase rel_p = getRelatedPhraseFromNorDepLine(related_words.get(offset),offset,nor_sent_obj.phraseNewOffsetMap);
//				if (rel_p != null && !related_phrases.contains(rel_p))
//				{
//					related_phrases.add(rel_p);
//				}
//			}
		}
		//get dependendts of that gove
		//convert them to corresponding phrase
		return related_phrases;
	}
	public static List<TimexPhrase> getSentenceDateTimeline(Artifact sentence) throws Exception
	{
		List<TimexPhrase> sent_sorted_timexes = new ArrayList<TimexPhrase>();
		List<TimexPhrase> timexs = TimexPhrase.findDatesAndTimesInSentence(sentence);
		if (timexs.size()==0)
		{
			return sent_sorted_timexes;
		}
		HashMap<Date,Integer> timex_date_map =  new HashMap<Date,Integer>();
		List<Date> sorted_dates = new ArrayList<Date>();
		for(TimexPhrase t: timexs)
		{
			Date rel_date = extractRelatedDate(t);
			if (rel_date==null)
			{
				throw new Exception("the timex with date type have wrong format"+t.getTimexId());
			}
			timex_date_map.put(rel_date,t.getTimexId());
			sorted_dates.add(rel_date);
		}
		
		Collections.sort(sorted_dates, new DateComparator());
		for (Date date:sorted_dates)
		{
			TimexPhrase rel_timex = TimexPhrase.getInstance(timex_date_map.get(date));
			sent_sorted_timexes.add(rel_timex);
		}
		return sent_sorted_timexes;
		
	}
	public static class DateComparator implements Comparator<Date>{
	    @Override
		public int compare(Date d1, Date d2) {
	        return d1.compareTo(d2);
	    }
	}
	public static List<Artifact> getWordArtifactsBetweenPhrases(Phrase p1, Phrase p2)
	{
		Artifact curArtifact =p1.getEndArtifact().getNextArtifact();
		
		List<Artifact> words_between= new ArrayList<Artifact>();
		Artifact toArtifact = p2.getStartArtifact();
		if(p1.getStartCharOffset()>p2.getStartCharOffset())
		{
			curArtifact = p2.getEndArtifact().getNextArtifact();
			
			toArtifact = p1.getStartArtifact();
		}
		while(curArtifact!=null && 
				!curArtifact.equals(toArtifact))
		{
			words_between.add(curArtifact);
			curArtifact = curArtifact.getNextArtifact();
		}
		return words_between;
	}
	
}
