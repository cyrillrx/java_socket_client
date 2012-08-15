package org.es.network;

import static org.es.network.ServerMessage.RC_ERROR;
import static org.es.network.ServerMessage.RC_SUCCES;

public class NetworkResponse extends NetworkMessage {

	protected String mMessage;
	protected String mReturnCode;
	protected String mErrorMessage;

	public NetworkResponse() {
		mReturnCode = RC_SUCCES;
		mMessage = "";
		mErrorMessage = "";
	}

	@Override
	public String getMessage() {

		if (RC_ERROR.equals(mReturnCode)) {
			return mErrorMessage;
		}
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
