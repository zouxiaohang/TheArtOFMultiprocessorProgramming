import java.util.concurrent.atomic.*;

class ArrayLock{
	private ThreadLocal<Integer> mySlotIndex = new ThreadLocal<Integer>(){
		protected Integer initialValue(){
			return 0;
		}
	};
	private AtomicInteger tail;
	private volatile boolean []flag;
	private final int size;
	public ArrayLock(int capacity){
		size = capacity;
		flag = new boolean[size];
		flag[0] = true;
		tail = new AtomicInteger(0);
	}
	/*
	 *当数组size很小而线程很多时，会造成数组下标(slot)回环，
	 *引发两个潜在bug，使得多个线程能同时进入critical section中 
	 */
	public void lock(){
		Integer slot = tail.getAndIncrement() % size;
		mySlotIndex.set(slot);
		while(!flag[slot]){}//在我的slot上spin，直到值为true
	}
	public void unlock(){
		Integer slot = mySlotIndex.get();
		flag[slot] = false;//将flag[slot]设为false表示我放弃使用lock
		flag[(slot + 1) % size] = true;//将我的后继位置上的值设为true，表示将lock转让给他
	}
}