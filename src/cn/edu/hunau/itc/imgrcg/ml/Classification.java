package cn.edu.hunau.itc.imgrcg.ml;

import java.util.Arrays;

import weka.classifiers.Classifier;
import weka.core.Instances;

public class Classification {
	
	private Classifier[] models;
	private double[] precisions;
	
	public Classification(Classifier[] models) {
		this.models = models;
	}
	public Classification(Classifier[] models, double[] precisions) {
		this(models);
		this.precisions = precisions;
	}
	
	
	public int[] cascade(Instances testData) {
		int[] classIndex = new int[testData.size()];
		Arrays.fill(classIndex, 1);
		
		try {
			int[][] classIndexes = this.classify(testData, this.models);
			
			for (int i = 0; i < classIndexes.length; i++) {
				for (int j = 0; j < classIndexes[i].length; i++) {
					if (classIndex[j] != 0) {
						classIndex[j] = classIndexes[i][j];
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			
			classIndex = null;
		}
		
		return classIndex;
	}
	
	public void vote(Instances testData) {
		
	}
	
	private int[][] classify(Instances testData, Classifier[] models) throws Exception {
		int[][] classIndexes = new int[models.length][];
		
		for (int i = 0; i < models.length; i++) {
			classIndexes[i] = this.classify(testData, models[i]);
		}
		
		return classIndexes;
	}
	
	public int[] classify(Instances testData, Classifier model) throws Exception {
		int[] classIndexes = new int[testData.size()];
		
		for (int i = 0; i < testData.size(); i++) {
			classIndexes[i] = (int) model.classifyInstance(testData.get(i));
		}
		
		return classIndexes;
	}

}
