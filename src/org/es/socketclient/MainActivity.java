package org.es.socketclient;

import static android.view.HapticFeedbackConstants.VIRTUAL_KEY;

import org.es.network.AsyncMessageMgr;
import org.es.network.NetworkMessage;
import org.es.uremote.R;
import org.es.uremote.computer.FragDashboard.DashboardMessageMgr;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

/**
 * Home activity of the SocketClient application.
 * @author Cyril Leroux
 *
 */
public class MainActivity extends Activity implements OnClickListener {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public void onClick(View _v) {
		_v.performHapticFeedback(VIRTUAL_KEY);
		
		switch (_v.getId()) {
		case R.id.btnSend:
			
			break;

		default:
			break;
		}
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Initialize the component that send messages over the network. 
	 * Send the message in parameter.
	 * @param _message The message to send.
	 */
	public void sendAsyncMessage(NetworkMessage _message) {
		if (AsyncMessageMgr.availablePermits() > 0) {
			new AsyncMessageMgr(mParent.getHandler()).execute(_message);
		} else {
			Toast.makeText(getApplicationContext(), R.string.msg_no_more_permit, Toast.LENGTH_SHORT).show();
		}
	}
}
