package org.es.network;

import static org.es.socketclient.Constants.MESSAGE_WHAT_TOAST;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.Semaphore;

import org.es.network.ExchangeProtos.Request;
import org.es.network.ExchangeProtos.Response;
import org.es.network.ExchangeProtos.Response.ReturnCode;
import org.es.utils.Log;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

/**
 * Class that handle asynchronous messages to send to the server.
 * 
 * @author Cyril Leroux
 * 
 */
public class AsyncMessageMgr extends AsyncTask<Request, int[], Response> {
	protected static Semaphore sSemaphore = new Semaphore(2);
	private static final String TAG = "AsyncMessageMgr";

	private final String mHost;
	private final int mPort;
	private final int mTimeout;

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
	}

	@Override
	protected void onPreExecute() {
		try {
			sSemaphore.acquire();
		} catch (InterruptedException e) {
			Log.error(TAG, "onPreExecute Semaphore acquire error.");
		}
		Log.info(TAG, "onPreExecute Semaphore acquire. " + sSemaphore.availablePermits() + " left");
	}

	@Override
	protected Response doInBackground(Request... _requests) {

		final Request request = _requests[0];
		String errorMessage = "";

		mSocket = null;
		try {
			// Socket creation
			mSocket = connectToRemoteSocket(mHost, mPort, mTimeout);
			if (mSocket != null && mSocket.isConnected()) {
				return sendAndReceive(mSocket, request);
			}

			errorMessage = "Socket null or not connected";

		} catch (IOException e) {
			errorMessage = "doInBackground() IOException :" + e.getMessage();
			Log.error(TAG, errorMessage);

		} catch (Exception e) {
			errorMessage = "doInBackground() Exception :" + e;
			Log.error(TAG, errorMessage);

		} finally {
			closeSocketIO();
		}

		return Response.newBuilder()
		.setReturnCode(ReturnCode.RC_ERROR)
		.setMessage(errorMessage)
		.build();
	}

	/**
	 * Runs on the UI thread after {@link #doInBackground(Request...)}.
	 * The specified result is the value returned by {@link #doInBackground(Request...)}.
	 * This method won't be invoked if the task was canceled.
	 * It releases the semaphore acquired in OnPreExecute method.
	 * @param _serverReply The response from the server returned by {@link #doInBackground(Request...)}.
	 */
	@Override
	protected void onPostExecute(Response _serverReply) {
		sSemaphore.release();
		Log.info(TAG, "Semaphore release");

		if (_serverReply == null) {
			Log.error(TAG, "_serverReply == null");
			return;
		}

		Log.info(TAG, "Got a reply : " + _serverReply.getMessage());
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
			Log.error(TAG, "showToast() handler is null");
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
	 * Send a message through a Socket to a server and get the reply.
	 * 
	 * @param _socket The socket on which to send the message.
	 * @param _req The request to send.
	 * @return The server reply.
	 * @throws IOException exception.
	 */
	private Response sendAndReceive(Socket _socket, Request _req) throws IOException {
		Log.info(TAG, "sendMessage: " + _req.toString());

		if (_socket.isConnected()) {
			//create BAOS for protobuf
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			//mClientDetails is a protobuf message object, dump it to the BAOS
			_req.writeDelimitedTo(baos);

			_socket.getOutputStream().write(baos.toByteArray());
			_socket.getOutputStream().flush();
			_socket.shutdownOutput();

			return Response.parseDelimitedFrom(_socket.getInputStream());
		}
		return null;
	}

	/** Close the socket IO then close the socket. */
	private void closeSocketIO() {
		if (mSocket == null) {
			return;
		}

		try {
			if (mSocket.getInputStream() != null) {
				mSocket.getInputStream().close();
			}
		} catch(IOException e) {}

		try {
			if (mSocket.getOutputStream() != null) {
				mSocket.getOutputStream().close();
			}
		} catch(IOException e) {}
		try {
			mSocket.close();
		} catch(IOException e) {}
	}

	/** @return The count of available permits. */
	public static int availablePermits() {
		return sSemaphore.availablePermits();
	}
}