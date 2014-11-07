import java.util.concurrent.atomic.*;

public class BackoffLock{
	private AtomicBoolean state = new AtomicBoolean(false);
	private final int MIN_DELAY;//you choose a appropriate value
	private final int MAX_DELAY;//you choose a appropriate value
	/*
	*这个锁的性能与MIN_DELAY、MAX_DELAY的值的选取有直接的关系，
	*且所有的线程都在同一个location上spin，使得当成功lock时造成cache-coherence traffic
	*/
	public BackoffLock(int min, int max){
		MIN_DELAY = min;
		MAX_DELAY = max;
	}
	public void lock(){
		Backoff backoff = new Backoff(MIN_DELAY, MAX_DELAY);
		while(true){
			while(state.get()){}
			if(!state.getAndSet(true)){
				return;
			}else{
				try {
					backoff.backoff();
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
		}
	}
	public void unlock(){
		state.set(false);
	}
}
