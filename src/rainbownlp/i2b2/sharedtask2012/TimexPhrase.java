package rainbownlp.i2b2.sharedtask2012;


import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
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
@Table( name = "TimexPhrase" )
public class TimexPhrase{
//	TIMEX3="02/22" 12:1 12:1||type="DATE"||val="1992-02-22"||mod="NA"

	Phrase relatedPhrase;
	
	private int timexId;

	
	private TimexType timexType;
	
	private String normalizedValue;
	
	private TimexMod timexMod;
	//in fact it is not head and for now it timex are normalized to type
	private String normalizedHead;
	

	public TimexPhrase()
	{
		
	}
	/**
	 * Loads Timex by id
	 * @param pPhraseID
	 * @return
	 */
	public static TimexPhrase getInstance(int pTimexId) {
		String hql = "from TimexPhrase where timexId = "+pTimexId;
		TimexPhrase timex_obj = 
			(TimexPhrase)HibernateUtil.executeReader(hql).get(0);
		return timex_obj;
	}

	
	
	/**
	 * Loads or creates the Timex
	 * @param pEventContent
	 * @param pFilePath
	 * @param pStartIndex
	 * @return
	 */
	public static TimexPhrase getInstance(Phrase pRelatedPhrase, TimexType pTimexType,
			TimexMod pMod, String pNorValue){
		String hql = "from TimexPhrase where relatedPhrase = "+
			pRelatedPhrase.getPhraseId();
		List<TimexPhrase> timex_objects = 
				(List<TimexPhrase>) HibernateUtil.executeReader(hql);
	    
		TimexPhrase timex_obj;
	    if(timex_objects.size()==0)
	    {
	    	timex_obj = new TimexPhrase();
	  
	    	timex_obj.setTimexType(pTimexType);
	    	timex_obj.setNormalizedValue(pNorValue);
	    	timex_obj.setTimexMod(pMod);
	    	timex_obj.setRelatedPhrase(pRelatedPhrase);
	    	
	    	HibernateUtil.save(timex_obj);
	    }else
	    {
	    	timex_obj = 
	    		timex_objects.get(0);
	    }
	    return timex_obj;
	}
	
	


	public void setTimexType(TimexType timexType) {
		this.timexType = timexType;
	}
	public TimexType getTimexType() {
		return timexType;
	}
	public void setNormalizedValue(String normalizedValue) {
		this.normalizedValue = normalizedValue;
	}
	public String getNormalizedValue() {
		return normalizedValue;
	}
	public void setTimexMod(TimexMod mod) {
		timexMod = mod;
	}
	public TimexMod getTimexMod() {
		return timexMod;
	}
	
	@ManyToOne( cascade = {CascadeType.PERSIST, CascadeType.MERGE} )
    @JoinColumn(name="relatedPhrase")
	public Phrase getRelatedPhrase() {
		return relatedPhrase;
	}
	public void setRelatedPhrase(Phrase relatedPhrase) {
		this.relatedPhrase = relatedPhrase;
	}
	@Id
	@GeneratedValue(generator="increment")
	@GenericGenerator(name="increment", strategy = "increment")
	public int getTimexId() {
		return timexId;
	}
	public void setTimexId(int timexId) {
		this.timexId = timexId;
	}
	public enum TimexType {
		DATE, TIME, DURATION, FREQUENCY;
		public static TimexType getEnum(String s){
	        if(DATE.name().equals(s)){
	            return DATE;
	        }else if(TIME.name().equals(s)){
	            return TIME;
	        }else if(DURATION.name().equals(s)){
	            return DURATION;
	        }else if (FREQUENCY.name().equals(s)){
	            return FREQUENCY;
	        }
	        throw new IllegalArgumentException("No Enum specified for this string");
	    }
	}
	public enum TimexMod {
		NA, APPROX, MORE, LESS, START, END, MIDDLE;
		public static TimexMod getEnum(String s){
	        if(NA.name().equals(s)){
	            return NA;
	        }else if(APPROX.name().equals(s)){
	            return APPROX;
	        }else if(MORE.name().equals(s)){
	            return MORE;
	        }else if (LESS.name().equals(s)){
	            return LESS;
	        }
	        else if (START.name().equals(s)){
	            return START;
	        }
	        else if (END.name().equals(s)){
	            return END;
	        }
	        else if (MIDDLE.name().equals(s)){
	            return MIDDLE;
	        }
	        throw new IllegalArgumentException("No Enum specified for this string");
	    }
	 }
	
	public static List<TimexPhrase> findTimexInSentence(Artifact pSentence){
		String hql = "from TimexPhrase e where e.relatedPhrase.startArtifact.parentArtifact = "+
					pSentence.getArtifactId()+ " order by e.relatedPhrase.startArtifact ";
			
		List<TimexPhrase> timexs = 
				(List<TimexPhrase>) HibernateUtil.executeReader(hql);
	   
	    return timexs;
	}
	public static List<TimexPhrase> findDatesAndTimesInSentence(Artifact pSentence){
		String hql = "from TimexPhrase e where e.relatedPhrase.startArtifact.parentArtifact = "+
					pSentence.getArtifactId()+ " and (timexType= "+TimexType.DATE.ordinal()
					+" or timexType= "+TimexType.TIME.ordinal()+") order by e.relatedPhrase.startArtifact ";
			
		List<TimexPhrase> timexs = 
				(List<TimexPhrase>) HibernateUtil.executeReader(hql);
	   
	    return timexs;
	}
	public static TimexPhrase getRelatedTimexFromPhrase(Phrase pRelatedPhrase) {
		String hql = "from TimexPhrase where relatedPhrase = "+
			pRelatedPhrase.getPhraseId();
		List<TimexPhrase> timexs = 
				(List<TimexPhrase>) HibernateUtil.executeReader(hql);
		TimexPhrase timex_obj = null;
		if(timexs.size()!=0)
		 timex_obj = timexs.get(0);
		return timex_obj;
	}
	public void setNormalizedHead(String normalizedHead) {
		this.normalizedHead = normalizedHead;
	}
	public String getNormalizedHead() {
		return normalizedHead;
	}
	//this method returns an ITagged mention interfac


	
}
