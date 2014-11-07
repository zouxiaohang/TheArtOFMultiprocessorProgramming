import java.util.concurrent.atomic.*;

public class TTASLock{
	private AtomicBoolean flag = new AtomicBoolean(false);
	
	public void lock(){
		while(true){
			/*
			*使用local spin技术，减少了无谓的cache coherence和bus占用
			*/
			while(flag.get()){}//local spin
			if(!flag.getAndSet(true))
				return;
		}
	}
	public void unlock(){
		/*
		*此时会使所有local spin的线程cache 失效，大量线程改写flag，
		*造成high contention
		*/
		flag.set(false);
	}
	public boolean isLocked(){
		return flag.get();
	}
}