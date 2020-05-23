import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Main {

	private final ActorSystem system = ActorSystem.create("system");
	private final LoggingAdapter log = Logging.getLogger(system, this);

	public void log(String s){
		log.info(s);
	}

	public void terminate(){
		system.terminate();
	}

	public static void main(String[] args) {
		@SuppressWarnings("unused")
		Main m = new Main();
		m.log("Teste");

		m.terminate();
	}
}