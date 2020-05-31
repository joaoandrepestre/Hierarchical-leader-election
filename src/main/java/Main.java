
import visualization.Visualization;

/*
* Main class. Initializes the visualization class that runsa the system 
 */
public class Main {
	
	public Main() {
		Visualization.main(new String[]{Visualization.class.getName()});
	}

	public static void main(String[] args) {
		new Main();
	}
}