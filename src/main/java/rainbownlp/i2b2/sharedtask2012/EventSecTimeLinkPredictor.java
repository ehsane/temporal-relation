package rainbownlp.i2b2.sharedtask2012;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import rainbownlp.core.Artifact;
import rainbownlp.core.Artifact.Type;
import rainbownlp.core.Phrase;
import rainbownlp.util.FileUtil;
import rainbownlp.util.HibernateUtil;

public class EventSecTimeLinkPredictor {
	
	private  Integer historySectionLineOffset = null;
	private  Integer hospitalCourseLineOffset = null;
	
	public static void main(String[] args) throws Exception
	{
		List<Artifact> documents = Artifact.listByType(Type.Document);
		for (Artifact doc:documents)
		{
			EventSecTimeLinkPredictor etp =  new EventSecTimeLinkPredictor();
			etp.setSecReferenceLineOffsets(doc);
			List<Phrase> history_events = new ArrayList();
			List<Phrase> hospital_course_events = new ArrayList();
			
			if (etp.getHistorySectionLineOffset() != null )
			{
				if (etp.getHospitalCourseLineOffset() !=null)
				{
					history_events =getEventsInSection(etp.getHistorySectionLineOffset(),etp.getHospitalCourseLineOffset(),doc.getAssociatedFilePath());
					hospital_course_events = getEventsAfterLine(etp.getHospitalCourseLineOffset(),doc.getAssociatedFilePath());
				}
				else
				{
					history_events =getEventsAfterLine(etp.getHistorySectionLineOffset(),doc.getAssociatedFilePath());
				}
				
			}
			else
			{
				if (etp.getHospitalCourseLineOffset() !=null)
				{
					history_events =getEventsBeforeLine(etp.getHospitalCourseLineOffset(),doc.getAssociatedFilePath());
					hospital_course_events = getEventsAfterLine(etp.getHospitalCourseLineOffset(),doc.getAssociatedFilePath());
				}
				else
				{
					history_events =getEventsInDoc(doc.getAssociatedFilePath());
				}
			}
		}
		
		
	}
	private static List<Phrase> getEventsBeforeLine(
			Integer endLine, String associatedFilePath) {
		String hql = "from Phrase where startArtifact.associatedFilePath = :filePath and" +
		" phraseEntityType = 'EVENT' "  +
		" and endArtifact.lineIndex < :end";

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("end", endLine);
		params.put("filePath",associatedFilePath);
		
		List<Phrase> phrase_objects = 
			(List<Phrase>) HibernateUtil.executeReader(hql,params);
		 
		return phrase_objects;
	}
	private static List<Phrase> getEventsInDoc(String associatedFilePath) {
		String hql = "from Phrase where startArtifact.associatedFilePath = :filePath and" +
		" phraseEntityType = 'EVENT' ";

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("filePath",associatedFilePath);
		
		List<Phrase> phrase_objects = 
			(List<Phrase>) HibernateUtil.executeReader(hql,params);
		 
		return phrase_objects;
	}
	//for  given document find the get all sentence
	private void setSecReferenceLineOffsets(Artifact document) throws Exception
	{
		//get all sentences
		Integer line_offset=null;

		Artifact sentence = hospitalCourseLineOffset(document);
		if (sentence != null)
		{
			setHospitalCourseLineOffset(sentence.getLineIndex());
		}
		else
		{
//			throw new Exception(" this document has no hopital course "+document.getArtifactId());
			FileUtil.logLine("/tmp/docs-Without-hospitalCourse.txt", document.getAssociatedFilePath()+"\n");
		}
		
		Artifact history_sentence = historyLineOffset(document);
		if (sentence != null)
		{
			setHistorySectionLineOffset(history_sentence.getLineIndex());
		}
		else
		{
//			throw new Exception(" this document has no hopital course "+document.getArtifactId());
			FileUtil.logLine("/tmp/docs-Without-historySection.txt", document.getAssociatedFilePath()+"\n");
			
		}

	}
	
	public void setHistorySectionLineOffset(Integer historySectionLineOffset) {
		this.historySectionLineOffset = historySectionLineOffset;
	}
	public Integer getHistorySectionLineOffset() {
		return historySectionLineOffset;
	}
	public void setHospitalCourseLineOffset(Integer hospitalCourseLineOffset) {
		this.hospitalCourseLineOffset = hospitalCourseLineOffset;
	}
	public Integer getHospitalCourseLineOffset() {
		return hospitalCourseLineOffset;
	}
	public static Artifact hospitalCourseLineOffset (Artifact document)
	{
		String hql = "from Artifact where artifactType=:artifactType and " +
				"parentArtifact=:parentArtifact and content like :content1 and " +
				"content like :content2";
		
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("artifactType", Artifact.Type.Sentence.ordinal());
		params.put("parentArtifact", document.getArtifactId());
		params.put("content1", "%hospital course%");
		params.put("content2", "%:%");
		
		List<Artifact> artifact_objects = 
			(List<Artifact>) HibernateUtil.executeReader(hql,params);
		 Artifact artifact_obj=null;
		    if(artifact_objects.size()!=0)
		    {
		    	artifact_obj = 
					artifact_objects.get(0);
		    }
		    return artifact_obj;
	}
	public static Artifact historyLineOffset (Artifact document)
	{
		String hql = "from Artifact where artifactType=:artifactType and " +
				"parentArtifact=:parentArtifact and (content like :content1 or content like :content3) and " +
				"content like :content2";
		
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("artifactType", Artifact.Type.Sentence.ordinal());
		params.put("parentArtifact", document.getArtifactId());
		params.put("content1", "history"+'%');
		params.put("content2", '%'+":"+'%');
		params.put("content3", "hpi "+ '%');
		
		List<Artifact> artifact_objects = 
			(List<Artifact>) HibernateUtil.executeReader(hql,params);
		 Artifact artifact_obj=null;
		    if(artifact_objects.size()!=0)
		    {
		    	artifact_obj = 
					artifact_objects.get(0);
		    }
		    return artifact_obj;
	}
	public static List<Artifact> listSentencesInDocument(Artifact document)
	{
		String hql = "from Artifact where artifactType =:artifactType  and parentArtifact = :document";
		
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("artifactType", Artifact.Type.Sentence.ordinal());
		params.put("document", document.getArtifactId());
		
		List<Artifact> artifact_objects = 
			(List<Artifact>) HibernateUtil.executeReader(hql,params);
	return artifact_objects;
	}
	public static List<Phrase> getEventsInSection (int startIndex, int endIndex, String partial_file_path)
	{
		String hql = "from Phrase where startArtifact.associatedFilePath = :filePath and" +
				" phraseEntityType = 'EVENT' and " +
				"startArtifact.lineIndex > :start " +
				" and endArtifact.lineIndex < :end";
		
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("start",startIndex);
		params.put("end", endIndex);
		params.put("filePath",partial_file_path);

		List<Phrase> phrase_objects = 
			(List<Phrase>) HibernateUtil.executeReader(hql,params);
		 
		return phrase_objects;
	}
	public static List<Phrase> getEventsAfterLine (int startIndex, String partial_file_path)
	{
		String hql = "from Phrase where startArtifact.associatedFilePath = :filePath and" +
		" phraseEntityType = 'EVENT' and " +
		"startArtifact.lineIndex > :start ";
		
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("start",startIndex);
		params.put("filePath",partial_file_path);


		List<Phrase> phrase_objects = 
			(List<Phrase>) HibernateUtil.executeReader(hql,params);
		 
		return phrase_objects;
	}
	public EventSecTimeLinkPredictor()
	{
		
	}
	
}
