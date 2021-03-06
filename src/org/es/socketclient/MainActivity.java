package org.es.socketclient;

import static android.view.HapticFeedbackConstants.VIRTUAL_KEY;
import static android.widget.Toast.LENGTH_SHORT;
import static org.es.socketclient.Constants.MESSAGE_WHAT_TOAST;

import org.es.network.AsyncMessageMgr;
import org.es.network.ExchangeProtos.Request;
import org.es.network.ExchangeProtos.Request.Code;
import org.es.network.ExchangeProtos.Request.Type;
import org.es.utils.Log;

import android.app.Activity;
import android.content.Context;
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
public class MainActivity extends Activity implements OnClickListener {
	private static final String TAG = "SocketClient_MainActivity";

	/** Handle the toast messages. */
	private static Handler sToastHandler;

	private static void initHandler(final Context _context) {
		if (sToastHandler != null) {
			return;
		}
		sToastHandler = new Handler() {
			@Override
			public void handleMessage(Message _msg) {
				switch (_msg.what) {
				case MESSAGE_WHAT_TOAST:
					sendToast(_context, (String) _msg.obj);
					break;
				default:
					break;
				}
				super.handleMessage(_msg);
			}

		};
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		((Button) findViewById(R.id.btnExplorer)).setOnClickListener(this);
		((Button) findViewById(R.id.btnHello)).setOnClickListener(this);
		((Button) findViewById(R.id.btnTest)).setOnClickListener(this);
		((Button) findViewById(R.id.btnKill)).setOnClickListener(this);
		((Button) findViewById(R.id.btnSend)).setOnClickListener(this);

		initHandler(getApplicationContext());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public void onClick(View _v) {
		_v.performHapticFeedback(VIRTUAL_KEY);

		Request request;
		switch (_v.getId()) {
		case R.id.btnExplorer:

			request = Request.newBuilder()
			.setType(Type.EXPLORER)
			.setCode(Code.GET_FILE_LIST)
			.setStringParam("L:\\")
			.build();

			if (request.isInitialized()) {
				sendAsyncMessage(request);
			} else {
				sendToast(getApplicationContext(), "is NOT initialized");
			}
			break;

		case R.id.btnHello:

			request = Request.newBuilder()
			.setType(Type.SIMPLE)
			.setCode(Code.HELLO)
			.build();

			if (request.isInitialized()) {
				sendAsyncMessage(request);
			} else {
				sendToast(getApplicationContext(), "is NOT initialized");
			}
			break;

		case R.id.btnTest:

			request = Request.newBuilder()
			.setType(Type.SIMPLE)
			.setCode(Code.TEST)
			.build();

			if (request.isInitialized()) {
				sendAsyncMessage(request);
			} else {
				sendToast(getApplicationContext(), "is NOT initialized");
			}
			break;

		case R.id.btnKill:

			request = Request.newBuilder()
			.setType(Type.SIMPLE)
			.setCode(Code.KILL_SERVER)
			.build();

			if (request.isInitialized()) {
				sendAsyncMessage(request);
			} else {
				sendToast(getApplicationContext(), "is NOT initialized");
			}
			break;

		case R.id.btnSend:

			request = Request.newBuilder()
			.setType(Type.KEYBOARD)
			.setCode(Code.DEFINE)
			.setStringParam(getTextToSend())
			.build();

			if (request.isInitialized()) {
				sendAsyncMessage(request);
			} else {
				sendToast(getApplicationContext(), "is NOT initialized");
			}
			break;

		default:
			break;
		}

	}

	/**
	 * Initialize the component that send messages over the network. Send the message in parameter.
	 * 
	 * @param _req The message to send.
	 */
	private void sendAsyncMessage(Request _req) {

		if (AsyncMessageMgr.availablePermits() > 0) {
			sendToast(getApplicationContext(), "Sending message...");
			addMessageToLog(_req.toString());
			new AsyncMessageMgr(sToastHandler, getHost(), getPort(), getTimeout()).execute(_req);

		} else {
			sendToast(getApplicationContext(), getString(R.string.msg_no_more_permit));
		}
	}

	private static void sendToast(Context _context, String _toastMessage) {
		if (_context != null) {
			Toast.makeText(_context, _toastMessage, LENGTH_SHORT).show();
		}
	}

	private void addMessageToLog(String _message) {
		((EditText) findViewById(R.id.etConsole)).getText().append(_message);
		((EditText) findViewById(R.id.etConsole)).getText().append("___________\n");
	}

	private String getHost() {
		return ((EditText) findViewById(R.id.etIpAddress)).getText().toString();
	}

	private int getPort() {
		final String portStr = ((EditText) findViewById(R.id.etPort)).getText().toString();

		try {
			return Integer.parseInt(portStr);
		} catch (NumberFormatException e) {
			Log.error(TAG, "getPort" + e.getMessage());
			return 0;
		}
	}

	private int getTimeout() {
		final String timeoutStr = ((EditText) findViewById(R.id.etTimeout)).getText().toString();

		try {
			return Integer.parseInt(timeoutStr);
		} catch (NumberFormatException e) {
			Log.error(TAG, "getTimeout :" + e.getMessage());
			return 500;
		}
	}

	private String getTextToSend() {
		return ((EditText) findViewById(R.id.etTextToSend)).getText().toString();
	}
}
