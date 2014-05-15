package tvla.util;

import java.io.PrintStream;
import java.util.Map;
import java.util.TreeMap;

/** A simple inaccurate benchmark utility.
 * Can start and stop measuring real time and return the total real time measured.
 * @author Tal Lev-Ami
 */

public class Timer {
	long total_time = 0;
	long saved_time = 0;
	boolean started = false;
	
	public synchronized boolean start() {
	    boolean result = started;
		if (!started) {
			started = true;
			saved_time = System.nanoTime();
		}
		return result;
	}
	public synchronized void stop() {
		if (started) {
			started = false;
			total_time += (System.nanoTime() - saved_time);
		}
	}
	
	public long total() {
		return total_time / 1000000;
	}
	
	// Returns the time measured so far in seconds.
	public String toString() {
		return "" + (total() / 1000.0) + "\tseconds";
	}

	protected static Map<String,Map<String,Timer>> timers = HashMapFactory.make();	
	
	public static Timer getTimer(String group, String id) {
 	    Map<String,Timer> groupTimers = timers.get(group);
	    if (groupTimers == null) {
	        groupTimers = HashMapFactory.make();
	        timers.put(group, groupTimers);
	    }
	    Timer timer = groupTimers.get(id);
        if (timer == null) {
            timer = new Timer();
            groupTimers.put(id, timer);
        }
        return timer;
	}
	
	public static void printTimerGroup(String group, PrintStream out) {
        Map<String, Timer> groupTimers = timers.get(group);
        if (groupTimers != null) {
            for (Map.Entry<String, Timer> entry : new TreeMap<String,Timer>(groupTimers).entrySet()) {
                out.println(entry.getKey() + ": " + entry.getValue());
            }	    
        }
	}
};
