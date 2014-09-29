package rainbownlp.i2b2.sharedtask2012;

import java.util.ArrayList;
import java.util.List;

import rainbownlp.analyzer.evaluation.classification.Evaluator;
import rainbownlp.core.Setting;
import rainbownlp.i2b2.sharedtask2012.loader.i2b2OutputGenerator;
import rainbownlp.i2b2.sharedtask2012.ruleengines.EventEventRuleBasedPrediction;
import rainbownlp.i2b2.sharedtask2012.ruleengines.SecTimeEventRuleBasedPrediction;
import rainbownlp.i2b2.sharedtask2012.ruleengines.TimexEventRuleBasedPrediction;
import rainbownlp.machinelearning.MLExample;
import rainbownlp.machinelearning.SVMMultiClass;
import rainbownlp.util.FileUtil;
import rainbownlp.util.HibernateUtil;
// there are three options justrules, justclassifer and after
public class TestMainPredictor {
	

	public static void main(String[] args) throws Exception
	{
		String run_option = args[1];
		boolean rules_after_classifier = false;
		
		if (run_option.equals("after"))
			rules_after_classifier = true;
		
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

		//////////////////***********///////////////////
		List<MLExample> testExamples_between = 
				MLExample.getAllExamples(LinkExampleBuilder.ExperimentGroupBetweenSentence, 
						false);
//		TODO: uncomment this section
		BetweenSentenceClassifier bsc = new BetweenSentenceClassifier();
		bsc.test(testExamples_between);	
		FileUtil.logLine(null,"BetweenSentenceClassifier done--------: ");
//		/////////////////***********///////////////////
//		for (MLExample test_bet:testExamples_between)
//		{
//			MLExample.setExamplePredictedClass(test_bet.getExampleId(),test_bet.getExpectedClass());
//			test_bet.setPredictedClass(test_bet.getExpectedClass());
//		}
		//////////////////////////////////////////////
		/// TimexEvent
		//////////////////////////////////////////////
//		MLExample.hibernateSession = HibernateUtil.clearSession(MLExample.hibernateSession);

		//set predicted to -1
		MLExample.resetExamplesPredicted(LinkExampleBuilder.ExperimentGroupTimexEvent, false);
		List<MLExample> testExamples_et = 
				MLExample.getAllExamples(LinkExampleBuilder.ExperimentGroupTimexEvent, false);
		
		if (run_option.equals("justrules"))
		{
			TimexEventRuleBasedPrediction et_rule_engine = new TimexEventRuleBasedPrediction();
			List<MLExample> classifierInputExamples_et = et_rule_engine.predictByRules(testExamples_et);
		}
		
		else if (run_option.equals("justclassifier"))
		{
			SVMMultiClass svmeventtimex = (SVMMultiClass) 
			SVMMultiClass.getLearnerEngine(LinkExampleBuilder.ExperimentGroupTimexEvent);
			List<MLExample> trainExamples_et = 
				MLExample.getAllExamples(LinkExampleBuilder.ExperimentGroupTimexEvent, true);
			svmeventtimex.train(trainExamples_et);
			svmeventtimex.test(testExamples_et);
		}
		else if (run_option.equals("twoClassifiers"))
		{
			//first set expected class for train data to relaexpected and train the classifire
			String resetExpectedClass = "update MLExample set expectedClass=0 where" +
					" corpusName ='"+LinkExampleBuilder.ExperimentGroupTimexEvent+"' and forTrain =1";
			HibernateUtil.executeNonReader(resetExpectedClass);
			
			String updateExpectedClasstoReal = 
			"update MLExample set expectedClass=expectedReal where "+
			" corpusName ='"+LinkExampleBuilder.ExperimentGroupTimexEvent+"' and forTrain =1";
			HibernateUtil.executeNonReader(updateExpectedClasstoReal);
			
			SVMMultiClass svmtimexeventWithoutClosure = (SVMMultiClass) 
			SVMMultiClass.getLearnerEngine(LinkExampleBuilder.ExperimentGroupTimexEvent);
			
			List<MLExample> trainExamples_et = 
				MLExample.getAllExamples(LinkExampleBuilder.ExperimentGroupTimexEvent, true);
			svmtimexeventWithoutClosure.train(trainExamples_et);
			
			// pass all the test to the first
			svmtimexeventWithoutClosure.test(testExamples_et);
			
			//then set expected to expectedClousure and train the classifier2
//			HibernateUtil.executeNonReader(resetExpectedClass);
	
			String updateExpectedClasstoClosure = 
				"update MLExample set expectedClass=expectedClosure where "+
				" expectedClass=0 and expectedClosure <> 0 and corpusName ='"+LinkExampleBuilder.ExperimentGroupTimexEvent+"' and forTrain =1";
				HibernateUtil.executeNonReader(updateExpectedClasstoClosure);
				
			SVMMultiClass svmtimexeventWithClosure = (SVMMultiClass) 
			SVMMultiClass.getLearnerEngine(LinkExampleBuilder.ExperimentGroupTimexEvent);
			
			trainExamples_et = 
				MLExample.getAllExamples(LinkExampleBuilder.ExperimentGroupTimexEvent, true);
			svmtimexeventWithClosure.train(trainExamples_et);
			
			
			MLExample.hibernateSession = HibernateUtil.clearSession(MLExample.hibernateSession);
			// the examples that predicted==0 yaa -1 pass to the second
			List<MLExample> closureTestExamples =  new ArrayList<MLExample>();
			
			for (MLExample test_et:testExamples_et)
			{
				if (test_et.getPredictedClass() == 0 ||
						test_et.getPredictedClass() == -1)
				{
					closureTestExamples.add(test_et);
				}
			}
			svmtimexeventWithClosure.test(closureTestExamples);
		}
		else
		{
			SVMMultiClass svmeventtimex = (SVMMultiClass) 
			SVMMultiClass.getLearnerEngine(LinkExampleBuilder.ExperimentGroupTimexEvent);
	
			List<MLExample> trainExamples_et = 
					MLExample.getAllExamples(LinkExampleBuilder.ExperimentGroupTimexEvent, true);
			svmeventtimex.train(trainExamples_et);
//			MLExample.hibernateSession = HibernateUtil.clearSession(MLExample.hibernateSession);
//			MLExample.hibernateSession = HibernateUtil.clearSession(MLExample.hibernateSession);
//			//set predicted to -1
//			MLExample.resetExamplesPredicted(LinkExampleBuilder.ExperimentGroupTimexEvent, false);
//			List<MLExample> testExamples_et = 
//					MLExample.getAllExamples(LinkExampleBuilder.ExperimentGroupTimexEvent, false);
			
			if (rules_after_classifier)
			{
				svmeventtimex.test(testExamples_et);
				List<MLExample> rule_inputs_et = new ArrayList<MLExample>();
				
				for (MLExample et: testExamples_et)
				{
					if (et.getPredictedClass()==0)
						rule_inputs_et.add(et);
				}
				TimexEventRuleBasedPrediction et_rule_engine = new TimexEventRuleBasedPrediction();
				List<MLExample> classifierInputExamples_et = et_rule_engine.predictByRules(rule_inputs_et);
			}
			else
			{
				TimexEventRuleBasedPrediction et_rule_engine = new TimexEventRuleBasedPrediction();
				List<MLExample> classifierInputExamples_et = et_rule_engine.predictByRules(testExamples_et);
				
				svmeventtimex.test(classifierInputExamples_et);
			}
	
		}
		
//		svmeventtimex.test(testExamples_et);
//		MLExample.hibernateSession = HibernateUtil.clearSession(MLExample.hibernateSession);
//		HibernateUtil.clearLoaderSession();
//		//////////////////////////////////////////////
//		//This is just for testing which percentage of the test is for In-Sentence relation
//		//////////////////////////////////////////////
////		for (MLExample test_et:testExamples_et)
////		{
////			MLExample.setExamplePredictedClass(test_et.getExampleId(),test_et.getExpectedClass());
////			test_et.setPredictedClass(test_et.getExpectedClass());
////
////		}
//		FileUtil.logLine(null,"TimexEvent-------- ");
////		//////////////////////////////////////////////
////		/// EventEvent
//		//////////////////////////////////////////////
		MLExample.hibernateSession = HibernateUtil.clearSession(MLExample.hibernateSession);
		MLExample.resetExamplesPredicted(LinkExampleBuilder.ExperimentGroupEventEvent, false);
		List<MLExample> testExamples_ee = 
			MLExample.getAllExamples(LinkExampleBuilder.ExperimentGroupEventEvent, false);
		
		
		if (run_option.equals("justrules"))
		{
			EventEventRuleBasedPrediction ee_rule_engine = new EventEventRuleBasedPrediction();
			List<MLExample> classifierInputExamples_ee = ee_rule_engine.predictByRules(testExamples_ee);
		}
		else if (run_option.equals("justclassifier"))
		{
			SVMMultiClass svmeventevent = (SVMMultiClass) 
			SVMMultiClass.getLearnerEngine(LinkExampleBuilder.ExperimentGroupEventEvent);
		////	
			List<MLExample> trainExamples_ee = 
				MLExample.getAllExamples(LinkExampleBuilder.ExperimentGroupEventEvent, true);
			svmeventevent.train(trainExamples_ee);
			svmeventevent.test(testExamples_ee);
		}
		else if (run_option.equals("twoClassifiers"))
		{
			//first set expected class for train data to relaexpected and train the classifire
			String resetExpectedClass = "update MLExample set expectedClass=0 where" +
					" corpusName ='"+LinkExampleBuilder.ExperimentGroupEventEvent+"' and forTrain =1";
			HibernateUtil.executeNonReader(resetExpectedClass);
			
			String updateExpectedClasstoReal = 
			"update MLExample set expectedClass=expectedReal where "+
			" corpusName ='"+LinkExampleBuilder.ExperimentGroupEventEvent+"' and forTrain =1";
			HibernateUtil.executeNonReader(updateExpectedClasstoReal);
			
			SVMMultiClass svmeventeventWithoutClosure = (SVMMultiClass) 
			SVMMultiClass.getLearnerEngine(LinkExampleBuilder.ExperimentGroupEventEvent);
			
			List<MLExample> trainExamples_ee = 
				MLExample.getAllExamples(LinkExampleBuilder.ExperimentGroupEventEvent, true);
			svmeventeventWithoutClosure.train(trainExamples_ee);
			
			//then set expected to expectedClousure and train the classifier2
//			HibernateUtil.executeNonReader(resetExpectedClass);
	
			String updateExpectedClasstoClosure = 
				"update MLExample set expectedClass=expectedClosure where "+
				" expectedClass=0 and expectedClosure <> 0 and corpusName ='"+LinkExampleBuilder.ExperimentGroupEventEvent+"' and forTrain =1";
				HibernateUtil.executeNonReader(updateExpectedClasstoClosure);
				
			SVMMultiClass svmeventeventWithClosure = (SVMMultiClass) 
			SVMMultiClass.getLearnerEngine(LinkExampleBuilder.ExperimentGroupEventEvent);
			
			trainExamples_ee = 
				MLExample.getAllExamples(LinkExampleBuilder.ExperimentGroupEventEvent, true);
			svmeventeventWithClosure.train(trainExamples_ee);
			
			// pass all the test to the first
			svmeventeventWithoutClosure.test(testExamples_ee);
			MLExample.hibernateSession = HibernateUtil.clearSession(MLExample.hibernateSession);
			// the examples that predicted==0 yaa -1 pass to the second
			List<MLExample> closureTestExamples =  new ArrayList<MLExample>();
			
			for (MLExample test_ee:testExamples_ee)
			{
				if (test_ee.getPredictedClass() == 0 ||
						test_ee.getPredictedClass() == -1)
				{
					closureTestExamples.add(test_ee);
				}
			}
			svmeventeventWithClosure.test(closureTestExamples);
		}
		else
		{
			SVMMultiClass svmeventevent = (SVMMultiClass) 
			SVMMultiClass.getLearnerEngine(LinkExampleBuilder.ExperimentGroupEventEvent);
	
			List<MLExample> trainExamples_ee = 
					MLExample.getAllExamples(LinkExampleBuilder.ExperimentGroupEventEvent, true);
			svmeventevent.train(trainExamples_ee);
			
//			MLExample.hibernateSession = HibernateUtil.clearSession(MLExample.hibernateSession);
//			HibernateUtil.clearLoaderSession();
			
//			List<MLExample> testExamples_ee = 
//					MLExample.getAllExamples(LinkExampleBuilder.ExperimentGroupEventEvent, false);
			
			if (rules_after_classifier)
			{
				svmeventevent.test(testExamples_ee);
				List<MLExample> rule_inputs_ee = new ArrayList<MLExample>();
				
				for (MLExample ee: testExamples_ee)
				{
					if (ee.getPredictedClass()==0)
						rule_inputs_ee.add(ee);
				}
				EventEventRuleBasedPrediction ee_rule_engine = new EventEventRuleBasedPrediction();
				List<MLExample> classifierInputExamples_ee = ee_rule_engine.predictByRules(rule_inputs_ee);
			}
			else
			{
				EventEventRuleBasedPrediction ee_rule_engine = new EventEventRuleBasedPrediction();
				List<MLExample> classifierInputExamples_ee = ee_rule_engine.predictByRules(testExamples_ee);
				svmeventevent.test(classifierInputExamples_ee);
			}
		}
//		MLExample.hibernateSession = HibernateUtil.clearSession(MLExample.hibernateSession);
//		HibernateUtil.clearLoaderSession();

	
		
//		//////////////////////////////////////////////
//		//This is just for testing which percentage of the test is for In-Sentence relation
//		//////////////////////////////////////////////
////		for (MLExample test_ee:testExamples_ee)
////		{
////			MLExample.setExamplePredictedClass(test_ee.getExampleId(),test_ee.getExpectedClass());
////			test_ee.setPredictedClass(test_ee.getExpectedClass());
////
////
////		}
//		FileUtil.logLine(null,"EventEvent-------- ");
		
//		//////////////////////////////////////////////
//		/// SecTimeEvent
//		//////////////////////////////////////////////
/////////////////***********///////////////////
		MLExample.resetExamplesPredicted(SecTimeEventExampleBuilder.ExperimentGroupSecTimeEvent, false);
		List<MLExample> testExamples_se = 
			MLExample.getAllExamples(
					SecTimeEventExampleBuilder.ExperimentGroupSecTimeEvent, false);
//		TODO: uncomment this section
		
		if (run_option.equals("justrules"))
		{
			SecTimeEventRuleBasedPrediction sectime_rule_engine_test = new SecTimeEventRuleBasedPrediction();
			List<MLExample> classifierInputExamples_se = sectime_rule_engine_test.predictByRules(testExamples_se);
		}
		else
			if (run_option.equals("justclassifier"))
		{
//			SVMMultiClass svmsectime = (SVMMultiClass) 
//			SVMMultiClass.getLearnerEngine(SecTimeEventExampleBuilder.ExperimentGroupSecTimeEvent);
//
//			List<MLExample> trainExamples_se = 
//				MLExample.getAllExamples(
//						SecTimeEventExampleBuilder.ExperimentGroupSecTimeEvent, true);
//			svmsectime.train(trainExamples_se);
//			svmsectime.test(testExamples_se);
				SVMMultiClass svmsectime = (SVMMultiClass) 
				SVMMultiClass.getLearnerEngine(SecTimeEventExampleBuilder.ExperimentGroupSecTimeEvent);

				List<MLExample> trainExamples_se = 
						MLExample.getAllExamples(
								SecTimeEventExampleBuilder.ExperimentGroupSecTimeEvent, true);
				
			//	SecTimeEventRuleBasedPrediction sectime_rule_engine = new SecTimeEventRuleBasedPrediction();
			//	List<MLExample> classifierInputTrainingExamples_se = sectime_rule_engine.predictByRules(trainExamples_se);
				
				svmsectime.train(trainExamples_se);
//				MLExample.hibernateSession = HibernateUtil.clearSession(MLExample.hibernateSession);
				
				
				if (rules_after_classifier)
				{
					svmsectime.test(testExamples_se);
					List<MLExample> rule_inputs_se = new ArrayList<MLExample>();
					
					for (MLExample se: rule_inputs_se)
					{
						if (se.getPredictedClass()==0)
							rule_inputs_se.add(se);
					}
					SecTimeEventRuleBasedPrediction se_rule_engine = new SecTimeEventRuleBasedPrediction();
					List<MLExample> classifierInputExamples_se = se_rule_engine.predictByRules(rule_inputs_se);
				}
				else
				{
					SecTimeEventRuleBasedPrediction sectime_rule_engine_test = new SecTimeEventRuleBasedPrediction();
					List<MLExample> classifierInputExamples_se = sectime_rule_engine_test.predictByRules(testExamples_se);
					svmsectime.test(classifierInputExamples_se);
				}
			
		}
		else
		{
			SVMMultiClass svmsectime = (SVMMultiClass) 
			SVMMultiClass.getLearnerEngine(SecTimeEventExampleBuilder.ExperimentGroupSecTimeEvent);

			List<MLExample> trainExamples_se = 
					MLExample.getAllExamples(
							SecTimeEventExampleBuilder.ExperimentGroupSecTimeEvent, true);
			
		//	SecTimeEventRuleBasedPrediction sectime_rule_engine = new SecTimeEventRuleBasedPrediction();
		//	List<MLExample> classifierInputTrainingExamples_se = sectime_rule_engine.predictByRules(trainExamples_se);
			
			svmsectime.train(trainExamples_se);
//			MLExample.hibernateSession = HibernateUtil.clearSession(MLExample.hibernateSession);
			
			
			if (rules_after_classifier)
			{
				svmsectime.test(testExamples_se);
				List<MLExample> rule_inputs_se = new ArrayList<MLExample>();
				
				for (MLExample se: rule_inputs_se)
				{
					if (se.getPredictedClass()==0)
						rule_inputs_se.add(se);
				}
				SecTimeEventRuleBasedPrediction se_rule_engine = new SecTimeEventRuleBasedPrediction();
				List<MLExample> classifierInputExamples_se = se_rule_engine.predictByRules(rule_inputs_se);
			}
			else
			{
				SecTimeEventRuleBasedPrediction sectime_rule_engine_test = new SecTimeEventRuleBasedPrediction();
				List<MLExample> classifierInputExamples_se = sectime_rule_engine_test.predictByRules(testExamples_se);
				svmsectime.test(classifierInputExamples_se);
			}
		}
///////////////////***********///////////////////
//		MLExample.hibernateSession = HibernateUtil.clearSession(MLExample.hibernateSession);
//		HibernateUtil.clearLoaderSession();
//		svmsectime.test(testExamples_se);
		
//		for (MLExample test_se:testExamples_se)
//		{
//			MLExample.setExamplePredictedClass(test_se.getExampleId(),test_se.getExpectedClass());
//			test_se.setPredictedClass(test_se.getExpectedClass());
//		}
		allTestData.addAll(testExamples_ee);
		allTestData.addAll(testExamples_et);
		allTestData.addAll(testExamples_se);
		allTestData.addAll(testExamples_between);

		
		List<MLExample> testExamples_rulesure = 
				MLExample.getAllExamples(Setting.RuleSureCorpus,  false);
		allTestData.addAll(testExamples_rulesure);
		
		i2b2OutputGenerator.generateOutPutFiles(allTestData, Setting.getValue("InputTestFolderOnTest"), 
				Setting.getValue("TestOutput"));
		Evaluator.getEvaluationResult(allTestData).printResult();
	}
}