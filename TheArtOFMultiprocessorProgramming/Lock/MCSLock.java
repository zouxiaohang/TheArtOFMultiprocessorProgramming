import java.util.concurrent.atomic.AtomicReference;

public class MCSLock{
	private class QNode{
		public boolean locked = false;
		public QNode next = null;
	}
	AtomicReference<QNode> tail;
	ThreadLocal<QNode> myNode;
	
	public MCSLock(){
		tail = new AtomicReference<QNode>(null);
		myNode = new ThreadLocal<QNode>(){
			protected QNode initialValue(){
				return new QNode();
			}
		};
	}
	
	public void lock(){
		QNode node = myNode.get();
		QNode pred = tail.getAndSet(node);
		if(pred != null){
			node.locked = true;
			pred.next = node;
			while(node.locked){}
		}
	}
	public void unlock(){
		QNode node = myNode.get();
		if(node.next == null){
			if(tail.compareAndSet(node, null))
				return;
			while(node.next == null){}
		}
		node.next.locked = false;
		node.next = null;
	}
}