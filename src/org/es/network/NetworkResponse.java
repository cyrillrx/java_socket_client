package org.es.network;

public class NetworkResponse extends NetworkMessage {

	protected String mMessage;
	protected String mReturnCode;
	protected String mErrorMessage;

	public NetworkResponse() {
		mReturnCode = ServerMessage.RC_SUCCES;
	}

	public String getMessage() {
		return mMessage;
	}

	public void setMessage(String _message) {
		mMessage = _message;
	}

	public String getReturnCode() {
		return mReturnCode;
	}

	public void setReturnCode(String _returnCode) {
		mReturnCode = _returnCode;
	}

	public String getErrorMessage() {
		return mErrorMessage;
	}

	public void setErrorMessage(String _errorMessage) {
		mErrorMessage = _errorMessage;
	}
}
