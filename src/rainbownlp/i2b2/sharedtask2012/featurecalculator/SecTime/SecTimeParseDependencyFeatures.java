/**
 * 
 */
package rainbownlp.i2b2.sharedtask2012.featurecalculator.SecTime;

import java.util.List;

import rainbownlp.analyzer.sentenceclause.SentenceClauseManager;
import rainbownlp.core.FeatureValuePair;
import rainbownlp.core.Phrase;
import rainbownlp.core.PhraseLink;
import rainbownlp.core.FeatureValuePair.FeatureName;
import rainbownlp.i2b2.sharedtask2012.SecTimeEventExampleBuilder;
import rainbownlp.machinelearning.featurecalculator.link.ParseDependencyFeatures;
import rainbownlp.machinelearning.IFeatureCalculator;
import rainbownlp.machinelearning.MLExample;
import rainbownlp.machinelearning.MLExampleFeature;

/**
 * @author Azadeh
 * 
 */
public class SecTimeParseDependencyFeatures implements IFeatureCalculator {
	
	
	public static void main (String[] args) throws Exception
	{
//		String experimentgroup = "BinaryLinkClassifier";
		String experimentgroup =SecTimeEventExampleBuilder.ExperimentGroupSecTimeEvent;
		List<MLExample> trainExamples = 
			MLExample.getAllExamples(experimentgroup, true,1000);
		for (MLExample example:trainExamples)
		{
			
			SecTimeParseDependencyFeatures lbf = new SecTimeParseDependencyFeatures();
			lbf.calculateFeatures(example);
		}
		//TODO remove this
		List<MLExample> trainExamples2 = 
			MLExample.getLastExamples(experimentgroup, true,1000);
		for (MLExample example:trainExamples2)
		{
			
			SecTimeParseDependencyFeatures lbf = new SecTimeParseDependencyFeatures();
			lbf.calculateFeatures(example);
		}
		
	}
	@Override
	public void calculateFeatures(MLExample exampleToProcess) throws Exception {
			
			PhraseLink phraseLink = exampleToProcess.getRelatedPhraseLink();
			Phrase phrase1 = phraseLink.getFromPhrase();
			Phrase phrase2 = phraseLink.getToPhrase();
			
			SentenceClauseManager clauseManager =
					new SentenceClauseManager(phrase1.getStartArtifact().getParentArtifact());
			
			String rel_prep1 = ParseDependencyFeatures.getRelPrepositionToPhrase(phrase1,clauseManager.sentDepLines);
			if (rel_prep1!=null)
			{
				FeatureValuePair fromPhraserelPrep = FeatureValuePair.getInstance
				(FeatureName.FromPhraseRelPrep, rel_prep1,"1");
			
				MLExampleFeature.setFeatureExample(exampleToProcess, fromPhraserelPrep);
			}
			
		    String gov_verb1 = ParseDependencyFeatures.getGovernorVerb(phrase1,clauseManager);
		    if (gov_verb1 !=null)
		    {
		    	FeatureValuePair fromGovVerb = FeatureValuePair.getInstance
				(FeatureName.FromPhraseGovVerb, gov_verb1, "1");
			
			    MLExampleFeature.setFeatureExample(exampleToProcess, fromGovVerb);
		    }
		    
		    //get the tense of the gov verb
		    Integer gov_verb1_tense = ParseDependencyFeatures.getGovernorVerbTense(phrase1, clauseManager);
		    if (gov_verb1_tense !=null)
		    {
		    	FeatureValuePair fromGovVerbTense = FeatureValuePair.getInstance
				(FeatureName.FromPhraseGovVerbTense, gov_verb1_tense.toString(),"1");
			
			    MLExampleFeature.setFeatureExample(exampleToProcess, fromGovVerbTense);
		    }
		    
		    //get verb ausxilaries
		    List<String> from_verb_auxs = ParseDependencyFeatures.getVerbAuxilaries(phrase1, clauseManager);
		    for (String verb_aux: from_verb_auxs)
		    {
		    	FeatureValuePair fromverbAuxFeatures = FeatureValuePair.getInstance
				(FeatureName.FromPhraseGovVerbAux, verb_aux,"1");
			
			    MLExampleFeature.setFeatureExample(exampleToProcess, fromverbAuxFeatures);
		    }

	}
	
	
}
