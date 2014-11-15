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
				while(writer){//只要有线程持有写锁则wait
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
					condition.signalAll();//通知所有线程此时可以持有读写锁了
			}finally{
				lock.unlock();
			}
		}
	}
	protected class WriteLock{
		public void lock() throws InterruptedException{
			lock.lock();
			try{
				//线程持有读锁或写锁则wait
				while(readers > 0 || writer){
					/*
					*缺点：如果reader过多则造成writer长久等待而得不到写锁
					*/
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
				condition.signalAll();//通知所有线程此时可以持有读写锁了
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