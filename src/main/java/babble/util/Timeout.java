package babble.util;
import java.util.concurrent.TimeUnit;

/**
 * A simple immutable structure to hold a time out value.
 * 
 * @author pinaki poddar
 *
 */
public class Timeout {
    private final int _time;
    private final TimeUnit _unit;
    
    public Timeout(int time, TimeUnit unit) {
        _time = time;
        _unit = unit;
    }
    
    
    
    public int value() {
        return _time;
    }
    
    public long value(TimeUnit unit) {
        return (_unit.equals(unit)) ? _time : unit.convert(_time, _unit);
    }

    
    public TimeUnit unit() {
        return _unit;
    }
    
    public String toString() {
        return "" + _time + " " + _unit;
    }



}
