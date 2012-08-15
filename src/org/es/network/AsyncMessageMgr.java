package org.es.network;

import static org.es.socketclient.BuildConfig.DEBUG;
import static org.es.socketclient.Constants.MESSAGE_WHAT_TOAST;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.Semaphore;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Class that handle asynchronous messages to send to the server.
 * @author Cyril Leroux
 */
public class AsyncMessageMgr extends AsyncTask<String, int[], NetworkResponse> {
	protected static Semaphore sSemaphore = new Semaphore(2);
	private static final String TAG = "AsyncMessageMgr";

	private final String mHost;
	private final int mPort;
	private final int mTimeout;

	protected NetworkResponse mReply;
	protected Handler mHandler;
	private Socket mSocket;

	/**
	 * Initialize class with the message handler as a parameter.
	 * 
	 * @param _handler The handler for toast messages.
	 * @param _host The remote server IP address.
	 * @param _port The port on which we want to establish a connection with the remote server.
	 * @param _timeout The timeout of the connection with the server (in milliseconds).
	 */
	public AsyncMessageMgr(Handler _handler, String _host, int _port, int _timeout) {
		mHandler	= _handler;
		mHost		= _host;
		mPort		= _port;
		mTimeout	= _timeout;

		mReply = new NetworkResponse();
	}

	@Override
	protected void onPreExecute() {
		try {
			sSemaphore.acquire();
		} catch (InterruptedException e) {
			if (DEBUG) {
				Log.e(TAG, "onPreExecute Semaphore acquire error.");
			}
		}
		if (DEBUG) {
			Log.i(TAG, "onPreExecute Semaphore acquire. " + sSemaphore.availablePermits() + " left");
		}
	}

	@Override
	protected NetworkResponse doInBackground(String... _requests) {

		final String requestMessage	= _requests[0];

		mReply.setCommand(requestMessage);

		mSocket = null;
		try {
			// Création du socket
			mSocket = connectToRemoteSocket(mHost, mPort, mTimeout);
			if (mSocket != null && mSocket.isConnected()) {
				mReply.setMessage(sendAsyncMessage(mSocket, requestMessage));
			}

		} catch (IOException e) {
			mReply.setReturnCode(ServerMessage.RC_ERROR);
			mReply.setErrorMessage("IOException" + e.getMessage());
			if (DEBUG) {
				Log.e(TAG, e.getMessage());
			}

		} catch (Exception e) {
			mReply.setReturnCode(ServerMessage.RC_ERROR);
			mReply.setErrorMessage("Exception" + e.getMessage());
			if (DEBUG) {
				Log.e(TAG, e.getMessage());
			}

		} finally {
			closeSocketIO();
		}

		return mReply;
	}

	@Override
	protected void onPostExecute(NetworkResponse _serverReply) {
		if (DEBUG) {
			Log.i(TAG, "Got a reply : " + _serverReply);
		}
		sSemaphore.release();
		if (DEBUG) {
			Log.i(TAG, "Semaphore release");
		}

		showToast(_serverReply.getMessage());
	}

	@Override
	protected void onCancelled() {
		closeSocketIO();
		super.onCancelled();
	}

	/**
	 * Send a toast message on the UI thread.
	 * @param _toastMessage The message to display.
	 */
	protected void showToast(String _toastMessage) {
		if (mHandler == null) {
			if (DEBUG) {
				Log.i(TAG, "showToast() handler is null");
			}
			return;
		}

		Message msg = new Message();
		msg.what = MESSAGE_WHAT_TOAST;
		msg.obj = _toastMessage;
		mHandler.sendMessage(msg);
	}

	/**
	 * Creates the socket, connects it to the server then returns it.
	 * 
	 * @param _host The remote server IP address.
	 * @param _port The port on which we want to establish a connection with the remote server.
	 * @param _timeout The timeout of the connection with the server (in milliseconds).
	 * @return The socket on which to send the message.
	 * @throws IOException exception
	 */
	private Socket connectToRemoteSocket(String _host, int _port, int _timeout) throws IOException {

		final SocketAddress socketAddress = new InetSocketAddress(_host, _port);
		Socket socket = new Socket();
		socket.connect(socketAddress, _timeout);
		return socket;
	}

	/**
	 * Called from the UI Thread.
	 * It allows to send a message through a Socket to a server.
	 * 
	 * @param _socket The socket on which to send the message.
	 * @param _message Le message to send.
	 * @return The server reply.
	 * @throws IOException exception.
	 */
	private String sendAsyncMessage(Socket _socket, String _message) throws IOException {
		if (DEBUG) {
			Log.i(TAG, "sendMessage: " + _message);
		}
		String serverReply = "";

		if (mSocket.isConnected()) {
			_socket.getOutputStream().write(_message.getBytes());
			_socket.getOutputStream().flush();
			_socket.shutdownOutput();
			serverReply = getServerReply(_socket);
		}
		return serverReply;
	}

	/**
	 * @param _socket The socket on which to send the message.
	 * @return The server reply.
	 * @throws IOException exception
	 */
	private String getServerReply(Socket _socket) throws IOException {
		final int BUFSIZ = 512;

		final BufferedReader bufferReader = new BufferedReader(new InputStreamReader(_socket.getInputStream()), BUFSIZ);
		String line = "", reply = "";
		while ((line = bufferReader.readLine()) != null) {
			reply += line;
		}

		if (DEBUG) {
			Log.i(TAG, "Got a reply : " + reply);
		}

		return reply;
	}

	/** Close the socket IO then close the socket. */
	private void closeSocketIO() {
		if (mSocket == null) {
			return;
		}

		try { if (mSocket.getInputStream() != null) {
			mSocket.getInputStream().close();
		}	} catch(IOException e) {}
		try { if (mSocket.getOutputStream() != null) {
			mSocket.getOutputStream().close();
		}	} catch(IOException e) {}
		try { mSocket.close(); } catch(IOException e) {}
	}

	/** @return The count of available permits. */
	public static int availablePermits() {
		return sSemaphore.availablePermits();
	}
}