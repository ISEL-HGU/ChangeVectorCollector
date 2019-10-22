package change.vector.collector;

public class BeforeBIC {
	public String pathBefore;
	public String pathBIC;
	public String shaBefore;
	public String shaBIC;
	public String key;
	public String pathFix;
	public String shaFix;
	
	public BeforeBIC(String pathBefore, String pathBIC, String shaBefore, String shaBIC, String pathFix, String shaFix, String key) {
		this.pathBefore = pathBefore;
		this.pathBIC = pathBIC;
		this.shaBefore = shaBefore;
		this.shaBIC = shaBIC;
		this.pathFix = pathFix;
		this.shaFix = shaFix;
		this.key = key;
	}
	
	@Override
	public String toString() {
		return "key=" + key + "\n";
	}
}
