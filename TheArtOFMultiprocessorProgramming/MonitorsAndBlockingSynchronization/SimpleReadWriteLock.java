import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SimpleReadWriteLock{
	int readers;
	boolean writer;
	Lock lock;
	Condition condition;
	ReadLock readLock;
	WriteLock writeLock;
	protected class ReadLock{
		public void lock() throws InterruptedException{
			lock.lock();
			try{
				while(writer){
					condition.await();
				}
				++readers; 
			}finally{
				lock.unlock();
			}
		}
		public void unlock(){
			lock.lock();
			try{
				--readers;
				if(readers == 0)
					condition.signalAll();
			}finally{
				lock.unlock();
			}
		}
	}
	protected class WriteLock{
		public void lock() throws InterruptedException{
			lock.lock();
			try{
				while(readers > 0 || writer){
					condition.await();
				}
				writer = true;
			}finally{
				lock.unlock();
			}
		}
		public void unlock(){
			lock.lock();
			try{
				writer = false;
				condition.signalAll();
			}finally{
				lock.unlock();
			}
		}
	}
	
	public SimpleReadWriteLock(){
		writer = false;
		readers = 0;
		lock = new ReentrantLock();
		readLock = new ReadLock();
		writeLock = new WriteLock();
		condition = lock.newCondition();
	}
	public ReadLock readLock(){
		return readLock;
	}
	public WriteLock writeLock(){
		return writeLock;
	}
}