package rainbownlp.i2b2.sharedtask2012.tests.unit.core;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import rainbownlp.core.Artifact;
import rainbownlp.core.Artifact.Type;
import rainbownlp.core.Phrase;
import rainbownlp.i2b2.sharedtask2012.loader.I2b2Annotation;
import rainbownlp.i2b2.sharedtask2012.loader.XMLLoader;

public class PhraseTest   {
	
	@Test
	public void testCreateArtifact() throws Exception {
		List<Artifact> docs = Artifact.listByType(Type.Document);
		for(Artifact doc:docs)
		{
			String file_path = doc.getAssociatedFilePath();
			List<Phrase> docPhrases = Phrase.getPhrasesInDocument(file_path);
			
			String xmlFilePath = file_path.replace(".txt", "");
			List<I2b2Annotation> annotationsInXML = XMLLoader.loadXMLMentions(xmlFilePath);
			
			assertTrue("file path: "+xmlFilePath, 
					annotationsInXML.size()- docPhrases.size()<3);

			//test content
			for (I2b2Annotation annotation : annotationsInXML)
				if(!annotation.altId.startsWith("S"))
				assertTrue("file path: "+xmlFilePath +
						" annotation: "+ annotation.toString(), 
						docPhrases.contains(annotation));
	
		}
	}

}
