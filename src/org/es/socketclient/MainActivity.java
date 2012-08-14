package org.es.socketclient;

import static android.view.HapticFeedbackConstants.VIRTUAL_KEY;
import static org.es.socketclient.Constants.MESSAGE_WHAT_TOAST;

import org.es.network.AsyncMessageMgr;
import org.es.network.DirectotyContentProtos.ProtoDirContent;
import org.es.network.DirectotyContentProtos.ProtoFile;
import org.es.network.DirectotyContentProtos.ProtoFile.FileType;
import org.es.network.NetworkMessage;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Home activity of the SocketClient application.
 * @author Cyril Leroux
 *
 */
@SuppressLint("ParserError")
public class MainActivity extends Activity implements OnClickListener {

	/** Handle the toast messages. */
	private Handler sToastHandler =	new Handler() {
		@Override
		public void handleMessage(Message _msg) {
			switch (_msg.what) {
			case MESSAGE_WHAT_TOAST:
				Toast.makeText(getApplicationContext(), (String)_msg.obj, Toast.LENGTH_SHORT).show();
				break;
			default : break;
			}
			super.handleMessage(_msg);
		}

	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		((Button) findViewById(R.id.btnSend)).setOnClickListener(this);
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

			ProtoFile.Builder fileBuilder = ProtoFile.newBuilder()
			.setName("DevTools")
			.setPath("c:")
			.setType(FileType.FILE)
			.setSize(10000);
			ProtoDirContent.Builder dirContentBuilder = ProtoDirContent.newBuilder().addFile(fileBuilder);

			ProtoDirContent dirContent = dirContentBuilder.build();

			if (dirContent.isInitialized()) {
				Toast.makeText(getApplicationContext(), "is initialized", Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(getApplicationContext(), "is NOT initialized", Toast.LENGTH_SHORT).show();
			}

			((EditText) findViewById(R.id.etConsole)).setText(dirContent.toString());
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
			new AsyncMessageMgr(sToastHandler).execute(_message);
		} else {
			Toast.makeText(getApplicationContext(), R.string.msg_no_more_permit, Toast.LENGTH_SHORT).show();
		}
	}
}
