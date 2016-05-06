import java.util.concurrent.atomic.AtomicBoolean;

public class ParallelDisconnectTest implements Runnable {
	
	protected static final int PORTS = 6;
	private static final int PORT_TO_DISCONNECT = 3;
	private AtomicBoolean canCountFlag = new AtomicBoolean( false );
	private FrameConsumer consumers[] = new FrameConsumer[ PORTS ];
	private SwitchSimulatorInterface ssi = new SwitchSimulator();

	private AtomicCounter maxConcurrentSwitchUsage = CountersFactory.prepareCommonMaxStorageCounter();
	private AtomicCounter concurrentSwitchUsage = CountersFactory.prepareCounterWithMaxStorageSet();

	private void configureSwitch() {
		ssi.setNumberOfPorts(PORTS);
		ssi.setAgingTime(100000); // dlugi okres przedawnienia wpisow!
	}

	private void prepareConsumers() {
		for ( int port = 0; port < PORTS-1; port++ ) {
			consumers[ port ] = new FramesCountingConsumer();
			consumers[ port ].setCanCount( canCountFlag );
			consumers[ port ].setPrintFrame( false );
			consumers[ port ].setGlobalConcurrentUsageCounter( concurrentSwitchUsage );
			ssi.connect( port, consumers[ port ]);
		}
		consumers[ PORTS - 1 ] = new FrameConsumer();
		consumers[ PORTS - 1 ].setPrintFrame( false );
		consumers[ PORTS - 1 ].setGlobalConcurrentUsageCounter( concurrentSwitchUsage );
		ssi.connect( PORTS - 1, consumers[ PORTS - 1 ]);
	}
	
	private FrameInserter prepareEndlessInserter( int port ) {
		FrameInserter fi = new FrameInserter(ssi, port) {
			public void run() {
				do {
					ssi.insertFrame( port, new Frame( Consts.MAC_ADDRESSES[port], Consts.MAC_ADDRESSES[ PORT_TO_DISCONNECT ]));
				} while ( true );
			};
		};
		return fi;
	}

	private void sendBroadcastFrames() {
		for (int port = 0; port < PORTS; port++) {
			ssi.insertFrame(port, new Frame(Consts.MAC_ADDRESSES[port], Consts.BROADCAST_MAC));
		}
		TimeHelper.sleep(250);
		consumers[ PORTS -1 ].clearFramesHistory();
	}
	
	private Thread startThread( Runnable run ) {
		Thread th = new Thread( run );
		th.setDaemon( true );
		th.start();
		return th;
	}
	
	private boolean test( Frame testFrame ) {
		boolean result = true;
		
		if ( consumers[ PORT_TO_DISCONNECT ].getTotalNumberOfReceivedFrames() > 0 ) {
			PMO_SystemOutRedirect.println( "BLAD: do odlaczonego konsumera dotarla ramka!");
			result = false;
		}
		
		if ( ! consumers[ PORTS - 1].wasFrameReceived( testFrame )) {
			PMO_SystemOutRedirect.println( "BLAD: ramka wyslana po odlaczeniu portu powinna dotrzec w trybie broadcast do innego portu");
			result = false;
		}
		
		for ( int port = PORT_TO_DISCONNECT +1; port < PORTS - 1; port++ ) {
			if ( consumers[ port ].getTotalNumberOfReceivedFrames() == 0 ) {
				PMO_SystemOutRedirect.println( "BLAD: ramki wyslane po odlaczeniu portu powinny dotrzec w trybie broadcast do innego portu");				
				result = false;
			}
		}
		
		
		return result;
	}
	
	@Override
	public void run() {
		configureSwitch();
		prepareConsumers();
		sendBroadcastFrames();
		for ( int port = 0; port < PORT_TO_DISCONNECT; port++ )
			startThread( prepareEndlessInserter( port ));
		
		TimeHelper.sleep( 1000 );
		Frame testFrame = new Frame( Consts.MAC_ADDRESSES[ PORT_TO_DISCONNECT + 1 ], Consts.MAC_ADDRESSES[ PORT_TO_DISCONNECT ]);
		ssi.disconnect( PORT_TO_DISCONNECT );
		canCountFlag.set( true );
		ssi.insertFrame( PORT_TO_DISCONNECT + 1, testFrame); 
		
		TimeHelper.sleep( 1000 );		
		
		Verdict.show( test( testFrame ));
	}

}
 