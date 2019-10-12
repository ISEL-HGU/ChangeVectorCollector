package change.vector.collector;

public class BeforeBIC {
	public String pathBefore;
	public String pathBIC;
	public String shaBefore;
	public String shaBIC;
	
	public BeforeBIC(String pathBefore, String pathBIC, String shaBefore, String shaBIC) {
		this.pathBefore = pathBefore;
		this.pathBIC = pathBIC;
		this.shaBefore = shaBefore;
		this.shaBIC = shaBIC;
	}
	
	@Override
	public String toString() {
		return "pathBefore= " + pathBefore + "pathBIC= " + pathBIC
				+ "shaBefore= " + shaBefore + "shaBIC= " + shaBIC;
	}
}
