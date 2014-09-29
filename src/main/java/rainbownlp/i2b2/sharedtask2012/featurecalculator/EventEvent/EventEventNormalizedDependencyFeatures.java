/**
 * 
 */
package rainbownlp.i2b2.sharedtask2012.featurecalculator.EventEvent;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import rainbownlp.core.Artifact;
import rainbownlp.core.FeatureValuePair;
import rainbownlp.core.FeatureValuePair.FeatureName;
import rainbownlp.core.Phrase;
import rainbownlp.core.PhraseLink;
import rainbownlp.i2b2.sharedtask2012.ClinicalEvent;
import rainbownlp.i2b2.sharedtask2012.ClinicalEvent.EventType;
import rainbownlp.i2b2.sharedtask2012.SharedConstants;
import rainbownlp.i2b2.sharedtask2012.analyzer.NormalizationMethod;
import rainbownlp.i2b2.sharedtask2012.analyzer.NormalizedSentence;
import rainbownlp.machinelearning.IFeatureCalculator;
import rainbownlp.machinelearning.MLExample;
import rainbownlp.machinelearning.MLExampleFeature;
import rainbownlp.parser.DependencyLine;
import rainbownlp.util.StanfordDependencyUtil;
/**
 * @author Azadeh
 * 
 */
public class EventEventNormalizedDependencyFeatures implements IFeatureCalculator {

	public static HashMap<Artifact, ArrayList<DependencyLine>> sent_nor_dep_line_map= 
			new HashMap<Artifact, ArrayList<DependencyLine>>();
	public static void main(String[] args) throws Exception
	{
		List<MLExample> trainExamples = 
			MLExample.getAllExamples(SharedConstants.EXPERIMENT_GROUP_EVENTEVENT, true);

		for(MLExample example_to_process: trainExamples)
		{
			EventEventNormalizedDependencyFeatures ndf =  new EventEventNormalizedDependencyFeatures();
			
			
			ndf.calculateFeatures(example_to_process);
		}		
		
	}

//	public static StanfordParser s_parser;
	public void calculateFeatures(MLExample exampleToProcess)  throws SQLException, Exception {
			
			PhraseLink phraseLink = exampleToProcess.getRelatedPhraseLink();

			boolean is_event_after_problem = isEventAfterProblem(phraseLink);
			FeatureValuePair is_event_after_problem_feature = FeatureValuePair.getInstance
			(FeatureName.isEventAfterProblem, is_event_after_problem?"1":"0");
		
		    MLExampleFeature.setFeatureExample(exampleToProcess, is_event_after_problem_feature);
			
	}
	// this checks if the link show the relation between problem and evidential
	public boolean isEventAfterProblem(PhraseLink phrase_link) throws Exception
	{
		boolean hold_condition = false;
		Phrase phrase1 = phrase_link.getFromPhrase();
		Phrase phrase2 = phrase_link.getToPhrase();
		
		ClinicalEvent event1 = ClinicalEvent.getRelatedEventFromPhrase(phrase1);
		ClinicalEvent event2 = ClinicalEvent.getRelatedEventFromPhrase(phrase2);
		if (event1.getEventType().equals(EventType.PROBLEM))
		{
			if(phrase2.getPOS().startsWith("VB") && (
					event2.getEventType().equals(EventType.EVIDENTIAL)
					|| event2.getEventType().equals(EventType.OCCURRENCE)))
			{
				ArrayList<DependencyLine> nor_dependencies = getSentNorDepList(phrase1.getStartArtifact().getParentArtifact());

				List<DependencyLine> all_govs = StanfordDependencyUtil.getAllGovernors
					(nor_dependencies, phrase1.getNormalizedHead(), phrase1.getNormalOffset()+1);
				for (DependencyLine gov: all_govs)
				{
					if(gov.firstPart.equals(phrase2.getNormalizedHead()))
					{
						hold_condition =true;
						break;
					}
						
				}
				
				
			}
		}
		else if (event2.getEventType().equals(EventType.PROBLEM))
		{
			if(phrase1.getPOS().startsWith("VB") && (
					event1.getEventType().equals(EventType.EVIDENTIAL)
					|| event1.getEventType().equals(EventType.OCCURRENCE)))
			{
				ArrayList<DependencyLine> nor_dependencies =getSentNorDepList(phrase1.getStartArtifact().getParentArtifact());

				List<DependencyLine> all_govs = StanfordDependencyUtil.getAllGovernors
					(nor_dependencies, phrase2.getNormalizedHead(), phrase2.getNormalOffset()+1);
				for (DependencyLine gov: all_govs)
				{
					if(gov.firstPart.equals(phrase1.getNormalizedHead()))
					{
						hold_condition =true;
						break;
					}
						
				}
				
				
			}
			
		}
		return hold_condition;
	}
	private ArrayList<DependencyLine> getSentNorDepList(Artifact rel_sent) throws SQLException {
		ArrayList<DependencyLine> sent_dep_lines = sent_nor_dep_line_map.get(rel_sent);
		if (sent_dep_lines==null)
		{
			NormalizedSentence normalized = NormalizedSentence.getInstance(rel_sent,
					NormalizationMethod.MethodType.MentionToHead);
			String normalized_dependency_string = normalized.getNormalizedDependency();
			sent_dep_lines =StanfordDependencyUtil.parseDepLinesFromString
			(normalized_dependency_string);
			sent_nor_dep_line_map.put(rel_sent, sent_dep_lines);
		}
		return sent_dep_lines;
	}
	
	
}
