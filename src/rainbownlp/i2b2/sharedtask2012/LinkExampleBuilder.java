package rainbownlp.i2b2.sharedtask2012;

import java.util.ArrayList;
import java.util.List;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultDirectedGraph;

import rainbownlp.core.Artifact;
import rainbownlp.core.Artifact.Type;
import rainbownlp.core.Phrase;
import rainbownlp.core.PhraseLink;
import rainbownlp.core.PhraseLink.LinkType;
import rainbownlp.core.Setting;
import rainbownlp.core.graph.GraphEdge;
import rainbownlp.i2b2.sharedtask2012.ClinicalEvent.EventType;
import rainbownlp.i2b2.sharedtask2012.ClinicalEvent.Modality;
import rainbownlp.i2b2.sharedtask2012.TimexPhrase.TimexType;
import rainbownlp.i2b2.sharedtask2012.featurecalculator.link.CustomDependencyGraphFeatures;
import rainbownlp.i2b2.sharedtask2012.featurecalculator.link.LinkArgumentBasicFeatures;
import rainbownlp.i2b2.sharedtask2012.featurecalculator.link.NormalizedDependencyFeatures;
import rainbownlp.i2b2.sharedtask2012.featurecalculator.link.PatternStatisticsFeatures;
import rainbownlp.i2b2.sharedtask2012.ruleengines.SecTimeEventUtils;
import rainbownlp.machinelearning.IFeatureCalculator;
import rainbownlp.machinelearning.MLExample;
import rainbownlp.machinelearning.featurecalculator.link.ConceptsBetweenWords;
import rainbownlp.machinelearning.featurecalculator.link.LinkGeneralFeatures;
import rainbownlp.machinelearning.featurecalculator.link.ParseDependencyFeatures;
import rainbownlp.machinelearning.featurecalculator.link.ParseTreeFeatures;
import rainbownlp.machinelearning.featurecalculator.sentence.SentenceNGram;
import rainbownlp.machinelearning.featurecalculator.sentence.SentenceSyntax;
import rainbownlp.util.FileUtil;
import rainbownlp.util.HibernateUtil;

public class LinkExampleBuilder {
	public final static String ExperimentGroupTimexEvent = "LinkClassificationTimexEvent";
	public final static String ExperimentGroupEventEvent = "LinkClassificationEventEvent";
	public final static String ExperimentGroupBetweenSentence = "LinkClassificationBetweenSentence";
	
	public static void main(String[] args) throws Exception
	{
//		LinkExampleBuilder.createBetweenSentenceLinkExamples();
		LinkExampleBuilder.createInSentenceLinkExamples(true);
		
//		calculateClousure(true);
	}
	
	public static void createBetweenSentenceLinkExamples(boolean for_train) throws Exception {
		List<Artifact> sentences = 
				Artifact.listByType(Artifact.Type.Sentence,for_train);
		
		
		for(int i=0;i<sentences.size()-1;i++)
		{
			MLExample.hibernateSession = HibernateUtil.clearSession(MLExample.hibernateSession);
			
			Artifact sentence1 = sentences.get(i);
			if(sentence1.getContent().toLowerCase().trim().matches("(admission)|(discharge).*:"))
			{
				//skip date also
				i++;
				continue;
			}
			FileUtil.logLine(null, "createBetweenSentenceLinkExamples----------Sentence processed"+i);
			Artifact sentence2 = sentence1.getPreviousArtifact();
			while(sentence2!=null)
			{
				createBetweenSentenceLinkExamples(sentence2,sentence1);
				sentence2 = sentence2.getPreviousArtifact();
			}
		}
	}

	static List<String> timexPrepositionTriggers = new ArrayList<String>();
	static {
		timexPrepositionTriggers.add("on");
		timexPrepositionTriggers.add("at");
		timexPrepositionTriggers.add("in");
	}
	
	static public void createBetweenSentenceLinkExamples(Artifact sentence1, 
			Artifact sentence2) throws Exception
	{
		
		List<IFeatureCalculator> featureCalculators = 
				new ArrayList<IFeatureCalculator>();

		featureCalculators.add(new SentenceNGram());
		featureCalculators.add(new SentenceSyntax());

		
		int counter = 0;
		int example_counter = 0;
		List<Phrase> phrases1 = Phrase.getPhrasesInSentence(sentence1);
		List<Phrase> phrases2 = Phrase.getPhrasesInSentence(sentence2);
		if(phrases1.size()==0 || phrases2.size()==0) return;
		
		String sentencePattern1 = SentencePattern.getGeneralizedContent(sentence1);
		if(sentencePattern1.matches("(<event_test> \\$num\\$( ; ?)?)+"))
			//all test results?
			//then crete examples between all tests
			linkAllPhrases(phrases1, LinkType.OVERLAP, featureCalculators);
		
		String sentencePattern2 = SentencePattern.getGeneralizedContent(sentence2);
		if(sentencePattern2.matches("(<event_test> \\$num\\$( ; ?)?)+"))
			//all test results?
			//then crete examples between all tests
			linkAllPhrases(phrases2, LinkType.OVERLAP, featureCalculators);
		
		if((sentence2.getLineIndex()-sentence1.getLineIndex())<2)
		{
			for(int i=phrases1.size()-1;i>-1;i--)
			{
				Phrase phrase1 = phrases1.get(i);
				if(sentencePattern2.matches("(<event_test> \\$num\\$( ; ?)?).*"))
				{
					String content2 = phrases2.get(0).getPhraseContent().toLowerCase().trim();
					String content1 = phrase1.getPhraseContent().toLowerCase().trim();
					//link to the previous treatment
					if(phrase1.getPhraseEntityType().equals("EVENT") &&
									!sentence2.getContent().toLowerCase().matches(".*repeat "+content2+".*")) 
							
					{
						createBetweenSentenceExample(phrase1, phrases2.get(0), featureCalculators
								, Setting.RuleSureCorpus, LinkType.OVERLAP);
						break;
					}
				}
			}
		}
		String related_sec1 = null;
		String related_sec2 = null;
		
		for(int i=phrases1.size()-1;i>-1;i--)
		{
			Phrase phrase1 = phrases1.get(i);
				
			if(related_sec1==null)
				related_sec1 = 
					SecTimeEventUtils.getEventRelatedSection
						(sentence1.getParentArtifact(),phrase1 );
			
			if(related_sec2==null)
				related_sec2 = 
					SecTimeEventUtils.getEventRelatedSection
						(sentence2.getParentArtifact(),phrases2.get(0) );
		
			if(!related_sec1.equals(related_sec2)) return;
			
			

			
			String content1 = phrase1.getPhraseContent().toLowerCase().trim();
			String content2 = phrases2.get(0).getPhraseContent().toLowerCase().trim();
			
			//one phrase in the sentence,link with the first phrase in previous sentence
			if((sentence2.getLineIndex()-sentence1.getLineIndex())<2 &&
					phrases2.size()==1 &&
					related_sec1.equals("hos") &&
					!content2.matches("repeat .*"))
			{
				if(phrase1.getPhraseEntityType().equals("EVENT"))
				{
					ClinicalEvent e1 = ClinicalEvent.getRelatedEventFromPhrase(phrase1);
					if(phrases2.get(0).getPhraseEntityType().equals("EVENT"))
					{
						ClinicalEvent e2 = ClinicalEvent.getRelatedEventFromPhrase(phrases2.get(0));
						if((e1.getEventType()==EventType.TREATMENT
								|| e1.getEventType()==EventType.CLINICAL_DEPT
								|| e1.getEventType()==EventType.TEST
								) &&
								e2.getEventType()==EventType.TEST)
						{
							createBetweenSentenceExample(phrase1, phrases2.get(0), featureCalculators
									, Setting.RuleSureCorpus, LinkType.OVERLAP);
							break;
						}
					}else
					{
						TimexPhrase t2 = TimexPhrase.getRelatedTimexFromPhrase(phrases2.get(0));
						if(e1.getEventType()==EventType.TREATMENT &&
								t2.getTimexType()==TimexType.DURATION)
						{
							createBetweenSentenceExample(phrase1, phrases2.get(0), featureCalculators
									, Setting.RuleSureCorpus, LinkType.OVERLAP);
							break;
						}
					}
				}
			}
			else
				for(int j=phrases2.size()-1;j>-1;j--)
				{
					Phrase phrase2 = phrases2.get(j);
					content2 = phrase2.getPhraseContent().toLowerCase().trim();
	
					if(
					//equal content
							content1.equals(content2)
					//a-the relation
							|| content1.replace("a ", "").equals(content2.replace("the ", ""))
							)
						createBetweenSentenceExample(phrase1, phrase2, featureCalculators
								, ExperimentGroupBetweenSentence, LinkType.OVERLAP);
					
					//repeat something? try to find what was repeated
					if(content2.matches("repeat .*") && phrase1.getPhraseEntityType().equals("EVENT"))
					{
						ClinicalEvent e2 = ClinicalEvent.getRelatedEventFromPhrase(phrase2);
						
						if(e2.getModality()==Modality.FACTUAL)
						{
							boolean matched = false;
							for(int k=j-1;k>-1;k--)
							{
								Phrase phrase_tmp = phrases2.get(k);
								String content_tmp = phrase_tmp.getPhraseContent().toLowerCase().trim();
								if(content2.matches("repeat "+content_tmp))
								{
										createBetweenSentenceExample(phrase_tmp, phrase2, featureCalculators
												, Setting.RuleSureCorpus, LinkType.BEFORE);
										matched = true;
										break;
								}					
							}
									
							if(!matched &&
									(sentence2.getLineIndex()-sentence1.getLineIndex())<5 &&
									content2.matches("repeat "+content1))
									createBetweenSentenceExample(phrase1, phrase2, featureCalculators
											, Setting.RuleSureCorpus, LinkType.BEFORE);
						}
					}
						
							
				}
			
		}

	}
	
	private static void linkAllPhrases(List<Phrase> phrases, LinkType linkType
			,List<IFeatureCalculator> featureCalculators) {
		for(int i=0;i<phrases.size()-1;i++)
		{
			Phrase phrase1 = phrases.get(i);
			Phrase phrase2 = phrases.get(i+1);
			createBetweenSentenceExample(phrase1, phrase2, featureCalculators
					, Setting.RuleSureCorpus, linkType);
		}
	}

	private static void createBetweenSentenceExample(Phrase phrase1,
			Phrase phrase2, List<IFeatureCalculator> featureCalculators,
			String corpusName, LinkType predicted) {
		
		String content1 = phrase1.getPhraseContent().toLowerCase().trim();
		String content2 = phrase2.getPhraseContent().toLowerCase().trim();
		
		if(content2.matches("repeat .*") && !content2.matches("repeat "+content1))
			return;
		
		PhraseLink p1_p2_link = 
				PhraseLink.getInstance(phrase1, 
						phrase2);
		
		PhraseLink p2_p1_link = 
				PhraseLink.getInstance(phrase2,
						phrase1);
		
		
		int expected_class = p1_p2_link.getLinkType().ordinal();
		if(p2_p1_link.getLinkType()!=LinkType.UNKNOWN)
		{//reverse link annotated?
			expected_class = 
					p2_p1_link.getLinkType().ordinal();
			
			//now reverse it
			if(expected_class==2)
				expected_class=1;
			else if(expected_class==1)
				expected_class=2;
		}
		
		MLExample link_example = 
				MLExample.getInstanceForLink(p1_p2_link, corpusName);
		link_example.setExpectedClass(expected_class);
		link_example.setRelatedPhraseLink(p1_p2_link);
			
	    link_example.setPredictedClass(predicted.ordinal());
	
		
		if(phrase1.getStartArtifact().getAssociatedFilePath().contains("/train/"))
			link_example.setForTrain(true);
		else
			link_example.setForTrain(false);
		
		MLExample.saveExample(link_example);

//		link_example.calculateFeatures(featureCalculators);
		
	}
	public static void calculateEventAndTimexEventFeatures(boolean for_train) throws Exception
	{
		List<IFeatureCalculator> timexEventFeatureCalculators = 
			new ArrayList<IFeatureCalculator>();
	
		timexEventFeatureCalculators.add(new LinkArgumentBasicFeatures());
		timexEventFeatureCalculators.add(new LinkGeneralFeatures());
		timexEventFeatureCalculators.add(new ConceptsBetweenWords());
	//	timexEventFeatureCalculators.add(new ConceptsDistance());
//	
//		timexEventFeatureCalculators.add(new NormalizedNGrams());
		timexEventFeatureCalculators.add(new NormalizedDependencyFeatures());
//		timexEventFeatureCalculators.add(new NonNormalizedNGrams());
		
		
		timexEventFeatureCalculators.add(new ParseDependencyFeatures());
//		timexEventFeatureCalculators.add(new ParseTreeFeatures());
		
		timexEventFeatureCalculators.add(new CustomDependencyGraphFeatures());
		timexEventFeatureCalculators.add(new ParseTreeFeatures());;
		
		timexEventFeatureCalculators.add(new PatternStatisticsFeatures());

		List<IFeatureCalculator> eventEventFeatureCalculators = 
			new ArrayList<IFeatureCalculator>();
		eventEventFeatureCalculators.addAll(timexEventFeatureCalculators);
		MLExample.hibernateSession = HibernateUtil.clearSession(MLExample.hibernateSession);
		List<MLExample> trainExamples_te = 
			MLExample.getAllExamples(LinkExampleBuilder.ExperimentGroupTimexEvent, for_train);
		int counter =0;
		for (MLExample example:trainExamples_te)
		{
			example.calculateFeatures(timexEventFeatureCalculators);
			counter++;
			FileUtil.logLine(null,"timexEventFeatureCalculators--------example processed: "+counter+"/"+trainExamples_te.size());
			HibernateUtil.clearLoaderSession();
		}
		counter =0;
//		MLExample.hibernateSession = HibernateUtil.clearSession(MLExample.hibernateSession);
		List<MLExample> trainExamples_ee = 
			MLExample.getAllExamples(LinkExampleBuilder.ExperimentGroupEventEvent, for_train);
		for (MLExample example:trainExamples_ee)
		{
			example.calculateFeatures(eventEventFeatureCalculators);
			counter++;
			FileUtil.logLine(null,"eventEventFeatureCalculators--------example processed: "+counter+"/"+trainExamples_ee.size());
			HibernateUtil.clearLoaderSession();
		}
	}
	public static void calculateEventAndTimexEventRiskyFeatures(boolean for_train) throws Exception
	{
		List<IFeatureCalculator> timexEventFeatureCalculators = 
			new ArrayList<IFeatureCalculator>();	
		
		timexEventFeatureCalculators.add(new CustomDependencyGraphFeatures());
//		timexEventFeatureCalculators.add(new ParseTreeFeatures());
		
//		timexEventFeatureCalculators.add(new PatternStatisticsFeatures());

		List<IFeatureCalculator> eventEventFeatureCalculators = 
			new ArrayList<IFeatureCalculator>();
		eventEventFeatureCalculators.addAll(timexEventFeatureCalculators);
		
		List<MLExample> trainExamples_te = 
			MLExample.getAllExamples(LinkExampleBuilder.ExperimentGroupTimexEvent, for_train);
		int counter=0;
		for (MLExample example:trainExamples_te)
		{
			
			example.calculateFeatures(timexEventFeatureCalculators);
			counter++;
			FileUtil.logLine(null,"eventEventFeatureCalculators--------example processed: "+counter+"/"+trainExamples_te.size());
		}
//		MLExample.hibernateSession = HibernateUtil.clearSession(MLExample.hibernateSession);
		counter=0;
		List<MLExample> trainExamples_ee = 
			MLExample.getAllExamples(LinkExampleBuilder.ExperimentGroupEventEvent, for_train);
		for (MLExample example:trainExamples_ee)
		{
			
			example.calculateFeatures(eventEventFeatureCalculators);
			counter++;
			FileUtil.logLine(null,"eventEventFeatureCalculators--------example processed: "+counter+"/"+trainExamples_ee.size());
		}
	}
	static public void createInSentenceLinkExamples(boolean is_training_mode) throws Exception
	{
		List<Artifact> sentences = 
				Artifact.listByType(Artifact.Type.Sentence,is_training_mode);
		
		int counter = 0;
		int example_counter = 0;
		for(Artifact sentence : sentences)
		{
//			if(sentence.getArtifactId()!= 134548) continue;
			MLExample.hibernateSession = HibernateUtil.clearSession(MLExample.hibernateSession);
			List<TimexPhrase> timexs = TimexPhrase.findTimexInSentence(sentence);
			List<ClinicalEvent> events = ClinicalEvent.findEventsInSentence(sentence);
			
			if(!possiblyHasLink(sentence, timexs, events)) continue;
			counter++;
			HibernateUtil.startTransaction();
			
			for(TimexPhrase timex : timexs)
				for(ClinicalEvent event : events)
				{
					example_counter++;
					PhraseLink left_to_right_link;
					PhraseLink right_to_left_link;
					PhraseLink timex_event_link = 
							PhraseLink.getInstance(timex.getRelatedPhrase(), 
									event.getRelatedPhrase());
					
					PhraseLink timex_event_reverse_link = 
							PhraseLink.getInstance(event.getRelatedPhrase(),
									timex.getRelatedPhrase());
					
					if(timex_event_link.isLeftToRight())
					{
						left_to_right_link = timex_event_link;
						right_to_left_link = timex_event_reverse_link;
					}
					else
					{
						left_to_right_link = timex_event_reverse_link;
						right_to_left_link = timex_event_link;
					}
					int expected_class = left_to_right_link.getLinkType().ordinal();
					if(right_to_left_link.getLinkType()!=LinkType.UNKNOWN)
					{//reverse link annotated?
						expected_class = 
								right_to_left_link.getLinkType().ordinal();
						//now reverse it
						if(expected_class==2)
						{
							expected_class=1;
							
						}
						else if(expected_class==1)
						{
							expected_class=2;
						}
						left_to_right_link.setLinkType(LinkType.getEnum(expected_class));
						HibernateUtil.save(left_to_right_link);
					}
					Setting.SaveInGetInstance = false;
					
					MLExample link_example = 
							MLExample.getInstanceForLink(left_to_right_link, ExperimentGroupTimexEvent);
					link_example.setExpectedClass(expected_class);
					link_example.setRelatedPhraseLink(left_to_right_link);
						
				    link_example.setPredictedClass(-1);
				
					
					if(sentence.getAssociatedFilePath().contains("/train/"))
						link_example.setForTrain(true);
					else
						link_example.setForTrain(false);
					
					MLExample.saveExample(link_example);
					
					Setting.SaveInGetInstance = true;

//					link_example.calculateFeatures(timexEventFeatureCalculators);
//					FileUtil.logLine("debug.log","example processed: "+example_counter);
				}
			
			
			for(int i=0;i<events.size()-1;i++)
			{
				ClinicalEvent event1 = events.get(i);
//				if(event1.getEventType()==EventType.TEST)
				for(int j=i+1;j<events.size();j++)
				{
					example_counter++;
					ClinicalEvent event2 = events.get(j);
					
					PhraseLink events_link = 
							PhraseLink.getInstance(event1.getRelatedPhrase(), 
									event2.getRelatedPhrase());
					PhraseLink reverse_link = 
							PhraseLink.getInstance(event2.getRelatedPhrase(),
									event1.getRelatedPhrase());
					
					Setting.SaveInGetInstance = false;
					MLExample link_example = 
							MLExample.getInstanceForLink(events_link, ExperimentGroupEventEvent);
					
					
					int expected_class = events_link.getLinkType().ordinal();
					if(reverse_link.getLinkType()!=LinkType.UNKNOWN)
					{//reverse link annotated?
						expected_class = 
								reverse_link.getLinkType().ordinal();
						
						//now reverse it
						if(expected_class==2)
							expected_class=1;
						else if(expected_class==1)
							expected_class=2;
						events_link.setLinkType(LinkType.getEnum(expected_class));
						HibernateUtil.save(events_link);
					}
					
					link_example.setExpectedClass(expected_class);
					link_example.setPredictedClass(-1);
					link_example.setRelatedPhraseLink(events_link);
					
					if(sentence.getAssociatedFilePath().contains("/train/"))
						link_example.setForTrain(true);
					else
						link_example.setForTrain(false);
					
					MLExample.saveExample(link_example);
					
					Setting.SaveInGetInstance = true;
					
//					link_example.calculateFeatures(eventEventFeatureCalculators);
//					FileUtil.logLine("debug.log","example processed: "+example_counter);

				}
			}
			
			HibernateUtil.clearLoaderSession();
		   FileUtil.logLine(null,"LinkExampleBuilder--------Sentence processed: "+counter);
			HibernateUtil.endTransaction();
		}
		
//		List<Artifact> documents = Artifact.listByType(Artifact.Type.Document);
//		for(Artifact doc : documents)
//		{
//			List<PhraseLink> phrases_in_sentence = PhraseLink.findAllPhraseLinkInDocument(doc);
//			expandWithClosure(phrases_in_sentence);
//		}
	}
	//<event> in the <event> 
	//<event> when <event> 
	static public boolean possiblyHasLink(Artifact sentence,
			List<TimexPhrase> timexs, List<ClinicalEvent> events)
	{
		boolean hasLink = true;
		if(events.size()==0) return false;
		
		if(timexs.size()>0 && events.size()==1)
		{
			boolean hasTriggerPreWord = false;
			//one event multiple timex
			for(TimexPhrase timex : timexs)
			{
				Artifact preArtifact = timex.getRelatedPhrase().getStartArtifact().getPreviousArtifact();
				if(preArtifact!=null && 
						timexPrepositionTriggers.contains(preArtifact.getContent()))
				{
					hasTriggerPreWord=true;
					break;
				}
			}
			if(!hasTriggerPreWord)
				hasLink = false;
		}
		return hasLink;
	}

	static public void expandWithClosure(List<PhraseLink> phraseLinks, boolean forTrain)
	{
		//1. create a directed graph of not negative phraseLinks
		 DirectedGraph<String, GraphEdge> g =
		            new DefaultDirectedGraph<String, GraphEdge>(GraphEdge.class);
		for(PhraseLink phrase_link : phraseLinks)
		{
			String fromPhraseId = 
					String.valueOf(phrase_link.getFromPhrase().getPhraseId()); 
			String toPhraseId = 
					String.valueOf(phrase_link.getToPhrase().getPhraseId()); 
//			String from_content =  phrase_link.getFromPhrase().getPhraseContent();
//			String to_content = phrase_link.getToPhrase().getPhraseContent();
			
			g.addVertex(fromPhraseId);
			g.addVertex(toPhraseId);
//			g.addVertex(from_content);
//			g.addVertex(to_content);
			
			switch(phrase_link.getLinkType())
			{
				case AFTER:
					g.addEdge(toPhraseId, fromPhraseId);
					break;
				case BEFORE:
					g.addEdge(fromPhraseId, toPhraseId);
					break;
				case OVERLAP:
					g.addEdge(toPhraseId, fromPhraseId);
					g.addEdge(fromPhraseId, toPhraseId);
					break;
			}
		}
		
		//2. add negative examples one by one as bi-direct link
		// if any loop formed, update the type of link based on the loop type
		boolean changed = false;
		do{
			 changed = false;
			for(PhraseLink phrase_link : phraseLinks)
			{
				String fromPhraseId = 
						String.valueOf(phrase_link.getFromPhrase().getPhraseId()); 
				String toPhraseId = 
						String.valueOf(phrase_link.getToPhrase().getPhraseId()); 
//				String from_content =  phrase_link.getFromPhrase().getPhraseContent();
//				String to_content = phrase_link.getToPhrase().getPhraseContent();
				g.addVertex(fromPhraseId);
				g.addVertex(toPhraseId);
				
//				g.addVertex(from_content);
//				g.addVertex(to_content);
				
				if(phrase_link.getLinkType()==LinkType.UNKNOWN)
				{
					
					//link tokens
					List<GraphEdge> from_to_path = 
							DijkstraShortestPath.findPathBetween(g, fromPhraseId, toPhraseId);
					List<GraphEdge> to_from_path = 
							DijkstraShortestPath.findPathBetween(g, toPhraseId, fromPhraseId);
					
//					List<GraphEdge> from_to_path = 
//							DijkstraShortestPath.findPathBetween(g, from_content, to_content);
//					List<GraphEdge> to_from_path = 
//							DijkstraShortestPath.findPathBetween(g, to_content, from_content);
					
					LinkType newLinkType = LinkType.UNKNOWN;
					if(from_to_path !=null && from_to_path.size()>0)
						if(to_from_path!=null && 
							to_from_path.size()>0)
						{
							//its overlap  
							newLinkType = LinkType.OVERLAP;
							g.addEdge(fromPhraseId, toPhraseId);
							g.addEdge(toPhraseId, fromPhraseId);
//							g.addEdge(from_content, to_content);
//							g.addEdge(to_content, from_content);
						}else
						{
							newLinkType = LinkType.BEFORE;
							g.addEdge(fromPhraseId, toPhraseId);
//							g.addEdge(from_content, to_content);
						}
					else
						if(to_from_path!=null && to_from_path.size()>0)
						{
							newLinkType = LinkType.AFTER;

							g.addEdge(toPhraseId, fromPhraseId);
						}
					
					if(newLinkType!= LinkType.UNKNOWN)
					{
						phrase_link.setLinkType(newLinkType);
						phrase_link.setLinkTypeClosure(newLinkType);
						HibernateUtil.save(phrase_link);
						// set the related example
						MLExample related_example = MLExample.findInstance(phrase_link);
						if (forTrain == true)
						{
							related_example.setExpectedClass(newLinkType.ordinal());
						}
						else
						{
							related_example.setPredictedClass(newLinkType.ordinal());
						}
						MLExample.saveExample(related_example);
						
						System.out.println(fromPhraseId);
						changed = true;
					}
				}
			}
			HibernateUtil.clearLoaderSession();
		}while(changed);
	}
	
	static public List<MLExample> expandTestExamplesWithClosure(List<MLExample> examples, boolean forTrain)
	{
		//1. create a directed graph of not negative phraseLinks
		 DirectedGraph<String, GraphEdge> g =
		            new DefaultDirectedGraph<String, GraphEdge>(GraphEdge.class);
		 
		for(MLExample example : examples)
		{
			if (example == null) continue;
			
			PhraseLink phrase_link = example.getRelatedPhraseLink();
			MLExample.hibernateSession.update(example);
			String fromPhraseId = 
					String.valueOf(phrase_link.getFromPhrase().getPhraseId()); 
			String toPhraseId = 
					String.valueOf(phrase_link.getToPhrase().getPhraseId()); 
//			String from_content =  phrase_link.getFromPhrase().getPhraseContent();
//			String to_content = phrase_link.getToPhrase().getPhraseContent();
			
			g.addVertex(fromPhraseId);
			g.addVertex(toPhraseId);
//			g.addVertex(from_content);
//			g.addVertex(to_content);
			
			switch(example.getPredictedClass().intValue())
			{
				case 2:
					g.addEdge(toPhraseId, fromPhraseId);
					break;
				case 1:
					g.addEdge(fromPhraseId, toPhraseId);
					break;
				case 3:
					g.addEdge(toPhraseId, fromPhraseId);
					g.addEdge(fromPhraseId, toPhraseId);
					break;
			}
		}
		
		//2. add negative examples one by one as bi-direct link
		// if any loop formed, update the type of link based on the loop type
		boolean changed = false;
		do{
			 changed = false;
			for(MLExample e : examples)
			{
				if (e ==null)
					continue;
				PhraseLink phrase_link = e.getRelatedPhraseLink();
				
				String fromPhraseId = 
						String.valueOf(phrase_link.getFromPhrase().getPhraseId()); 
				String toPhraseId = 
						String.valueOf(phrase_link.getToPhrase().getPhraseId()); 
//				String from_content =  phrase_link.getFromPhrase().getPhraseContent();
//				String to_content = phrase_link.getToPhrase().getPhraseContent();
				g.addVertex(fromPhraseId);
				g.addVertex(toPhraseId);
				
//				g.addVertex(from_content);
//				g.addVertex(to_content);
				
				if(e.getPredictedClass()== 0)
				{
					
					//link tokens
					List<GraphEdge> from_to_path = 
							DijkstraShortestPath.findPathBetween(g, fromPhraseId, toPhraseId);
					List<GraphEdge> to_from_path = 
							DijkstraShortestPath.findPathBetween(g, toPhraseId, fromPhraseId);
					
//					List<GraphEdge> from_to_path = 
//							DijkstraShortestPath.findPathBetween(g, from_content, to_content);
//					List<GraphEdge> to_from_path = 
//							DijkstraShortestPath.findPathBetween(g, to_content, from_content);
					
					int newPredicted = 0;
					if(from_to_path !=null && from_to_path.size()>0)
						if(to_from_path!=null && 
							to_from_path.size()>0)
						{
							//its overlap  
							newPredicted = 3;
							g.addEdge(fromPhraseId, toPhraseId);
							g.addEdge(toPhraseId, fromPhraseId);
//							g.addEdge(from_content, to_content);
//							g.addEdge(to_content, from_content);
						}else
						{
							newPredicted = 1;
							g.addEdge(fromPhraseId, toPhraseId);
//							g.addEdge(from_content, to_content);
						}
					else
						if(to_from_path!=null && to_from_path.size()>0)
						{
							newPredicted = 2;

							g.addEdge(toPhraseId, fromPhraseId);
						}
					
					if(newPredicted != 0)
					{
						e.setPredictedClass(newPredicted);
						
//						MLExample.saveExample(e);
						HibernateUtil.save(e);
						
						System.out.println(fromPhraseId);
						changed = true;
					}
				}
			}
			MLExample.hibernateSession = HibernateUtil.clearSession(MLExample.hibernateSession);
		}while(changed);
		return examples;
	}
	
	public static List<MLExample> calculateClousure(List<MLExample> examples, boolean forTrain)
	{
		List<MLExample> expanded = new ArrayList<MLExample>();
		if (forTrain == true)
		{
			List<Artifact> all_train_sentences = Artifact.listByType(Type.Sentence,forTrain);
			 for(Artifact sentence:all_train_sentences )
			 {
				 List<PhraseLink> sent_phrase_links = 
						 PhraseLink.findAllPhraseLinkInSentence(sentence);
				 expandWithClosure(sent_phrase_links,forTrain);
			 }
		}
		else
		{
			List<Artifact> all_test_sentences = Artifact.listByType(Type.Sentence,forTrain);
//			 for(Artifact sentence:all_test_sentences )
//			 {
//				 List<PhraseLink> sent_phrase_links = 
//						 PhraseLink.findAllPhraseLinkInSentence(sentence);
//				 List<MLExample> examples = new ArrayList<MLExample>();
//				 for (PhraseLink p : sent_phrase_links)
//				 {
//					 MLExample e = MLExample.findInstance(p);
//					 if (e != null)
//					 {
//						 examples.add(e);
//					 }
//						 
//					 
//				 }
				 expanded = expandTestExamplesWithClosure(examples,forTrain);
//			 }
		}
		return expanded;	 
	}
}
