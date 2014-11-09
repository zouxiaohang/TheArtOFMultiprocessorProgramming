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
		if(pred != null){//此时queue中有node不能直接拿锁
			node.locked = true;
			pred.next = node;//将自己加入到queue中
			/*
			*缺点是需要前继来改变自己的locked域，可能会造成unbounded wait
			*/
			while(node.locked){}//在自己的locked域上spin
		}
	}
	public void unlock(){
		QNode node = myNode.get();
		if(node.next == null){//此时我的node没有后继
			if(tail.compareAndSet(node, null))//判断此时queue中是否只有我一个node
				return;
			//此时tail不指向我的node说明有新的node正在加入queue中
			//此时等待我的后继node完成add操作，将我的next域设为后继node
			while(node.next == null){}
		}
		node.next.locked = false;
		node.next = null;//将我的node从queue中dequeue
	}
}