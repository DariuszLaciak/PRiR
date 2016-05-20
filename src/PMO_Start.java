import java.util.Arrays;
import java.util.List;

public class PMO_Start {
	
	public static void startTestThread( Runnable run ) {
		Thread th = new Thread( run );
		th.setDaemon( true );
		th.start();
	}
	
	public static void main(String[] args) {
		PMO_SystemOutRedirect.startRedirectionToNull();

		QueuesInterface qi = new Queues();
		args = new String[1];
		args[0] = "t3";
		List<String > argl = Arrays.asList( args );
		
		if ( argl.contains( "t1") ) {
			startTestThread( new Test01(qi) );
			TimeHelper.sleep( Test01.TASK_TIME * 2 );
		}
		
		if ( argl.contains( "t2") ) {
			startTestThread( new Test02(qi) );
			TimeHelper.sleep( Test01.TASK_TIME * 3 );
		}

		if ( argl.contains( "t3") ) {
			startTestThread( new Test03(qi) );
			TimeHelper.sleep( Test01.TASK_TIME * 5 );
		}

		if ( argl.contains( "t4") ) {
			startTestThread( new Test04(qi) );
			TimeHelper.sleep( Test01.TASK_TIME * 5 );
		}

		if ( argl.contains( "t5") ) {
			startTestThread( new Test05(qi) );
			TimeHelper.sleep( 4500 );
		}
		
		if ( argl.contains( "t6") ) {
			startTestThread( new Test06(qi) );
			TimeHelper.sleep( 4000 );
		}
		
		PMO_SystemOutRedirect.println("------  THE END  ------");
		PMO_SystemOutRedirect.println("");
		System.exit(0);
	}
}
