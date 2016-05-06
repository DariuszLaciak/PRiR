import java.util.concurrent.atomic.AtomicInteger;

public class Frame implements SwitchSimulatorInterface.FrameInterface {

	private final int id;
	private final String src;
	private final String dst;
	private static final AtomicInteger idGenerator = new AtomicInteger();

	public static Frame create(SwitchSimulatorInterface.FrameInterface frame) {

		if (!(frame instanceof SwitchSimulatorInterface.FrameInterface)) {
			return null;
		}

		if (frame instanceof Frame)
			return new Frame(frame.getSource(), frame.getDestination(), ((Frame) frame).id);
		else
			return new Frame(frame.getSource(), frame.getDestination(), -1);
	}

	public Frame(String src, String dst) {
		this(src, dst, idGenerator.incrementAndGet());
	}

	public Frame(String src, String dst, int id) {
		this.src = src;
		this.dst = dst;
		this.id = id;
	}

	@Override
	public String getDestination() {
		return dst;
	}

	@Override
	public String getSource() {
		return src;
	}

	@Override
	public boolean equals(Object obj) {

		// strasznie to udziwnione, ale to na wypadek gdyby z przelacznika
		// wychodzily obiekty innego typu niz moj Frame

		if (obj instanceof Frame) {
			if ((id != -1) && ((Frame) obj).id != -1)
				return id == ((Frame) obj).id;
		}

		if (obj instanceof SwitchSimulatorInterface.FrameInterface) {
			SwitchSimulatorInterface.FrameInterface frame = (SwitchSimulatorInterface.FrameInterface) obj;
			PMO_SystemOutRedirect.println("UWAGA: equals wykonuje porownanie na poziomie adresow");
			return src.equals(frame.getSource()) && dst.equals(frame.getDestination());
		}

		return false;

	}
	
	@Override
	public String toString() {
		return "[ Frame " + id + " " + src + " -> " + dst + " ]";
	}
	
	
	public static void main(String[] args) {
		Frame f1 = new Frame( "A", "B" );
		Frame f2 = new Frame( "A", "B" );
		
		System.out.println( "f1 == f2 ? " + ( f1.equals( f2 ) ? "tak" : "nie") );
		Frame f3 = Frame.create( f1 );
		System.out.println( "f1 == f3 ? " + ( f1.equals( f3 ) ? "tak" : "nie") );
		
		Frame f4 = Frame.create( new SwitchSimulatorInterface.FrameInterface() {
			
			@Override
			public String getSource() {
				return "A";
			}
			
			@Override
			public String getDestination() {
				return "B";
			}
		}) ;

		System.out.println( "f1 == f4 ? " + ( f1.equals( f4 ) ? "tak" : "nie") );
	}
	
}
