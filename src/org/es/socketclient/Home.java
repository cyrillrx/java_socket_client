package org.es.socketclient;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;

/**
 * Home activity of the SocketClient application.
 * @author Cyril Leroux
 *
 */
public class Home extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_home, menu);
		return true;
	}
}
