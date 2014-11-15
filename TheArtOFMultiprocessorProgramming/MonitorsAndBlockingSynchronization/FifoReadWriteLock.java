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
				/*
				*潜在bug：readAcquires只加不减会造成溢出从而和readReleases匹配出错
				*/
				++readAcquires;
			}finally{
				lock.unlock();
			}
		}
		public void unlock(){
			lock.lock();
			try{
				/*
				*潜在bug：readReleases只加不减会造成溢出从而和readAcquires匹配出错
				*/
				++readReleases;
				if(readAcquires == readReleases)//当两者相等时表示无线程持有lock
					condition.signalAll();
				//我的fix bug代码：
				//将上两句改为：
				/*
				if(readAcquires == readReleases){
					readAcquires = readReleases = 0;
					condition.signalAll();
				}
				*/
			}finally{
				lock.unlock();
			}
		}
	}
	private class WriteLock{
		public void lock() throws InterruptedException{
			lock.lock();
			try{
				while(writer){//当线程持有写锁时才wait
					condition.await();
				}
				//设置writer为true，防止有新的readLock被持有
				writer = true;
				//当还有读锁被持有时则等待
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