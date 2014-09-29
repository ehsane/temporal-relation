package rainbownlp.i2b2.sharedtask2012;

import java.util.HashMap;
import java.util.List;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultDirectedGraph;

import rainbownlp.analyzer.evaluation.classification.Evaluator;
import rainbownlp.core.Artifact;
import rainbownlp.core.Phrase;
import rainbownlp.core.PhraseLink;
import rainbownlp.core.PhraseLink.LinkType;
import rainbownlp.core.graph.GraphEdge;
import rainbownlp.i2b2.sharedtask2012.analyzer.NormalizationMethod.MethodType;
import rainbownlp.i2b2.sharedtask2012.analyzer.NormalizedSentence;
import rainbownlp.machinelearning.MLExample;
import rainbownlp.parser.DependencyLine;
import rainbownlp.util.HibernateUtil;

public class TemporalGraph {
	

	public static void main(String[] args) throws Exception
	{
		List<MLExample> examples = 
			MLExample.getExamplesByDocument(SharedConstants.EXPERIMENT_GROUP_TIMEXEVENT, true,1);
	
		int counter = 0;
		int example_counter = 0;
		HashMap<Integer, TemporalGraph> cache = new HashMap<Integer, TemporalGraph>();
		for(MLExample example : examples)
		{
			Artifact sentence = example.getRelatedPhraseLink().getFromPhrase().
				getStartArtifact().getParentArtifact();
			TemporalGraph tgraph = 
				cache.get(sentence.getArtifactId());
			if(tgraph == null)
			{
				tgraph = new TemporalGraph(sentence);
				cache.put(sentence.getArtifactId(), tgraph);
			}
			PhraseLink plinke = example.getRelatedPhraseLink();
			LinkType linkType =
				tgraph.getLinkType(plinke.getFromPhrase(), 
						plinke.getToPhrase());
			int predicted = -1;
			if(linkType!=LinkType.UNKNOWN)
				predicted = linkType.ordinal();
			example.setPredictedClass(predicted);
			HibernateUtil.save(example);
		}
		
		Evaluator.getEvaluationResult(examples).printResult();
	}
	
	Artifact sentence;
	NormalizedSentence norSentence;
	DirectedGraph<String, GraphEdge> g =
         new DefaultDirectedGraph<String, GraphEdge>(GraphEdge.class);

	public TemporalGraph(Artifact p_sentence)
	{
		sentence = p_sentence;
		norSentence = NormalizedSentence.getInstance(sentence, MethodType.MentionToHead);
		makeTemporalGraph();
	}

	public LinkType getLinkType(Phrase phrase1, Phrase phrase2)
	{
		LinkType res = LinkType.UNKNOWN;
		String node1 = phrase1.getNormalizedHead()+"-"+(phrase1.getNormalOffset()+1);
		String node2 = phrase2.getNormalizedHead()+"-"+(phrase2.getNormalOffset()+1);
		node1 = node1.toLowerCase().replaceAll("/", "\\\\/");
		node2 = node2.toLowerCase().replaceAll("/", "\\\\/");
		List<GraphEdge> from_to_path = 
			DijkstraShortestPath.findPathBetween(g, node1, node2);
		List<GraphEdge> to_from_path = 
			DijkstraShortestPath.findPathBetween(g, node2, node1);
		
		
		if(from_to_path !=null && from_to_path.size()>0)
			if(to_from_path!=null && 
				to_from_path.size()>0)
			{
				//its overlap
				res = LinkType.OVERLAP;
			}else
			{
				res = LinkType.BEFORE;
			}
		else
			if(to_from_path!=null && to_from_path.size()>0)
			{
				res = LinkType.AFTER;
			}
		return res;
	}
	private void makeTemporalGraph() {
		g = new DefaultDirectedGraph<String, GraphEdge>(GraphEdge.class);
		List<DependencyLine> deps = norSentence.getNormalizedDependencies();
		for(DependencyLine dep : deps)
		{
			LinkType linkType = mapToTemporal(dep);
			String node1 = dep.firstPart+"-"+dep.firstOffset;
			String node2 = dep.secondPart+"-"+dep.secondOffset;
			
			g.addVertex(node1);
			g.addVertex(node2);
			
			switch (linkType) {
			case AFTER:
				g.addEdge(node2, node1);
				break;
			case BEFORE:
				g.addEdge(node1, node2);
				break;
			case OVERLAP:
				g.addEdge(node1, node2);
				g.addEdge(node2, node1);
				break;
			}
		}
	}

	private LinkType mapToTemporal(DependencyLine dep) {
		LinkType res = LinkType.UNKNOWN;
		String relationName = dep.relationName;
		if(relationName.equals("prep_to") ||
				relationName.equals("det") ||
				relationName.equals("appos") ||
				relationName.equals("conj_and")
				)
		{
			res = LinkType.OVERLAP;
			
		}
//			if(SemanticSimilarity.getSimilarity(dep.firstPart, "response") > 0.9)
//				res = LinkType.AFTER;
//				
//		}
		
		return res;
	}
}
