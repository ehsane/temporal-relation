package rainbownlp.i2b2.sharedtask2012;

import java.io.IOException;
import java.util.List;

import rainbownlp.core.Artifact;
import rainbownlp.core.Phrase;
import rainbownlp.core.PhraseLink;
import rainbownlp.core.PhraseLink.LinkType;
import rainbownlp.i2b2.sharedtask2012.ClinicalEvent.EventType;
import rainbownlp.i2b2.sharedtask2012.TimexPhrase.TimexType;
import rainbownlp.machinelearning.ILearnerEngine;
import rainbownlp.machinelearning.MLExample;
import rainbownlp.machinelearning.SVMMultiClass;
import rainbownlp.util.HibernateUtil;
import rainbownlp.util.StringUtil;

public class BetweenSentenceClassifier  implements ILearnerEngine  {
	public static final String experimentgroup = "BinaryLinkClassifier";
	
	
	public static void main(String[] args) throws Exception
	{
		
	}
	SVMMultiClass svm = (SVMMultiClass) SVMMultiClass.getLearnerEngine(experimentgroup);
	
	 @Override
	public void train(List<MLExample> exampleForTrain) throws IOException
	 {
		 
	 }

	@Override
	public void test(List<MLExample> pTestExamples) throws Exception {
		for(int i=0;i< pTestExamples.size();i++)
			{
			 MLExample example =
				 pTestExamples.get(i);
			 PhraseLink pl = example.getRelatedPhraseLink();
			 Phrase phrase1 = pl.getFromPhrase();
			 Phrase phrase2 = pl.getToPhrase();
			 String p1_content = phrase1.getPhraseContent().trim();
			 String p2_content = phrase2.getPhraseContent().trim();
			 example.setPredictedClass((double) LinkType.UNKNOWN.ordinal());
			 String porter1 = StringUtil.getTermByTermPorter(p1_content.toLowerCase());
			 String porter2 = StringUtil.getTermByTermPorter(p2_content.toLowerCase());
//			 String generalized1 = SentencePattern.generalizeString(p1_content);
//			 String generalized2 = SentencePattern.generalizeString(p2_content);
			 TimexPhrase timex1 = TimexPhrase.getRelatedTimexFromPhrase(phrase1);
			 ClinicalEvent event1 = null;
			 if(timex1==null)
				 event1 = ClinicalEvent.getRelatedEventFromPhrase(phrase1);
			 TimexPhrase timex2 = TimexPhrase.getRelatedTimexFromPhrase(phrase2);
			 ClinicalEvent event2 = null;
			 if(timex2==null)
				 event2 = ClinicalEvent.getRelatedEventFromPhrase(phrase2);
			 
			 List<Phrase> phrases_in_sentence2 = 
					 Phrase.getPhrasesInSentence(phrase2.getStartArtifact().getParentArtifact());
				
			//one test in the sentence,link with the first treatment in previous sentence
			 if((phrase1.getStartArtifact().getLineIndex()-phrase2.getStartArtifact().getLineIndex())<3 &&
					 phrases_in_sentence2.size()==1 && 
					 phrase2.getPhraseEntityType().equals("EVENT") &&
				phrase1.getPhraseEntityType().equals("EVENT") && 
				event1.getEventType()==EventType.TREATMENT &&
						event1.getEventType()==EventType.TEST)
			{
				 example.setPredictedClass((double) LinkType.OVERLAP.ordinal());
				 HibernateUtil.save(example);
				 continue;
			}
			 String c1_lower = p1_content.toLowerCase();
			 String c2_lower = p2_content.toLowerCase();
			if(c2_lower.matches("^the .*") && 
					c1_lower.equals(c2_lower.replaceAll("^the ", "a ")))
				 example.setPredictedClass((double) LinkType.BEFORE.ordinal());
			 if(porter1.equals(porter2) || porter1.replaceAll("(a)|(the) ", "").equals(porter2))
			 {
				 
//				 if(p1_content.toLowerCase()
//						 	.matches("(.*((operat((ion)|(ing)))|(stay unit)).*)|(it)|(this)|(asa)|(bp)|(pc(p|d))"))
//					 continue;
				if(p1_content.toLowerCase().matches("(admission)|(admitted)"))
				{
					Artifact nextWord = phrase1.getEndArtifact().getNextArtifact();
					if(nextWord!= null && nextWord.getContent().toLowerCase().matches("to"))
						continue;
					nextWord = phrase2.getEndArtifact().getNextArtifact();
					if(nextWord!= null && nextWord.getContent().toLowerCase().matches("to"))
						continue;
				}
				 if(event1!=null && event2!=null)
				 {
					 EventType event1_type = event1.getEventType();
					 EventType event2_type = event2.getEventType();
					 if(
							 event1_type != event2_type 
							 || 
							 		 event1_type == EventType.TEST
									 )
						 continue;
				 }
				 if(timex1!=null && timex2!=null)
				 {
					 TimexType timex1_type = timex1.getTimexType();
					 TimexType timex2_type = timex2.getTimexType();
					 if(
							 timex1_type != timex2_type 
							 ||
								timex1_type != TimexType.DATE ||
											 timex2_type != TimexType.DATE
									 )
						 continue;
				 }
				 if(
						 (p1_content.matches("(([A-Z]|[0-9]| )+)|((t|T)he [A-Z][a-z]+.*)|([A-Z]([a-z]| )+)+")
						  || p1_content.split(" ").length>2 
						  || c1_lower.matches("(h((is)|(er)) [a-z]+.*)|(admission)|" +
						  		"(discharg(\\w+)?)|(died)|(birth)|(born)")
//						  || (generalized1.equals(generalized2) && generalized1.matches("\\$(date)\\$"))
						  )
						 && 
						 !c1_lower
						 	.matches("(.*((operat((ion)|(ing)))|(stay unit)).*)|" +
						 			"(it)|(this)|(asa)|(s?bp)|(pc(p|d))|(hypertension)|(hypotension)"+
						 			"|(overnight)|(imipenem)|(denies)|(shortness of breath)|(stress dose steroids)" +
						 			"|(un\\w+)")
						 )
					 example.setPredictedClass((double) LinkType.OVERLAP.ordinal());
			 }
			 HibernateUtil.save(example);
		 }
	}
}
