package cn.edu.hunau.itc.imgrcg.ml;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import cn.edu.hunau.itc.imgrcg.util.Global;
import weka.classifiers.Classifier;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.functions.SMO;

public class Model {
	
	private DataSet dataSet;
	
	public <T extends Classifier> void building(T classifier) throws Exception {
		String start = "Building %s ...";
		String finish = "%s building completed.";
		
		String type = "Classifier";
		
		if (classifier instanceof SMO) {
			type = "SVM";
		} else if (classifier instanceof MultilayerPerceptron) {
			type = "NerualNetwork";
		} else {
			
		}
		
		System.out.println(String.format(start, type));
		classifier.buildClassifier(this.dataSet.getTrainData());
		System.out.println(String.format(finish, type));
	}
	
	public double evalation(Classifier classifier) throws Exception {
		Evaluation evaluation = new Evaluation(dataSet.getTrainData());
		evaluation.evaluateModel(classifier, dataSet.getEvalData());
		
		return 1 - evaluation.errorRate();
	}
	
	public <T extends Classifier> void asyncBuilding(T[] classifiers, BuildListener listener) {
		ExecutorService executor = Global.getThreadPool();
		
		AtomicInteger i = new AtomicInteger(0);
		AtomicInteger j = new AtomicInteger(classifiers.length);
		
		for (Classifier classifier : classifiers) {
			executor.execute(new Runnable() {
				
				@Override
				public void run() {
					try {
						Model.this.building(classifier);
					} catch (Exception e) {
						e.printStackTrace();
						
						executor.shutdownNow();
						listener.failed();
					}
					
					
				}
			});
		}
		while (i.get() < classifiers.length) {
			if (!executor.isShutdown()) {
				executor.execute(new Runnable() {
					
					@Override
					public void run() {
						try {
							Model.this.building(classifiers[i.getAndIncrement()]);
						} catch (Exception e) {
							e.printStackTrace();
							
							executor.shutdownNow();
							listener.failed();
						}
						
						if (!executor.isShutdown() && j.decrementAndGet() == 0) {
							listener.success();
						}
					}
					
				});
			} else {
				break;
			}
		}
		
	}
	
	
} 
