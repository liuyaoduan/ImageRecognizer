package cn.edu.hunau.itc.imgrcg.ml;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import weka.classifiers.Classifier;

public class MultiThreadLearning {
	
	public Model[] models;
	public String[] types;
	
	public Classifier[] classifiers;
	
	private MultiThreadLearning() {
		
	}
	
	private MultiThreadLearning(Model[] models, String[] types) throws Exception {
		if (models.length != types.length) {
			throw new Exception("models.length != types.length");
		} else {
			this.models = models;
			this.types = types;
			this.classifiers = new Classifier[models.length];
		}
	}
	
	public static MultiThreadLearning learning(Model[] models, String[] types) {
		MultiThreadLearning learning = null;
		try {
			learning = new MultiThreadLearning(models, types);
			learning.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return learning;
	}
	
	private void start() throws InterruptedException {
		int threadNum = this.models.length;
		CountDownLatch threadSignal = new CountDownLatch(threadNum);
		
		Executor executor = Executors.newFixedThreadPool(threadNum);
		for (int i = 0; i < threadNum; i++) {
			Runnable task = new LearningThread(threadSignal, i);
			executor.execute(task);
		}
		threadSignal.await();
	}
	
	private class LearningThread implements Runnable {
		private CountDownLatch threadsSignal;
		private Model model;
		private String type;
		private int serial;

		public LearningThread(CountDownLatch threadsSignal, int serial) {
			this.threadsSignal = threadsSignal;
			this.model = MultiThreadLearning.this.models[serial];
			this.type = MultiThreadLearning.this.types[serial];
			this.serial = serial;
		}

		public void run() {
			MultiThreadLearning.this.classifiers[serial] = model.buildClassifier(type);
			threadsSignal.countDown();
		}
	}

}
