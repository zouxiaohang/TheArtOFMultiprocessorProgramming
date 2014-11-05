import java.util.concurrent.atomic.*;

class TASLock{
	private AtomicBoolean flag = new AtomicBoolean(false);
	void lock(){
		while(flag.getAndSet(true)){}
	}
	void unlock(){
		flag.set(false);
	}
}