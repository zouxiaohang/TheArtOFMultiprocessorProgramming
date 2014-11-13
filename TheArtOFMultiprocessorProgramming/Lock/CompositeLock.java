import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

public class CompositeLock{
	enum State{FREE, WAITING, RELEASE, ABORTED};
	class QNode{
		AtomicReference<State> state;
		QNode pred;
		public QNode(){
			state = new AtomicReference<State>(State.FREE);
		}
	}
	
	private final int SIZE;
	private final int MIN_BACKOFF;
	private final int MAX_BACKOOF;
	//避免ABA问题
	AtomicStampedReference<QNode> tail;
	QNode[] waiting;
	Random random;
	ThreadLocal<QNode> myNode = new ThreadLocal<QNode>(){
		protected QNode initialValue(){return null;}
	};
	
	public CompositeLock(int size, int min, int max){
		SIZE = size;
		MIN_BACKOFF = min;
		MAX_BACKOOF = max;
		tail = new AtomicStampedReference<QNode>(null, 0);
		waiting = new QNode[SIZE];
		for(int i = 0; i != SIZE; ++i){
			waiting[i] = new QNode();
		}
		random = new Random();
	}
	private boolean timeout(long patience, long startTime){
		if(System.currentTimeMillis() - startTime > patience){
			return true;
		}
		return false;
	}
	private QNode acquireQNode(Backoff backoff, long startTime, long patience) throws TimeoutException, InterruptedException {
		QNode node = waiting[random.nextInt(SIZE)];
		QNode currTail;
		int []currStamp = {0};
		while(true){
			//如果此时node状态为free(没线程占用且不在queue中)
			if(node.state.compareAndSet(State.FREE, State.WAITING)){
				return node;
			}
			currTail = tail.get(currStamp);
			State state = node.state.get();
			//如果node没线程占用但在queue中
			if(state == State.ABORTED || state == State.RELEASE){
				if(node == currTail){//此时node为queue tail
					QNode myPred = null;
					//如果tail为aborted则redirect至pred，否则tail为release，设为null
					if(state == State.ABORTED){
						myPred = node.pred;
					}
					if(tail.compareAndSet(currTail, myPred, currStamp[0], currStamp[0] + 1)){
						node.state.set(State.WAITING);
						return node;
					}
				}
			}
			//如果node为waiting状态则wait
			backoff.backoff();
			if(timeout(patience, startTime)){
				throw new TimeoutException();
			}
		}
	}
	private QNode spliceQNode(QNode node, long startTime, long patience) throws TimeoutException{
		QNode currTail;
		int [] currStamp = {0};
		do{
			currTail = tail.get(currStamp);
			if(timeout(patience, startTime)){
				node.state.set(State.FREE);
				throw new TimeoutException();
			}
		//在此间隙，tail没被其他线程更改，则将node设为tail并返回tail
		}while(!tail.compareAndSet(currTail, node, currStamp[0], currStamp[0] + 1));
		return currTail;
	}
	private void waitForPredecessor(QNode pred, QNode node, long startTime, long patience) throws TimeoutException{
		int []stamp = {0};
		if(pred == null){//表明node是queue head
			myNode.set(node);
			return ;
		}
		State predState = pred.state.get();
		while(predState != State.RELEASE){//等待pred被线程设为release
			if(predState == State.ABORTED){//pred等待超时
				QNode temp = pred;
				pred = pred.pred;//将超时的node从queue中去除
				temp.state.set(State.FREE);
			}
			if(timeout(patience, startTime)){//node超时
				node.pred = pred;//为了让后面的node跳过自己
				node.state.set(State.ABORTED);
				throw new TimeoutException();
			}
			predState = pred.state.get();//再次读取state
		}
		pred.state.set(State.FREE);
		myNode.set(node);
		return;
	}
	public boolean tryLock(long time, TimeUnit unit) throws InterruptedException{
		long patience = TimeUnit.MILLISECONDS.convert(time,  unit);
		long startTime = System.currentTimeMillis();
		Backoff backoff = new Backoff(MIN_BACKOFF, MAX_BACKOOF);
		try{
			//从waiting中拿到一个node
			QNode node = acquireQNode(backoff, startTime, patience);
			//将这个node加入queue中
			QNode pred = spliceQNode(node, startTime, patience);
			//等待node成为queue head
			waitForPredecessor(pred, node, startTime, patience);
			return true;
		}catch (TimeoutException e) {
			return false;
		}
	}
	public void unlock(){
		QNode acqNode = myNode.get();
		acqNode.state.set(State.RELEASE);
		myNode.set(null);
	}
}