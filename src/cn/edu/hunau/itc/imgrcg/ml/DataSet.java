package cn.edu.hunau.itc.imgrcg.ml;

import java.io.File;

import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public class DataSet {
	
	private Instances trainData;
	private Instances evalData;
	
	
	public Instances getTrainData() {
		return trainData;
	}
	public Instances getEvalData() {
		return evalData;
	}
	
	public static DataSet loadDataSet(File train, File eval) {
		return DataSet.loadDataSet(train.getAbsolutePath(), eval.getAbsolutePath());
	}
	public static DataSet loadDataSet(String train, String eval) {
		DataSet dataSet = new DataSet();
		try {
			dataSet.trainData = DataSource.read(train);
			dataSet.evalData = DataSource.read(eval);
		} catch (Exception e) {
			e.printStackTrace();
			dataSet = null;
		}
		
		dataSet.setClassColumn(dataSet.trainData.numAttributes() - 1, dataSet.evalData.numAttributes() - 1);
		
		return dataSet;
	}
	
	public void setClassColumn(int trainDataClassIndex, int evalDataClassIndex) {
		this.trainData.setClassIndex(trainDataClassIndex);
		this.evalData.setClassIndex(evalDataClassIndex);
	}
}
