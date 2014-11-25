import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Semaphore{
	final int capacity;
	int state;
	Lock lock;
	Condition condition;
	
	public Semaphore(int c){
		capacity = c;
		state = 0;
		lock = new ReentrantLock();
		condition = lock.newCondition();
	}
	public void acquire() throws InterruptedException{
		lock.lock();
		try{
			while(state == capacity){
				condition.await();//等待有线程离开CS区
			}
			++state;
		}finally{
			lock.unlock();
		}
	}
	public void release(){
		lock.lock();
		try{
			--state;
			condition.signalAll();//通知有线程离开CS区了
		}finally{
			lock.unlock();
		}
	}
}