package rainbownlp.i2b2.sharedtask2012;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.ClassBasedEdgeFactory;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedMultigraph;

import rainbownlp.analyzer.evaluation.Evaluator;
import rainbownlp.core.Artifact;
import rainbownlp.core.Phrase;
import rainbownlp.core.PhraseLink;
import rainbownlp.core.PhraseLink.LinkType;
import rainbownlp.i2b2.sharedtask2012.ClinicalEvent.EventType;
import rainbownlp.i2b2.sharedtask2012.TimexPhrase.TimexType;
import rainbownlp.i2b2.sharedtask2012.analyzer.NormalizationMethod.MethodType;
import rainbownlp.i2b2.sharedtask2012.analyzer.NormalizedSentence;
import rainbownlp.i2b2.sharedtask2012.featurecalculator.link.NormalizedDependencyFeatures;
import rainbownlp.i2b2.sharedtask2012.featurecalculator.link.PatternStatisticsFeatures;
import rainbownlp.i2b2.sharedtask2012.patternminer.GeneralizedSentence;
import rainbownlp.i2b2.sharedtask2012.patternminer.GeneralizedSentence.GeneralizationModels;
import rainbownlp.machinelearning.MLExample;
import rainbownlp.machinelearning.featurecalculator.link.ParseDependencyFeatures;
import rainbownlp.parser.DependencyLine;
import rainbownlp.util.FileUtil;
import rainbownlp.util.StanfordDependencyUtil;
import rainbownlp.util.StringUtil;

public class CustomDependencyGraph {
	
	public static HashMap<Integer, CustomDependencyGraph> cache = new HashMap<Integer, CustomDependencyGraph>();
	public static void main(String[] args) throws Exception
	{
		List<MLExample> examples = 
			MLExample.getExamplesByDocument(LinkExampleBuilder.ExperimentGroupEventEvent, true,95);
	
		int counter = 0;
		int example_counter = 0;
		
		for(MLExample example : examples)
		{
			Artifact sentence = example.getRelatedPhraseLink().getFromPhrase().
				getStartArtifact().getParentArtifact();
			CustomDependencyGraph cgraph = 
				cache.get(sentence.getArtifactId());
			if(cgraph == null)
			{
				cgraph = new CustomDependencyGraph(sentence,false);
				cache.put(sentence.getArtifactId(), cgraph);
			}
			PhraseLink plinke = example.getRelatedPhraseLink();
			Phrase phrase1 = plinke.getFromPhrase();
			Phrase phrase2 = plinke.getToPhrase();
			
			List<String> path = cgraph.getPath(phrase1, phrase2);
			PhraseLink plinketemp = example.getRelatedPhraseLink();
//			boolean has_path =
//				cgraph.isThereAPath(plinke.getFromPhrase(), 
//						plinke.getToPhrase());
//			int predicted = -1;
////			if(has_path)
////				predicted = LinkType.OVERLAP.ordinal();
////			example.setPredictedClass(predicted);
////			HibernateUtil.save(example);
		}
		
		Evaluator.getEvaluationResult(examples).printResult();
	}

	Artifact sentence;
	NormalizedSentence norSentence;
	List<Phrase> ordered_phrases;
	List<MLExample> decidedExamples;
	private DirectedGraph<String, RelationshipEdge> g =
		  new DirectedMultigraph<String, RelationshipEdge>(
                  new ClassBasedEdgeFactory<String, RelationshipEdge>(RelationshipEdge.class));

	public CustomDependencyGraph(Artifact p_sentence,boolean for_train) throws Exception
	{
		sentence = p_sentence;
		norSentence = NormalizedSentence.getInstance(sentence, MethodType.MentionToHead);
		ordered_phrases = Phrase.getOrderedPhrasesInSentence(sentence);
	    decidedExamples = MLExample.getDecidedExamplesForGraph(p_sentence);
		makeDependencyGraph(for_train);
	}

	
	public boolean isThereAPath(Phrase phrase1, Phrase phrase2)
	{
		boolean res = false;
		String node1 = phrase1.getNormalizedHead()+"-"+(phrase1.getNormalOffset()+1);
		String node2 = phrase2.getNormalizedHead()+"-"+(phrase2.getNormalOffset()+1);
		node1 = node1.toLowerCase().replaceAll("/", "\\\\/");
		node2 = node2.toLowerCase().replaceAll("/", "\\\\/");
		List<RelationshipEdge> from_to_path = 
			DijkstraShortestPath.findPathBetween(g, node1, node2);
//		List<GraphEdge> to_from_path = 
//			DijkstraShortestPath.findPathBetween(g, node2, node1);
		
		
		if(from_to_path !=null && from_to_path.size()>0)
		{
			res = true;
		}
		return res;
	}
	public Integer getPathSize(Phrase phrase1, Phrase phrase2)
	{
		Integer size = 0;
		if (phrase2.getNormalOffset()==-1 ||phrase1.getNormalOffset()==-1 )
		{
			FileUtil.logLine("/tmp/phraseidsWithWrongNormalization.txt", ((Integer)phrase2.getPhraseId()).toString()+" "+
					((Integer)phrase1.getPhraseId()).toString());
			return 0;
		}
		String node1 = phrase1.getNormalizedHead().toLowerCase()+"-"+(phrase1.getNormalOffset()+1);
		String node2 = phrase2.getNormalizedHead().toLowerCase()+"-"+(phrase2.getNormalOffset()+1);
		
//		node1 = node1.toLowerCase().replaceAll("/", "\\\\/");
//		node2 = node2.toLowerCase().replaceAll("/", "\\\\/");
			
		List<RelationshipEdge> from_to_path = 
			DijkstraShortestPath.findPathBetween(g, node1, node2);
		
		
		if(from_to_path !=null)
		{
			size = from_to_path.size();
		}
		return size;
	}
	public List<String>  getPath(Phrase phrase1, Phrase phrase2)
	{
		List<String> edge_names = new ArrayList<String>();
		if (phrase2.getNormalOffset()==-1 ||phrase1.getNormalOffset()==-1 )
		{
			FileUtil.logLine("/tmp/phraseidsWithWrongNormalization.txt", ((Integer)phrase2.getPhraseId()).toString()+" "+
					((Integer)phrase1.getPhraseId()).toString());
			return edge_names;
		}
		String node1 = phrase1.getNormalizedHead().toLowerCase()+"-"+(phrase1.getNormalOffset()+1);
		String node2 = phrase2.getNormalizedHead().toLowerCase()+"-"+(phrase2.getNormalOffset()+1);
		
//		node1 = node1.toLowerCase().replaceAll("/", "\\\\/");
//		node2 = node2.toLowerCase().replaceAll("/", "\\\\/");
		List<RelationshipEdge> from_to_path = 
			DijkstraShortestPath.findPathBetween(g, node1, node2);
		
		if (from_to_path != null)
		{
			for (RelationshipEdge e:from_to_path )
			{
				edge_names.add(e.toString());
			}
		}


		return edge_names;
	}
//	public LinkType getLinkType(Phrase phrase1, Phrase phrase2)
//	{
//		LinkType res = LinkType.UNKNOWN;
//		String node1 = phrase1.getNormalizedHead()+"-"+(phrase1.getNormalOffset()+1);
//		String node2 = phrase2.getNormalizedHead()+"-"+(phrase2.getNormalOffset()+1);
//		node1 = node1.toLowerCase().replaceAll("/", "\\\\/");
//		node2 = node2.toLowerCase().replaceAll("/", "\\\\/");
//		List<GraphEdge> from_to_path = 
//			DijkstraShortestPath.findPathBetween(g, node1, node2);
//		List<GraphEdge> to_from_path = 
//			DijkstraShortestPath.findPathBetween(g, node2, node1);
//		
//		
//		if(from_to_path !=null && from_to_path.size()>0)
//			if(to_from_path!=null && 
//				to_from_path.size()>0)
//			{
//				//its overlap
//				res = LinkType.OVERLAP;
//			}else
//			{
//				res = LinkType.BEFORE;
//			}
//		else
//			if(to_from_path!=null && to_from_path.size()>0)
//			{
//				res = LinkType.AFTER;
//			}
//		return res;
//	}
	//this will return gov if gov is in sent before dependent
	private static int getFirstNode(String node1,String node2)
	{
		Integer node1_offset = Integer.parseInt(node1.replaceAll(".*-(\\d+)$", "$1"));
		Integer node2_offset = Integer.parseInt(node2.replaceAll(".*-(\\d+)$", "$1"));
		if (node1_offset<node2_offset)
		{
			return 1;
		}
		else
		{
			return 2;
		}
	}
	public static HashMap<String,Integer> patternToClass = new HashMap<String, Integer>();
	
	
	private void makeDependencyGraph(boolean for_train) throws Exception {
		
		g =new DefaultDirectedGraph<String, RelationshipEdge>(new ClassBasedEdgeFactory<String, RelationshipEdge>(RelationshipEdge.class));
		
		
		
		//create a node for each phrase
		for (Phrase p:ordered_phrases)
		{
			Integer offset = p.getNormalOffset()+1;
			String node1 = p.getNormalizedHead().toLowerCase()+"-"+offset;
			g.addVertex(node1);
		}
		// get all examples for the sentence that predicted <> 0 and 
		// add edge accordingly 
		for (MLExample ml_decided :decidedExamples )
		{
			Phrase p_from = ml_decided.getRelatedPhraseLink().getFromPhrase();
			Integer p_from_offset = p_from.getNormalOffset()+1;
			String node1 = p_from.getNormalizedHead().toLowerCase()+"-"+p_from_offset;
			Phrase p2 = ml_decided.getRelatedPhraseLink().getToPhrase();
			Integer offset2 = p2.getNormalOffset()+1;
			String node2 = p2.getNormalizedHead().toLowerCase()+"-"+offset2;
			
			int predicted = ml_decided.getPredictedClass().intValue();
			overwriteEdge(node1,node2,predicted);
		}
		List<DependencyLine> deps = norSentence.getNormalizedDependencies();
		
		for(DependencyLine dep : deps)
		{
			SignalType signal_type = getSignalType(dep);
			
//			TODO:change it
//			SignalType signal_type = SignalType.unknown;
			
			String node1 = dep.firstPart.toLowerCase()+"-"+dep.firstOffset;
			String node2 = dep.secondPart.toLowerCase()+"-"+dep.secondOffset;
			int first_node = getFirstNode(node1, node2);
			
			if (!signal_type.equals(SignalType.unknown))
			{				
				g.addVertex(node1);
				g.addVertex(node2);
				
				switch (signal_type) {
				case gov_after_dep:
					createOrOverrideEdge(node1,node2,"#a#");
					createOrOverrideEdge(node2,node1,"#b#");
//					g.addEdge(node1, node2,new RelationshipEdge<String>(node1, node2, "#a#"));
//					g.addEdge(node2, node1,new RelationshipEdge<String>(node2, node1, "#b#"));
					
					break;
				case gov_before_dep:
					createOrOverrideEdge(node1,node2,"#b#");
					createOrOverrideEdge(node2,node1,"#a#");
//					TODO: remove if the above is working
//					g.addEdge(node1, node2,new RelationshipEdge<String>(node1, node2, "#b#"));
//					g.addEdge(node2, node1,new RelationshipEdge<String>(node2, node1, "#a#"));
//					if (first_node==1)
//					{
//						g.addEdge(node1, node2,new RelationshipEdge<String>(node1, node2, "#b#"));
//					}
//					else
//					{
//						g.addEdge(node2, node1,new RelationshipEdge<String>(node2, node1, "#a#"));
//					}
					break;
				case overlap:
					createOrOverrideEdge(node1,node2,"#o#");
					createOrOverrideEdge(node2,node1,"#o#");
//					g.addEdge(node1, node2,new RelationshipEdge<String>(node1, node2, "#o#"));
//					g.addEdge(node2, node1,new RelationshipEdge<String>(node2, node1, "#o#"));
					break;
				case overlapWithPrev:
					
					Phrase gov_rel_phrase = NormalizedDependencyFeatures.getRelatedPhraseFromNorDepLine
								(dep.firstPart,dep.firstOffset,norSentence.getPhraseNewOffsetMap());
					Phrase prev_phrase = gov_rel_phrase.getPreviousPhrase(ordered_phrases);
					if (prev_phrase!=null)
					{
						int offset = prev_phrase.getNormalOffset()+1;
						node2 = prev_phrase.getNormalizedHead().toLowerCase()+"-"+offset;					
						
						createOrOverrideEdge(node1,node2,"#o#");
						createOrOverrideEdge(node2,node1,"#o#");
						//if get path is not lableed then add o
//						g.addEdge(node1, node2,new RelationshipEdge<String>(node1, node2, "#o#"));
//						g.addEdge(node2, node1,new RelationshipEdge<String>(node2, node1, "#o#"));
					}
					
					break;
				case transition:
//					List<CustomDependencyGraph.RelationshipEdge>  edges_between = DijkstraShortestPath.findPathBetween(g, node1, node2);
//					if (edges_between !=null && edges_between.size()==0)
//					{
						g.addEdge(node1, node2,new RelationshipEdge<String>(node1, node2, "#t#"));
						g.addEdge(node2, node1,new RelationshipEdge<String>(node2, node1, "#t#"));
//					}
					
				case normalGovDep:
//					g.addEdge(node1, node2,new RelationshipEdge<String>(node1, node2, dep.relationName));
//					g.addEdge(node2, node1,new RelationshipEdge<String>(node2, node1, dep.relationName));
					
					createEdgeIfNotExist(node1, node2, dep.relationName);
					createEdgeIfNotExist(node2, node1, dep.relationName);
				}
			}
			else if ( (g.containsVertex(node1) && g.containsVertex(node2))
					)
			{
				g.addVertex(node1);
				g.addVertex(node2);
//				List<RelationshipEdge> from_to_path = 
//					DijkstraShortestPath.findPathBetween(g, node1, node2);
//				if (from_to_path != null)
//				{
//					g.addEdge(node1, node2,new RelationshipEdge<String>(node1, node2, dep.relationName));
//					g.addEdge(node2, node1,new RelationshipEdge<String>(node1, node2, dep.relationName));
					createEdgeIfNotExist(node1, node2, dep.relationName);
					createEdgeIfNotExist(node2, node1, dep.relationName);
//				}
				
			}
			
		}
		// add a set of signals related to each phrase
		for (int i=0;i<ordered_phrases.size()-1;i++)
		{
			Phrase p1 = ordered_phrases.get(i);
			Integer offset = p1.getNormalOffset()+1;
			String node1 = p1.getNormalizedHead().toLowerCase()+"-"+offset;
			Phrase p2 = ordered_phrases.get(i+1);
			Integer offset2 = p2.getNormalOffset()+1;
			String node2 = p2.getNormalizedHead().toLowerCase()+"-"+offset2;
			DependencyLine dep_line_between = StanfordDependencyUtil.getRelatedDependencyBetween
				(p1, p2, norSentence.getNormalizedDependencies());
			if (dep_line_between != null)
			{
				createEdgeIfNotExist(node1, node2, dep_line_between.relationName);
				createEdgeIfNotExist(node2, node1, dep_line_between.relationName);
			}
//			ClinicalEvent event1 = ClinicalEvent.getRelatedEventFromPhrase(p1);
//			ClinicalEvent event2 = ClinicalEvent.getRelatedEventFromPhrase(p2);
//			boolean is_negative = false;
//			
//			if ((event1!= null && event1.getPolarity()==Polarity.NEG) ||
//					(event2!= null && event2.getPolarity()==Polarity.NEG))
//			{
//				is_negative = true;
//			}
			//////////////////////////Add common Patterns Here ////////////////

			Integer predicted_by_high_conf_patterns=-1;
//			
//			GeneralizedSentence generalized_sent =
//				GeneralizedSentence.findInstance(p1,p2, p1.getStartArtifact().getParentArtifact(),GeneralizationModels.TTO);
//			predicted_by_high_conf_patterns  = PatternBasedPredictionTLink.predictByHighConfPatterns
//					(p1,p2,generalized_sent,LinkExampleBuilder.ExperimentGroupTimexEvent);
//			if ( predicted_by_high_conf_patterns ==-1
//					//&& is_negative==false 		
//			)
//			{
//				predicted_by_high_conf_patterns  = PatternBasedPredictionTLink.predictByHighConfPatterns
//				(p1,p2,generalized_sent,LinkExampleBuilder.ExperimentGroupEventEvent);
//			}
		
			
			///////////////////////////////////////////////////////////////////
			GeneralizedSentence gs = GeneralizedSentence.findInstance(p1, p2,
					p1.getStartArtifact().getParentArtifact(), GeneralizationModels.TTPP);
			
			if (gs==null)
			{
				
				LinkType type = PhraseLink.getInstance(p1, p2).getLinkType();
				
				gs = GeneralizedSentence.getInstance(p1, p2,
						p1.getStartArtifact().getParentArtifact(), GeneralizationModels.TTPP,type,for_train);
				FileUtil.logLine("/tmp/nonexistinggeneralized.txt", p1.getPhraseId()+"   "+p2.getPhraseId()+
						" fortrain:"+for_train);
			
			}
			
			String between_pattern = gs.getBetweenMentionsChunk();
			
			Integer max_prob_class =patternToClass.get(between_pattern);
			if(max_prob_class==null)
			{
				max_prob_class = 
					PatternStatisticsFeatures.getLinkTypeWithMaxProb(GeneralizationModels.TTPP, between_pattern);
				patternToClass.put(between_pattern, max_prob_class);
			}
			if (max_prob_class == 1
					|| predicted_by_high_conf_patterns ==1
					)
			{
//				g.addEdge(node1, node2,new RelationshipEdge<String>(node1, node2, "#b#"));
//				g.addEdge(node2, node1,new RelationshipEdge<String>(node2, node1, "#a#"));
				createIfPathNotExist(node1, node2, max_prob_class);
//				createOrOverrideEdge(node1,node2,"#b#");
//				createOrOverrideEdge(node2,node1,"#a#");
			}
			else if (
					max_prob_class == 2
					|| predicted_by_high_conf_patterns ==2
					|| isTreatmentForProblem(p1,p2)
			        || isTreatmentForProblem(p1,p2))
			{
//				g.addEdge(node1, node2,new RelationshipEdge<String>(node1, node2, "#a#"));
//				g.addEdge(node2, node1,new RelationshipEdge<String>(node2, node1, "#b#"));
//				createOrOverrideEdge(node1,node2,"#a#");
//				createOrOverrideEdge(node2,node1,"#b#");
				
				createIfPathNotExist(node1,node2,max_prob_class);
			}
			else if (max_prob_class == 3
//					||
//					|| predicted_by_high_conf_patterns ==3
//					|| isSequenceOfSameEvents(p1,p2)
//					|| PatternBasedPredictionTLink.areSeqOfSameTypeEvents(p1, p2)
//					|| areSeqTreatmentDuration(p1,p2)
//					|| (dep_line_between != null && isSameEventsConjuncted(dep_line_between, p1, p2))
					
					)
			{
//				List<String> edges_between = getPath(p1,p2);
//				if (edges_between.size()==0)
//				{
//					g.addEdge(node1, node2,new RelationshipEdge<String>(node1, node2, "#o#"));
//					g.addEdge(node2, node1,new RelationshipEdge<String>(node2, node1, "#o#"));
//					createOrOverrideEdge(node1,node2,"#o#");
//					createOrOverrideEdge(node2,node1,"#o#");
					createIfPathNotExist(node1,node2,max_prob_class);
//				}
			}
			else if(predicted_by_high_conf_patterns ==3)
			{
				List<String> edges_between = getPath(p1,p2);
				if (edges_between.size()==0)
				{
//					g.addEdge(node1, node2,new RelationshipEdge<String>(node1, node2, "#o#"));
//					g.addEdge(node2, node1,new RelationshipEdge<String>(node2, node1, "#o#"));
//					createOrOverrideEdge(node1,node2,"#o#");
//					createOrOverrideEdge(node2,node1,"#o#");
					createIfPathNotExist(node1,node2,predicted_by_high_conf_patterns);
				}
			}
			
			
//			else
//			{
//				List<RelationshipEdge> from_to_path = 
//					DijkstraShortestPath.findPathBetween(g, node1, node2);
//				if (from_to_path==null)
//				{
//				
//					g.addEdge(node1, node2,new RelationshipEdge<String>(node1, node2, "#nt#"));
//				}
//			}
	
			
		}

	}
	private void overwriteEdge(String node1, String node2, int edgeType) {
		if (edgeType == 1)
		{	
			createOrOverrideEdge(node1,node2,"#b#");
			createOrOverrideEdge(node2,node1,"#a#");
		}
		else if (edgeType == 2)
		{
			createOrOverrideEdge(node1,node2,"#a#");
			createOrOverrideEdge(node2,node1,"#b#");
		}
		else if (edgeType == 3)
		{

			createOrOverrideEdge(node1,node2,"#o#");
			createOrOverrideEdge(node2,node1,"#o#");
		}
		
	}
	private void createIfPathNotExist(String node1, String node2, int edgeType) {
		if (edgeType == 1)
		{	
			createEdgeIfNotExist(node1,node2,"#b#");
			createEdgeIfNotExist(node2,node1,"#a#");
		}
		else if (edgeType == 2)
		{
			createEdgeIfNotExist(node1,node2,"#a#");
			createEdgeIfNotExist(node2,node1,"#b#");
		}
		else if (edgeType == 3)
		{

			createEdgeIfNotExist(node1,node2,"#o#");
			createEdgeIfNotExist(node2,node1,"#o#");
		}
		
	}


	// this will update the lable pf the edge, if it already exist
	private void createOrOverrideEdge(String node1, String node2, String label)
	{
		RelationshipEdge edge = g.getEdge(node1, node2);
		
		if (edge != null)
		{
			g.removeEdge(node1, node2);
			g.addEdge(node1, node2,new RelationshipEdge<String>(node1, node2, label));
		}
		else
		{
			g.addEdge(node1, node2,new RelationshipEdge<String>(node1, node2, label));
		}
	}
	// this will add an edge, only if there is no edge there
	private void createEdgeIfNotExist(String node1, String node2, String label)
	{
		List<RelationshipEdge> from_to_path = 
			DijkstraShortestPath.findPathBetween(g, node1, node2);
		if ( from_to_path == null || from_to_path.isEmpty())
		{
			g.addEdge(node1, node2,new RelationshipEdge<String>(node1, node2, label));
		}
	}

	private String mapToEdgeType(DependencyLine dep) {
		String res = "one_way";
		String relationName = dep.relationName;
		Phrase gov_rel_phrase = NormalizedDependencyFeatures.getRelatedPhraseFromNorDepLine
		(dep.firstPart,dep.firstOffset,norSentence.getPhraseNewOffsetMap());
		
		Phrase dependent_rel_phrase = NormalizedDependencyFeatures.getRelatedPhraseFromNorDepLine
		(dep.secondPart,dep.secondOffset,norSentence.getPhraseNewOffsetMap());
		
		if ((gov_rel_phrase != null && gov_rel_phrase.getPhraseEntityType().equals("TIMEX3") )||
				(dependent_rel_phrase != null && dependent_rel_phrase.getPhraseEntityType().equals("TIMEX3")))
		{
			res = "two_way";
		}
		if(relationName.equals("cc") ||
				relationName.equals("ccomp")||
				relationName.equals("complm") ||
				relationName.startsWith("conj") ||
				relationName.startsWith("csubj")||
				relationName.startsWith("csubjpass")||
				relationName.startsWith("pcomp")||
				relationName.startsWith("csubjpass")||
				relationName.startsWith("prepc") ||
				relationName.startsWith("purpcl") ||
				relationName.startsWith("rcmod") ||
				relationName.startsWith("xcomp") )
			
		{
			res = "two_way";
		}
		return res;
	}
	public static SignalType getSignalType(Phrase phrase1, Phrase phrase2,DependencyLine dep) {
		
		SignalType res = SignalType.unknown;
		
		if (isOverlapSignal(dep, phrase1, phrase2) ||
			isTestRevealing(dep, phrase1, phrase2)||
			isAcionInClinicalDep(dep, phrase1, phrase2) ||
			
			(dep.relationName.equals("tmod")))
		{
			res = SignalType.overlap;
		}
		else if (isForProblemSignal(dep,phrase1,phrase2) ||
				isProblemRevealed(dep, phrase1, phrase2) ||
				
				isDepartmentBeforeDischarge(dep, phrase1, phrase2) ||
				isTreatmentForProblemSignal(dep,phrase1,phrase2)
				|| dep.relationName.equals("prep_after")
				|| dep.relationName.equals("prepc_after"))
		{
			res = SignalType.gov_after_dep;
		}
		else if (isForTreatmentSignal(dep,phrase1,phrase2)
				||isProblemStarting(dep, phrase1, phrase2)
				|| isDepartmentAfterTransfer(dep,phrase1,phrase2)
//				|| isProblemTestRel(dep,phrase1,phrase2)
				|| dep.relationName.equals("prep_before")
				|| dep.relationName.equals("prep_prior_to")
				|| dep.relationName.equals("prepc_prior_to")
				|| dep.relationName.equals("prepc_until")
				|| dep.relationName.equals("prep_until")
				|| dep.relationName.equals("prep_towards"))
		{
			res = SignalType.gov_before_dep;
		}
//		else if (isOverlappingWithPrevious(dep,phrase1,phrase2))
//		{
//			res = SignalType.overlapWithPrev;
//		}
//		else if (isTransition(dep))
//		{
//			res = SignalType.transition;
//		}

		return res;
	}
	
	public static SignalType getBetwenPhraseSignalType(Phrase phrase1, Phrase phrase2,DependencyLine dep) {
		
		SignalType res = SignalType.unknown;
		if (phrase2== null ||phrase1==null )
		{
			return res;
		}
		if (phrase2.getNormalOffset()==-1 ||phrase1.getNormalOffset()==-1 )
		{
			FileUtil.logLine("/tmp/phraseidsWithWrongNormalization.txt", ((Integer)phrase2.getPhraseId()).toString()+" "+
					((Integer)phrase1.getPhraseId()).toString());
			return res;
		}
		String node1 = phrase1.getNormalizedHead().toLowerCase()+"-"+(phrase1.getNormalOffset()+1);
		String node2 = phrase2.getNormalizedHead().toLowerCase()+"-"+(phrase2.getNormalOffset()+1);
		
		node1 = node1.toLowerCase().replaceAll("/", "\\\\/");
		node2 = node2.toLowerCase().replaceAll("/", "\\\\/");
		//especial handling for an exception
		if (node2.equals("magnesium-23") || node1.equals("magnesium-23")
				|| node2.startsWith(".-") || node1.startsWith(".-")
				|| node2.startsWith(";-") || node1.startsWith(";-")
				|| node2.startsWith("or-") || node1.startsWith("or-")
				|| node2.startsWith("for-") || node1.startsWith("for-")
				|| node2.startsWith("out-") || node1.startsWith("out-")
				|| node2.startsWith(":-") || node1.startsWith(":-")
				|| node2.matches("\\W-\\d+") || node1.matches("\\W-\\d+")
				|| node2.matches(".*\\.-\\d+") || node1.matches(".*\\.-\\d+")
				|| node2.matches("phototherapy-18") || node1.matches("phototherapy-18")
				|| node2.matches("which-\\d+") || node1.matches("which-\\d+"))
		{
			FileUtil.logLine("/tmp/phraseidsWithWrongNormalization.txt", ((Integer)phrase2.getPhraseId()).toString()+" "+
					((Integer)phrase1.getPhraseId()).toString());
			return res;
		}
		res =getSignalType(phrase1, phrase2,dep);

		return res;
	}
	private SignalType getSignalType(DependencyLine dep) {
		
		SignalType res = SignalType.unknown;

		Phrase gov_rel_phrase = NormalizedDependencyFeatures.getRelatedPhraseFromNorDepLine
		(dep.firstPart,dep.firstOffset,norSentence.getPhraseNewOffsetMap());
		
		Phrase dependent_rel_phrase = NormalizedDependencyFeatures.getRelatedPhraseFromNorDepLine
		(dep.secondPart,dep.secondOffset,norSentence.getPhraseNewOffsetMap());
		
		res = getSignalType(gov_rel_phrase,dependent_rel_phrase,dep);
		
		if (res.equals(SignalType.unknown))
		{
			if (gov_rel_phrase != null|| dependent_rel_phrase!=null)
			{
				res =SignalType.normalGovDep;
			}
		}
		return res;
	}
	public void setG(DirectedGraph<String, RelationshipEdge> g) {
		this.g = g;
	}


	public DirectedGraph<String, RelationshipEdge> getG() {
		return g;
	}

	public static boolean isForProblemSignal(DependencyLine dep,Phrase gov_phrase,Phrase dep_phrase)
	{
		boolean is_signal = false;
		if (dep_phrase != null && dep.relationName.startsWith("prep_for"))
		{
			if (dep_phrase!= null && dep_phrase.getPhraseEntityType().equals("EVENT"))
			{
				ClinicalEvent rel_event = ClinicalEvent.getRelatedEventFromPhrase(dep_phrase);
				if (rel_event.getEventType().equals(EventType.PROBLEM))
				{
					is_signal = true;
				}
			}
		}
		return is_signal;
	}

	static boolean isSequenceOfSameEvents(Phrase phrase1,Phrase phrase2) throws Exception
	{
		boolean is_seq = false;
		if (phrase1.getPhraseEntityType().equals("EVENT")
				&& phrase2.getPhraseEntityType().equals("EVENT"))
		{
			ClinicalEvent p1_event = ClinicalEvent.getRelatedEventFromPhrase(phrase1);
			EventType p1_event_type = p1_event.getEventType();
			ClinicalEvent p2_event = ClinicalEvent.getRelatedEventFromPhrase(phrase2);
			if (p1_event.getEventType().equals(p2_event.getEventType()) &&
					(p1_event_type.equals(EventType.TEST) || p1_event_type.equals(EventType.TREATMENT)))
			{
				GeneralizedSentence gs = GeneralizedSentence.findInstance(phrase1, phrase2, phrase1.getStartArtifact().getParentArtifact(), GeneralizationModels.TTO);
				if(gs==null) return is_seq;
				String between =  gs.getBetweenMentionsChunkExclusive();
				if (between.matches("\\d+\\s+?\\w+?,?\\s+?"))
				{
					is_seq = true;
				}
			}
		}
		return is_seq;
	}
	public static boolean isTreatmentForProblemSignal(DependencyLine dep,Phrase gov_phrase,Phrase dep_phrase)
	{
		boolean is_signal = false;
		if (dep_phrase != null && gov_phrase != null && dep.relationName.startsWith("prep_for"))
		{
			if (dep_phrase.getPhraseEntityType().equals("EVENT")
					&& gov_phrase.getPhraseEntityType().equals("EVENT"))
			{
				ClinicalEvent rel_event = ClinicalEvent.getRelatedEventFromPhrase(dep_phrase);
				ClinicalEvent gov_rel_event = ClinicalEvent.getRelatedEventFromPhrase(gov_phrase);
				if (rel_event.getEventType().equals(EventType.PROBLEM) && 
						gov_rel_event.getEventType().equals(EventType.TREATMENT))
				{
					is_signal = true;
				}
			}
		}
		return is_signal;
	}
	public static boolean isProblemTestRel(DependencyLine dep,Phrase gov_phrase,Phrase dep_phrase)
	{
		boolean is_signal = false;
		if (dep_phrase != null && gov_phrase != null)
		{
			if (dep_phrase.getPhraseEntityType().equals("EVENT")
					&& gov_phrase.getPhraseEntityType().equals("EVENT"))
			{
				ClinicalEvent dep_event = ClinicalEvent.getRelatedEventFromPhrase(dep_phrase);
				ClinicalEvent gov_rel_event = ClinicalEvent.getRelatedEventFromPhrase(gov_phrase);
				if (dep_event.getEventType().equals(EventType.TEST) && 
						gov_rel_event.getEventType().equals(EventType.PROBLEM))
				{
					is_signal = true;
				}
			}
		}
		return is_signal;
	}
//	prep_in(received-6, Department-2)
	public static boolean isAcionInClinicalDep(DependencyLine dep,Phrase gov_phrase,Phrase dep_phrase)
	{
		boolean is_signal = false;
		if (dep_phrase != null && dep.relationName.equals("prep_in"))
		{
			if (dep_phrase.getPhraseEntityType().equals("EVENT") &&
					ClinicalEvent.getRelatedEventFromPhrase(dep_phrase).getEventType().equals(EventType.CLINICAL_DEPT))
			{
				
				is_signal = true;
				
			}
		}
		return is_signal;
	}
	public static boolean isTransition(DependencyLine dep)
	{
		boolean is_signal = false;
		if(	dep.relationName.equals("complm") ||
//				dep.relationName.startsWith("conj") ||
				dep.relationName.startsWith("csubj")||
				dep.relationName.startsWith("xcomp")||
				dep.relationName.startsWith("csubjpass")||
				dep.relationName.startsWith("csubjpass")||
//				dep.relationName.startsWith("prepc")|| 
				dep.relationName.startsWith("rcmod") ||
				dep.relationName.startsWith("advcl")
				  )

			
		{
			is_signal = true;
		}
		return is_signal;
	}
	public static boolean isForTreatmentSignal(DependencyLine dep,Phrase gov_phrase,Phrase dep_phrase)
	{
		boolean is_signal = false;
		if (dep_phrase != null && dep.relationName.startsWith("prep_for"))
		{
			if (dep_phrase!= null && dep_phrase.getPhraseEntityType().equals("EVENT"))
			{
				ClinicalEvent rel_event = ClinicalEvent.getRelatedEventFromPhrase(dep_phrase);
				if (rel_event.getEventType().equals(EventType.TREATMENT))
				{
					is_signal = true;
				}
			}
		}
		return is_signal;
	}

	public static boolean isOverlapSignal(DependencyLine dep,Phrase gov_phrase,Phrase dep_phrase)
	{
		boolean is_signal = false;
		if (dep_phrase != null &&
				(dep.relationName.startsWith("prep_on")
				|| dep.relationName.startsWith("prep_in")
				|| dep.relationName.startsWith("prep_at")
				|| dep.relationName.startsWith("prep_during")
				|| dep.relationName.startsWith("tmod")
				|| dep.relationName.startsWith("advmod")))
		{
			if (dep_phrase!= null && dep_phrase.getPhraseEntityType().equals("TIMEX3"))
			{
				is_signal = true;
			}
		}
		else if (gov_phrase!= null &&
				dep.relationName.startsWith("prep_of"))
		{
			if (gov_phrase.getPhraseEntityType().equals("TIMEX3"))
			{
				is_signal = true;
			}
		}
		return is_signal;
	}
	public static boolean isOverlappingWithPrevious(DependencyLine dep,Phrase phrase1,Phrase phrase2)
	{
		boolean is_signal = false;
		
		if (dep.relationName.startsWith("advmod") &&
				phrase1 != null &&
				overlap_withprev_list.contains(dep.secondPart.toLowerCase()))
		{
	
			is_signal = true;
			
		}
		return is_signal;
	}
	public static boolean isSameEventsConjuncted(DependencyLine dep,Phrase phrase1,Phrase phrase2)
	{
		boolean is_signal = false;
		if (dep.relationName.startsWith("conj_and") &&
				phrase1.getPhraseEntityType().equals("EVENT")
				&& phrase2.getPhraseEntityType().equals("EVENT"))
		{
			ClinicalEvent p1_event = ClinicalEvent.getRelatedEventFromPhrase(phrase1);
			EventType p1_event_type = p1_event.getEventType();
			ClinicalEvent p2_event = ClinicalEvent.getRelatedEventFromPhrase(phrase2);
			if (p1_event.getEventType().equals(p2_event.getEventType()) &&
					(p1_event_type.equals(EventType.TEST) || p1_event_type.equals(EventType.TREATMENT)))
			{
				is_signal = true;
			}
		}
		return is_signal;
	}
	public static boolean isTreatmentForProblem(Phrase phrase1,Phrase phrase2) throws SQLException
	{
		boolean is_signal = false;
		if (phrase1.getPhraseEntityType().equals("EVENT")
				&& phrase2.getPhraseEntityType().equals("EVENT"))
		{
			ClinicalEvent p1_event = ClinicalEvent.getRelatedEventFromPhrase(phrase1);
			EventType p1_event_type = p1_event.getEventType();
			ClinicalEvent p2_event = ClinicalEvent.getRelatedEventFromPhrase(phrase2);
			String prep = ParseDependencyFeatures.getRelPrepositionToPhrase(phrase2,null);
			if (prep!= null && p1_event_type.equals(EventType.TREATMENT) && p2_event.getEventType().equals(EventType.PROBLEM))
			{
				if(prep.equals("for"))
				{
					is_signal = true;
				}
				
			}
		}
		return is_signal;
	}
	//This applies just for consequent phrases
	public static boolean areSeqTreatmentDuration(Phrase phrase1,Phrase phrase2) throws SQLException
	{
		boolean is_signal = false;
		if (phrase1.getPhraseEntityType().equals("EVENT")
				&& phrase2.getPhraseEntityType().equals("Timex3"))
		{
			ClinicalEvent p1_event = ClinicalEvent.getRelatedEventFromPhrase(phrase1);
			EventType p1_event_type = p1_event.getEventType();
			TimexPhrase p2_timex = TimexPhrase.getRelatedTimexFromPhrase(phrase2);
			TimexType p2_type = p2_timex.getTimexType();
		
			if ( p1_event_type.equals(EventType.TREATMENT) &&
					(p2_type.equals(TimexType.DURATION) || p2_type.equals(TimexType.FREQUENCY)) )
			{
				
				is_signal = true;
				
			}
		}
		return is_signal;
	}
	//dedine revealed list
	//TODO complete
	public static ArrayList<String> revealed_list = new ArrayList<String>(
		    Arrays.asList("revealed", "showed","found","demonstrated"));
	public static ArrayList<String> start_list = new ArrayList<String>(
		    Arrays.asList("developed", "started"));
	public static ArrayList<String> admission_list = new ArrayList<String>(
		    Arrays.asList("presented", "admitted"));
	public static ArrayList<String> overlap_withprev_list = new ArrayList<String>(
		    Arrays.asList("where", "when"));
	
	public static boolean isProblemRevealed(DependencyLine dep,Phrase gov_phrase,Phrase dep_phrase)
	{
		boolean is_signal = false;
		if (dep_phrase != null)
		{
			if (revealed_list.contains(dep.firstPart.toLowerCase()) &&
					dep_phrase!= null && dep_phrase.getPhraseEntityType().equals("EVENT"))
			{
				ClinicalEvent rel_event = ClinicalEvent.getRelatedEventFromPhrase(dep_phrase);
				if (rel_event.getEventType().equals(EventType.PROBLEM))
				{
					is_signal = true;
				}
			}		
		}
		return is_signal;
	}
	public static boolean isProblemStarting(DependencyLine dep,Phrase gov_phrase,Phrase dep_phrase)
	{
		boolean is_signal = false;
		if (dep_phrase != null)
		{
			if (start_list.contains(dep.firstPart.toLowerCase()) &&
					dep_phrase!= null && dep_phrase.getPhraseEntityType().equals("EVENT"))
			{
				ClinicalEvent rel_event = ClinicalEvent.getRelatedEventFromPhrase(dep_phrase);
				if (rel_event.getEventType().equals(EventType.PROBLEM))
				{
					is_signal = true;
				}
			}		
		}
		return is_signal;
	}
	public static boolean isTestRevealing(DependencyLine dep,Phrase gov_phrase,Phrase dep_phrase)
	{
		boolean is_signal = false;
		if (dep_phrase != null)
		{
			if (revealed_list.contains(dep.firstPart.toLowerCase()) &&
					dep_phrase!= null && dep_phrase.getPhraseEntityType().equals("EVENT"))
			{
				ClinicalEvent rel_event = ClinicalEvent.getRelatedEventFromPhrase(dep_phrase);
				if (rel_event.getEventType().equals(EventType.TEST))
				{
					is_signal = true;
				}
			}		
		}
		return is_signal;
	}
	public static boolean isDepartmentAfterAddmission(DependencyLine dep,Phrase gov_phrase,Phrase dep_phrase)
	{
		boolean is_signal = false;
		if (dep_phrase != null)
		{
			if (admission_list.contains(dep.firstPart.toLowerCase()) &&
					dep_phrase!= null && dep_phrase.getPhraseEntityType().equals("EVENT"))
			{
				ClinicalEvent rel_event = ClinicalEvent.getRelatedEventFromPhrase(dep_phrase);
				if (rel_event.getEventType().equals(EventType.CLINICAL_DEPT))
				{
					is_signal = true;
				}
			}		
		}
		return is_signal;
	}
	public static boolean isDepartmentAfterTransfer(DependencyLine dep,Phrase gov_phrase,Phrase dep_phrase)
	{
		boolean is_signal = false;
		if (dep_phrase != null)
		{
			String gov_content = StringUtil.getWordLemma(dep.firstPart);
			if (dep_phrase!= null && dep_phrase.getPhraseEntityType().equals("EVENT") && 
					(dep.relationName.equals("prep_to") || 
							(dep_phrase.getStartArtifact().getPreviousArtifact() != null && 
							dep_phrase.getStartArtifact().getPreviousArtifact().getContent().equals("to"))) &&
					(gov_content.equals("transfer") || gov_content.equals("transition")))
			{
				ClinicalEvent rel_event = ClinicalEvent.getRelatedEventFromPhrase(dep_phrase);
				if (rel_event.getEventType().equals(EventType.CLINICAL_DEPT))
				{
					is_signal = true;
				}
			}		
		}
		return is_signal;
	}
	public static boolean isDepartmentBeforeDischarge(DependencyLine dep,Phrase gov_phrase,Phrase dep_phrase)
	{
		boolean is_signal = false;
		if (dep_phrase != null)
		{
			String gov_content = StringUtil.getWordLemma(dep.firstPart);
			if (dep_phrase!= null)
			{
				Artifact previous_preposition = dep_phrase.getStartArtifact().getPreviousArtifact();
				if ((dep.relationName.equals("prep_from") || dep.relationName.equals("out_of") ||
						(previous_preposition != null && previous_preposition.getContent().equals("from"))) &&
						dep_phrase!= null && dep_phrase.getPhraseEntityType().equals("EVENT") && 
						(gov_content.equals("discharge") || gov_content.equals("transfer")))
				{
					ClinicalEvent rel_event = ClinicalEvent.getRelatedEventFromPhrase(dep_phrase);
					if (rel_event.getEventType().equals(EventType.CLINICAL_DEPT))
					{
						is_signal = true;
					}
				}
			}
			
					
		}
		return is_signal;
	}
	public Integer reasonBasenOnPathBetween(Phrase phrase1, Phrase phrase2)
	{
		Integer result = -1;
		List<String> edges_between = getPath(phrase1,phrase2);
		if (edges_between.size()==0)
		{
			result = -1;
			return result;
		}
		//check is there is consistant signals there
		boolean has_after_signal = false;
		boolean has_before_signal = false;
		boolean has_overlap_signal = false;
		boolean has_transition_signal = false;
		
		boolean has_nt_signal = false;
		
		if (edges_between.contains("#a#"))
		{
			has_after_signal = true;
			
		}
		if (edges_between.contains("#b#"))
		{
			has_before_signal = true;
			
		} 
		if (edges_between.contains("#o#"))
		{
			has_overlap_signal = true;
		}
//		if (edges_between.contains("#t#"))
//		{
//			has_transition_signal = true;
//		}
		if (edges_between.contains("#nt#"))
		{
			has_nt_signal = true;
		}
		
//		if (has_after_signal &&
//				!has_before_signal && !has_transition_signal && !has_nt_signal)
		if (has_after_signal &&
					!has_before_signal )
		{
			result = LinkType.AFTER.ordinal();
			return result;
		}
		if (has_before_signal  &&
				!has_after_signal  )
		{
			result = LinkType.BEFORE.ordinal();
			return result;
		}
		if (has_overlap_signal && !has_after_signal  &&
				!has_before_signal )
		{
			result = LinkType.OVERLAP.ordinal();
			return result;
		}
		
		return result;
	}
	public enum SignalType {
		overlap,
		gov_before_dep,
		gov_after_dep,
		transition,
		unknown, overlapWithPrev, normalGovDep
	}
	
	public static class RelationshipEdge<V> extends DefaultEdge {
	    private V v1;
	    private V v2;
	    private String label;

	    public RelationshipEdge(V v1, V v2, String label) {
	        this.v1 = v1;
	        this.v2 = v2;
	        this.label = label;
	    }

	    public V getV1() {
	        return v1;
	    }

	    public V getV2() {
	        return v2;
	    }

	    public String toString() {
	        return label;
	    }
	}

}

