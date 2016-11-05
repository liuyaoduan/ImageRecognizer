package cn.edu.hunau.itc.imgrcg.ml;

import java.util.concurrent.ExecutorService;

public abstract class BuildListener {
	protected ExecutorService executor;
	
	public abstract void success();
	public void failed() {
		if (this.executor != null && !this.executor.isShutdown()) {
			this.executor.shutdownNow();
		}
		
	};
}
