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
import rainbownlp.core.Phrase;
import rainbownlp.util.HibernateUtil;

@Entity
@Table( name = "SecTime" )
public class SecTime{

	Phrase relatedPhrase;
	
	private int secTimeId;

	private String secTimeType;
	
	private String normalizedValue;
	

	public SecTime()
	{
		
	}
	/**
	 * Loads Timex by id
	 * @param pPhraseID
	 * @return
	 */
	public static SecTime getInstance(int pSecTimeId) {
		String hql = "from SecTime where secTimeId = "+pSecTimeId;
		SecTime sectime_obj = 
			(SecTime)HibernateUtil.executeReader(hql).get(0);
		return sectime_obj;
	}

	
	
	/**
	 * Loads or creates the Timex
	 * @param pEventContent
	 * @param pFilePath
	 * @param pStartIndex
	 * @return
	 */
	public static SecTime getInstance(Phrase pRelatedPhrase, String pSecTimeType,
			String pNorValue){
		String hql = "from SecTime where relatedPhrase = "+
			pRelatedPhrase.getPhraseId();
		List<SecTime> secTime_objects = 
				(List<SecTime>) HibernateUtil.executeReader(hql);
	    
		SecTime sectime_obj;
	    if(secTime_objects.size()==0)
	    {
	    	sectime_obj = new SecTime();
	  
	    	sectime_obj.setSecTimeType(pSecTimeType);
	    	sectime_obj.setNormalizedValue(pNorValue);

	    	sectime_obj.setRelatedPhrase(pRelatedPhrase);
	    	
	    	HibernateUtil.save(sectime_obj);
	    }else
	    {
	    	sectime_obj = 
	    		secTime_objects.get(0);
	    }
	    return sectime_obj;
	}
	
	


	public void setSecTimeType(String pSecTimeType) {
		this.secTimeType = pSecTimeType;
	}
	public String getSecTimeType() {
		return secTimeType;
	}
	public void setNormalizedValue(String normalizedValue) {
		this.normalizedValue = normalizedValue;
	}
	public String getNormalizedValue() {
		return normalizedValue;
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
	public int getSecTimeId() {
		return secTimeId;
	}
	public void setSecTimeId(int secTimeId) {
		this.secTimeId = secTimeId;
	}
	public static SecTime findInstance(Phrase pRelatedPhrase){
		String hql = "from SecTime where relatedPhrase = "+
			pRelatedPhrase.getPhraseId();
		List<SecTime> secTime_objects = 
				(List<SecTime>) HibernateUtil.executeReader(hql);
	    
		SecTime sectime_obj = null;
	    if(secTime_objects.size()!=0)
	    {
	    	sectime_obj = 
	    		secTime_objects.get(0);
	    }
	    return sectime_obj;
	}
	
}
