package rainbownlp.i2b2.sharedtask2012;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import rainbownlp.core.Artifact;
import rainbownlp.core.Phrase;
import rainbownlp.util.HibernateUtil;

@Entity
@Table( name = "Event" )

public class ClinicalEvent{
	Phrase relatedPhrase;
	
	private int eventId;
	
	private EventType eventType;
	private Modality modality;
	private Polarity polarity;
	private String normalizedHead;
	
	public ClinicalEvent()
	{
		
	}
	/**
	 * Loads Phrase by id
	 * @param pPhraseID
	 * @return
	 */
	public static ClinicalEvent getInstance(int pEventID) {
		String hql = "from ClinicalEvent where eventId = "+pEventID;
		ClinicalEvent event_obj = 
			(ClinicalEvent)HibernateUtil.executeReader(hql).get(0);
		return event_obj;
	}

	
	
	/**
	 * Loads or creates the Event
	 * @param pEventContent
	 * @param pFilePath
	 * @param pStartIndex
	 * @return
	 */
	public static ClinicalEvent getInstance(Phrase pRelatedPhrase, 
			EventType pEventType, Modality pModality, Polarity pPol){
		String hql = "from ClinicalEvent where relatedPhrase = "+
			pRelatedPhrase.getPhraseId();
		List<ClinicalEvent> event_objects = 
				(List<ClinicalEvent>) HibernateUtil.executeReader(hql);
	    
		ClinicalEvent event_obj;
	    if(event_objects.size()==0)
	    {
	    	event_obj = new ClinicalEvent();
	    	
	    	event_obj.setRelatedPhrase(pRelatedPhrase);
	    	event_obj.setEventType(pEventType);
	    	event_obj.setModality(pModality);
	    	event_obj.setPolarity(pPol);
	    	
	    	HibernateUtil.save(event_obj);
	    }else
	    {
	    	event_obj = 
	    		event_objects.get(0);
	    }
	    return event_obj;
	}

	public void setEventId(int pEventId) {
		this.eventId = pEventId;
	}
	
	@Id
	@GeneratedValue(generator="increment")
	@GenericGenerator(name="increment", strategy = "increment")
	public int getEventId() {
		return eventId;
	}

	public void setEventType(EventType pEventType) {
		this.eventType = pEventType;
	}
	public EventType getEventType() {
		return eventType;
	}
	
	public void setModality(Modality pModality) {
		this.modality = pModality;
	}
	public Modality getModality() {
		return modality;
	}
	
	public void setPolarity(Polarity polarity) {
		this.polarity = polarity;
	}
	public Polarity getPolarity() {
		return polarity;
	}
	@ManyToOne( cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch=FetchType.LAZY )
    @JoinColumn(name="relatedPhrase")
	public Phrase getRelatedPhrase() {
		return relatedPhrase;
	}
	public void setRelatedPhrase(Phrase relatedPhrase) {
		this.relatedPhrase = relatedPhrase;
	}
	
	public enum EventType {
		OCCURRENCE, EVIDENTIAL, TEST, PROBLEM, TREATMENT, CLINICAL_DEPT,UNKNOWN;
	}
	public enum Modality {
		ACTUAL, HYPOTHETICAL, HEDGED, PROPOSED, FACTUAL, POSSIBLE,CONDITIONAL;
	}
	public enum Polarity {
		POS, NEG;
	}
	
	public static List<ClinicalEvent> findEventsInSentence(Artifact pSentence){
		String hql = "from ClinicalEvent e where e.relatedPhrase.startArtifact.parentArtifact = "+
					pSentence.getArtifactId()+ " order by e.relatedPhrase.startArtifact ";
			
		List<ClinicalEvent> events = 
				(List<ClinicalEvent>) HibernateUtil.executeReader(hql);
	   
	    return events;
	}
	public static ClinicalEvent getRelatedEventFromPhrase(Phrase pRelatedPhrase) {
		String hql = "from ClinicalEvent where relatedPhrase = "+
			pRelatedPhrase.getPhraseId();
		List<ClinicalEvent> events = 
				(List<ClinicalEvent>) HibernateUtil.executeReader(hql);
	   ClinicalEvent event_obj = null;
	   if(events.size()!=0)
		   event_obj = events.get(0);
		return event_obj;
	}
	public void setNormalizedHead(String normalizedHead) {
		this.normalizedHead = normalizedHead;
	}
	public String getNormalizedHead() {
		return normalizedHead;
	}
	
}
