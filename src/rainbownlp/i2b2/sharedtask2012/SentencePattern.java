package rainbownlp.i2b2.sharedtask2012;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import rainbownlp.core.Artifact;
import rainbownlp.util.HibernateUtil;
import rainbownlp.util.StringUtil;

@Entity
@Table( name = "SentencePattern" )
public class SentencePattern {
	public static void main(String[] args)
	{
		List<Artifact> sentences = 
				Artifact.listByType(Artifact.Type.Sentence);
		for(Artifact sentence : sentences)
		{
			SentencePattern sentencePattern = 
					SentencePattern.getInstance(sentence);
			String generalized = getGeneralizedContent(sentence);
			sentencePattern.setSentencePattern(generalized);
			HibernateUtil.save(sentencePattern);
		}
	}
	
	public static String getGeneralizedContent(Artifact pSentence)
	{
		String generalized = pSentence.getContent();
		List<TimexPhrase> timexs = 
				TimexPhrase.findTimexInSentence(pSentence);
		for(TimexPhrase timex : timexs)
		{
			generalized = generalized.replace(timex.getRelatedPhrase().getPhraseContent(), 
					"<TIMEX_"+timex.getTimexType()+">");
		}
		List<ClinicalEvent> events = 
				ClinicalEvent.findEventsInSentence(pSentence);
		for(ClinicalEvent event : events)
		{
			generalized =
					generalized.replace(event.getRelatedPhrase().getPhraseContent(), 
							"<EVENT_"+event.getEventType()+">");
		}
		
		return generalizeString(generalized);
	}
	
	
	public static String generalizeString(String generalized) {
		//Sodium of 138 ; potassium 3.5 ; 106 , 28 , 31 , 1.0 , glucose 134 .
		generalized = generalized.replaceAll("\\\\+%", "%");
		
		//CASE-SENSITIVE patterns
		generalized = generalized.replaceAll("\\\\\\*\\\\\\* (\\w+) \\( .* \\)","\\$$1\\$");
		generalized = generalized.replaceAll("([A-Z][a-z]+ [A-Z]\\.? [A-Z][a-z]+)", "\\$name\\$");
		generalized = generalized.replaceAll("((D(r|R))|(M(r|R)). )[A-Z][a-z]+", "$1\\$name\\$");
		generalized = generalized.replaceAll("[A-Z][a-z]+( [A-Z][a-z]+)?( MD)", "\\$name\\$$2");

		
		generalized = generalized.trim().toLowerCase(); 
		
		//CASE-INSENSITIVE patterns
		String[] tokens = generalized.replaceAll("(\\D):(\\S)", "$1 : $2")
				.split(" ");
		generalized = "";
		for(String token: tokens)
		{
			if(token.length()>3 && 
				StringUtil.isDate(token))
				generalized+= "$date$ ";
			else {
				try {
					Double.parseDouble(token.replaceAll("(^\\W+)|(\\W+$)", ""));
					generalized+= "$num$ ";
				} catch (NumberFormatException e) {
					generalized+= token+" ";
				}
			}
		}

		
		generalized = generalized.replaceAll("(^((\\( on )|((t|d) ?: ?\\$date\\$)))|(\\$date\\$ )+", "\\$date\\$ ");

		generalized = generalized.replaceAll("\\$date\\$ ?(a|p)m(\\W|$)", "\\$date\\$");
		
		generalized = generalized.replaceAll("(\\$num\\$ ?)((%)|(mg)|(cm)|(pg)|(cc))(\\W|$)", "$1\\$unit\\$$8");
		generalized = generalized.replaceAll("([1-2][0-9]{3})", "\\$year\\$");
		generalized = generalized.replaceAll("(^|\\W)[0-9]+\\-[0-9]+(\\\\%)?($|\\W)", "$1\\$range\\$$3");
		
//		generalized = generalized.replaceAll(
//				"^((\\$num\\$ ((\\.?\\))|\\.|\\-))|(#|\\\\|_|\\*|\\s)+)","");
		generalized = generalized.replaceAll(
				"((\\s|\\.|:|\\-)+)$","");
		generalized = generalized.replaceAll("\\$num\\$ ((year(s)?(\\s|\\-)old)|(y/?o))", "\\$age\\$");
		generalized = generalized.replaceAll("\\d\\d?\\-year(s)?\\-old", "\\$age\\$");
		generalized = generalized.replaceAll("\\$age\\$ ?years", "\\$age\\$");
		
		generalized = generalized.replaceAll("\\$age\\$\\s?(f|(woman))(\\s|$)", "\\$age\\$ female$3");
		generalized = generalized.replaceAll("\\$age\\$\\s?m(an)?", "\\$age\\$ male");
		generalized = generalized.replaceAll("\\( s \\)", "");
		
//		<event_test> of $num$ ; <event_test> $num$ ; $num$ , $num$ , $num$ , $num$ , <event_test> $num$
		generalized = generalized.replaceAll("(\\$num\\$ (;|,) ?)+", "\\$num\\$ ; ");
//		<event_test> of $num$ ; <event_test> $num$ ; <event_test> $num$
		generalized = generalized.replaceAll("<event_test> of \\$num\\$", "<event_test> \\$num\\$");
		generalized = generalized.replaceAll("^\\$num\\$ (;|,)", "");
		
		return generalized.trim();
	}


	String sentencePattern;
	Artifact relatedSentence;
	int sentencePatternId;
	
	

	public SentencePattern()
	{
		
	}
	
	
	public static SentencePattern getInstance(Artifact pRelatedSentence){
		String hql = "from SentencePattern where relatedSentence = "+
				pRelatedSentence.getArtifactId();
		List<SentencePattern> event_objects = 
				(List<SentencePattern>) HibernateUtil.executeReader(hql);
	    
		SentencePattern event_obj;
	    if(event_objects.size()==0)
	    {
	    	event_obj = new SentencePattern();
	    	
	    	event_obj.setRelatedSentence(pRelatedSentence);
	    
	    	HibernateUtil.save(event_obj);
	    }else
	    {
	    	event_obj = 
	    		event_objects.get(0);
	    }
	    return event_obj;
	}


	@Column( length = 10000 )
	public String getSentencePattern() {
		return sentencePattern;
	}


	public void setSentencePattern(String sentencePattern) {
		this.sentencePattern = sentencePattern;
	}

	@OneToOne( cascade = {CascadeType.PERSIST, CascadeType.MERGE} )
    @JoinColumn(name="relatedSentence")
	public Artifact getRelatedSentence() {
		return relatedSentence;
	}


	public void setRelatedSentence(Artifact relatedSentence) {
		this.relatedSentence = relatedSentence;
	}

	@Id
	@GeneratedValue(generator="increment")
	@GenericGenerator(name="increment", strategy = "increment")
	public int getSentencePatternId() {
		return sentencePatternId;
	}


	public void setSentencePatternId(int sentencePatternId) {
		this.sentencePatternId = sentencePatternId;
	}

}
