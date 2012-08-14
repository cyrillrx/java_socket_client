package org.es.network;

public class NetworkMessage {

	protected String mCommand;
	protected String mParam;
	
	public NetworkMessage() {
		// TODO Auto-generated constructor stub
	}
	
	public String getMessage() {
		return mCommand + "|" + mParam;
	}

	/**
	 * @param _param the mParam to set
	 */
	public void setParam(String _param) {
		mParam = _param;
	}

	/**
	 * @param _command the mCommand to set
	 */
	public void setCommand(String _command) {
		mCommand = _command;
	}
}
