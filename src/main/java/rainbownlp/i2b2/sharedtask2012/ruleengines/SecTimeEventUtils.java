package rainbownlp.i2b2.sharedtask2012.ruleengines;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import rainbownlp.analyzer.sentenceclause.SentenceClauseManager;
import rainbownlp.analyzer.sentenceclause.Verb.VerbTense;
import rainbownlp.core.Artifact;
import rainbownlp.core.PhraseLink;
import rainbownlp.core.Phrase;
import rainbownlp.core.PhraseLink.LinkType;
import rainbownlp.i2b2.sharedtask2012.ClinicalEvent;
import rainbownlp.i2b2.sharedtask2012.SecTime;
import rainbownlp.i2b2.sharedtask2012.TimexPhrase;
import rainbownlp.i2b2.sharedtask2012.ClinicalEvent.EventType;
import rainbownlp.i2b2.sharedtask2012.featurecalculator.SecTime.SecTimeBasicFeatures;
import rainbownlp.i2b2.sharedtask2012.loader.I2b2DocumentDetails;
import rainbownlp.machinelearning.MLExample;
import rainbownlp.machinelearning.featurecalculator.link.ParseDependencyFeatures;
import rainbownlp.util.FileUtil;
import rainbownlp.util.HibernateUtil;

public class SecTimeEventUtils {
	
	private  Integer historySectionLineOffset = null;
	private  Integer hospitalCourseLineOffset = null;
	
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
		if (history_sentence != null)
		{
			setHistorySectionLineOffset(history_sentence.getLineIndex());
		}
		else
		{
//			throw new Exception(" this document has no hopital course "+document.getArtifactId());
			FileUtil.logLine("/tmp/docs-Without-historySection.txt", document.getAssociatedFilePath()+"\n");
			
		}

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
	public SecTimeEventUtils()
	{
		
	}
	public List<Phrase> getHistorySectionEvents(SecTimeEventUtils etp, Artifact doc)
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
			// removing discharge in the Discharge Date from history event
			//TODO: test if it works fine
			for (int i=0;i< history_events.size();i++)
			{
				Phrase event = history_events.get(i);
				
				if (event.getPhraseContent().toLowerCase().equals("discharge")
						&& event.getStartArtifact().getLineIndex() <=5)
				{
					history_events.remove(i);
					break;
				}
			}
			for (int i=0;i< history_events.size();i++)
			{
				Phrase event = history_events.get(i);
				if (event.getPhraseContent().toLowerCase().equals("admission")
						&& event.getStartArtifact().getLineIndex() <=2)
				{
					history_events.remove(i);
					break;
				}
			}
		}

		return history_events;
	}
	public List<Phrase> getHospitalCourseSectionEvents(SecTimeEventUtils etp, Artifact doc)
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
	public static Phrase getDocAddmisionTime(Artifact doc) {
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
	public static Phrase getDocDischargeTime(Artifact doc) {
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
			params2.put("prevLineContent","Discharge Date"+'%');
			
			Phrase sec_time_phrase = null;
		
			List<Phrase> sec_time_phrases = 
				(List<Phrase>) HibernateUtil.executeReader(hql2,params2);
			 if(sec_time_phrases.size()!=0)
			 {
				 related_phrase = sec_time_phrases.get(0);
			 }
		
		}
		if (related_phrase ==null)
		{
			related_phrase = Phrase.getMadeUpInstance("Discharge", "Discharge", "MADEUP");
		}
		return related_phrase;
	}
	//here I assume that the first phrase is the targer event to be compared with a time

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
	//is it history or hospital
	public static String getEventRelatedSection(Artifact doc,Phrase p) throws Exception
	{
		String event_sec = null;
		// get doc 
		SecTimeEventUtils etp =  new SecTimeEventUtils();
		etp.setSecReferenceLineOffsets(doc);
		
		List<Phrase> history_events = etp.getHistorySectionEvents(etp, doc);
//		List<Phrase> hospital_course_events = etp.getHospitalCourseSectionEvents(etp, doc);
		if (history_events.contains(p))
		{
			event_sec = "his";
		}
		else
		{
			event_sec = "hos";
		}
		return event_sec;
	}
//	there are some direct mentions of admission or discharge in text that 
	public static Integer reasonBeaseOnAdmissionDischargeMentions(MLExample example_to_process,
			HashMap<Integer, TimexPhrase> phrase_date_map, Artifact doc) throws Exception
	{
		Integer predicted =-1;
		PhraseLink phrase_link = example_to_process.getRelatedPhraseLink();
		Phrase from_phrase = phrase_link.getFromPhrase();
		Phrase to_phrase = phrase_link.getToPhrase();
		
//		String content_lemma = StringUtil.getWordLemma(from_phrase.getPhraseContent());
		String content_lemma =from_phrase.getPhraseContent().toLowerCase();
		
		if (content_lemma.matches("admission") )
		{
			
			String sec_time_type = SecTimeBasicFeatures.getSecTimeType(to_phrase).toLowerCase();
				
			//compare with admission time
			predicted = comparePhraseTimeWithRelatedDocTime(from_phrase, sec_time_type, phrase_date_map, doc);
			
			if (predicted ==-1)
			{
				Integer verb_tense_number = ParseDependencyFeatures.getGovernorVerbTense(from_phrase, null);
				
				if(verb_tense_number != null)
				{
					VerbTense verbtense = VerbTense.getEnum(verb_tense_number);
					if (sec_time_type.equals("admission"))
					{
//						if (!(verbtense.equals(VerbTense.PAST) || verbtense.equals(VerbTense.PASTPERFECT)))
							predicted = LinkType.OVERLAP.ordinal();
//						else
//							predicted = LinkType.BEFORE.ordinal();
					}
					else
					{
						predicted = LinkType.BEFORE.ordinal();
					}
				}


			}
			
			
		}
		else if (content_lemma.matches(".*discharge") )
		{
			// we are comparing to discharge time
			String sec_time_type = SecTimeBasicFeatures.getSecTimeType(to_phrase).toLowerCase();
			
			//compare with admission time
			predicted = comparePhraseTimeWithRelatedDocTime(from_phrase, sec_time_type, phrase_date_map, doc);
			
			if (predicted ==-1)
			{
				
				if (sec_time_type.equals("discharge"))
				{
					predicted = LinkType.OVERLAP.ordinal();
				}
				else
				{
					predicted = LinkType.BEFORE.ordinal();
				}
				//time of the phrase is unknown do reason based on verb tense
//				Integer verb_tense_number = ParseDependencyFeatures.getGovernorVerbTense(from_phrase, null);
//				VerbTense verbtense = VerbTense.getEnum(verb_tense_number);
			}
		}
		return predicted;
	}
	public static Integer comparePhraseTimeWithRelatedDocTime(Phrase pPhrase,String sec_time_type,
			HashMap<Integer, TimexPhrase> phrase_date_map, Artifact doc) throws Exception
	{
		Integer predicted = -1;
		//compare with admission time
		TimexPhrase from_phrase_time = phrase_date_map.get(pPhrase.getPhraseId());
		if (from_phrase_time ==null)
		{
			return predicted;
		}
		Phrase related_section_phrase = null;
		if (sec_time_type.equals("admission"))
		{
			related_section_phrase = SecTimeEventUtils.getDocAddmisionTime(doc);
		}
		else if (sec_time_type.equals("discharge"))
		{
			related_section_phrase = getDocDischargeTime(doc);
		}
			
	
		if (related_section_phrase != null && from_phrase_time != null)
		{
			TimexPhrase related_timex = TimexPhrase.getRelatedTimexFromPhrase(related_section_phrase);
			LinkType result = RuleEngineUtils.compareTimexes(from_phrase_time,related_timex);
			if (result!=null)
			{
//				if (result.equals(LinkType.OVERLAP) && from_phrase_time.getTimexMod().name().equals("START"))
//				{
//					result = LinkType.BEFORE;
//				}
				predicted = result.ordinal();
			}
		}
		return predicted;
	}
	public static Integer reasonBeaseOnPhraseTime(MLExample example_to_process,
			HashMap<Integer, TimexPhrase> phrase_date_map, Artifact doc) throws Exception
	{
		Integer predicted =-1;
		PhraseLink phrase_link = example_to_process.getRelatedPhraseLink();
		Phrase from_phrase = phrase_link.getFromPhrase();
		Phrase to_phrase = phrase_link.getToPhrase();
		
		String sec_time_type = SecTimeBasicFeatures.getSecTimeType(to_phrase).toLowerCase();
		predicted = comparePhraseTimeWithRelatedDocTime(from_phrase, sec_time_type, phrase_date_map, doc);
		//especial handling of some exceptions
		if(sec_time_type.equals("discharge") &&  LinkType.getEnum(predicted) == LinkType.OVERLAP)
		{
			ClinicalEvent event = ClinicalEvent.getRelatedEventFromPhrase(from_phrase);
			if (event.getEventType().equals(EventType.TREATMENT))
			{
				predicted= LinkType.BEFORE.ordinal();
			}
		}


		return predicted;
	}
	public static Phrase getAdmDischargeEvent(Artifact doc,String phraseContent, String sentContent) {
		
		String hql = "from Phrase where phraseContent = :phraseContent "+
			" and startArtifact.associatedFilePath= :filePath" +
			" and startArtifact.parentArtifact.content like :sentContent";
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("phraseContent", phraseContent);
		params.put("filePath", doc.getAssociatedFilePath());
		params.put("sentContent", sentContent+'%');
		List<Phrase> phrase_objects = 
			(List<Phrase>) HibernateUtil.executeReader(hql, params);
	    
		Phrase phrase_obj = null;
	    if(phrase_objects.size()!=0)
	    {
	    	phrase_obj = phrase_objects.get(0);
	    }
		return phrase_obj;
	}
	public static int checkDocTargetEventTimes(MLExample example, I2b2DocumentDetails doc_details) {
		int predicted = -1;
		
		Phrase admissionEvent = doc_details.getAdmissionEvent();
		
		Phrase dischargeEvent = doc_details.getDischargeEvent();
		Phrase admission_time = doc_details.getAdmissionTimePhrase();
		Phrase discharge_time = doc_details.getDischargeTimePhrase();
		
		PhraseLink phras_link = example.getRelatedPhraseLink();
		Phrase from_p = phras_link.getFromPhrase();
		Phrase to_p = phras_link.getToPhrase();
		
		if (from_p.equals(admissionEvent)
				&& to_p.equals(admission_time))
		{
			predicted = LinkType.OVERLAP.ordinal();
		}
		else if (from_p.equals(dischargeEvent)
				&& to_p.equals(discharge_time))
		{
			predicted = LinkType.OVERLAP.ordinal();
		}
		
		return predicted;
	}
	public static I2b2DocumentDetails setDocumentRelatedDetails(Artifact doc)
	{
		I2b2DocumentDetails doc_details = new I2b2DocumentDetails();
		
		Phrase admissionEvent = getAdmDischargeEvent(doc,"admission", "admission date :");
		Phrase dischargeEvent = getAdmDischargeEvent(doc,"discharge", "discharge date :");
		Phrase admission_time = getDocAddmisionTime(doc);
		Phrase discharge_time = getDocDischargeTime(doc);
		
		doc_details.setAdmissionEvent(admissionEvent);
		doc_details.setAdmissionTimePhrase(admission_time);
		doc_details.setDischargeEvent(dischargeEvent);
		doc_details.setDischargeTimePhrase(discharge_time);
		doc_details.setRelatedDoc(doc);
		return doc_details;
		
	}

}
