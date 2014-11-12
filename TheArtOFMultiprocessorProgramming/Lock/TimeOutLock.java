import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class TimeOutLock{
	static class QNode{
		//此变量不是为了连接queue的，而是起到表明当前线程对lock的状态
		public QNode pred = null;
	}
	static QNode AVAILABLE = new QNode();
	AtomicReference<QNode> tail;
	ThreadLocal<QNode> myNode;
	
	public TimeOutLock(){
		tail = new AtomicReference<QNode>(null);
		myNode = new ThreadLocal<QNode>(){
			protected QNode initialValue(){
				return new QNode();
			}
		};
	}
	public boolean tryLock(long time, TimeUnit unit){
		long startTime = System.currentTimeMillis();
		long patience = TimeUnit.MILLISECONDS.convert(time, unit);
		//缺点：每次都要进行昂贵的new操作，而不能像前面那样reuse node
		QNode qnode = new QNode();
		myNode.set(qnode);
		qnode.pred = null;
		QNode myPred = tail.getAndSet(qnode);
		if(myPred == null || //前面无线程持有锁
			myPred.pred == AVAILABLE){//前面线程已unlock
			return true;
		}
		while(System.currentTimeMillis() - startTime < patience){
			QNode predPred = myPred.pred;//缓存前面线程的lock状态
			if(predPred == AVAILABLE){//表明pred已经unlock
				return true;
			}else if(predPred != null){//表明pred因超时放弃lock，并且已经redirect至其pred上
				myPred = predPred;
			}
		}
		if(!tail.compareAndSet(qnode, myPred))
			qnode.pred = myPred;//redirect（我后继查看我的状态相当于看我的pred的状态）
		return false;
	}
	public void unlock(){
		QNode qnode = myNode.get();
		if(!tail.compareAndSet(qnode, null))//此时不止我一个线程在queue中
			qnode.pred = AVAILABLE;
	}
}