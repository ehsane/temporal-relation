package rainbownlp.i2b2.sharedtask2012.ruleengines;

import java.util.List;

import rainbownlp.machinelearning.MLExample;

public interface IRuleBasedPrediction {
	public  List<MLExample> predictByRules(List<MLExample> examples) throws Exception;
	public  int predictByRules(MLExample example) throws Exception;

}
