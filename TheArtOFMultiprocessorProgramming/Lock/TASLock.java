import java.util.concurrent.atomic.*;

public class TASLock{
	private AtomicBoolean flag = new AtomicBoolean(false);
	public void lock(){
		/*（1）flag的每一次getAndSet都会重写其cache中的值，
		*由于flag是其他线程共享的因此会invalidate其他线程的cache，
		*造成无谓的cache coherence，降低性能。
		*（2）又由于每次都重写flag使得写的过程中占用了bus，造成bus traffic jam，
		*delay了其他线程，甚至是delay了需要release lock的线程。
		*/
		while(flag.getAndSet(true)){}
	}
	public void unlock(){
		flag.set(false);
	}
	public boolean isLocked(){
		return flag.get();
	}
}