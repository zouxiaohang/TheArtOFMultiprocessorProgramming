import java.util.concurrent.atomic.*;

class TTASLock{
	private AtomicBoolean flag = new AtomicBoolean(false);
	
	void lock(){
		while(true){
			while(flag.get()){}//local spin
			if(!flag.getAndSet(true))
				return;
		}
	}
	void unlock(){
		flag.set(false);
	}
}