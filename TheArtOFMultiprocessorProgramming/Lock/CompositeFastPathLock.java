import java.security.KeyStore.LoadStoreParameter;
import java.util.concurrent.TimeUnit;

public class CompositeFastPathLock extends CompositeLock{
	private final int FASTPATH;
	
	public CompositeFastPathLock(int size, int min, int max, int path) {
		super(size, min, max);
		FASTPATH = path;
	}
	
	private boolean fastPathLock(){
		int oldStamp, newStamp;
		int stamp[] = {0};
		QNode qnode;
		qnode = tail.get(stamp);
		oldStamp = stamp[0];
		if(qnode != null){//已有node加入了queue
			return false;
		}
		if((oldStamp & FASTPATH) != 0){//已经set了FASTPATH flag
			return false;
		}
		newStamp = (oldStamp + 1) | FASTPATH;//set FASTPATH flag
		return tail.compareAndSet(qnode, null, oldStamp, newStamp);
	}
	public boolean tryLock(long time, TimeUnit unit) throws InterruptedException{
		if(fastPathLock()){
			return true;
		}
		if(super.tryLock(time, unit)){
			//spin直到没有任何线程hold fast-path lock
			while((tail.getStamp() & FASTPATH) != 0){};
			return true;
		}
		return false;
	}
	private boolean fastPathUnlock(){
		int oldStamp, newStamp;
		oldStamp = tail.getStamp();
		if((oldStamp & FASTPATH) == 0){//没有set FASTPATH flag
			return false;
		}
		int[] stamp = {0};
		QNode qnode;
		do{
			qnode = tail.get(stamp);
			oldStamp = stamp[0];
			newStamp = oldStamp & (~FASTPATH);
		}while(!tail.compareAndSet(qnode, qnode, oldStamp, newStamp));
		return true;
	}
	public void unlock(){
		if(!fastPathUnlock()){
			super.unlock();//没有set FASTPATH flag则调用父类的方法
		}
	}
}