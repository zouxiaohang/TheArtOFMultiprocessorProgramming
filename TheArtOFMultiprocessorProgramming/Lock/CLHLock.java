import java.util.concurrent.atomic.AtomicReference;

class QNode{
	public boolean locked = false;
}

public class CLHLock{
	private AtomicReference<QNode> tail;
	private ThreadLocal<QNode> myPred;
	private ThreadLocal<QNode> myNode;
	
	public CLHLock(){
		//tail = new AtomicReference<QNode>(null);
		tail = new AtomicReference<QNode>(new QNode());
		myNode = new ThreadLocal<QNode>(){
			protected QNode initialValue(){
				return new QNode();
			}
		};
		myPred = new ThreadLocal<QNode>(){
			protected QNode initialValue(){
				return null;
			}
		};
	}
	
	public void lock(){
		QNode node = myNode.get();
		node.locked = true;
		QNode pred = tail.getAndSet(node);
		myPred.set(pred);
		while(pred.locked){}
	}
	public void unlock(){
		QNode node = myNode.get();
		node.locked = false;
		myNode.set(myPred.get());
	}
}