package tvla.api;

public interface ITVLAAPIDebuggingServices {
	/**
	 * @returns whehter traces are printed or 
	 * ignored
	 * 
	 */
	public abstract boolean trace();

	/**
	 * Prints a trace print if current trace level is 
	 * @param str the trace message to print
	 */
	public abstract void tracePrint(String str);

	/**
	 * Prints a trace print with trace level 0 (with a new line)
	 * @param str the trace message to print
	 */
	public abstract void tracePrintln(String str);
		
	public abstract void debugAssert(boolean b);
	public abstract void debugAssert(boolean b, String msg);


    public abstract void UNREACHABLE();
    public abstract void UNREACHABLE(String msg);

    
	/**
	 * TVLA raised an exception.
	 * Instead of throwing it to the client, it "registers" it 
	 * and returns an error code o the client.
	 * this way we do not ned to specify throws clauses etc.
	 * (this is a qucik, but not too dirty solution as the oly exception TVLA have
	 *  is ParserError and (maybe) CoerceAfterUdateBreach))
	 * @param e
	 */
	public abstract void registerException(Exception e);
}
