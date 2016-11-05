package cn.edu.hunau.itc.imgrcg.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Global {
	
	public static ExecutorService getThreadPool() {
		return Executors.newFixedThreadPool(4);
	}
}
