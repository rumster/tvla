package tvla.language.BUC;

public class BuchiStateAST
{
	public String name;
	public boolean isAccepting;
	
	public BuchiStateAST(String name,boolean accepting) {
		this.name = name;
		this.isAccepting = accepting;
	}
}
