package rainbownlp.i2b2.sharedtask2012.patternminer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rainbownlp.core.Artifact;
import rainbownlp.core.Phrase;
import rainbownlp.core.PhraseLink;
import rainbownlp.i2b2.sharedtask2012.patternminer.GeneralizedSentence.GeneralizationModels;
import rainbownlp.machinelearning.MLExample;

public class BinaryLinkPattern {
	
	private static String file_path = "/home/azadeh/projects/rainbowNLP/data/patterns/binaryLinkPatterns";
	public static void patternBasedPrediction(MLExample example ) throws Exception
	{
		int predicted = -1;
		
		PhraseLink phrase_link =  example.getRelatedPhraseLink();

		Phrase fromPhrase = phrase_link.getFromPhrase();
		Phrase toPhrase = phrase_link.getToPhrase();
		Artifact sentence = fromPhrase.getStartArtifact().getParentArtifact();
		
		GeneralizedSentence gs = 
			GeneralizedSentence.getInstance(fromPhrase, toPhrase, sentence, 
					GeneralizationModels.TTPP, phrase_link.getLinkType(),example.getForTrain());
		String example_between_chunk_exclusivev = gs.getBetweenMentionsChunkExclusive();
		// check all patterns
		List<String> lines =rainbownlp.util.FileUtil.loadLineByLine(file_path);
		for (String pattern: lines)
		{
			Pattern p = Pattern.compile(pattern);
			Matcher m = p.matcher(example_between_chunk_exclusivev);
			if (m.matches())
			{
				predicted=2;
				break;
			}
				
		}
		if (predicted==-1)
			predicted=1;
		example.setPredictedClass(predicted);
	}

}
