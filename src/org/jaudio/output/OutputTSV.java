/**
 * 
 */
package org.jaudio.output;

import java.util.HashMap;
import java.util.Vector;

import org.jaudio.Buffer;
import org.jaudio.FeatureIn;
import org.jaudio.FeatureOut;
import org.jaudio.SynchronizedBuffer;

/**
 * @author dmcennis
 * 
 */
public class OutputTSV extends Thread implements FeatureIn {

	int getWindowSize = 0;

	Integer monitor = new Integer(0);

	long currentOutputCount = 1;

	String name = "Output TSV";

	HashMap<SynchronizedBuffer, double[]> inputDataMaps = new HashMap<SynchronizedBuffer, double[]>();
	Vector<SynchronizedBuffer> inputDataVector = new Vector<SynchronizedBuffer>();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jaudio.FeatureIn#getInputWindowSize()
	 */
	@Override
	public int getInputWindowSize(SynchronizedBuffer buffer) {
		if(!inputDataMaps.containsKey(buffer)){
			return 0;
		}
		return inputDataMaps.get(buffer).length;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jaudio.FeatureIn#doGet(org.jaudio.Buffer)
	 */
	@Override
	public void doGet(Buffer buff) {
		synchronized (monitor) {
			monitor.notifyAll();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jaudio.FeatureIn#setWindowSize(int)
	 */
	@Override
	public void setWindowSize(int windowSize,SynchronizedBuffer buffer) {
		inputDataMaps.put(buffer, new double[windowSize]);
		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jaudio.FeatureIn#setBuffer(org.jaudio.SynchronizedBuffer,
	 * org.jaudio.FeatureOut)
	 */
	@Override
	public void setBuffer(SynchronizedBuffer buffer, FeatureOut out) {
		double[] data = new double[buffer.getWindowSize()];
		inputDataMaps.put(buffer, data);
		if (buffer.getWindowCount() > currentOutputCount) {
			currentOutputCount = buffer.getWindowCount();
		}
		inputDataVector.add(buffer);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jaudio.FeatureIn#getNameID()
	 */
	@Override
	public String getNameID() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jaudio.FeatureIn#setNameID(java.lang.String)
	 */
	@Override
	public void setNameID(String name) {
		this.name = name;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jaudio.FeatureIn#getWindowCount()
	 */
	@Override
	public long getWindowCount() {
		return currentOutputCount - 1;
	}

	@Override
	public void run() {
		boolean[] doneArray = new boolean[inputDataVector.size()];
		for(int i=0;i<doneArray.length;++i){
			doneArray[i] = false;
		}
		boolean done = false;
		boolean notEven;
		while (!done) {
			do {
				notEven = false;
				for (int i=0;i<inputDataVector.size();++i){
					if (inputDataVector.get(i).isEof()) {
						doneArray[i]=true;
					}
					if (inputDataVector.get(i).getWindowCount(this) < currentOutputCount) {
						notEven = true;
						inputDataVector.get(i).lock(inputDataMaps.get(inputDataVector.get(i)), this, null, true);
					}
				}
				done = true;
				for(int i=0;i<doneArray.length;++i){
					if(!doneArray[i]){
						done = false;
					}
				}
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} while (notEven && !done);
			if(!done){
				print();
				currentOutputCount++;
			}
		}
		notEven=false;
		for (SynchronizedBuffer buff : inputDataVector) {
			buff.lock(inputDataMaps.get(buff), this, null, true);
//			System.out.println(buff.getWindowCount(this));
			if (buff.getWindowCount(this) < currentOutputCount) {
				notEven = true;
			}
		}
//		System.out.println(currentOutputCount);
		if(!notEven){
			print();
		}
		System.out.println("Output Done");
	}

	private void print() {
		int count = 0;
		for (SynchronizedBuffer buff : inputDataVector) {
			if (count != 0) {
				System.out.print("\t");
			}
			int count2 = 0;
			double[] data = inputDataMaps.get(buff);
			for (int i = 0; i < data.length; ++i) {
				if (count2 != 0) {
					System.out.print("\t");
				}
				System.out.print(data[i]);
				count2++;
			}
			count++;
		}
		System.out.println();
	}
}
