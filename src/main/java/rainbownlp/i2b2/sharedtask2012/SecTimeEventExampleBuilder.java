package rainbownlp.i2b2.sharedtask2012;

import java.util.ArrayList;
import java.util.List;

import rainbownlp.core.Artifact;
import rainbownlp.core.Phrase;
import rainbownlp.core.PhraseLink;
import rainbownlp.core.Artifact.Type;
import rainbownlp.i2b2.sharedtask2012.featurecalculator.SecTime.SecTimeBasicFeatures;
import rainbownlp.i2b2.sharedtask2012.featurecalculator.SecTime.SecTimeParseDependencyFeatures;
import rainbownlp.i2b2.sharedtask2012.ruleengines.SecTimeEventUtils;
import rainbownlp.machinelearning.IFeatureCalculator;
import rainbownlp.machinelearning.MLExample;
import rainbownlp.util.FileUtil;
import rainbownlp.util.HibernateUtil;

public class SecTimeEventExampleBuilder {
	public final static String ExperimentGroupSecTimeEvent = "LinkClassificationSecTimeEvent";
	public static void main(String[] args) throws Exception
	{
		 buildSectimeExamples(true);
	}
	public static void calculateSecTimeEventFeatures(String experimentGroup,boolean for_train) throws Exception
	{
		List<IFeatureCalculator> secTimeEventFeatureCalculators = 
			new ArrayList<IFeatureCalculator>();
	
		secTimeEventFeatureCalculators.add(new SecTimeBasicFeatures());
		secTimeEventFeatureCalculators.add(new SecTimeParseDependencyFeatures());
		MLExample.hibernateSession = HibernateUtil.clearSession(MLExample.hibernateSession);
		
		List<MLExample> trainExamples = 
			MLExample.getAllExamples(experimentGroup, for_train);
		int counter = 0;
		for (MLExample example:trainExamples)
		{
			
			example.calculateFeatures(secTimeEventFeatureCalculators);
			counter++;
			FileUtil.logLine(null,"calculateSecTimeEventFeatures--------example processed: "+counter);
		}
	}
	public static void buildSectimeExamples(boolean is_training_mode) throws Exception
	{
		List<MLExample> added_examples = new ArrayList<MLExample>();
		List<Artifact> documents = Artifact.listByType(Type.Document,is_training_mode);
		int counter = 0;
		for (Artifact doc:documents)
		{
			MLExample.hibernateSession = HibernateUtil.clearSession(MLExample.hibernateSession);
			
//			doc =Artifact.getInstance(72);
//			TODO: add Discharge and discharge time for overlap
			SecTimeEventUtils etp =  new SecTimeEventUtils();
			etp.setSecReferenceLineOffsets(doc);
			
			List<Phrase> history_events = etp.getHistorySectionEvents(etp, doc);
			List<Phrase> hospital_course_events = etp.getHospitalCourseSectionEvents(etp, doc);
			
			Phrase doc_addmision_time = SecTimeEventUtils.getDocAddmisionTime(doc);
			Phrase doc_discharge_time = SecTimeEventUtils.getDocDischargeTime(doc);
			if (doc_addmision_time == null || doc_discharge_time==null )
			{
//				throw new Exception("admission or discharge time null...doc id:"+doc.getArtifactId());
				FileUtil.logLine("/home/azadeh/projects/rainbownlp/data/log files/finalLogs/files-without-adm-discharge.txt", 
						doc.getAssociatedFilePath());
			}
			//build the admission and discharge examples
			Phrase admissionEvent = SecTimeEventUtils.getAdmDischargeEvent(doc,"admission", "admission date :");
			Phrase dischargeEvent = SecTimeEventUtils.getAdmDischargeEvent(doc,"discharge", "discharge date :");
			
			if (doc_addmision_time != null && admissionEvent != null)
			{
				MLExample example = buildSecTimeEventExample(admissionEvent,doc_addmision_time,doc);
//				added_examples.add(example)
			}
			if (doc_discharge_time != null && dischargeEvent != null && !doc_discharge_time.getPhraseEntityType().equals("MADEUP"))
			{
				buildSecTimeEventExample(dischargeEvent,doc_discharge_time,doc);
			}
			
//			HibernateUtil.startTransaction();
			if (doc_addmision_time != null)
			{	
				for(Phrase history_event :history_events)
				{
					// double check to see it doesn't include the discharge time
					if (history_event.getPhraseContent().toLowerCase().equals("discharge")
							&& history_event.getStartArtifact()
							.getParentArtifact().getContent().toLowerCase().equals("discharge date :"))
					{
	
						continue;
					}
					MLExample link_example =buildSecTimeEventExample(history_event,doc_addmision_time,doc);
				
//				    link_example.calculateFeatures(secTimeEventFeatureCalculators);
				}
				
			}
			else
			{
				FileUtil.logLine("/home/azadeh/projects/rainbownlp/data/log files/" +
						"finalLogs/doc-without-addmissionTimeTimex.txt", 
						doc.getAssociatedFilePath());
			}
			
			if (doc_discharge_time != null)
			{
				for(Phrase  hospital_event:hospital_course_events)
				{
					buildSecTimeEventExample(hospital_event,doc_discharge_time,doc);
//				    link_example.calculateFeatures(timexEventFeatureCalculators);
				}
			}
			else
			{
				FileUtil.logLine("/home/azadeh/projects/rainbownlp/data/log files/" +
						"finalLogs/doc-without-DischargeTimeTimex.txt", 
						doc.getAssociatedFilePath());
			}
			counter++;
			FileUtil.logLine(null,"SecTimeExampleBuilder-----Doc processed: "+counter);
				
			HibernateUtil.clearLoaderSession();
//			HibernateUtil.endTransaction();
		}
	
	}
	private static MLExample buildSecTimeEventExample(Phrase from_phrase, Phrase to_phrase, Artifact doc)
	{
		PhraseLink sectime_event_link = 
			PhraseLink.getInstance(from_phrase, to_phrase);
		int expected_class = sectime_event_link.getLinkType().ordinal();
		
		MLExample link_example = 
			MLExample.getInstanceForLink(sectime_event_link, ExperimentGroupSecTimeEvent);
		link_example.setExpectedClass(expected_class);
		link_example.setRelatedPhraseLink(sectime_event_link);
		
		link_example.setPredictedClass(-1);

	
		if(doc.getAssociatedFilePath().contains("/train/"))
			link_example.setForTrain(true);
		else
			link_example.setForTrain(false);
	
		MLExample.saveExample(link_example);
		return link_example;
	}


}
