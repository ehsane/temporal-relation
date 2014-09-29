package rainbownlp.i2b2.sharedtask2012;

import java.util.HashMap;
import java.util.List;
import rainbownlp.core.Artifact;
import rainbownlp.core.Phrase;
import rainbownlp.core.PhraseLink;
import rainbownlp.i2b2.sharedtask2012.patternminer.GeneralizedSentence;
import rainbownlp.i2b2.sharedtask2012.patternminer.GeneralizedSentence.GeneralizationModels;
import rainbownlp.util.FileUtil;
import rainbownlp.util.HibernateUtil;

public class test {
	public static void main(String[] args) throws Exception
	{
//
////		MLExample.updateAssociatedFilePath();
//		//get all timex eventexamples
//		List<MLExample> all_timex_event = MLExample.getAllExamples(LinkExampleBuilder.ExperimentGroupTimexEvent, true);
//		List<MLExample> to_be_deleted = new ArrayList<MLExample>();
//		//get relared phraselink
//		for (MLExample example: all_timex_event)
//		{
//			PhraseLink  p_link= example.getRelatedPhraseLink();
//			if (Phrase.isSecTime(p_link.getFromPhrase()))
//			{
//				to_be_deleted.add(example);
//			}
//		}
//		
//		
//		String example_ids = "";
//		for(MLExample e : to_be_deleted)
//			example_ids = example_ids.concat(","+e.getExampleId()+" ");
//		example_ids = example_ids.replaceFirst(",", "");
//		
//		String hql = "FROM MLExample "  +
//				"where corpusName =:corpusName " +
//				" and forTrain=1 and exampleId in (" +
//				example_ids + ") " ;
//		
//		HashMap<String, Object> params = new HashMap<String, Object>();
//		params.put("corpusName", LinkExampleBuilder.ExperimentGroupTimexEvent);

		
//		HibernateUtil.executeNonReader(hql);
//		***************************8


		
//		GeneralizedSentence.getNNOFromTTO();
//		****************************
//		List<PhraseLink> links = PhraseLink.getPhraseLinkBetweenSent();
//		System.out.println(" size: "+links.size());
//		for (PhraseLink pl: links)
//		{
//			String doc = pl.getFromPhrase().getStartArtifact().getParentArtifact().getParentArtifact().getAssociatedFilePath();
//			String linkid = pl.getAltLinkID();
//			String from_id =  pl.getFromPhrase().getAltID();
//			String to_id =  pl.getToPhrase().getAltID();
//			
//			System.out.println(pl.getPhraseLinkId()+" linktype: "+pl.getLinkType().name());
//		}
//		List<PhraseLink> overlap_links = PhraseLink.getPhraseLinkBetweenSentByType(LinkType.OVERLAP);
//		List<PhraseLink> bef_links = PhraseLink.getPhraseLinkBetweenSentByType(LinkType.BEFORE);
//		List<PhraseLink> after_links = PhraseLink.getPhraseLinkBetweenSentByType(LinkType.AFTER);
//		System.out.println(" size: "+overlap_links.size()+"  before: "+
//				bef_links.size()+ "after: "+after_links.size());
//		********************************
//		List<MLExample> all_examples =MLExample.getExampleByExpectedClass(
//				LinkExampleBuilder.ExperimentGroupEventEvent, true,LinkType.AFTER);
//		HashMap<String, Integer> from_verb_count = new HashMap<String, Integer>();
//		HashMap<String, Integer> to_verb_count = new HashMap<String, Integer>();
//		for(MLExample example:all_examples)
//		{
//			List<MLExampleFeature> features = example.getExampleFeatures();
//			for(MLExampleFeature ef:features)
//			{
//				if (ef.getFeatureValuePair().getFeatureName().equals(FeatureName.FromPhraseGovVerb.name()))
//				{
//					Integer count = from_verb_count.get(ef.getFeatureValuePair().getFeatureValue());
//					if (count == null)
//					{
//						count = 1;		
//						
//					}
//					else
//					{
//						count++;	
//					}
//					from_verb_count.put(ef.getFeatureValuePair().getFeatureValue(), count);
//					FileUtil.logLine("/home/azadeh/projects/rainbownlp/data/log files/AfterVerbs.txt",
//							"example:	"+example.getExampleId()+"		from gov:		"+ef.getFeatureValuePair().getFeatureValue());
//				}
//				if (ef.getFeatureValuePair().getFeatureName().equals(FeatureName.ToPhraseGovVerb.name()))
//				{
//					Integer count = to_verb_count.get(ef.getFeatureValuePair().getFeatureValue());
//					if (count == null)
//					{
//						count = 1;		
//						
//					}
//					else
//					{
//						count++;	
//					}
//					to_verb_count.put(ef.getFeatureValuePair().getFeatureValue(), count);
//					FileUtil.logLine("/home/azadeh/projects/rainbownlp/data/log files/AfterVerbs.txt",
//							"example:   "+example.getExampleId()+"		tttto gov		"+ef.getFeatureValuePair().getFeatureValue());
//				}
//			}
//			
//		}
//		System.out.println();
//		for(String verb:from_verb_count.keySet())
//		{
//			FileUtil.logLine("/home/azadeh/projects/rainbownlp/data/log files/AfterFromPhraseVerbs.txt",
//					","+verb+","+from_verb_count.get(verb));
//		}
//		for(String verb:to_verb_count.keySet())
//		{
//			FileUtil.logLine("/home/azadeh/projects/rainbownlp/data/log files/AfterToPhraseVerbs.txt",
//					","+verb+","+to_verb_count.get(verb));
//		}
//		*******************************************
//		List<MLExample> all_examples =MLExample.getExampleByExpectedClass(
//				LinkExampleBuilder.ExperimentGroupEventEvent, true,LinkType.BEFORE);
//		HashMap<String, Integer> from_verb_count = new HashMap<String, Integer>();
//		HashMap<String, Integer> to_verb_count = new HashMap<String, Integer>();
//		for(MLExample example:all_examples)
//		{
//			List<MLExampleFeature> features = example.getExampleFeatures();
//			for(MLExampleFeature ef:features)
//			{
//				if (ef.getFeatureValuePair().getFeatureName().equals(FeatureName.FromPhraseGovVerb.name()))
//				{
//					Integer count = from_verb_count.get(ef.getFeatureValuePair().getFeatureValue());
//					if (count == null)
//					{
//						count = 1;		
//						
//					}
//					else
//					{
//						count++;	
//					}
//					from_verb_count.put(ef.getFeatureValuePair().getFeatureValue(), count);
//					FileUtil.logLine("/home/azadeh/projects/rainbownlp/data/log files/BeforeVerbs.txt",
//							"example:	"+example.getExampleId()+"		from gov:		"+ef.getFeatureValuePair().getFeatureValue());
//				}
//				if (ef.getFeatureValuePair().getFeatureName().equals(FeatureName.ToPhraseGovVerb.name()))
//				{
//					Integer count = to_verb_count.get(ef.getFeatureValuePair().getFeatureValue());
//					if (count == null)
//					{
//						count = 1;		
//						
//					}
//					else
//					{
//						count++;	
//					}
//					to_verb_count.put(ef.getFeatureValuePair().getFeatureValue(), count);
//					FileUtil.logLine("/home/azadeh/projects/rainbownlp/data/log files/beforeVerbs.txt",
//							"example:   "+example.getExampleId()+"		tttto gov		"+ef.getFeatureValuePair().getFeatureValue());
//				}
//			}
//			
//		}
//		System.out.println();
//		for(String verb:from_verb_count.keySet())
//		{
//			FileUtil.logLine("/home/azadeh/projects/rainbownlp/data/log files/beforeFromPhraseVerbs.txt",
//					","+verb+","+from_verb_count.get(verb));
//		}
//		for(String verb:to_verb_count.keySet())
//		{
//			FileUtil.logLine("/home/azadeh/projects/rainbownlp/data/log files/beforeToPhraseVerbs.txt",
//					","+verb+","+to_verb_count.get(verb));
//		}
//		
//		
//		
//		
//	}
//	******************************************************
//		GeneralizedSentence.updateGeneralizedSentenceLinkType(GeneralizationModels.TTO);
//		GeneralizedSentence.calculateGeneralizedSentences(GeneralizationModels.TTO);
		List<Artifact> sentences = 
			Artifact.listByType(Artifact.Type.Sentence);
		int counter=0;
		for(Artifact sentence : sentences)
		{
			
			List<PhraseLink> phrase_links = PhraseLink.findAllPhraseLinkInSentence(sentence);
			
			for (PhraseLink phrase_link:phrase_links)
			{
				Phrase from_phrase = phrase_link.getFromPhrase();
				
				Phrase to_phrase = phrase_link.getToPhrase();
				GeneralizedSentence gs = findInstance(from_phrase, to_phrase,
						sentence,GeneralizationModels.TTO);
				HibernateUtil.clearLoaderSession();
				
			}
			
			counter++;
			 FileUtil.logLine(null,"calculateGeneralizedSentences--------Sentence processed: "+counter);
				
		}
		
		
	}
	public static GeneralizedSentence findInstance(Phrase pFromPhrase, Phrase pToPhrase, 
			Artifact sentence,GeneralizationModels genModel) throws Exception{

		
		String hql = "from GeneralizedSentence where fromPhrase = :fromPhraseID "+
		" and toPhrase = :toPhraseId  and generalizationModel= :model ";

		
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("fromPhraseID", pFromPhrase.getPhraseId());
		params.put("toPhraseId", pToPhrase.getPhraseId());	
		params.put("model", genModel.name());
		
		
		List<GeneralizedSentence> generalized_sentences = 
				(List<GeneralizedSentence>) HibernateUtil.executeReader(hql,params);
	    
		GeneralizedSentence gen_sentence = null;
	    if(generalized_sentences.size()>1)
	    {
	    	gen_sentence = 
	    		generalized_sentences.get(0);
	    }
	    return gen_sentence;
	}
}
