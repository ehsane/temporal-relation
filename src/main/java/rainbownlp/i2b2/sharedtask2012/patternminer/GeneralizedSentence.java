package rainbownlp.i2b2.sharedtask2012.patternminer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hibernate.hql.internal.classic.ParserHelper;

import rainbownlp.core.Artifact;
import rainbownlp.core.Phrase;
import rainbownlp.core.PhraseLink;
import rainbownlp.core.PhraseLink.LinkType;
import rainbownlp.i2b2.sharedtask2012.ClinicalEvent;
import rainbownlp.i2b2.sharedtask2012.SharedConstants;
import rainbownlp.i2b2.sharedtask2012.TimexPhrase;
import rainbownlp.i2b2.sharedtask2012.analyzer.NormalizationMethod;
import rainbownlp.machinelearning.MLExample;
import rainbownlp.machinelearning.examplebuilder.link.InSentenceExampleBuilder;
import rainbownlp.i2b2.sharedtask2012.analyzer.NormalizedSentence;
import rainbownlp.parser.ParseHandler;
import rainbownlp.util.FileUtil;
import rainbownlp.util.HibernateUtil;

@Entity
@Table( name = "TLinkPattern" )
//get all links
public class GeneralizedSentence {
//	TODO: 	T:Type : Original N:Name(Entity |Timex) P:POS
		// calculate TTO and save 
		//calculate TTP
		//calculate TNO
		//calculate TNP
		//calculate NNP
		//calculate NPP
	public static void main(String[] args) throws Exception
	{	
		

	}
	public static void calculateGeneralizedSentences(GeneralizationModels g_model,
			boolean for_train) throws Exception
	{
		List<MLExample> examples = 
				MLExample.getAllExamples(SharedConstants.EXPERIMENT_GROUP_TIMEXEVENT, for_train);
		
		int counter = 0;
		
		for (MLExample example: examples)
		{
//			if (example.getRelatedPhraseLink().getFromPhrase().getStartArtifact().getParentArtifact().getArtifactId() != 42830)
//				continue;
			PhraseLink phrase_link = PhraseLink.getInstance(example.getRelatedPhraseLink().getPhraseLinkId());
			Phrase from_phrase = phrase_link.getFromPhrase();
			
			Phrase to_phrase = phrase_link.getToPhrase();
			
			Artifact sentence = from_phrase.getStartArtifact().getParentArtifact();
			
			GeneralizedSentence gs = getInstance(from_phrase, to_phrase, sentence, g_model,
					LinkType.getEnum(example.getExpectedClass().intValue()),
					for_train);
			String generalised = gs.getGeneralizedContent();
			HibernateUtil.clearLoaderSession();
			counter++;
			 FileUtil.logLine(null,"calculateGeneralizedSentences----ExperimentGroupTimexEvent----Sentence processed: "+counter);
		}
//		MLExample.hibernateSession = HibernateUtil.clearSession(MLExample.hibernateSession);
		MLExample.hibernateSession = HibernateUtil.sessionFactory.openSession();
		List<MLExample> examples_ee = 
				MLExample.getAllExamples(SharedConstants.EXPERIMENT_GROUP_EVENTEVENT, for_train);
		counter = 0;
		for (MLExample example: examples_ee)
		{
//			if (example.getRelatedPhraseLink().getFromPhrase().getStartArtifact().getParentArtifact().getArtifactId() != 42830)
//				continue;
			PhraseLink phrase_link = PhraseLink.getInstance(example.getRelatedPhraseLink().getPhraseLinkId());
			Phrase from_phrase = phrase_link.getFromPhrase();
			
			Phrase to_phrase = phrase_link.getToPhrase();
			Artifact sentence = from_phrase.getStartArtifact().getParentArtifact();
			
			GeneralizedSentence gs = getInstance(from_phrase, to_phrase, sentence, g_model,
					LinkType.getEnum(example.getExpectedClass().intValue()),for_train);
			String generalised = gs.getGeneralizedContent();
			HibernateUtil.clearLoaderSession();
			counter++;
			 FileUtil.logLine(null,"calculateGeneralizedSentences----ExperimentGroupEventEvent----Sentence processed: "+counter);
		}
//		List<Artifact> sentences = 
//			Artifact.listByType(Artifact.Type.Sentence);
//		int counter = 0;
//		for(Artifact sentence : sentences)
//		{
//			
//			List<PhraseLink> phrase_links = PhraseLink.findAllPhraseLinkInSentence(sentence);
//			
//			for (PhraseLink phrase_link:phrase_links)
//			{
//				Phrase from_phrase = phrase_link.getFromPhrase();
//				
//				Phrase to_phrase = phrase_link.getToPhrase();
//				GeneralizedSentence gs = getInstance(from_phrase, to_phrase, sentence, g_model,phrase_link.getLinkType());
//				String generalised = gs.getGeneralizedContent();
//				HibernateUtil.clearLoaderSession();
//			}
//			counter++;
//			 FileUtil.logLine(null,"calculateGeneralizedSentences--------Sentence processed: "+counter);
//				
//		}
	}

	//TODO change name to the old method
	private static HashMap<Integer, String> generalizeLinkArgsToType(Artifact pSentence, Phrase from_phrase,
			Phrase to_phrase, GeneralizationModels model) {
		String generalized = "";
		
		String from_type;
		String to_type;
		if (model.equals(GeneralizationModels.NNOID))
		{
			from_type= "["+extractMentionType(from_phrase,GeneralizationModels.NNOID)+"]";
			to_type="["+extractMentionType(to_phrase,GeneralizationModels.NNOID)+"]";
		}
		else if (model.equals(GeneralizationModels.TTOID))
		{
			from_type= "["+extractMentionType(from_phrase,GeneralizationModels.TTOID)+"]";
			to_type="["+extractMentionType(to_phrase,GeneralizationModels.TTOID)+"]";
		}
		else if (model.equals(GeneralizationModels.ModTP))
		{
			from_type= "["+extractMentionType(from_phrase,GeneralizationModels.ModTP)+"]";
			to_type="["+extractMentionType(to_phrase,GeneralizationModels.ModTP)+"]";
		}
		else
		{
			from_type= "["+extractMentionType(from_phrase,null)+"_1]";
			to_type="["+extractMentionType(to_phrase,null)+"_2]";
		}
	
		//get all the artifacts of the sentence
		List<Artifact> words_in_sent = pSentence.getChildsArtifact();
		HashMap<Integer, String> offset_content_map = new HashMap<Integer, String>();
		for (Artifact word:words_in_sent)
		{
			offset_content_map.put(word.getWordIndex(), word.getContent());
		}
		// get replace the content for the start artifact 
		
		offset_content_map = replacePhraseWithType(offset_content_map,from_phrase,from_type);
		
		offset_content_map = replacePhraseWithType(offset_content_map,to_phrase,to_type);
	
		return offset_content_map;
		
	}
	//generalize nonMention to Type
	public static String extractMentionType(Phrase phrase, GeneralizationModels model)
	{
		String mention_type = "";
		if (model == null)
		{
			model = GeneralizationModels.TTO;
		}
		if (model.equals(GeneralizationModels.NNOID))
		{
			if (phrase.getPhraseEntityType().equals("EVENT"))
			{
				ClinicalEvent from_event = ClinicalEvent.getRelatedEventFromPhrase(phrase);
				mention_type = "EVENT_"+from_event.getRelatedPhrase().getPhraseId();
			}
			else if (phrase.getPhraseEntityType().equals("TIMEX3"))
			{
				TimexPhrase from_timex = TimexPhrase.getRelatedTimexFromPhrase(phrase);
				mention_type = from_timex.getTimexType().name()+"_"+from_timex.getTimexId();
			}
		}
		else if (model.equals(GeneralizationModels.TTOID))
		{
			if (phrase.getPhraseEntityType().equals("EVENT"))
			{
				ClinicalEvent from_event = ClinicalEvent.getRelatedEventFromPhrase(phrase);
				mention_type = from_event.getEventType().name()+"_"+from_event.getRelatedPhrase().getPhraseId();
			}
			else if (phrase.getPhraseEntityType().equals("TIMEX3"))
			{
				TimexPhrase from_timex = TimexPhrase.getRelatedTimexFromPhrase(phrase);
				mention_type = from_timex.getTimexType().name()+"_"+from_timex.getRelatedPhrase().getPhraseId();
			}
		}
		else if (model.equals(GeneralizationModels.ModTP))
		{
			if (phrase.getPhraseEntityType().equals("EVENT"))
			{
				ClinicalEvent from_event = ClinicalEvent.getRelatedEventFromPhrase(phrase);
				mention_type = from_event.getEventType().name()+"_"+from_event.getModality();
			}
			else if (phrase.getPhraseEntityType().equals("TIMEX3"))
			{
				TimexPhrase from_timex = TimexPhrase.getRelatedTimexFromPhrase(phrase);
				mention_type = from_timex.getTimexType().name()+"_"+from_timex.getTimexMod();
			}
		}
		else
		{
			if (phrase.getPhraseEntityType().equals("EVENT"))
			{
				ClinicalEvent from_event = ClinicalEvent.getRelatedEventFromPhrase(phrase);
				mention_type = from_event.getEventType().name();
			}
			else if (phrase.getPhraseEntityType().equals("TIMEX3"))
			{
				TimexPhrase from_timex = TimexPhrase.getRelatedTimexFromPhrase(phrase);
				mention_type = from_timex.getTimexType().name();
			}
		}
		
		return mention_type;
	}
	// get the artifacts of the sentence in hash map
	// get all mentions
	//for each of them do the same
	private static HashMap<Integer, String> generalizeOutOfLinkMentionsToType(Phrase from_phrase,
			Phrase to_phrase, Artifact pSentence, HashMap<Integer, String> offset_content_map,
			GeneralizationModels model) {
		
		//get all phrase
		List<Phrase> sent_phrases  =Phrase.getPhrasesInSentence(pSentence);
		
		for (Phrase phrase: sent_phrases)
		{
			if (phrase.getPhraseId()== from_phrase.getPhraseId()
					|| phrase.getPhraseId()== to_phrase.getPhraseId() )
				continue;
			
			String mention_type;
			mention_type= "["+extractMentionType(phrase,model)+"]";

			offset_content_map = replacePhraseWithType(offset_content_map,phrase,mention_type);
		}
		 
		return offset_content_map;
	}
	public static String convertWordMapsToText(HashMap<Integer, String> offset_content_map)
	{
		String generalized = "";
		int count =0;
		int[] sorted_offsets= new int[offset_content_map.keySet().size()];
		for (Integer key:offset_content_map.keySet())
		{
			sorted_offsets[count] = key;
			count++;
		}
		Arrays.sort(sorted_offsets);
		for(Integer word_offset:sorted_offsets)
		{
			generalized += offset_content_map.get(word_offset)+" ";
		}
		generalized = generalized.replace("\\s$", "");
		generalized = generalized.trim();
		return generalized.toString();
	}
	private static HashMap<Integer, String> replacePhraseWithType(HashMap<Integer, String> offset_content_map,
			Phrase phrase, String mention_type)
	{
		Artifact start_art = phrase.getStartArtifact();
		Artifact end_art = phrase.getEndArtifact();
		offset_content_map.put(start_art.getWordIndex(),mention_type);
		
		//remove all other words of the phrase
		if(!start_art.equals(end_art))
		{
			Artifact curr_art  = start_art.getNextArtifact();
			while(!curr_art.equals(end_art))
			{
				offset_content_map.remove(curr_art.getWordIndex());
				curr_art = curr_art.getNextArtifact();
			}
			offset_content_map.remove(curr_art.getWordIndex());
		}
		return offset_content_map;
	}
	

	public GeneralizedSentence()
	{
		
	}
	
	
	public static GeneralizedSentence getInstance(Phrase pFromPhrase, Phrase pToPhrase, 
			Artifact sentence,GeneralizationModels genModel, PhraseLink.LinkType pLinkType,
			boolean for_train) throws Exception{

		
		String hql = "from GeneralizedSentence where fromPhrase = :fromPhraseID "+
		" and toPhrase = :toPhraseId  and generalizationModel= :model and forTrain= :for_train";

		
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("fromPhraseID", pFromPhrase.getPhraseId());
		params.put("toPhraseId", pToPhrase.getPhraseId());	
		params.put("model", genModel.name());
		params.put("for_train", for_train?1:0);
		
		
		List<GeneralizedSentence> generalized_sentences = 
				(List<GeneralizedSentence>) HibernateUtil.executeReader(hql,params);
	    
		GeneralizedSentence gen_sentence;
	    if(generalized_sentences.size()==0)
	    {
	    	gen_sentence = new GeneralizedSentence();
	    	
	    	gen_sentence.setFromPhrase(pFromPhrase);
	    	gen_sentence.setToPhrase(pToPhrase);
	    	gen_sentence.setSentenceContent(sentence.getContent());
	    	gen_sentence.setGeneralizationModel(genModel.name());
	    	gen_sentence.setRelatedSentence(sentence);
	    	gen_sentence.setForTrain(for_train);
	    	if (pLinkType!=null)
	    		gen_sentence.setLinkType(pLinkType);
	    	String gen_sent = gen_sentence.BuildGeneralizedSentence();
	    	gen_sentence.setGeneralizedContent(gen_sent);
	    	HibernateUtil.save(gen_sentence);
	    }else
	    {
	    	gen_sentence = 
	    		generalized_sentences.get(0);
	    }
	    return gen_sentence;
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
	    if(generalized_sentences.size()!=0)
	    {
	    	gen_sentence = 
	    		generalized_sentences.get(0);
	    }
	    return gen_sentence;
	}



	public void setFromPhrase(Phrase fromPhrase) {
		this.fromPhrase = fromPhrase;
	}

	@OneToOne( cascade = {CascadeType.PERSIST, CascadeType.MERGE} )
    @JoinColumn(name="fromPhrase")
	public Phrase getFromPhrase() {
		return fromPhrase;
	}

	public void setToPhrase(Phrase toPhrase) {
		this.toPhrase = toPhrase;
	}

	@OneToOne( cascade = {CascadeType.PERSIST, CascadeType.MERGE} )
    @JoinColumn(name="toPhrase")
	public Phrase getToPhrase() {
		return toPhrase;
	}

	public void setGenSentId(int pGenSentId) {
		this.genSentId = pGenSentId;
	}
	@Id
	@GeneratedValue(generator="increment")
	@GenericGenerator(name="increment", strategy = "increment")
	public int getGenSentId() {
		return genSentId;
	}

	public void setSentenceContent(String sentenceContent) {
		this.sentenceContent = sentenceContent;
	}
	
	@Column(length = 1000)
	@Index(name = "index_content")
	public String getSentenceContent() {
		return sentenceContent;
	}



	public static ArrayList<GeneralizedSentence> getListByType(
			GeneralizationModels pTargetMentionNonMention) {
		String hql = "from GeneralizedSentence where generalizationModel = "+pTargetMentionNonMention.ordinal();
		
		List<GeneralizedSentence> generalized_objects = 
				(List<GeneralizedSentence>) HibernateUtil.executeReader(hql);
		return (ArrayList<GeneralizedSentence>) generalized_objects;
	}

	public void setGeneralizationModel(String genralizationModel) {
		this.generalizationModel = genralizationModel;
	}

	@Column( length = 10000 )
	public String getGeneralizationModel() {
		return generalizationModel;
	}
	//finds the related generalized content to a Phrase link
	public static GeneralizedSentence findGeneralizedSent(Phrase pFromPhrase, Phrase pToPhrase,String genModel){
		String hql = "from GeneralizedSentence where fromPhrase = "+pFromPhrase.getPhraseId()+
		" and toPhrase = "+pToPhrase.getPhraseId()+" and genralizationModel='"+genModel+"'";
				
		List<GeneralizedSentence> link_objects = 
				(List<GeneralizedSentence>) HibernateUtil.executeReader(hql);
	    
		GeneralizedSentence link_obj = link_objects.get(0);
	    
	    return link_obj;
	}
	// the input is the generalized content that have e.g.: [problem_1]
	private String getBetweenPhraseChunk(String pGeneralized, String pModel,BufferedWriter output) throws Exception
	{
		String sent_chunk = pGeneralized;
		if (pModel.equals("TTPP") || pModel.equals("TTO"))
		{
			Pattern p1 = Pattern.compile(".*(\\[\\w+_[1|2]\\].*\\[\\w+_[1|2]\\]).*");
			Matcher m1 = p1.matcher(sent_chunk);
			
			if (m1.matches())
			{
				sent_chunk = m1.group(1);
			}
			else
			{
				output.write("The generalized content won't match tegex  "+pGeneralized+"\n");
//				throw new Exception("The generalized content won't match tegex TTO"+pGeneralized);
			}
		}
		
		return sent_chunk;
		
	}
	private String getBetweenPhraseChunkExclusive(String pGeneralized, String pModel, BufferedWriter output) throws Exception
	{
		String sent_chunk = "";
		if (pModel.equals("TTPP") || pModel.equals("TTO"))
		{
//			DT NN was DT JJ NN NN NN , WP was [OCCURRENCE_1] IN DT [DURATION] NN IN [PROBLEM_2] CC DT [DURATION] NN IN [PROBLEM] .
//			pGeneralized="DT NN was DT JJ NN NN NN , WP was [OCCURRENCE_1] IN DT [DURATION] NN IN [PROBLEM] CC DT [DURATION] NN IN [PROBLEM_2] .";
			Pattern p1 = Pattern.compile(".*\\[\\w+_[1|2]\\](.*)\\[\\w+_[1|2]\\].*");
			Matcher m1 = p1.matcher(pGeneralized);
			
			if (m1.matches())
			{
				sent_chunk = m1.group(1);
			}
			else
			{
				output.write("The generalized content won't match regex "+pGeneralized+"\n");
//				throw new Exception("The generalized content won't match tegex TTO"+pGeneralized);
			}
				
		}
		
		return sent_chunk;
		
	}
	private String trimGeneralizedSent(String pGenSent,String pModel)
	{
		String cleaned_gen_sent = pGenSent;
		cleaned_gen_sent =cleaned_gen_sent.toLowerCase().replaceAll("\\[(\\w+)_(1|2)\\]", "$1");
		cleaned_gen_sent = cleaned_gen_sent.trim();
		cleaned_gen_sent =cleaned_gen_sent.toLowerCase().replaceAll
		("\\[(occurence|evidential|test|problem|treatment|clinical_dept|date|time|duration|frequency)\\]", "$1");
		
		Pattern p = Pattern.compile("\\b(\\w+)\\b[\\s+|,]+\\b\\1\\b", Pattern.MULTILINE+Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(cleaned_gen_sent);
		
        while (m.find())
        {
        	cleaned_gen_sent = cleaned_gen_sent.replaceAll(m.group(), m.group(1));
        }
	    
		return cleaned_gen_sent;
	}
	
	public String getChunkBetweenPhrases(String pGeneralizedContent,String pModel,BufferedWriter file) throws Exception
	{
		String sent_chunk = pGeneralizedContent;
	
		sent_chunk = getBetweenPhraseChunk(pGeneralizedContent, pModel,file);
		sent_chunk = trimGeneralizedSent(sent_chunk,pModel);
		
		return sent_chunk;
	}

	public static List<GeneralizedSentence> listAllGeneralizedSentences(PhraseLink.LinkType pExcludedLinkType,GeneralizationModels pModel) {
		String hql = "from GeneralizedSentence where genralizationModel=:model and" +
				" linkType <>:pLinkType";
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("model", pModel.name());
		params.put("pLinkType", pExcludedLinkType.ordinal());	
		
		List<GeneralizedSentence> generalized_objects = 
				(List<GeneralizedSentence>) HibernateUtil.executeReader(hql,params);
		return generalized_objects;
	}
	public static List<GeneralizedSentence> listAllGeneralizedSentencesByType(PhraseLink.LinkType pLinkType,GeneralizationModels pModel) {
		String hql = "from GeneralizedSentence where generalizationModel=:model and" +
				" linkType =:pLinkType ";
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("model", pModel.name());
		params.put("pLinkType", pLinkType.ordinal());	
		
		List<GeneralizedSentence> generalized_objects = 
				(List<GeneralizedSentence>) HibernateUtil.executeReader(hql,params);
		return generalized_objects;
	}

	public void setBetweenMentionsChunk(String betweenMentionsChunk) {
		this.betweenMentionsChunk = betweenMentionsChunk;
	}

	@Column( length = 10000 )
	public String getBetweenMentionsChunk() {
		return betweenMentionsChunk;
	}
	
//	public static String replaceNonMentionsWithPOS(Artifact sent) throws SQLException
//	{
//		String pos_tagged_sent = "";
//		List<Artifact>  annotated_artifacts =Phrase.getAnnotatedWordsInSentence(sent);
//		for(Artifact word:sent.getChildsArtifact())
//		{
//			if (annotated_artifacts.contains(word))
//			{
//				pos_tagged_sent += word.getContent()+" ";
//			}
//			else if(word.getPOS().startsWith("VB") )//if it is not verb  put POS
//			{
//				pos_tagged_sent += word.getContent()+" ";
//			}
//			else
//			{
//				pos_tagged_sent += word.getPOS()+" ";
//			}
//		}
//		//get the artifacts that are mention
//		pos_tagged_sent = pos_tagged_sent.replaceAll("\\s$", "");
//		return pos_tagged_sent;
//		
//	}
	public static HashMap<Integer, String> replaceNonMentionsWithPOS(Artifact sent, HashMap<Integer, String> word_map) throws SQLException
	{
		String pos_tagged_sent = "";
		List<Artifact>  annotated_artifacts =Phrase.getAnnotatedWordsInSentence(sent);
		List<Integer> annotated_offsets =  new ArrayList<Integer>();
		for (Artifact a: annotated_artifacts)
		{
			annotated_offsets.add(a.getWordIndex());
		}

		for(Artifact word:sent.getChildsArtifact())
		{
			if (!annotated_offsets.contains(word.getWordIndex()))
			{
				if(!word.getPOS().startsWith("VB") )//if it is not verb  put POS
				{
					
					word_map.put(word.getWordIndex(), word.getPOS());
				}
			
			}
		
		}
		return word_map;
		
	}

	public void setLinkType(PhraseLink.LinkType linkType) {
		this.linkType = linkType;
	}

	public PhraseLink.LinkType getLinkType() {
		return linkType;
	}
	
	private  String BuildGeneralizedSentence() throws Exception
	{		
		File file = new File("/tmp/GeneralizationErrors.txt");
		BufferedWriter output = new BufferedWriter(new FileWriter(file));
		
		String generalized_sentence = null;
		
		generalized_sentence = getRelatedGeneralizedSentence
			(this.fromPhrase,this.toPhrase,this.relatedSentence,GeneralizationModels.valueOf(this.generalizationModel));
		
		this.setGeneralizedContent(generalized_sentence);
		
//			create exclusive chunk and trim and save
		String exclusive_chunk = getBetweenPhraseChunkExclusive(generalized_sentence,this.generalizationModel,output);
		exclusive_chunk = trimGeneralizedSent(exclusive_chunk, this.generalizationModel);
		this.setBetweenMentionsChunkExclusive(exclusive_chunk);
		
		//chunk including the target mentions
		String inclusive_chunk = getBetweenPhraseChunk(generalized_sentence,this.generalizationModel,output);
		inclusive_chunk = trimGeneralizedSent(inclusive_chunk, this.generalizationModel);
		this.setBetweenMentionsChunk(inclusive_chunk);			
		
		HibernateUtil.save(this);
		output.flush();
		output.close();
		
		return generalized_sentence;
	}
	public static String getRelatedGeneralizedSentence(Phrase from_phrase,
			Phrase to_phrase, Artifact related_sentence, GeneralizationModels model) throws SQLException
	{
		String generalized_sentence = null;
		
		HashMap<Integer, String> word_offset_map = new HashMap<Integer, String>();
		
		
		if(model.equals(GeneralizationModels.TTO))
		{
			word_offset_map =
				generalizeLinkArgsToType(related_sentence,from_phrase,to_phrase,GeneralizationModels.TTO);
			word_offset_map = generalizeOutOfLinkMentionsToType(from_phrase,to_phrase, related_sentence,
					word_offset_map,GeneralizationModels.TTO);
			
		}
		// here the the non mentions are normalized to POS
		else if (model.equals(GeneralizationModels.TTPP))
		{
			word_offset_map =
				generalizeLinkArgsToType(related_sentence,from_phrase,to_phrase,GeneralizationModels.TTPP);
			word_offset_map = replaceNonMentionsWithPOS(related_sentence, word_offset_map);
			generalizeOutOfLinkMentionsToType(from_phrase,to_phrase, related_sentence,
					word_offset_map,GeneralizationModels.TTPP);
		}
		else if (model.equals(GeneralizationModels.NNOID))
		{
			word_offset_map =
				generalizeLinkArgsToType(related_sentence,from_phrase,to_phrase,GeneralizationModels.NNOID);
	
			word_offset_map = generalizeOutOfLinkMentionsToType(from_phrase,to_phrase, related_sentence,
					word_offset_map,GeneralizationModels.NNOID);
		}
		//target: type-mention
		else if(model.equals(GeneralizationModels.ModTP))
		{
			word_offset_map =
				generalizeLinkArgsToType(related_sentence,from_phrase,to_phrase,GeneralizationModels.ModTP);
			word_offset_map = replaceNonMentionsWithPOS(related_sentence, word_offset_map);
			word_offset_map = generalizeOutOfLinkMentionsToType(from_phrase,to_phrase, related_sentence,
					word_offset_map,GeneralizationModels.TTO);
			
		}
		
		generalized_sentence = convertWordMapsToText(word_offset_map);

		
		return generalized_sentence.toLowerCase();
	}
	public static String convertToNNO(String gen_sent_TTO)
	{
		String NNO = gen_sent_TTO;
		Pattern event_pattern = Pattern.compile(
			"\\[(occurrence|evidential|test|problem|treatment|clinical_dept)_?[1|2]?\\]");
		Pattern timex_pattern = Pattern.compile(
		"\\[(date)_?[1|2]?\\]");
//		Pattern event_pattern = Pattern.compile(
//		"\\[(test|problem)_?[1|2]?\\]");
		
		NNO = gen_sent_TTO.toLowerCase().trim();
		Matcher m = event_pattern.matcher(NNO);
//		Matcher date_matcher = timex_pattern.matcher(NNP);
		
		while(m.find())
		{
			String temp = m.group(1);
			NNO = NNO.replaceAll(temp, "event");
		}
//		while(date_matcher.find())
//		{
//			String temp = m.group(1);
//			NNP = NNP.replaceAll(temp, "timex");
//		}
		
		return NNO;
	}

	//this will conc\vert all TTPP patterns to NNP and write in a file
	public static void getNNOFromTTO() throws Exception
	{
		//list all TTPP
		List<GeneralizedSentence> ttpps =listAllGeneralizedSentencesByType(LinkType.OVERLAP,GeneralizationModels.TTO);
		List<String> NNPs = new ArrayList<String>();
		//convert
		for(GeneralizedSentence gen_sent : ttpps)
		{
			String sent = gen_sent.getGeneralizedContent();
			NNPs.add(convertToNNO(sent));
		}
		//add to file
		FileUtil.createFile( "/tmp/generalized-NNO.txt",NNPs);
	}
//	public static void updateGeneralizedSentenceLinkType(GeneralizationModels g_model) throws Exception
//	{
//		List<Artifact> sentences = 
//			Artifact.listByType(Artifact.Type.Sentence);
//		int counter = 0;
//		for(Artifact sentence : sentences)
//		{
//			
//			List<PhraseLink> phrase_links = PhraseLink.findAllPhraseLinkInSentence(sentence);
//			
//			for (PhraseLink phrase_link:phrase_links)
//			{
//				Phrase from_phrase = phrase_link.getFromPhrase();
//				
//				Phrase to_phrase = phrase_link.getToPhrase();
//				GeneralizedSentence gs = GeneralizedSentence.findInstance(from_phrase, to_phrase,
//						sentence,g_model);
//				if (gs==null)
//				{
//					gs =GeneralizedSentence.getInstance(from_phrase, to_phrase, sentence, g_model, phrase_link.getLinkType());
//				}
//				else if(!gs.getLinkType().equals(phrase_link.getLinkType()))
//				{
//					gs.setLinkType(phrase_link.getLinkType());
//					HibernateUtil.save(gs);
//				}
//				
//				
//			}
//			HibernateUtil.clearLoaderSession();
//			counter++;
//			 FileUtil.logLine(null,"calculateGeneralizedSentences--------Sentence processed: "+counter);
//				
//		}
//	}
	public void setRelatedSentence(Artifact relatedSentence) {
		this.relatedSentence = relatedSentence;
	}
	@OneToOne( cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch=FetchType.LAZY  )
    @JoinColumn(name="relatedSentence")
	public Artifact getRelatedSentence() {
		if (relatedSentence!=  null)
			return relatedSentence;
		else
			return fromPhrase.getStartArtifact().getParentArtifact();
	}

	public void setBetweenMentionsChunkExclusive(
			String betweenMentionsChunkExclusive) {
		this.betweenMentionsChunkExclusive = betweenMentionsChunkExclusive;
	}

	@Column( length = 10000 )
	public String getBetweenMentionsChunkExclusive() {
		return betweenMentionsChunkExclusive;
	}

	public void setGeneralizedContent(String generalizedContent) {
		this.generalizedContent = generalizedContent;
	}
	@Column(length = 1000)
	@Index(name = "index_content")
	public String getGeneralizedContent() throws Exception {
		if (generalizedContent !=null)
			return generalizedContent;
		else
			return BuildGeneralizedSentence();
	}
	public boolean isForTrain() {
		return forTrain;
	}
	public void setForTrain(boolean forTrain) {
		this.forTrain = forTrain;
	}
	private String generalizedContent;
	private Artifact relatedSentence;
	private String sentenceContent;
	private Phrase fromPhrase;
	private Phrase toPhrase;
	private int genSentId;
	private boolean forTrain;
	//This variable keeps the method for generalizing tarhet mentions, 
	//Other tagged mentions and other words in the sentence
	// there are three ways, each of them can be:
	//	type:T or POS:P or typeName:N(event or timex) or Original:O 
	private String generalizationModel;
	private String betweenMentionsChunk;
	private String betweenMentionsChunkExclusive;
	private PhraseLink.LinkType linkType;
	@Transient
	public HashMap<Integer, Phrase> startWordIndexToPhraseMap = new HashMap<Integer, Phrase>();
	
	public static enum GeneralizationModels {
		//TTPP:typtTypePartialPOS
		TTO,TTPP,
		//this contains the id of the related phrase
		TTOID, NNPID,NNOID,
		NOTYPE, ModTP,
//		TTP,TNO, TNP,NNP,NPP;
	}
	
	
	public static void calculateStanfordParseAndNormalize(boolean is_training_mode) throws Exception
	{
		ParseHandler ph = new ParseHandler();
		
		List<Artifact> sentences = 
			Artifact.listByType(Artifact.Type.Sentence,is_training_mode);
		for (Artifact sentence:sentences)
		{
			ph.calculatePOS(sentence);
			
			//now parse the normalized sentence( here just normalized to head)
			NormalizedSentence.getInstance(sentence,NormalizationMethod.MethodType.MentionToHead);
			
			HibernateUtil.clearLoaderSession();
		}

	}
}
