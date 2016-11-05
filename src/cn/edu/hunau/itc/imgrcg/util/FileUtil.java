package cn.edu.hunau.itc.imgrcg.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;

public class FileUtil {
	public static void writeObject(File outputFile, Object object) {
		FileOutputStream fileOutputStream = null;
    	ObjectOutputStream objectOutputStream = null;
    	try {
        	fileOutputStream = new FileOutputStream(outputFile);
        	objectOutputStream = new ObjectOutputStream(fileOutputStream);
        	objectOutputStream.writeObject(object);
    	} catch (IOException e) {
    		e.printStackTrace();
    	} finally {
    		try {
    			if (objectOutputStream != null) {
        			objectOutputStream.close();
        		}
    			if (fileOutputStream != null) {
        			fileOutputStream.close();
        		}
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
		}
	}
	
	public static Object readObject(File inputFile) {
		Object object = null;
		
		FileInputStream fileInputStream = null;
    	ObjectInputStream objectInputStream = null;
    	
    	try {
    		if (inputFile.exists()) {
        		fileInputStream = new FileInputStream(inputFile);
        		objectInputStream = new ObjectInputStream(fileInputStream);
        		object = objectInputStream.readObject();
        	}
    	} catch (IOException e) {
    		e.printStackTrace();
    	} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				if (objectInputStream != null) {
					objectInputStream.close();
				}
				if (fileInputStream != null) {
					fileInputStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
    	return object;
	}
	
	public static String readString(File inputFile, String charsetName) {
		StringBuffer sb = new StringBuffer();
		
		FileInputStream fileInputStream = null;
		byte[] buf = new byte[4096];
		
		try {
			fileInputStream = new FileInputStream(inputFile);
			
			while (fileInputStream.read(buf) != -1) {
				sb.append(new String(buf, charsetName));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return sb.toString();
		
	}
	
	public static void copy(File srcFile, File desFile) {
		FileInputStream fin = null;
		FileOutputStream fout = null;
		FileChannel in = null;
		FileChannel out = null;
		
		
		try {
			fin = new FileInputStream(srcFile);
			fout = new FileOutputStream(desFile);
			in = fin.getChannel();
			out = fout.getChannel();
			in.transferTo(0, in.size(), out);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				in.close();
				out.close();
				fin.close();
				fout.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
