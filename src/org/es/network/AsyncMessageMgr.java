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
 * Class that handle asynchronous messages de gestion d'envoi de command avec param�tres au serveur.
 * @author cyril.leroux
 */
public class AsyncMessageMgr extends AsyncTask<String, int[], String> {
	protected static Semaphore sSemaphore = new Semaphore(2);
	private static final String TAG = "AsyncMessageMgr";

	private static String sHost;
	private static int sPort;
	private static int sTimeout;

	protected String mReturnCode;
	protected String mCommand;
	protected String mParam;
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
	 * Cette fonction est ex�cut�e avant l'appel � {@link #doInBackground(String...)}.<br />
	 * Elle retient un s�maphore qui sera lib�r� dans la fonction {@link #onPostExecute(String)}.<br />
	 * Ex�cut�e dans le thread principal.
	 */
	@Override
	protected void onPreExecute() {
		mReturnCode = ServerMessage.RC_SUCCES;
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
	 * Cette fonction est ex�cut�e sur un thread diff�rent du thread principal
	 * @param _params le tableau de string contenant les param�tres (commande et param�tre de la commande).
	 * @return La r�ponse du serveur.
	 */
	@Override
	protected String doInBackground(String... _params) {
		String serverReply = "";

		mCommand	= _params[0];
		mParam 		= (_params.length > 1) ? _params[1] : "";
		final String message	= mCommand + "|" + mParam;

		mSocket = null;
		try {
			// Cr�ation du socket
			mSocket = connectToRemoteSocket(sHost, sPort, sTimeout, message);
			if (mSocket != null && mSocket.isConnected()) {
				serverReply = sendAsyncMessage(mSocket, message);
			}

		} catch (IOException e) {
			mReturnCode = ServerMessage.RC_ERROR;
			serverReply = "IOException" + e.getMessage();
			if (DEBUG) {
				Log.e(TAG, serverReply);
			}

		} catch (Exception e) {
			mCommand = ServerMessage.RC_ERROR;
			serverReply = "IOException" + e.getMessage();
			if (DEBUG) {
				Log.e(TAG, serverReply);
			}

		} finally {
			closeSocketIO();
		}

		return serverReply;
	}

	/**
	 * Cette fonction est ex�cut�e apr�s l'appel � {@link #doInBackground(String...)}
	 * Ex�cut�e dans le thread principal.
	 * @param _serverReply La r�ponse du serveur renvoy�e par la fonction {@link #doInBackground(String...)}.
	 */
	@Override
	protected void onPostExecute(String _serverReply) {
		if (DEBUG) {
			Log.i(TAG, "Got a reply : " + _serverReply);
			Log.i(TAG, "mCommand  : " + mCommand);
			Log.i(TAG, "mParam  : " + mParam);
		}
		sSemaphore.release();
		if (DEBUG) {
			Log.i(TAG, "Semaphore release");
		}

		if (mCommand.equals(CODE_VOLUME)) {
			showToast(_serverReply);
		}
	}

	@Override
	protected void onCancelled() {
		closeSocketIO();
		super.onCancelled();
	}

	/**
	 * Envoi d'un message Toast sur le thread de l'UI.
	 * @param _message Le message � afficher.
	 */
	protected void showToast(String _message) {
		Message msg = new Message();
		msg.what = MESSAGE_WHAT_TOAST;
		msg.obj = _message;
		mHandler.sendMessage(msg);
	}

	/**
	 * Fonction de connexion � un socket disant.
	 * @param _host L'adresse ip de l'h�te auquel est li� le socket.
	 * @param _port Le num�ro de port de l'h�te auquel est li� le socket.
	 * @param _message Le message � envoyer.
	 * @return true si la connexion s'est effectu�e correctement, false dans les autres cas.
	 * @throws IOException excteption
	 */
	private Socket connectToRemoteSocket(String _host, int _port, int _timeout, String _message) throws IOException {

		final SocketAddress socketAddress = new InetSocketAddress(_host, _port);
		Socket socket = new Socket();
		socket.connect(socketAddress, _timeout);

		return socket;
	}

	/**
	 * Cette fonction est appel�e depuis le thread principal
	 * Elle permet l'envoi d'une commande et d'un param�tre
	 * @param _socket Le socket sur lequel on envoie le message.
	 * @param _message Le message � transmettre
	 * @return La r�ponse du serveur.
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
	 * @param _socket Le socket auquel le message a �t� envoy�.
	 * @return La r�ponse du serveur.
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
	 * Ferme les entr�es/sortie du socket puis ferme le socket.
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