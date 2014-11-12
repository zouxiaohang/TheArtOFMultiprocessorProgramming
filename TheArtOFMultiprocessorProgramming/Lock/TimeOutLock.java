import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class TimeOutLock{
	static class QNode{
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
		QNode qnode = new QNode();
		myNode.set(qnode);
		qnode.pred = null;
		QNode myPred = tail.getAndSet(qnode);
		if(myPred == null || myPred.pred == AVAILABLE){
			return true;
		}
		while(System.currentTimeMillis() - startTime < patience){
			QNode predPred = myPred.pred;
			if(predPred == AVAILABLE){
				return true;
			}else if(predPred != null){
				myPred = predPred;
			}
		}
		if(!tail.compareAndSet(qnode, myPred))
			qnode.pred = myPred;
		return false;
	}
	public void unlock(){
		QNode qnode = myNode.get();
		if(!tail.compareAndSet(qnode, null))
			qnode.pred = AVAILABLE;
	}
}