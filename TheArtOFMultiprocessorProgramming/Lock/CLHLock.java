import java.util.concurrent.atomic.AtomicReference;

class QNode{
	public boolean locked = false;
}

public class CLHLock{
	private AtomicReference<QNode> tail;
	private ThreadLocal<QNode> myPred;
	private ThreadLocal<QNode> myNode;
	
	public CLHLock(){
		//原书有误，tail初始化的时候应该指向一个QNode否则第一个加入的thread就不能重用先前的QNode了
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
		/*
		*无限制的wait存在缺陷，且实在pred node的locked域上spin，
		*在cache-less NUMA架构的环境上性能会变得poor
		*/
		while(pred.locked){}
	}
	public void unlock(){
		QNode node = myNode.get();
		node.locked = false;
		//重用pred node
		myNode.set(myPred.get());
	}
}