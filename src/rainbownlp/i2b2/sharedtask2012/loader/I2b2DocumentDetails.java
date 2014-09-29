package rainbownlp.i2b2.sharedtask2012.loader;

import java.util.Date;
import rainbownlp.core.Artifact;
import rainbownlp.core.Phrase;

public class I2b2DocumentDetails {
	
	private Phrase admissionTimePhrase;
	private Phrase dischargeTimePhrase;
	private Date admDate;
	private Date dischargeDate;

	private Phrase admissionEvent;
	private Phrase dischargeEvent;
	private Artifact relatedDoc;
	
//	TODO add history and hospital course infor here
	
//	@Override
//	public boolean equals(Object objectToCompare)
//	{
//		if(objectToCompare instanceof Phrase)
//		{
//			Phrase phrase = (Phrase) objectToCompare;
//			int phraseStart = phrase.getStartArtifact().getStartIndex() + 
//				phrase.getStartArtifact().getLineIndex();
//			if(phrase.getPhraseContent().trim().equals(content.trim()) 
//					&&
//				phraseStart ==  startChar)
//				return true;
//		}
//		return false;
//	}
	public I2b2DocumentDetails(){
		
	}
	
	@Override
	public String toString()
	{
		return "relatedDoc="+getRelatedDoc().getArtifactId()+"/path="+
		getRelatedDoc().getAssociatedFilePath();
	}

	public void setAdmissionTimePhrase(Phrase admissionTime) {
		this.admissionTimePhrase = admissionTime;
	}

	public Phrase getAdmissionTimePhrase() {
		return admissionTimePhrase;
	}

	public void setDischargeTimePhrase(Phrase dischargeTime) {
		this.dischargeTimePhrase = dischargeTime;
	}

	public Phrase getDischargeTimePhrase() {
		return dischargeTimePhrase;
	}

	public void setAdmissionEvent(Phrase admissionEvent) {
		this.admissionEvent = admissionEvent;
	}

	public Phrase getAdmissionEvent() {
		return admissionEvent;
	}

	public void setDischargeEvent(Phrase dischargeEvent) {
		this.dischargeEvent = dischargeEvent;
	}

	public Phrase getDischargeEvent() {
		return dischargeEvent;
	}

	public void setRelatedDoc(Artifact relatedDoc) {
		this.relatedDoc = relatedDoc;
	}

	public Artifact getRelatedDoc() {
		return relatedDoc;
	}

	public void setAdmDate(Date admDate) {
		this.admDate = admDate;
	}

	public Date getAdmDate() {
		return admDate;
	}

	public void setDischargeDate(Date dischargeDate) {
		this.dischargeDate = dischargeDate;
	}

	public Date getDischargeDate() {
		return dischargeDate;
	}
}
