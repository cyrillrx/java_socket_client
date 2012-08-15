package org.es.network;

public class NetworkRequest extends NetworkMessage {

	public String getMessage() {
		return mCommand + "|" + mParam;
	}
}
