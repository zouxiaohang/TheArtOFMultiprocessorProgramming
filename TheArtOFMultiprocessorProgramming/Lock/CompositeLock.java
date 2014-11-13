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
			if(node.state.compareAndSet(State.FREE, State.WAITING)){
				return node;
			}
			currTail = tail.get(currStamp);
			State state = node.state.get();
			if(state == State.ABORTED || state == State.RELEASE){
				if(node == currTail){
					QNode myPred = null;
					if(state == State.ABORTED){
						myPred = node.pred;
					}
					if(tail.compareAndSet(currTail, myPred, currStamp[0], currStamp[0] + 1)){
						node.state.set(State.WAITING);
						return node;
					}
				}
			}
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
		}while(!tail.compareAndSet(currTail, node, currStamp[0], currStamp[0] + 1));
		return currTail;
	}
	private void waitForPredecessor(QNode pred, QNode node, long startTime, long patience) throws TimeoutException{
		int []stamp = {0};
		if(pred == null){
			myNode.set(node);
			return ;
		}
		State predState = pred.state.get();
		while(predState != State.RELEASE){
			if(predState == State.ABORTED){
				QNode temp = pred;
				pred = pred.pred;
				temp.state.set(State.FREE);
			}
			if(timeout(patience, startTime)){
				node.pred = pred;
				node.state.set(State.ABORTED);
				throw new TimeoutException();
			}
			predState = pred.state.get();
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
			QNode node = acquireQNode(backoff, startTime, patience);
			QNode pred = spliceQNode(node, startTime, patience);
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