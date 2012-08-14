package org.es.network;

import static org.es.socketclient.BuildConfig.DEBUG;
import static org.es.socketclient.Constants.MESSAGE_WHAT_TOAST;
import static org.es.network.ServerMessage.CODE_VOLUME;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.Semaphore;

import org.es.network.ServerMessage;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Class that handle asynchronous messages de gestion d'envoi de command avec paramètres au serveur.
 * @author cyril.leroux
 */
public class AsyncMessageMgr extends AsyncTask<NetworkMessage, int[], String> {
	protected static Semaphore sSemaphore = new Semaphore(2);
	private static final String TAG = "AsyncMessageMgr";

	private static String sHost;
	private static int sPort;
	private static int sTimeout;

	protected NetworkMessage mRequest;
	protected NetworkResponse mReply;
	protected Handler mHandler;
	private Socket mSocket;

	/**
	 * Initialize class with the message handler as a parameter.
	 * @param _handler The handler for toast messages.
	 */
	public AsyncMessageMgr(Handler _handler) {
		mHandler = _handler;
	}

	/**
	 * Cette fonction est exécutée avant l'appel à {@link #doInBackground(String...)}.<br />
	 * Elle retient un sémaphore qui sera libéré dans la fonction {@link #onPostExecute(String)}.<br />
	 * Exécutée dans le thread principal.
	 */
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

	/**
	 * Cette fonction est exécutée sur un thread différent du thread principal
	 * @param _params le tableau de string contenant les paramètres (commande et paramètre de la commande).
	 * @return La réponse du serveur.
	 */
	@Override
	protected String doInBackground(NetworkMessage... _requests) {
		
		final String requestMessage	= _requests[0].getMessage();

		mSocket = null;
		try {
			// Création du socket
			mSocket = connectToRemoteSocket(sHost, sPort, sTimeout);
			if (mSocket != null && mSocket.isConnected()) {
				mReply.setMessage(sendAsyncMessage(mSocket, requestMessage));
			}

		} catch (IOException e) {
			mReply.setReturnCode(ServerMessage.RC_ERROR);
			mReply.setErrorMessage("IOException" + e.getMessage());
			if (DEBUG) {
				Log.e(TAG, mReply.getMessage());
			}

		} catch (Exception e) {
			mReply.setReturnCode(ServerMessage.RC_ERROR);
			mReply.setErrorMessage("Exception" + e.getMessage());
			if (DEBUG) {
				Log.e(TAG, mReply.getMessage());
			}

		} finally {
			closeSocketIO();
		}

		return mReply.getMessage();
	}

	/**
	 * Cette fonction est exécutée après l'appel à {@link #doInBackground(String...)}
	 * Exécutée dans le thread principal.
	 * @param _serverReply La réponse du serveur renvoyée par la fonction {@link #doInBackground(String...)}.
	 */
	@Override
	protected void onPostExecute(String _serverReply) {
		if (DEBUG) {
			Log.i(TAG, "Got a reply : " + _serverReply);
			Log.i(TAG, "mRequest message  : " + mRequest.getMessage());
		}
		sSemaphore.release();
		if (DEBUG) {
			Log.i(TAG, "Semaphore release");
		}
		
		showToast(_serverReply);
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
	 * Fonction de connexion à un socket disant.
	 * @param _host L'adresse ip de l'hôte auquel est lié le socket.
	 * @param _port Le numéro de port de l'hôte auquel est lié le socket.
	 * @param _timeout Timeout of the messages send through the socket.
	 * @return The socket to connect to the server si la connexion s'est effectuée correctement, false dans les autres cas.
	 * @throws IOException excteption
	 */
	private Socket connectToRemoteSocket(String _host, int _port, int _timeout) throws IOException {

		final SocketAddress socketAddress = new InetSocketAddress(_host, _port);
		Socket socket = new Socket();
		socket.connect(socketAddress, _timeout);

		return socket;
	}

	/**
	 * Cette fonction est appelée depuis le thread principal
	 * Elle permet l'envoi d'une commande et d'un paramètre
	 * @param _socket The socket on wich to send the message.
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
	 * @param _socket Le socket auquel le message a été envoyé.
	 * @return La réponse du serveur.
	 * @throws IOException exeption
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

	/**
	 * Ferme les entrées/sortie du socket puis ferme le socket.
	 */
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

	/** @return Concatenation of host and port of the remote server. */
	public static String getServerInfos() {
		return sHost + ":" + sPort;
	}

	/**
	 * Set the ip address of the remote server.
	 * @param _host the ip address of the remote server.
	 */
	public static void setHost(String _host) {
		sHost = _host;
	}

	/**
	 * Set the port on which we want to establish a connexion with the remote server.
	 * @param _port the port value.
	 */
	public static void setPort(int _port) {
		sPort = _port;
	}

	/**
	 * Define the timeout of the connexion with the server.
	 * @param _timeout The timeout in millisecondes.
	 */
	public static void setTimeout(int _timeout) {
		sTimeout = _timeout;
	}

}