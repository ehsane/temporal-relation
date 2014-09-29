/**
 * 
 */
package rainbownlp.i2b2.sharedtask2012.featurecalculator.link;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import rainbownlp.core.Artifact;
import rainbownlp.core.Artifact.Type;
import rainbownlp.core.FeatureValuePair;
import rainbownlp.core.FeatureValuePair.FeatureName;
import rainbownlp.core.Phrase;
import rainbownlp.core.PhraseLink;
import rainbownlp.i2b2.sharedtask2012.ClinicalEvent;
import rainbownlp.i2b2.sharedtask2012.CustomDependencyGraph;
import rainbownlp.i2b2.sharedtask2012.SecTimeEventExampleBuilder;
import rainbownlp.i2b2.sharedtask2012.TimexPhrase;
import rainbownlp.i2b2.sharedtask2012.analyzer.NormalizationMethod;
import rainbownlp.i2b2.sharedtask2012.analyzer.NormalizedSentence;
import rainbownlp.machinelearning.IFeatureCalculator;
import rainbownlp.machinelearning.MLExample;
import rainbownlp.machinelearning.MLExampleFeature;
import rainbownlp.parser.DependencyLine;
import rainbownlp.util.FileUtil;
import rainbownlp.util.HibernateUtil;
import rainbownlp.util.StanfordDependencyUtil;
/**
 * @author Azadeh
 * 
 */
public class NormalizedDependencyFeatures implements IFeatureCalculator {
//	private static StanfordParser s_parser = new StanfordParser();
	//this is just for test
	public static void main(String[] args) throws Exception
	{
		List<Artifact> docs = Artifact.listByType(Type.Document,false);
		int counter = 0;
		for(Artifact doc : docs)
		{
				MLExample.hibernateSession = HibernateUtil.clearSession(MLExample.hibernateSession);
				String doc_path = doc.getAssociatedFilePath();
//				List<MLExample> trainExamples = 
//					MLExample.getExamplesInDocument(LinkExampleBuilder.ExperimentGroupTimexEvent, doc_path);
//				List<MLExample> trainExamples2 = 
//					MLExample.getExamplesInDocument(LinkExampleBuilder.ExperimentGroupEventEvent, doc_path);

				List<MLExample> trainExamples3 = 
					MLExample.getExamplesInDocument(SecTimeEventExampleBuilder.ExperimentGroupSecTimeEvent,
							doc_path);
				
				List<MLExample> all_train_examples = new ArrayList<MLExample>();
//				all_train_examples.addAll(trainExamples);
//				all_train_examples.addAll(trainExamples2);
				all_train_examples.addAll(trainExamples3);
				
				
				for(MLExample example_to_process: all_train_examples)
				{
					NormalizedDependencyFeatures ndf =  new NormalizedDependencyFeatures();
					
					
					ndf.calculateFeatures(example_to_process);

					HibernateUtil.clearLoaderSession();
				}
				counter++;
				FileUtil.logLine(null, "Doc processed:"+counter);
		//		MLExample example_to_process = MLExample.getInstanceForLink
		//		(PhraseLink.getInstance(4117), experimentgroup);
		}
		
	}
	public ArrayList<DependencyLine> getNorDependencyList(PhraseLink phrase_link) throws Exception {
		if (norDependencyList!=null)
			return norDependencyList;
		else
		{
			norDependencyList =getNormalizedDep(phrase_link);
			return norDependencyList;
		}
			
	}
	private static ArrayList<DependencyLine> norDependencyList;
	public static HashMap<Integer, CustomDependencyGraph> cache = new HashMap<Integer, CustomDependencyGraph>();
//	public static StanfordParser s_parser;
	@Override
	public void calculateFeatures(MLExample exampleToProcess) throws Exception {
		
			
			//are directly connected?			
			PhraseLink phraseLink = exampleToProcess.getRelatedPhraseLink();
			
			Phrase phrase1 = phraseLink.getFromPhrase();
			Phrase phrase2 = phraseLink.getToPhrase();
			Artifact related_sent = phrase1.getStartArtifact().getParentArtifact();
			
			//relatedArgInPrep
			NormalizedSentence normalized_sent_obj = 
				NormalizedSentence.getInstance(related_sent, NormalizationMethod.MethodType.MentionToHead);
		
			String phrase1_head = phrase1.getNormalizedHead();
			String phrase2_head = phrase2.getNormalizedHead();
			ArrayList<DependencyLine> nor_dependencies =normalized_sent_obj.getNormalizedDependencies();
			norDependencyList = nor_dependencies;
			
			for (DependencyLine nor_dep: nor_dependencies)
			{
				String lex_dep = nor_dep.relationName+"-"+nor_dep.firstPart+"-"+nor_dep.secondPart;
				FeatureValuePair nor_dep_feature = FeatureValuePair.getInstance(FeatureName.normalizedDependencies,
						lex_dep,"1");
				MLExampleFeature.setFeatureExample(exampleToProcess, nor_dep_feature);	
			}
//			TODO: 8 march, remove comment
//			//Checks whether they are conjuncted
//			Boolean conjuncted_and = areConjunctedBy(phraseLink,"and");
//			FeatureValuePair are_conjucted_and_feature = FeatureValuePair.getInstance(FeatureName.AreConjunctedAnd,
//					conjuncted_and?"1":"0");
//			MLExampleFeature.setFeatureExample(exampleToProcess, are_conjucted_and_feature);	
//			
//			//Checks whether they are directly connected
//			Boolean directly_connected = haveDirectRelation(phraseLink,nor_dependencies);
//			
//			FeatureValuePair areDirectlyConnectedFeature = FeatureValuePair.getInstance(
//						FeatureName.AreDirectlyConnected, 
//						directly_connected?"1":"0");
//				
//			MLExampleFeature.setFeatureExample(exampleToProcess, areDirectlyConnectedFeature);	
//			//Checks common governors
//			Boolean have_common_govs = haveCommonGoverners(phrase1,phrase2,nor_dependencies);
//			
//			FeatureValuePair haveCommonGovs = FeatureValuePair.getInstance(
//						FeatureName.HaveCommonGovernors, 
//						have_common_govs?"1":"0");
//				
//			MLExampleFeature.setFeatureExample(exampleToProcess, haveCommonGovs);
//			
//			
//			//This feature gets the arguments that are in prep relation with phrases
//			List<String> from_args_in_pre =StanfordDependencyUtil.getAllArgsInPrep
//				(phrase1_head, phrase1.getNormalOffset()+1, nor_dependencies);
//			for(String prep_arg:from_args_in_pre )
//			{
//				FeatureValuePair from_prep_arg_features = FeatureValuePair.getInstance
//				(FeatureName.fromPrepArg, prep_arg,"1");
//			
//			    MLExampleFeature.setFeatureExample(exampleToProcess, from_prep_arg_features);
//			}
//			List<String> to_args_in_pre =StanfordDependencyUtil.getAllArgsInPrep
//			(phrase2_head, phrase2.getNormalOffset()+1, normalized_sent_obj.getNormalizedDependencies());
//			
//			for(String prep_arg:to_args_in_pre )
//			{
//				FeatureValuePair to_prep_arg_features = FeatureValuePair.getInstance
//				(FeatureName.toPrepArg, prep_arg,"1");
//			
//			    MLExampleFeature.setFeatureExample(exampleToProcess, to_prep_arg_features);
//			}
			/////////////////////
			////////////////////
			/////////////////////
			// Check if to phrase is the preposition arg of from phrase(timex in timex event) 
			
//			if (phrase1.getPhraseEntityType().equals("TIMEX3"))
//			{
//				TimexPhrase related_timex = TimexPhrase.getRelatedTimexFromPhrase(phrase1);
//				boolean is_direct_arg = isEventDirectPrepArgOfTimex(
//							related_timex,phrase2,normalized_sent_obj);
//				FeatureValuePair is_event_direct_arg = FeatureValuePair.getInstance
//				(FeatureName.isToPhDirectPrepArgOfFromPh, is_direct_arg?"1":"0");
//			
//			    MLExampleFeature.setFeatureExample(exampleToProcess, is_event_direct_arg);
//			}
			/////////////////////////////normalized ti type dep lines//////////////////////////////////////////
//			ArrayList<String> sent_nor_dep = getSentNorToTypeDepLines(related_sent);
//			for (String dep_string:sent_nor_dep)
//			{
//				FeatureValuePair nor_dep_feature = FeatureValuePair.getInstance
//				(FeatureName.norToTypeDep,dep_string,"1");
//			
//			    MLExampleFeature.setFeatureExample(exampleToProcess, nor_dep_feature);
//			}
	    	
	}
	public Boolean areConjunctedBy(PhraseLink phrase_link,String connector) throws Exception
	{
		Boolean are_connected = false;
		Phrase phrase1 = phrase_link.getFromPhrase();
		Phrase phrase2 = phrase_link.getToPhrase();
		
		ArrayList<DependencyLine> nor_dependencies = getNorDependencyList(phrase_link);
		
		are_connected = StanfordDependencyUtil.areConjuncted(nor_dependencies,phrase1,phrase2,connector);
		return are_connected;
	}

	public  Boolean haveCommonGoverners(Phrase phrase1, Phrase phrase2,
			ArrayList<DependencyLine> nor_dependencies) throws Exception {
		Boolean have_common_governors = false;
		
//		if (LinkGeneralFeatures.getInterMentionLocationType(phrase1, phrase2).equals("betweenSent"))
//		{
//			return false;
//		}
		//get related sentence normalized dependency

		String target1= phrase1.getNormalizedHead();
		String target2 = phrase2.getNormalizedHead();
		
		List<DependencyLine> target1_govs = StanfordDependencyUtil.
			getAllGovernors(nor_dependencies, target1);
		List<DependencyLine> target2_govs =  StanfordDependencyUtil.
			getAllGovernors(nor_dependencies, target2);
		
		have_common_governors=  StanfordDependencyUtil.haveGovernorInCommon(target1_govs,target2_govs);

		return have_common_governors;
	}
	public Boolean areGovernersDirectlyConnected(PhraseLink phrase_link,
			ArrayList<DependencyLine> nor_dependencies) throws Exception {
		Boolean are_govs_directly_connected = false;
		Phrase phrase1 = phrase_link.getFromPhrase();
		Phrase phrase2 = phrase_link.getToPhrase();
//		if (LinkGeneralFeatures.getInterMentionLocationType(phrase1, phrase2).equals("betweenSent"))
//		{
//			return false;
//		}
		
		String target1= phrase1.getNormalizedHead();
		String target2 = phrase2.getNormalizedHead();
		
		are_govs_directly_connected = StanfordDependencyUtil.areGovernorsDirectlyConnected(
				target1, target2, nor_dependencies);

		return are_govs_directly_connected;
	}
	public Boolean haveDirectRelation(PhraseLink phrase_link,
			ArrayList<DependencyLine> nor_dependencies) throws Exception {
		Boolean have_direct_relation = false;
		Phrase phrase1 = phrase_link.getFromPhrase();
		Phrase phrase2 = phrase_link.getToPhrase();
//		if (LinkGeneralFeatures.getInterMentionLocationType(phrase1, phrase2).equals("betweenSent"))
//		{
//			return false;
//		}
		
		have_direct_relation = 
			StanfordDependencyUtil.haveDirectRelation(phrase1,phrase2,nor_dependencies);
		
		return have_direct_relation;
	}
	public  static String getNormalizedMentionFromPhrase(Phrase pPhrase) throws Exception
	{
		String normalized_mention = "";
		
		if(pPhrase.getPhraseEntityType().equals("EVENT"))
		{
			ClinicalEvent event1 = ClinicalEvent.getRelatedEventFromPhrase(pPhrase);
			
			
			if (event1==null)
				throw new Exception("there is no event for this phrase! "+pPhrase.getPhraseId());
			
			normalized_mention= event1.getNormalizedHead();
			
			
		}
		else if(pPhrase.getPhraseEntityType().equals("TIMEX3"))
		{
			TimexPhrase timex1 = TimexPhrase.getRelatedTimexFromPhrase(pPhrase);
			
			if (timex1==null)
				throw new Exception("there is no timex for this phrase! "+
						pPhrase.getPhraseId());
			
			normalized_mention= timex1.getNormalizedHead();
			
		}
		if (normalized_mention==null)
			normalized_mention = pPhrase.getPhraseContent();
		return normalized_mention;
		
	}
	//TODO: solution when they are not in the same line
	public static ArrayList<DependencyLine> getNormalizedDep(PhraseLink phraseLink) throws Exception
	{
		
		Phrase phrase1 = phraseLink.getFromPhrase();
		Phrase phrase2 = phraseLink.getToPhrase();
		
		NormalizedSentence normalized = NormalizedSentence.getInstance(phrase1.getStartArtifact().getParentArtifact(),
				NormalizationMethod.MethodType.MentionToHead);
		String normalized_dependency_string = normalized.getNormalizedDependency();
		
		ArrayList<DependencyLine> nor_dependencies =StanfordDependencyUtil.parseDepLinesFromString
		(normalized_dependency_string);
		
		normalized.setNormalizedDependencies(nor_dependencies);
		return nor_dependencies;
	}
	//phrase offset map, has the new offsets of the phrase,
	//if the target offset is related to a phrase we returns that phrase
	public static Phrase getRelatedPhraseFromNorDepLine(String content, Integer offset,
			HashMap<Integer, Integer> phrase_offset_map)
	{
		Phrase related_phrase = null;
		if (phrase_offset_map.values().contains(offset-1))
		{
			for(Integer p_id:phrase_offset_map.keySet())
			{
				
				if (phrase_offset_map.get(p_id)==offset-1)
				{
					Phrase p =Phrase.getInstance(p_id);
					return p;
				}
			}
		}
		
		return related_phrase;
	}
	public static boolean isEventDirectPrepArgOfTimex
		(TimexPhrase from_timex,Phrase toPhrase, NormalizedSentence nor_sent) throws SQLException
	{	
		boolean is_direct_arg  = false;
		// get the new offset of the timex and get the head
		String mention_head = from_timex.getRelatedPhrase().getNormalizedHead();
		if (mention_head == null)
		{
			mention_head = NormalizedSentence.getPhraseHead(nor_sent.getRelatedSentence(), from_timex.getRelatedPhrase(), "TIMEX3");
		}
		Integer nor_offset = from_timex.getRelatedPhrase().getNormalOffset();
		// get govs in prep
		List<DependencyLine> govs_in_pre = StanfordDependencyUtil.getRelatedGovsInPrep
			(mention_head, nor_offset+1, nor_sent.getNormalizedDependencies());
		//get first parts and add to 
		for (DependencyLine gov_prep:govs_in_pre)
		{
			if ((gov_prep.firstPart.equals(toPhrase.getNormalizedHead())
					&& gov_prep.firstOffset == toPhrase.getNormalOffset()+1))
			{
				is_direct_arg = true;
				return is_direct_arg;
			}

		}
		List<DependencyLine> dependents_in_pre = StanfordDependencyUtil.getRelatedDependentsInPrep
		(mention_head, nor_offset+1, nor_sent.getNormalizedDependencies());
		//get first parts and add to 
		for (DependencyLine dep_prep:dependents_in_pre)
		{
			if ((dep_prep.secondPart.equals(toPhrase.getNormalizedHead())
					&& dep_prep.secondOffset == toPhrase.getNormalOffset()+1))
			{
				is_direct_arg = true;
				return is_direct_arg;
			}

		}
		return is_direct_arg;
		
	}
	public static HashMap<Integer, ArrayList<String>> sentNormalizedToTypeDepLines =
		new HashMap<Integer, ArrayList<String>>();
	//This will generate the dep line strings that are normalized to type and offsets are removed
	public static ArrayList<String> getSentNorToTypeDepLines(Artifact sent) throws SQLException
	{
		ArrayList<String> sent_deps = sentNormalizedToTypeDepLines.get(sent.getArtifactId());
		if (sent_deps==null)
		{
			sent_deps= new ArrayList<String>();
			NormalizedSentence normalized = NormalizedSentence.getInstance(sent,
					NormalizationMethod.MethodType.MentionToHead);
			String normalized_dependency_string = normalized.getNormalizedDependency();
			//get all Phrases in the sent
			List<Phrase> sent_phrases = Phrase.getPhrasesInSentence(sent);
			for(Phrase p:sent_phrases)
			{
				String phrase_type=p.getPhraseContent();
				if (p.getPhraseEntityType().equals("EVENT"))
				{
					ClinicalEvent event = ClinicalEvent.getRelatedEventFromPhrase(p);
					phrase_type = event.getEventType().name();
				}
				else if (p.getPhraseEntityType().equals("TIMEX3"))
				{
					TimexPhrase timex = TimexPhrase.getRelatedTimexFromPhrase(p);
					phrase_type = timex.getTimexType().name();
				}
				Integer nor_offset = p.getNormalOffset()+1;
				try {
					normalized_dependency_string = normalized_dependency_string.
					replaceAll(p.getNormalizedHead()+"-"+nor_offset.toString(), phrase_type+"-70");
				} catch (Exception e) {
					// TODO: handle exception
				}
				
			}
//				root(ROOT-0, resolved-5)
			ArrayList<DependencyLine> sent_dep_lines = StanfordDependencyUtil.parseDepLinesFromString(normalized_dependency_string);
			for (DependencyLine dep: sent_dep_lines)
			{
				String dep_line_string = dep.relationName+"_"+dep.firstPart.toLowerCase()+"_"+dep.secondPart.toLowerCase();
				sent_deps.add(dep_line_string);
			}
			sentNormalizedToTypeDepLines.put(sent.getArtifactId(), sent_deps);
		}
		return sent_deps;
	}
	
}
