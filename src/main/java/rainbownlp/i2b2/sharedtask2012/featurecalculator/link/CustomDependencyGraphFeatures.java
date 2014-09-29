/**
 * 
 */
package rainbownlp.i2b2.sharedtask2012.featurecalculator.link;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import rainbownlp.core.Artifact;
import rainbownlp.core.Artifact.Type;
import rainbownlp.core.FeatureValuePair;
import rainbownlp.core.FeatureValuePair.FeatureName;
import rainbownlp.core.Phrase;
import rainbownlp.core.PhraseLink;
import rainbownlp.i2b2.sharedtask2012.CustomDependencyGraph;
import rainbownlp.i2b2.sharedtask2012.CustomDependencyGraph.RelationshipEdge;
import rainbownlp.i2b2.sharedtask2012.LinkExampleBuilder;
import rainbownlp.i2b2.sharedtask2012.analyzer.NormalizationMethod.MethodType;
import rainbownlp.i2b2.sharedtask2012.analyzer.NormalizedSentence;
import rainbownlp.i2b2.sharedtask2012.patternminer.SimpleLabledTree;
import rainbownlp.machinelearning.IFeatureCalculator;
import rainbownlp.machinelearning.MLExample;
import rainbownlp.machinelearning.MLExampleFeature;
import rainbownlp.parser.DependencyLine;
import rainbownlp.util.HibernateUtil;
import rainbownlp.util.StanfordDependencyUtil;

/**
 * @author Azadeh
 * 
 */
public class CustomDependencyGraphFeatures implements IFeatureCalculator {
	public static HashMap<Integer, CustomDependencyGraph> cache = new HashMap<Integer, CustomDependencyGraph>();
	public static HashMap<Integer, SimpleLabledTree> lCache = new HashMap<Integer, SimpleLabledTree>();
	public static void main (String[] args) throws Exception
	{
		List<Artifact> docs = Artifact.listByType(Type.Document,false);
//		 ArrayList<Integer> doc_ids = new ArrayList<Integer>(
//			    Arrays.asList(49));
		for(Artifact doc : docs)
//			for(Integer doc_id : doc_ids)
		{
//				Artifact doc = Artifact.getInstance(doc_id);
				MLExample.hibernateSession = HibernateUtil.clearSession(MLExample.hibernateSession);
				String doc_path = doc.getAssociatedFilePath();
				List<MLExample> trainExamples = 
					MLExample.getExamplesInDocument(LinkExampleBuilder.ExperimentGroupTimexEvent, doc_path);
				List<MLExample> trainExamples2 = 
					MLExample.getExamplesInDocument(LinkExampleBuilder.ExperimentGroupEventEvent, doc_path);

				List<MLExample> all_train_examples = new ArrayList<MLExample>();
				all_train_examples.addAll(trainExamples);
				all_train_examples.addAll(trainExamples2);
				
				
				for(MLExample example_to_process: all_train_examples)
				{
					CustomDependencyGraphFeatures lbf = new CustomDependencyGraphFeatures();
					lbf.calculateFeatures(example_to_process);

					HibernateUtil.clearLoaderSession();
				}
		//		MLExample example_to_process = MLExample.getInstanceForLink
		//		(PhraseLink.getInstance(4117), experimentgroup);
		}
		
		// Noe for test
		docs = Artifact.listByType(Type.Document,false);
//		 ArrayList<Integer> doc_ids = new ArrayList<Integer>(
//			    Arrays.asList(49));
		for(Artifact doc : docs)
//			for(Integer doc_id : doc_ids)
		{
//				Artifact doc = Artifact.getInstance(doc_id);
				MLExample.hibernateSession = HibernateUtil.clearSession(MLExample.hibernateSession);
				String doc_path = doc.getAssociatedFilePath();
				List<MLExample> trainExamples = 
					MLExample.getExamplesInDocument(LinkExampleBuilder.ExperimentGroupTimexEvent, doc_path);
				List<MLExample> trainExamples2 = 
					MLExample.getExamplesInDocument(LinkExampleBuilder.ExperimentGroupEventEvent, doc_path);

				List<MLExample> all_train_examples = new ArrayList<MLExample>();
				all_train_examples.addAll(trainExamples);
				all_train_examples.addAll(trainExamples2);
				
				
				for(MLExample example_to_process: all_train_examples)
				{
					CustomDependencyGraphFeatures lbf = new CustomDependencyGraphFeatures();
					lbf.calculateFeatures(example_to_process);

					HibernateUtil.clearLoaderSession();
				}
		//		MLExample example_to_process = MLExample.getInstanceForLink
		//		(PhraseLink.getInstance(4117), experimentgroup);
		}
	
		
	}
	public void calculateFeatures(MLExample exampleToProcess) throws Exception {
			
			PhraseLink phraseLink = exampleToProcess.getRelatedPhraseLink();
			Phrase phrase1 = phraseLink.getFromPhrase();
			Phrase phrase2 = phraseLink.getToPhrase();
			Artifact related_sent = phrase1.getStartArtifact().getParentArtifact();

			
		    CustomDependencyGraph cgraph = 
				cache.get(related_sent.getArtifactId());
			if(cgraph == null)
			{
				cgraph = new CustomDependencyGraph(related_sent,exampleToProcess.getForTrain());
				cache.put(related_sent.getArtifactId(), cgraph);
			}
			List<String> edges = cgraph.getPath(phrase1, phrase2);
			
			String edge_sequence="";
			for(String edge:edges)
			{
//				FeatureValuePair customGraphPath_f = FeatureValuePair.getInstance
//				(FeatureName.customGraphPath,edge.toString(),"1");
//				 MLExampleFeature.setFeatureExample(exampleToProcess, customGraphPath_f);
				edge_sequence += edge+">";
			}
			
			edge_sequence =edge_sequence.replaceAll(">$", "");
			FeatureValuePair customGraphPath_f = FeatureValuePair.getInstance
			(FeatureName.customGraphPath,edge_sequence,"1");
			 MLExampleFeature.setFeatureExample(exampleToProcess, customGraphPath_f);
			
//			edge_sequence =edge_sequence.replaceAll(">$", "");
//			FeatureValuePair customGraphPath_f = FeatureValuePair.getInstance
//			(FeatureName.customGraphPathString,edge_sequence,"1");
//			 MLExampleFeature.setFeatureExample(exampleToProcess, customGraphPath_f);
//			 
			for(String edge:edges)
			{
				FeatureValuePair customGraphPath_indiv = FeatureValuePair.getInstance
				(FeatureName.customGraphIndividualPath,edge.toString(),"1");
				 MLExampleFeature.setFeatureExample(exampleToProcess, customGraphPath_f);
			}
			System.out.println(exampleToProcess);
//		    calculateSignalTypeFeatures(phrase1,phrase2,related_sent,exampleToProcess);
		    
//		    calculateLabeledGraphFeatures(phrase1,phrase2,related_sent,exampleToProcess);
			//////////Custom Dep graph features///////////////////////////
//			CustomDependencyGraph dep_graph = new CustomDependencyGraph(related_sent);
//			Integer from_to_to_path = cgraph.getPathSize(phrase1, phrase2);
//			Integer to_to_from_path = dep_graph.getPathSize(phrase2, phrase1);
//			
//			FeatureValuePair from_to_to_path_feature = FeatureValuePair.getInstance
//			(FeatureName.fromToToPathExist,from_to_to_path>0?"1":"0");
//		
//		    MLExampleFeature.setFeatureExample(exampleToProcess, from_to_to_path_feature);
//		    
//		    FeatureValuePair to_to_from_path_feature = FeatureValuePair.getInstance
//			(FeatureName.toToFromPathExist,to_to_from_path>0?"1":"0");
//		
//		    MLExampleFeature.setFeatureExample(exampleToProcess, to_to_from_path_feature);
//		    
		    // Path size
//		    FeatureValuePair from_to_to_path_size = FeatureValuePair.getInstance
//			(FeatureName.fromToToPathSize,from_to_to_path.toString());
//		
//		    MLExampleFeature.setFeatureExample(exampleToProcess, from_to_to_path_size);
//		    
//		    FeatureValuePair to_to_from_path_size = FeatureValuePair.getInstance
//			(FeatureName.toToFromPathSize,to_to_from_path.toString());
//		
//		    MLExampleFeature.setFeatureExample(exampleToProcess, to_to_from_path_size);
			//////  Signal type /////////////
//			TODO: UNCOMMENT
//			calculateSignalTypeFeatures(phrase1,phrase2,related_sent,exampleToProcess);	
				
	}

	public static void calculateSignalTypeFeatures(Phrase phrase1, Phrase phrase2,Artifact rel_sentence,
			MLExample exampleToProcess) throws Exception
	{
		NormalizedSentence nor_sent_obj = NormalizedSentence.getInstance(rel_sentence, MethodType.MentionToHead);
		DependencyLine from_dep_line_between = StanfordDependencyUtil.getRelatedDependencyBetween(phrase1, phrase2, nor_sent_obj.getNormalizedDependencies());
//		DependencyLine toToFrom_dep_line_between = StanfordDependencyUtil.getRelatedDependencyBetween(phrase2, phrase1, nor_sent_obj.getNormalizedDependencies());

		CustomDependencyGraph cgraph = 
			cache.get(rel_sentence.getArtifactId());
		if(cgraph == null)
		{
			cgraph = new CustomDependencyGraph(rel_sentence,exampleToProcess.getForTrain());
			cache.put(rel_sentence.getArtifactId(), cgraph);
		}
		
		String from_signal_type = "no_dep_line";
		
		if (from_dep_line_between != null)
		{
			from_signal_type = CustomDependencyGraph.getBetwenPhraseSignalType(phrase1,phrase2,from_dep_line_between).name();	
		}
//		else
//		{
//			from_signal_type = "noSignal";
//		}
		
//		if (toToFrom_dep_line_between != null)
//		{
//			to_signal_type = CustomDependencyGraph.
//			getSignalType(phrase2,phrase1,toToFrom_dep_line_between).name();	
//		}
//		else
//		{
//			to_signal_type = "noLink";
//		}
//		if (from_signal_type.equals("overlap") || to_signal_type.equals("overlap"))
//		{
//			from_signal_type = "overlap";
//			to_signal_type = "overlap";
//		}
//		MLExampleFeature.deleteExampleFeatures(exampleToProcess,FeatureName.TemporalSignal.name());
//		String temporal_signal =cgraph.reasonBasenOnPathBetween(phrase1,phrase2).toString();

		FeatureValuePair temporal_signal_Feature = FeatureValuePair.getInstance(
				FeatureName.TemporalSignal, 
				from_signal_type, "1");
		MLExampleFeature.setFeatureExample(exampleToProcess, temporal_signal_Feature);
		
	}
	public static void calculateLabeledGraphFeatures(Phrase phrase1, Phrase phrase2,
			Artifact rel_sentence,MLExample exampleToProcess) throws Exception
	{
		NormalizedSentence nor_sent_obj = NormalizedSentence.getInstance(rel_sentence, MethodType.MentionToHead);


		SimpleLabledTree lgraph = 
			lCache.get(rel_sentence.getArtifactId());
		if(lgraph == null)
		{
			lgraph = new SimpleLabledTree(rel_sentence);
			lCache.put(rel_sentence.getArtifactId(), lgraph);
		}
		
		List<RelationshipEdge> path = lgraph.getLabeledTreePath(phrase1, phrase2);
		String path_string="";
		if (path != null)
		{
			Integer path_size = path.size();
			 FeatureValuePair from_to_to_path_size = FeatureValuePair.getInstance
				(FeatureName.fromToToPathSize,path_size.toString());
			
			  MLExampleFeature.setFeatureExample(exampleToProcess, from_to_to_path_size);
			    
			for (RelationshipEdge e:path)
			{
				FeatureValuePair path_string_Feature = FeatureValuePair.getInstance(
						FeatureName.LabeledGraphNorDepPath, 
						e.toString(), "1");
				MLExampleFeature.setFeatureExample(exampleToProcess, path_string_Feature);
			}
			
		}
		
//		    
		
		
		
	}
	
}
