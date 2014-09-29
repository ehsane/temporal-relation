package rainbownlp.i2b2.sharedtask2012;

import java.util.ArrayList;
import java.util.List;

import rainbownlp.analyzer.evaluation.Evaluator;
import rainbownlp.core.Setting;
import rainbownlp.i2b2.sharedtask2012.loader.i2b2OutputGenerator;
import rainbownlp.i2b2.sharedtask2012.ruleengines.EventEventRuleBasedPrediction;
import rainbownlp.i2b2.sharedtask2012.ruleengines.SecTimeEventRuleBasedPrediction;
import rainbownlp.i2b2.sharedtask2012.ruleengines.TimexEventRuleBasedPrediction;
import rainbownlp.machinelearning.MLExample;
import rainbownlp.machinelearning.SVMMultiClass;
import rainbownlp.util.FileUtil;
import rainbownlp.util.HibernateUtil;

public class TestOnTrain {
	

	public static void main(String[] args) throws Exception
	{
		List<MLExample> allTestData =  new ArrayList<MLExample>();
		MLExample.hibernateSession = HibernateUtil.sessionFactory.openSession();
//		FeatureValuePair.resetIndexes();
//		//////////////////////////////////////////////
//		/// SentenceSentence
//		//////////////////////////////////////////////
//		SVMMultiClass blc = (SVMMultiClass)
//				SVMMultiClass.getLearnerEngine(SentenceExampleBuilder.ExperimentGroup);
//		CrossValidation cv = new CrossValidation(blc);
//		
//		List<MLExample> trainExamples = 
//			MLExample.getAllExamples(SentenceExampleBuilder.ExperimentGroup, true, 1);
//		
//		cv.crossValidation(trainExamples, 2).printResult();

		List<MLExample> testExamples_between = 
				MLExample.getAllExamples(SharedConstants.EXPERIMENT_GROUP_BETWEENSENTENCE, 
						true);
		BetweenSentenceClassifier bsc = new BetweenSentenceClassifier();
		bsc.test(testExamples_between);
	
		
		FileUtil.logLine(null,"BetweenSentenceClassifier done--------: ");
		//////////////////////////////////////////////
		/// TimexEvent
		//////////////////////////////////////////////
		MLExample.hibernateSession = HibernateUtil.clearSession(MLExample.hibernateSession);
		
//		MLExample.resetExamplesPredicted(LinkExampleBuilder.ExperimentGroupTimexEvent, true);
//		List<MLExample> testExamples_et =buildEventEventTypedClassifiers(trainCount,testCount, 
//				exampleTypeList, LinkExampleBuilder.ExperimentGroupTimexEvent, new TimexEventRuleBasedPrediction());

		
		SVMMultiClass svmeventtimex = (SVMMultiClass) 
				SVMMultiClass.getLearnerEngine(SharedConstants.EXPERIMENT_GROUP_TIMEXEVENT);
		
		List<MLExample> trainExamples_et = 
				MLExample.getAllExamples(SharedConstants.EXPERIMENT_GROUP_TIMEXEVENT, true);
		svmeventtimex.train(trainExamples_et);
		MLExample.hibernateSession = HibernateUtil.clearSession(MLExample.hibernateSession);
		//set predicted to -1
		MLExample.resetExamplesPredicted(SharedConstants.EXPERIMENT_GROUP_TIMEXEVENT, true);
		List<MLExample> testExamples_et = 
				MLExample.getAllExamples(SharedConstants.EXPERIMENT_GROUP_TIMEXEVENT, true);

		TimexEventRuleBasedPrediction et_rule_engine = new TimexEventRuleBasedPrediction();
		List<MLExample> classifierInputExamples_et = et_rule_engine.predictByRules(testExamples_et);
		
		svmeventtimex.test(classifierInputExamples_et);
	
		HibernateUtil.clearLoaderSession();

		FileUtil.logLine(null,"TimexEvent-------- ");
//		//////////////////////////////////////////////
//		/// EventEvent
		//////////////////////////////////////////////
		MLExample.hibernateSession = HibernateUtil.clearSession(MLExample.hibernateSession);
		MLExample.resetExamplesPredicted(SharedConstants.EXPERIMENT_GROUP_EVENTEVENT, true);
		SVMMultiClass svmeventevent = (SVMMultiClass) 
				SVMMultiClass.getLearnerEngine(SharedConstants.EXPERIMENT_GROUP_EVENTEVENT);
		
		List<MLExample> trainExamples_ee = 
				MLExample.getAllExamples(SharedConstants.EXPERIMENT_GROUP_EVENTEVENT, true);
		svmeventevent.train(trainExamples_ee);
		
		MLExample.hibernateSession = HibernateUtil.clearSession(MLExample.hibernateSession);
		
		List<MLExample> testExamples_ee = 
				MLExample.getAllExamples(SharedConstants.EXPERIMENT_GROUP_EVENTEVENT, true);
		EventEventRuleBasedPrediction ee_rule_engine = new EventEventRuleBasedPrediction();
		List<MLExample> classifierInputExamples_ee = ee_rule_engine.predictByRules(testExamples_ee);
		svmeventevent.test(classifierInputExamples_ee);
		
		
//		TODO: uncomment
//		List<MLExample> testExamples_ee =buildEventEventTypedClassifiers(trainCount,testCount, 
//				exampleTypeList, LinkExampleBuilder.ExperimentGroupEventEvent,
//				new EventEventRuleBasedPrediction());
//		
		MLExample.hibernateSession = HibernateUtil.clearSession(MLExample.hibernateSession);
		HibernateUtil.clearLoaderSession();
		
		FileUtil.logLine(null,"EventEvent-------- ");
		
//		//////////////////////////////////////////////
//		/// SecTimeEvent
//		//////////////////////////////////////////////
		SVMMultiClass svmsectime = (SVMMultiClass) 
				SVMMultiClass.getLearnerEngine(SecTimeEventExampleBuilder.ExperimentGroupSecTimeEvent);

		List<MLExample> trainExamples_se = 
				MLExample.getAllExamples(
						SecTimeEventExampleBuilder.ExperimentGroupSecTimeEvent, true);
		
//		SecTimeEventRuleBasedPrediction sectime_rule_engine = new SecTimeEventRuleBasedPrediction();
//		List<MLExample> classifierInputTrainingExamples_se = sectime_rule_engine.predictByRules(trainExamples_se);
		
		svmsectime.train(trainExamples_se);
		MLExample.hibernateSession = HibernateUtil.clearSession(MLExample.hibernateSession);
		
		//reset the predicted to before since most of them are before
		MLExample.resetExamplesPredicted(SecTimeEventExampleBuilder.ExperimentGroupSecTimeEvent, true);
		List<MLExample> testExamples_se = 
				MLExample.getAllExamples(
						SecTimeEventExampleBuilder.ExperimentGroupSecTimeEvent, true);
//		// rule engine sectime event
		SecTimeEventRuleBasedPrediction sectime_rule_engine_test = new SecTimeEventRuleBasedPrediction();
		List<MLExample> classifierInputExamples_se = sectime_rule_engine_test.predictByRules(testExamples_se);
		svmsectime.test(classifierInputExamples_se);
		

		allTestData.addAll(testExamples_ee);
		allTestData.addAll(testExamples_et);
		allTestData.addAll(testExamples_se);
		allTestData.addAll(testExamples_between);
		
		List<MLExample> testExamples_rulesure = 
				MLExample.getAllExamples(Setting.RuleSureCorpus,  true);
		allTestData.addAll(testExamples_rulesure);
		
		i2b2OutputGenerator.generateOutPutFiles(allTestData, Setting.getValue("InputTestFolderOnTrain"), 
				Setting.getValue("TestOutput"));
		Evaluator.getEvaluationResult(allTestData).printResult();
	}
}
