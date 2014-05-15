import java.io.File;

public class TVLAResetTest {
	public static void main(String[] args) throws Exception {
//		String outputTVS = "program.out.tvs";
		tvla.Runner.main(args);
//		System.out.println(outputTVS + " exists: "
//				+ (new File(outputTVS)).exists()); // TRUE
		
		tvla.Runner.reset();
//		(new File(outputTVS)).delete();
		tvla.Runner.main(args);
//		System.out.println(outputTVS + " exists: "
//				+ (new File(outputTVS)).exists()); // FALSE
	}
}