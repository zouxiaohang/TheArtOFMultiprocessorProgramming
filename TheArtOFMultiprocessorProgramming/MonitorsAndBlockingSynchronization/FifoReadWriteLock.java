import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FifoReadWriteLock{
	int readAcquires, readReleases;
	boolean writer;
	Lock lock;
	Condition condition;
	ReadLock readLock;
	WriteLock writeLock;
	private class ReadLock{
		public void lock() throws InterruptedException{
			lock.lock();
			try{
				while(writer){
					condition.await();
				}
				++readAcquires;
			}finally{
				lock.unlock();
			}
		}
		public void unlock(){
			lock.lock();
			try{
				++readReleases;
				if(readAcquires == readReleases)
					condition.signalAll();
			}finally{
				lock.unlock();
			}
		}
	}
	private class WriteLock{
		public void lock() throws InterruptedException{
			lock.lock();
			try{
				while(writer){
					condition.await();
				}
				writer = true;
				while(readAcquires != readReleases){
					condition.await();
				}
			}finally{
				lock.unlock();
			}
		}
		public void unlock(){
			writer = false;
			condition.signalAll();
		}
	}
	
	public FifoReadWriteLock(){
		readAcquires = readReleases = 0;
		writer = true;
		lock = new ReentrantLock(true);
		condition = lock.newCondition();
		readLock = new ReadLock();
		writeLock = new WriteLock();
	}
	public ReadLock readLock(){
		return readLock;
	}
	public WriteLock writeLock(){
		return writeLock;
	}
}