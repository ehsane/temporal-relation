package rainbownlp.i2b2.sharedtask2012.patternminer;

import java.util.HashMap;
import java.util.List;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.ClassBasedEdgeFactory;
import org.jgrapht.graph.SimpleGraph;

import rainbownlp.core.Artifact;
import rainbownlp.core.Phrase;
import rainbownlp.core.PhraseLink;
import rainbownlp.i2b2.sharedtask2012.CustomDependencyGraph.RelationshipEdge;
import rainbownlp.i2b2.sharedtask2012.SharedConstants;
import rainbownlp.i2b2.sharedtask2012.analyzer.NormalizationMethod.MethodType;
import rainbownlp.i2b2.sharedtask2012.analyzer.NormalizedSentence;
import rainbownlp.machinelearning.MLExample;
import rainbownlp.parser.DependencyLine;

// This uses  Stanford Dependencies (SD)  file
public class SimpleLabledTree {
	public static HashMap<Integer, SimpleLabledTree> cache = new HashMap<Integer, SimpleLabledTree>();
	public static void main(String[] args) throws Exception
	{
		List<MLExample> examples = 
			MLExample.getExamplesByDocument(SharedConstants.EXPERIMENT_GROUP_EVENTEVENT, true,95);
	
		int counter = 0;
		int example_counter = 0;
		
		for(MLExample example : examples)
		{
			Artifact sentence = example.getRelatedPhraseLink().getFromPhrase().
				getStartArtifact().getParentArtifact();
			SimpleLabledTree lgraph = 
				cache.get(sentence.getArtifactId());
			if(lgraph == null)
			{
				lgraph = new SimpleLabledTree(sentence);
				cache.put(sentence.getArtifactId(), lgraph);
			}
			PhraseLink plinke = example.getRelatedPhraseLink();
			Phrase phrase1 = plinke.getFromPhrase();
			Phrase phrase2 = plinke.getToPhrase();
			
			List<RelationshipEdge> path = lgraph.getLabeledTreePath(phrase1, phrase2);
			 
		}
	}
	Artifact sentence;
	NormalizedSentence norSentence;
	List<Phrase> ordered_phrases;
	private UndirectedGraph<String, RelationshipEdge> sentenceTree =
		  new SimpleGraph<String, RelationshipEdge>(
                  new ClassBasedEdgeFactory<String, RelationshipEdge>(RelationshipEdge.class));

	
	public SimpleLabledTree(Artifact pSentence) throws Exception
	{
		sentence = pSentence;
		norSentence = NormalizedSentence.getInstance(sentence, MethodType.MentionToHead);
		ordered_phrases = Phrase.getOrderedPhrasesInSentence(sentence);
		makeTrees();
	}

	private void makeTrees() {
		List<Phrase> p_list = Phrase.getOrderedPhrasesInSentence(sentence);
		for (Phrase p:p_list)
		{
			String node1 = p.getNormalizedHead().toLowerCase()+"-"+(p.getNormalOffset()+1);
			
			if(!sentenceTree.containsVertex(node1))
				sentenceTree.addVertex(node1);
		}
		for(DependencyLine depLine:norSentence.getNormalizedDependencies())
		{
			String node1 = depLine.firstPart.toLowerCase()+"-"+depLine.firstOffset;
			String node2 = depLine.secondPart.toLowerCase()+"-"+depLine.secondOffset;
			
			if(!sentenceTree.containsVertex(node1))
				sentenceTree.addVertex(node1);
			if(!sentenceTree.containsVertex(node2))
				sentenceTree.addVertex(node2);
			if (node1.equals(node2))
				continue;
			//make link v1 & v2
			sentenceTree.addEdge(node1, 
					node2,
					new RelationshipEdge<String>(node1, node2, depLine.relationName));
		}
	}
	

	public List<RelationshipEdge> getLabeledTreePath(Phrase phrase1, Phrase phrase2) {
		
		List<RelationshipEdge> path = null;
	
		String node1 = phrase1.getNormalizedHead().toLowerCase()+"-"+(phrase1.getNormalOffset()+1);
		String node2 = phrase2.getNormalizedHead().toLowerCase()+"-"+(phrase2.getNormalOffset()+1);
//		node1 = node1.toLowerCase().replaceAll("/", "\\\\/");
//		node2 = node2.toLowerCase().replaceAll("/", "\\\\/");
		//link tokens
		 try {
//			int word1Offset = headWord1.getWordIndex()+1;
//			int word2Offset = headWord2.getWordIndex()+1;
			
			path = 
				DijkstraShortestPath.findPathBetween(sentenceTree, 
						node1, node2);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("error in getParseTreeDistance:"+node1+node2);
		}
		
		return path;
	}
}
