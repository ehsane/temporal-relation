package rainbownlp.i2b2.sharedtask2012.analyzer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.GenericGenerator;

import rainbownlp.core.Artifact;
import rainbownlp.core.Phrase;
import rainbownlp.i2b2.sharedtask2012.ClinicalEvent;
import rainbownlp.i2b2.sharedtask2012.TimexPhrase;
import rainbownlp.i2b2.sharedtask2012.patternminer.GeneralizedSentence;
import rainbownlp.parser.DependencyLine;
import rainbownlp.parser.StanfordParser;
import rainbownlp.util.HibernateUtil;
import rainbownlp.util.StanfordDependencyUtil;

@Entity
@Table( name = "NormalizedSentence" )
public class NormalizedSentence {
	
	private Artifact relatedSentence;
	private String normalizedContent;
	private String normalizedPennTree;
	private String normalizedDependency;
	//e.g. MentiontoHead
	private String normalizationMethod;
	private int normalizedSentId;
	
	@Transient
	private ArrayList<DependencyLine> normalizedDependencies = new ArrayList<DependencyLine>();
	@Transient
	private  HashMap<Integer, Integer> phraseNewOffsetMap = null;
	
	public static void main (String args[]) throws SQLException
	{
//		List<Artifact> docs = Artifact.listByType(Type.Document);
//		
//		for(Artifact doc : docs)
//		{
//				
//			MLExample.hibernateSession = HibernateUtil.clearSession(MLExample.hibernateSession);
//			String doc_path = doc.getAssociatedFilePath();
//			List<MLExample> trainExamples = 
//				MLExample.getExamplesInDocument(LinkExampleBuilder.ExperimentGroupTimexEvent, doc_path);
//			List<MLExample> trainExamples2 = 
//				MLExample.getExamplesInDocument(LinkExampleBuilder.ExperimentGroupEventEvent, doc_path);
//
//			List<MLExample> all_train_examples = new ArrayList<MLExample>();
//			all_train_examples.addAll(trainExamples);
//			all_train_examples.addAll(trainExamples2);
//			
//			for (MLExample example:all_train_examples)
//			{
//				PhraseLink phraseLink = example.getRelatedPhraseLink();
//				
//				Phrase phrase1 = phraseLink.getFromPhrase();
//				Phrase phrase2 = phraseLink.getToPhrase();
//				Artifact related_sent = phrase1.getStartArtifact().getParentArtifact();
//				String nor_head =NormalizedSentence.getPhraseHead(
//						related_sent, phrase1, phrase1.getPhraseEntityType());
//				String nor_head2 =NormalizedSentence.getPhraseHead(
//						related_sent, phrase2, phrase2.getPhraseEntityType());
//			}
//				
//		}
		//get all sentences
		List<Artifact> sentences = Artifact.listByType(Artifact.Type.Sentence,true);
		
		for (Artifact sentence:sentences)
		{
//			sentence = Artifact.getInstance(57376);
			NormalizedSentence ns = getInstance(sentence,NormalizationMethod.MethodType.MentionToHead);
	
//			String simplified = ns.getNormalizedToHeadSentence(sentence);
			HibernateUtil.clearLoaderSession();
		}
		//normalize to head
	}
	
	
//	This method replaces a phrase in the sentence with the head of the phrase
	//this method simply replace with the last word
	//TODO find the exact head word
	public static String getPhraseHead(Artifact sentence, Phrase ph, String type) 
	{
		//For [blood pressure control] she was maintained on metoprolol 
		//at the request of her primary care physician and enalapril was discontinued .
		String phrase_head = "";
		if(type.equals("EVENT"))
		{
			String pos = ph.getPOS();
			if (Phrase.isNestedPhrase(ph, sentence))
			{
				phrase_head = Phrase.getLastNounInPhrase(ph, sentence).getContent();
			}
			else
			{
//				if(ph.getStartArtifact().getPOS() != null && ph.getStartArtifact().getPOS().equals("DT"))
//					phrase_head = ph.getStartArtifact().getContent()+" "+ ph.getEndArtifact().getContent();
//				else
//					phrase_head = ph.getEndArtifact().getContent();
				
				if (pos != null && (pos.equals("VB-RP") || pos.equals("VBG-RP")|| pos.equals("VBN-JJ") || pos.equals("VB-IN") || pos.equals("VBN-IN"))
						||  pos.equals("JJ-TO-VB") ||  pos.startsWith("VBG-TO"))
				{
					phrase_head = ph.getStartArtifact().getContent();
				}
				else if(!ph.getEndArtifact().getContent().matches(".*\\W$"))
				{
					phrase_head = ph.getEndArtifact().getContent();
				}
				else
				{
					Artifact cur_artifact = ph.getEndArtifact();
					while(cur_artifact.getContent().matches("\\W") ||
							cur_artifact.getContent().matches("\\W.*"))
					{
						cur_artifact = cur_artifact.getPreviousArtifact();
					}
					phrase_head = cur_artifact.getContent();
				}
			}
//			
		}
// normalize the timex to type
		else if (type.equals("TIMEX3"))
		{
			//get the type of the timex
			TimexPhrase timex_obj = TimexPhrase.getRelatedTimexFromPhrase(ph);
			TimexPhrase.TimexType  timex_type = timex_obj.getTimexType();
			Phrase next_p= Phrase.getNextPhraseInSentence(ph, sentence);
			if (next_p != null && 
					ph.getEndArtifact().getArtifactId()==next_p.getStartArtifact().getArtifactId())
			{
				phrase_head = Phrase.getFirstNounInPhrase(ph, sentence).getContent();
			}
			else
			{
//				if(ph.getStartArtifact().getPOS().equals("DT")|| ph.getStartArtifact().getPOS().equals("IN"))
//				phrase_head = ph.getStartArtifact().getContent()+" "+ timex_type.name().toLowerCase();
//			else
//				phrase_head = timex_type.name().toLowerCase();
			if(timex_type.equals(TimexPhrase.TimexType.DATE))
			{
				if (ph.getPhraseContent().matches("\\w+"))
				{
					phrase_head = ph.getEndArtifact().getContent();
				}
				else
				{
					phrase_head = timex_obj.getNormalizedValue();
					if (phrase_head.isEmpty())
					{
						phrase_head = Phrase.getFirstNounInPhrase(ph, sentence).getContent();
					}
				}
				
			}
			else if (timex_type.equals(TimexPhrase.TimexType.DURATION) || timex_type.equals(TimexPhrase.TimexType.FREQUENCY))
			{
				
//				phrase_head = ph.getEndArtifact().getContent();
				phrase_head = Phrase.getFirstNounInPhrase(ph, sentence).getContent();
				if(phrase_head.matches(".*\\W$"))
				{
					phrase_head = timex_type.name().toLowerCase();
				}
			}
			else
			{
				phrase_head = timex_type.name().toLowerCase();
			}
			}

		}
			
		
		return phrase_head;
	}
	//depricated
//	private String getSimplifiedToHead(Artifact sent) throws SQLException
//	{
//		String simplifiedToHead = sent.getContent();
//		List<ClinicalEvent> sent_events = ClinicalEvent.findEventsInSentence(sent);
//		List<TimexPhrase> sent_timex = TimexPhrase.findTimexInSentence(sent);
//		
//		for(ClinicalEvent event:sent_events)
//		{
//			//get all events that have more than one tokens
//			if (event.getRelatedPhrase().getStartArtifact().equals(event.getRelatedPhrase().getEndArtifact()))
//				continue;
//			//for each normalize to head
//			String mention_head = getPhraseHead(sent, event.getRelatedPhrase(),"ClinicalEvent");
////			int curr_offset = event.getRelatedPhrase().getStartArtifact().getWordIndex()+1;
////			if(event.getRelatedPhrase().getStartArtifact().getPOS().equals("DT"))
////			{
////				curr_offset = curr_offset++;
////			}
//			//eventToHeadMap.put(event, mention_head);
//			
//			
//			
//			simplifiedToHead = simplifiedToHead.replace(event.getRelatedPhrase().getPhraseContent(), mention_head);
//			if(mention_head.matches(".*\\s+.*"))
//			{
//				mention_head = mention_head.split("\\s")[1];
//			}
//			event.setNormalizedHead(mention_head);
//			HibernateUtil.save(event);
//		}
//
//		for(TimexPhrase timex:sent_timex)
//		{
//			//get all events that have more than one tokens
//			if (timex.getRelatedPhrase().getStartArtifact().equals(timex.getRelatedPhrase().getEndArtifact()))
//				continue;
//			//for each normalize to the type of the timex
//			String mention_head = getPhraseHead(sent, timex.getRelatedPhrase(),"Timex");
//			simplifiedToHead = simplifiedToHead.replace(timex.getRelatedPhrase().getPhraseContent(), mention_head);
//			
//			if(mention_head.matches(".*\\s+.*"))
//			{
//				mention_head = mention_head.split("\\s")[1];
//			}
//			timex.setNormalizedHead(mention_head);
//			HibernateUtil.save(timex);
//			
//				
//		}
//		this.setNormalizedContent(simplifiedToHead);
//		// put in the hashmap
//		//replace i sent
//		return simplifiedToHead;
//	}
	@Transient
	public String getNormalizedToHeadSentence(Artifact sent)
	{
		String generalized_sentence = null;
		
		List<ClinicalEvent> sent_events = ClinicalEvent.findEventsInSentence(sent);
		List<TimexPhrase> sent_timex = TimexPhrase.findTimexInSentence(sent);
		
		HashMap<Integer, String> word_offset_map = new HashMap<Integer, String>();

		//get all the artifacts of the sentence
		List<Artifact> words_in_sent = sent.getChildsArtifact();
		HashMap<Integer, String> offset_content_map = new HashMap<Integer, String>();
		for (Artifact word:words_in_sent)
		{
			offset_content_map.put(word.getWordIndex(), word.getContent());
		}

		for (ClinicalEvent event:sent_events)
		{
			String mention_head;
			//get all events that have more than one tokens
			if (event.getRelatedPhrase().getStartArtifact().equals(event.getRelatedPhrase().getEndArtifact()))
			{
				mention_head = event.getRelatedPhrase().getPhraseContent();
			}
			else
			{
				mention_head = getPhraseHead(sent, event.getRelatedPhrase(),"EVENT");
				offset_content_map = replacePhraseWithHead(offset_content_map,
						event.getRelatedPhrase(),mention_head);
			}		
			event.setNormalizedHead(mention_head);
			
			HibernateUtil.save(event);

			
		}
		for(TimexPhrase timex:sent_timex)
		{
			String mention_head;
			
			//get all events that have more than one tokens
//			if (timex.getRelatedPhrase().getStartArtifact().equals(timex.getRelatedPhrase().getEndArtifact()))
//			{
//				mention_head = timex.getRelatedPhrase().getPhraseContent();
//			}
//			else
			{
				mention_head = getPhraseHead(sent, timex.getRelatedPhrase(),"TIMEX3");
				offset_content_map = replacePhraseWithHead(offset_content_map,
						timex.getRelatedPhrase(),mention_head);
			}

			timex.setNormalizedHead(mention_head);
			HibernateUtil.save(timex);
			
		}
		ArrayList<String> sent_word_and_offsets = new ArrayList<String>();
		int count =0;
		int[] sorted_offsets= new int[offset_content_map.keySet().size()];
		for (Integer key:offset_content_map.keySet())
		{
			sorted_offsets[count] = key;
			count++;
		}
		Arrays.sort(sorted_offsets);
		for (Integer offset: sorted_offsets)
		{
			sent_word_and_offsets.add(offset_content_map.get(offset)+"_"+offset);
		}
		for (ClinicalEvent event:sent_events)
		{
			Phrase related_phrase = event.getRelatedPhrase();
			String target;
			if (Phrase.isNestedPhrase(event.getRelatedPhrase(), sent))
			{
				Artifact noun = Phrase.getLastNounInPhrase(related_phrase, sent);
				 target = noun.getContent()+"_"+noun.getWordIndex();
			}
			else
			{
				target= event.getNormalizedHead()+"_"+related_phrase.getStartArtifact().getWordIndex();
			}
			Integer new_index = sent_word_and_offsets.indexOf(target);
			related_phrase.setNormalizedHead(event.getNormalizedHead());
			related_phrase.setNormalOffset(new_index);
			HibernateUtil.save(related_phrase);
			getPhraseNewOffsetMap().put(event.getRelatedPhrase().getPhraseId(),new_index );
		}
		for(TimexPhrase timex:sent_timex)
		{
			Phrase related_phrase = timex.getRelatedPhrase();
			String target;
			Phrase next_p= Phrase.getNextPhraseInSentence(related_phrase, sent);
			if (next_p != null &&
					related_phrase.getEndArtifact().getArtifactId()==next_p.getStartArtifact().getArtifactId())
			{
				Artifact noun = Phrase.getFirstNounInPhrase(related_phrase, sent);
				 target = noun.getContent()+"_"+noun.getWordIndex();
			}
			else
			{
				target= timex.getNormalizedHead()+"_"+related_phrase.getStartArtifact().getWordIndex();
			}
			Integer new_index = sent_word_and_offsets.indexOf(target);
			related_phrase.setNormalizedHead(timex.getNormalizedHead());
			related_phrase.setNormalOffset(new_index);
			HibernateUtil.save(related_phrase);
			getPhraseNewOffsetMap().put(timex.getRelatedPhrase().getPhraseId(),new_index );
		}
		generalized_sentence = GeneralizedSentence.convertWordMapsToText(offset_content_map);
	
		return generalized_sentence;
	}
	private static HashMap<Integer, String> replacePhraseWithHead
	(HashMap<Integer, String> offset_content_map,
			Phrase phrase, String mention_head)
	{
		Artifact start_art = phrase.getStartArtifact();
		Artifact end_art = phrase.getEndArtifact();
		String start_pos = phrase.getStartArtifact().getPOS();
		String entity_type = phrase.getPhraseEntityType();
		Artifact sent = phrase.getStartArtifact().getParentArtifact();
		Phrase next_p= Phrase.getNextPhraseInSentence(phrase, sent);
		if (next_p != null &&
				phrase.getEndArtifact().getArtifactId()==next_p.getStartArtifact().getArtifactId())
		{
			return offset_content_map;
		}
		if(Phrase.isNestedPhrase(phrase, sent))
		{
			return offset_content_map;
		}
		offset_content_map.put(start_art.getWordIndex(),mention_head);
		
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
	public void NormalizedSentence()
	{
		
	}
	@Id
	@GeneratedValue(generator="increment")
	@GenericGenerator(name="increment", strategy = "increment")
	public int getNormalizedSentId() {
		return normalizedSentId;
	}
	public void setNormalizedSentId(int pId) {
		  normalizedSentId = pId;
	}
	public void setRelatedSentence(Artifact relatedSentence) {
		this.relatedSentence = relatedSentence;
	}
	@ManyToOne( cascade = {CascadeType.PERSIST, CascadeType.MERGE} )
    @JoinColumn(name="relatedSentence")
	public Artifact getRelatedSentence() {
		return relatedSentence;
	}
	public void setNormalizedContent(String pNormalizedContent) {
		this.normalizedContent = pNormalizedContent;
	}
	
	public String getNormalizedContent() throws SQLException {
		return normalizedContent;
	}
	public void setNormalizedPennTree(String normalizedPennTree) {
		this.normalizedPennTree = normalizedPennTree;
	}
	@Column(length=3000)
	public String getNormalizedPennTree() {
		return normalizedPennTree;
	}
	public void setNormalizedDependency(String normalizedDependency) {
		this.normalizedDependency = normalizedDependency;
	}
	public String getNormalizedDependency() {
		return normalizedDependency;
	}
	public void setNormalizationMethod(String normalizationMethod) {
		this.normalizationMethod = normalizationMethod;
	}
	public String getNormalizationMethod() {
		return normalizationMethod;
	}
	
	private static StanfordParser s_parser = new StanfordParser();
	
	public static NormalizedSentence getInstance(Artifact pRelatedSentence, NormalizationMethod.MethodType pType){
		String hql = "from NormalizedSentence where relatedSentence = "+
			pRelatedSentence.getArtifactId() + " and normalizationMethod ='"+pType.name()+"'";
		List<NormalizedSentence> objects = 
				(List<NormalizedSentence>) HibernateUtil.executeReader(hql);
	    
		NormalizedSentence nor_sent_obj;
	    if(objects.size()==0)
	    {
	    	nor_sent_obj = new NormalizedSentence();
	    	
	    	nor_sent_obj.setRelatedSentence(pRelatedSentence);
	    	nor_sent_obj.setNormalizationMethod(pType.name());
	    	String normalized_sent = nor_sent_obj.getNormalizedToHeadSentence(pRelatedSentence);
	    	nor_sent_obj.setNormalizedContent(normalized_sent);
	    	s_parser.parse(normalized_sent);
			
			String nor_dependencies_string = s_parser.getDependencies();
			String nor_penn_tree = s_parser.getPenn();
			nor_sent_obj.setNormalizedDependency(nor_dependencies_string);
			nor_sent_obj.setNormalizedPennTree(nor_penn_tree);
			HibernateUtil.save(nor_sent_obj);
	    }else
	    {
	    	nor_sent_obj = 
	    		objects.get(0);
	    }
	    return nor_sent_obj;
	}
	
	public void setNormalizedDependencies(ArrayList<DependencyLine> normalizedDependencies) {
		this.normalizedDependencies = normalizedDependencies;
	}
	@Transient
	public ArrayList<DependencyLine> getNormalizedDependencies() {
		if (!normalizedDependencies.isEmpty())
		{
			return normalizedDependencies;
		}
		else
		{
			return StanfordDependencyUtil.parseDepLinesFromString(normalizedDependency);
		}
	}


	public void setPhraseNewOffsetMap(HashMap<Integer, Integer> phraseNewOffsetMap) {
		this.phraseNewOffsetMap = phraseNewOffsetMap;
	}

	@Transient
	public HashMap<Integer, Integer> getPhraseNewOffsetMap() {
		if (phraseNewOffsetMap!= null)
			return phraseNewOffsetMap;
		else
		{
			//get all events of the sent
			List<Phrase> sent_phList = Phrase.getPhrasesInSentence(relatedSentence);
			phraseNewOffsetMap = new HashMap<Integer, Integer>();
			for (Phrase p:sent_phList)
			{
				phraseNewOffsetMap.put(p.getPhraseId(), p.getNormalOffset());
			}
			return phraseNewOffsetMap;
		}
	}
}
