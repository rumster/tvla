package tvla.api;

import java.io.PrintStream;

public interface ITVLAAssertion {
	void dumpTVS(PrintStream printTo);
	String toString();
}
