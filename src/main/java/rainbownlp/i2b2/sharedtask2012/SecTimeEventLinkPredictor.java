package rainbownlp.i2b2.sharedtask2012;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import rainbownlp.analyzer.evaluation.classification.Evaluator;
import rainbownlp.analyzer.sentenceclause.SentenceClauseManager;
import rainbownlp.core.Artifact;
import rainbownlp.core.Phrase;
import rainbownlp.core.PhraseLink;
import rainbownlp.core.PhraseLink.LinkType;
import rainbownlp.machinelearning.LearnerEngine;
import rainbownlp.machinelearning.MLExample;
import rainbownlp.machinelearning.featurecalculator.link.ParseDependencyFeatures;
import rainbownlp.util.FileUtil;
import rainbownlp.util.HibernateUtil;

public class SecTimeEventLinkPredictor   extends LearnerEngine{
	
	private  Integer historySectionLineOffset = null;
	private  Integer hospitalCourseLineOffset = null;
	
	public static void main(String[] args) throws Exception
	{
		SecTimeEventLinkPredictor p = new SecTimeEventLinkPredictor();
//		CrossValidation cv = new CrossValidation(p);
		
		MLExample.resetExamplesPredicted(SecTimeEventExampleBuilder.ExperimentGroupSecTimeEvent, true);
		
		List<MLExample> trainExamples = 
			MLExample.getAllExamples(SecTimeEventExampleBuilder.ExperimentGroupSecTimeEvent, true,100);
		
		for(MLExample example : trainExamples)
		{
			example.setPredictedClass(-1);
			HibernateUtil.save(example);
		}
		
		p.test(trainExamples);
		Evaluator.getEvaluationResult(trainExamples).printResult();
		
//		cv.crossValidation(trainExamples, 2).printResult();
		
		
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
	public void setSecReferenceLineOffsets(Artifact document) throws Exception
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
	public SecTimeEventLinkPredictor()
	{
		
	}
	public List<Phrase> getHistorySectionEvents(SecTimeEventLinkPredictor etp, Artifact doc)
	{
		List<Phrase> history_events = new ArrayList<Phrase>();
		if (etp.getHistorySectionLineOffset() != null )
		{
			if (etp.getHospitalCourseLineOffset() !=null)
			{
				history_events =getEventsInSection
				(etp.getHistorySectionLineOffset(),etp.getHospitalCourseLineOffset(),doc.getAssociatedFilePath());
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
			}
			else
			{
				history_events =getEventsInDoc(doc.getAssociatedFilePath());
			}
		}
		return history_events;
	}
	public List<Phrase> getHospitalCourseSectionEvents(SecTimeEventLinkPredictor etp, Artifact doc)
	{
		List<Phrase> hospital_course_events = new ArrayList<Phrase>();
		if (etp.getHistorySectionLineOffset() != null )
		{
			if (etp.getHospitalCourseLineOffset() !=null)
			{
		
				hospital_course_events = getEventsAfterLine(etp.getHospitalCourseLineOffset(),doc.getAssociatedFilePath());
			}
			
		}
		else
		{
			if (etp.getHospitalCourseLineOffset() !=null)
			{
				hospital_course_events = getEventsAfterLine(etp.getHospitalCourseLineOffset(),doc.getAssociatedFilePath());
			}
		
		}
		return hospital_course_events;
	}
//	TODO: change it to directly find from Phrase when we have SeCTIMES inside
	public Phrase getDocAddmisionTime(Artifact doc) {
		String hql = "from SecTime where secTimeType = 'ADMISSION' and " +
				" relatedPhrase.startArtifact.parentArtifact.parentArtifact = :docId";
		
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("docId",doc.getArtifactId());
		
		SecTime sec_time_obj = null;
		
		List<SecTime> sec_time_objs = 
			(List<SecTime>) HibernateUtil.executeReader(hql,params);
		 if(sec_time_objs.size()!=0)
		 {
			 sec_time_obj = sec_time_objs.get(0);
		 }
		 else
		 {
			 sec_time_obj = null;
		 }
		
		 Phrase related_phrase = null;
		if (sec_time_obj != null)
		{
			Phrase sectime_phrase = sec_time_obj.getRelatedPhrase();
			related_phrase = Phrase.findInstance(sectime_phrase.getStartArtifact(), sectime_phrase.getEndArtifact(),
					"TIMEX3");
		}
		if (related_phrase ==null)
		{
			String hql2 = "from Phrase where  startArtifact.associatedFilePath = :filePath " +
					"and PhraseEntityType ='TIMEX3' and " +
					"startArtifact.parentArtifact.previousArtifact.content like :prevLineContent";
			HashMap<String, Object> params2 = new HashMap<String, Object>();
			params2.put("filePath",doc.getAssociatedFilePath());
			params2.put("prevLineContent","admission date"+'%');
			
			Phrase sec_time_phrase = null;
		
			List<Phrase> sec_time_phrases = 
				(List<Phrase>) HibernateUtil.executeReader(hql2,params2);
			 if(sec_time_objs.size()!=0)
			 {
				 related_phrase = sec_time_phrases.get(0);
			 }
		
		}
		return related_phrase;
	}
	public Phrase getDocDischargeTime(Artifact doc) {
		String hql = "from SecTime where secTimeType = 'DISCHARGE' and " +
				" relatedPhrase.startArtifact.parentArtifact.parentArtifact = :docId";
		
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("docId",doc.getArtifactId());
		
		SecTime sec_time_obj = null;
		
		List<SecTime> sec_time_objs = 
			(List<SecTime>) HibernateUtil.executeReader(hql,params);
		
		 if(sec_time_objs.size()!=0)
		 {
			 sec_time_obj = sec_time_objs.get(0);
		 }
		 else
		 {
			 sec_time_obj = null;
		 }
		
		 Phrase related_phrase = null;
		if (sec_time_obj != null)
		{
			Phrase sectime_phrase = sec_time_obj.getRelatedPhrase();
			related_phrase = Phrase.findInstance(sectime_phrase.getStartArtifact(), sectime_phrase.getEndArtifact(),
					"TIMEX3");
		}
		if (related_phrase ==null)
		{
			String hql2 = "from Phrase where  startArtifact.associatedFilePath = :filePath " +
					"and PhraseEntityType = :phraseEntity and " +
					"startArtifact.parentArtifact.previousArtifact.content like :prevLineContent";
			HashMap<String, Object> params2 = new HashMap<String, Object>();
			params2.put("filePath",doc.getAssociatedFilePath());
			params2.put("phraseEntity","TIMEX3");
			params2.put("prevLineContent","discharge Date"+'%');
			
			Phrase sec_time_phrase = null;
		
			List<Phrase> sec_time_phrases = 
				(List<Phrase>) HibernateUtil.executeReader(hql2,params2);
			 if(sec_time_objs.size()!=0)
			 {
				 related_phrase = sec_time_phrases.get(0);
			 }
		
		}
		return related_phrase;
	}
	@Override
	public void train(List<MLExample> pTrainExamples) throws Exception {
		// TODO Auto-generated method stub
		
	}
	//here I assume that the first phrase is the targer event to be compared with a time
	@Override
	public void test(List<MLExample> pTestExamples) throws Exception {
		for (MLExample t_example:pTestExamples)
		{
			PhraseLink phrase_link =  t_example.getRelatedPhraseLink();
			Phrase from_phrase =  phrase_link.getFromPhrase();
			Phrase to_Phrase = phrase_link.getToPhrase();
			
			if (from_phrase.getPhraseContent().toLowerCase().matches("admission"))
			{
				t_example.setPredictedClass(LinkType.OVERLAP.ordinal());
				continue;
			}
		
			
			SentenceClauseManager clauseManager = new SentenceClauseManager(from_phrase.getStartArtifact().getParentArtifact());
			Integer gov_verb_tense = ParseDependencyFeatures.getGovernorVerbTense(from_phrase,clauseManager);
			String rel_prep = ParseDependencyFeatures.getRelPrepositionToPhrase(from_phrase,clauseManager.sentDepLines);
			
			ClinicalEvent rel_from_event = null;
			ClinicalEvent.EventType from_event_type=null;
			if (from_phrase.getPhraseEntityType().equals("EVENT"))
			{
				rel_from_event = ClinicalEvent.getRelatedEventFromPhrase(from_phrase);
				from_event_type = rel_from_event.getEventType();
				if (rel_prep != null && rel_prep.toLowerCase().equals("for")
						&& from_event_type.name().matches("OCCURENCE|TREATMENT|TEST"))
				{
					t_example.setPredictedClass(LinkType.OVERLAP.ordinal());
				}
				else if (gov_verb_tense!= null && gov_verb_tense ==1)
				{
					t_example.setPredictedClass(LinkType.BEFORE.ordinal());
				}
				
				
			}
			HibernateUtil.save(t_example);
		}
		
	}
}
