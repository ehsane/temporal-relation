package rainbownlp.i2b2.sharedtask2012.loader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rainbownlp.core.Phrase;
import rainbownlp.core.PhraseLink;
import rainbownlp.core.PhraseLink.LinkType;
import rainbownlp.core.Setting;
import rainbownlp.machinelearning.MLExample;
import rainbownlp.machinelearning.examplebuilder.link.InSentenceExampleBuilder;
import rainbownlp.util.FileUtil;
import rainbownlp.util.HibernateUtil;
import edu.umass.cs.mallet.base.pipe.iterator.FileIterator;


public class I2b2ExampleUpdateManager{

	public static void main(String[] args) throws Exception
	{
		Setting.TrainingMode = false;
		List<MLExample> test_examples = MLExample.getAllExamples(false);
//		for (MLExample test_example: test_examples)
//		{
//			PhraseLink phrase_link = test_example.getRelatedPhraseLink();
//			LinkType type = phrase_link.getLinkType();
//			phrase_link.getPhraseLinkId();
//			test_example.setExpectedClass(type.ordinal());
//			
//			
//			String savePredictedQuery = "update MLExample set expectedClass ="
//				+type.ordinal()+
//			" where exampleId="+test_example.getExampleId();
//			HibernateUtil.executeNonReader(savePredictedQuery);
////			
//		}
		
		// get all examples of the EE
//		List<MLExample> ee_examples = MLExample.getAllExamples(LinkExampleBuilder.ExperimentGroupEventEvent,false);
	
		String selectQuery_all =" from PhraseLink where forTrain=0";
		List<PhraseLink> examples_all = 
			(List<PhraseLink>) HibernateUtil.executeReader(selectQuery_all);

		String selectQuery =" from PhraseLink where fromPhrase.startArtifact.parentArtifact <> toPhrase.startArtifact.parentArtifact" +
				" and forTrain=0";
		List<PhraseLink> examples = 
			(List<PhraseLink>) HibernateUtil.executeReader(selectQuery);
		
		
		
		
		
		int missing_link_count = 0;
		int duplicates = 0;
		int existing_examples = 0;
		int count_existing_in_sent = 0;
		
		//here it updates the in timex event and event event
		for (PhraseLink p:examples)
		{
			Phrase from = p.getFromPhrase();
			Phrase to_phrase = p.getToPhrase();
			
			String filepath =  from.getStartArtifact().getAssociatedFilePath();
			String  link_type = p.getLinkType().toString();
			
			
			String from_content = from.getPhraseContent();
			String to_content = to_phrase.getPhraseContent();
			
			//now test if we have an exmple for that
			PhraseLink p_e = PhraseLink.findPhraseLinkForExamples(from, to_phrase);
			
			if (p_e != null)
			{
				MLExample related_example = MLExample.findInstance(p_e);
				if (related_example == null)
				{
					p_e = PhraseLink.findPhraseLinkForExamples(to_phrase,from);
					if (p_e != null)
					{
						related_example = MLExample.findInstance(p_e);
						if (related_example == null)
						{
							System.out.println("NOOOOOOOOOOo Example  for this "+p_e.getAltLinkID()+"*******" + p.getAltLinkID()+"********"+filepath);
							System.out.println("the phraselink id " +p_e.getPhraseLinkId());
						}
						else
						{
							count_existing_in_sent++;
							String savePredictedQuery = "update MLExample set expectedClass ="
								+p.getLinkType().ordinal()+
							" where exampleId="+related_example.getExampleId();
							HibernateUtil.executeNonReader(savePredictedQuery);
						}
					}
					else
					{
						System.out.println("we have no phraseLink  for this "+p.getAltLinkID()+"*******" + p.getAltLinkID()+"********"+filepath);
					}
					
					
				}
				else
				{
					count_existing_in_sent++;
					String savePredictedQuery = "update MLExample set expectedClass ="
						+p.getLinkType().ordinal()+
					" where exampleId="+related_example.getExampleId();
					HibernateUtil.executeNonReader(savePredictedQuery);
				}
				
			}
			else
			{
				System.out.println("we have no phraseLink  for this "+p.getAltLinkID()+"********"+filepath);
			}
		
		}
		System.out.println("***************** "+count_existing_in_sent);
		
		
		
		
		
		
		
		
		
//		insentence
//		String selectQuery_insentence =" from PhraseLink where fromPhrase.startArtifact.parentArtifact = toPhrase.startArtifact.parentArtifact" +
//		" and forTrain=0";
//		List<PhraseLink> examples_in = 
//			(List<PhraseLink>) HibernateUtil.executeReader(selectQuery_insentence);
//		int missing_link_count = 0;
//		int duplicates = 0;
//		int existing_examples = 0;
//		int count_existing_in_sent = 0;
//		
//		//here it updates the in timex event and event event
//		for (PhraseLink p:examples_in)
//		{
//			Phrase from = p.getFromPhrase();
//			Phrase to_phrase = p.getToPhrase();
//			
//			String filepath =  from.getStartArtifact().getAssociatedFilePath();
//			String  link_type = p.getLinkType().toString();
//			
//			
//			String from_content = from.getPhraseContent();
//			String to_content = to_phrase.getPhraseContent();
//			
//			//now test if we have an exmple for that
//			PhraseLink p_e = PhraseLink.findPhraseLinkForExamples(from, to_phrase);
//			
//			if (p_e != null)
//			{
//				MLExample related_example = MLExample.findInstance(p_e);
//				if (related_example == null)
//				{
//					p_e = PhraseLink.findPhraseLinkForExamples(to_phrase,from);
//					if (p_e != null)
//					{
//						related_example = MLExample.findInstance(p_e);
//						if (related_example == null)
//						{
//							System.out.println("NOOOOOOOOOOo Example  for this "+p_e.getAltLinkID()+"*******" + p.getAltLinkID()+"********"+filepath);
//							System.out.println("the phraselink id " +p_e.getPhraseLinkId());
//						}
//						else
//						{
//							count_existing_in_sent++;
//							String savePredictedQuery = "update MLExample set expectedClass ="
//								+p.getLinkType().ordinal()+
//							" where exampleId="+related_example.getExampleId();
//							HibernateUtil.executeNonReader(savePredictedQuery);
//						}
//					}
//					else
//					{
//						System.out.println("we have no phraseLink  for this "+p.getAltLinkID()+"*******" + p.getAltLinkID()+"********"+filepath);
//					}
//					
//					
//				}
//				else
//				{
//					count_existing_in_sent++;
//					String savePredictedQuery = "update MLExample set expectedClass ="
//						+p.getLinkType().ordinal()+
//					" where exampleId="+related_example.getExampleId();
//					HibernateUtil.executeNonReader(savePredictedQuery);
//				}
//				
//			}
//			else
//			{
//				System.out.println("we have no phraseLink  for this "+p.getAltLinkID()+"********"+filepath);
//			}
//		
//		}
//		System.out.println("***************** "+count_existing_in_sent);
		
//		for (PhraseLink p:examples_in)
//		{
//			//select the related example
//			MLExample ee = MLExample.findInstance(p,LinkExampleBuilder.ExperimentGroupEventEvent);
//			MLExample te = MLExample.findInstance(p,LinkExampleBuilder.ExperimentGroupTimexEvent);
//			
//			if (ee== null && te==null)
//			{
//				//find the reverse instance 
//				PhraseLink reverese = PhraseLink.findPhraseLink(p.getToPhrase(), p.getFromPhrase());
//				
//				if (reverese == null)
//				{
//					missing_link_count++;
//					 
//					 continue;
//				}
//				int id = reverese.getPhraseLinkId();
//				 ee = MLExample.findInstance(reverese,LinkExampleBuilder.ExperimentGroupEventEvent);
//				 te = MLExample.findInstance(reverese,LinkExampleBuilder.ExperimentGroupTimexEvent);
//				 if (ee== null && te==null)
//					{
//					 if (p.getToPhrase().getPhraseId() ==p.getFromPhrase().getPhraseId())
//					 {
//						 duplicates++;
//						 continue;
//						 
//					 }
//						
//					 System.out.println("******"+p.getPhraseLinkId());
//					}
//				 else
//				 {
//					 existing_examples++;
//					 if (ee != null)
//					 {
//						 System.out.println("******"+ee.getExpectedClass());
//					 }
//					 else
//					 {
//						 System.out.println("******"+te.getExpectedClass());
//					 }
//				 }
//				
//			}
//			else
//			{
//				existing_examples++;
//				if (ee != null)
//				 {
//					 System.out.println("******"+ee.getExpectedClass());
//				 }
//				 else
//				 {
//					 System.out.println("******"+te.getExpectedClass());
//				 }
//			}
//		}
//		System.out.println("*****************"+missing_link_count+"**************"+duplicates+"*********"+ examples_in.size()+"*********"+existing_examples);
	}
	
}
