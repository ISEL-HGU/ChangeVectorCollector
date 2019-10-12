package change.vector.collector;

public class BeforeBIC {
	public String pathBefore;
	public String pathBIC;
	public String shaBefore;
	public String shaBIC;
	public String key;
	
	public BeforeBIC(String pathBefore, String pathBIC, String shaBefore, String shaBIC, String key) {
		this.pathBefore = pathBefore;
		this.pathBIC = pathBIC;
		this.shaBefore = shaBefore;
		this.shaBIC = shaBIC;
		this.key = key;
	}
	
	@Override
	public String toString() {
		return "key= " + key + "pathBefore= " + pathBefore + "pathBIC= " + pathBIC
				+ "shaBefore= " + shaBefore + "shaBIC= " + shaBIC;
	}
}
