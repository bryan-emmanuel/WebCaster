/*
 * WebCaster - Chromecast Web Media Library
 * Copyright (C) 2013 Bryan Emmanuel
 *
 * This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Bryan Emmanuel piusvelte@gmail.com
 */
package com.piusvelte.webcaster;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.cast.ApplicationChannel;
import com.google.cast.ApplicationMetadata;
import com.google.cast.ApplicationSession;
import com.google.cast.CastContext;
import com.google.cast.CastDevice;
import com.google.cast.ContentMetadata;
import com.google.cast.MediaProtocolCommand;
import com.google.cast.MediaProtocolMessageStream;
import com.google.cast.MediaProtocolMessageStream.PlayerState;
import com.google.cast.MediaRouteAdapter;
import com.google.cast.MediaRouteHelper;
import com.google.cast.MediaRouteStateChangeListener;
import com.google.cast.SessionError;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.MediaRouteButton;
import android.support.v7.app.MediaRouteDialogFactory;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements
		LoaderCallbacks<List<Medium>>, MediaListFragment.Listener,
		MediaRouteAdapter, OnClickListener, OnSeekBarChangeListener {

	private static final String TAG = "MainActivity";
	protected static final double MAX_VOLUME_LEVEL = 20;
	private static final double VOLUME_INCREMENT = 0.05;
	private static final int FRAGMENT_MEDIA_ROOT = 0;

	private String mediaHost = null;
	private String appId = null;
	private List<Medium> media = new ArrayList<Medium>();
	private List<Integer> dirIdx = new ArrayList<Integer>();
	private DirPagerAdapter dirPagerAdapter;
	private ViewPager viewPager;
	private CastContext castContext;
	private MediaRouter mediaRouter;
	private MediaRouteSelector mediaRouteSelector;
	private MediaRouteButton mediaRouteButton;
	private MediaRouter.Callback mediaRouterCallback;
	private CastDevice castDevice;
	private ApplicationSession applicationSession;
	private MediaProtocolMessageStream mediaProtocolMessageStream;
	private Button btnPlay;
	private MediaRouteStateChangeListener mediaRouteStateChangeListener;
	private SeekBar seekBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the app.
		dirPagerAdapter = new DirPagerAdapter(getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		viewPager = (ViewPager) findViewById(R.id.pager);
		viewPager.setAdapter(dirPagerAdapter);

		castContext = new CastContext(getApplicationContext());

		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(this);
		mediaHost = sp.getString(getString(R.string.preference_host), null);
		appId = sp.getString(getString(R.string.preference_app_id), null);

		if (appId != null) {
			setupCasting();
		}

		seekBar = (SeekBar) findViewById(R.id.seek);
		seekBar.setOnSeekBarChangeListener(this);

		btnPlay = (Button) findViewById(R.id.btn_play);
		btnPlay.setOnClickListener(this);

		getActionBar().setDisplayHomeAsUpEnabled(true);

		if (mediaHost != null) {
			getSupportLoaderManager().initLoader(0, null, this);
		}
	}

	private void setupCasting() {
		MediaRouteHelper.registerMinimalMediaRouteProvider(castContext, this);
		mediaRouter = MediaRouter.getInstance(getApplicationContext());
		mediaRouteSelector = MediaRouteHelper.buildMediaRouteSelector(
				MediaRouteHelper.CATEGORY_CAST, appId, null);

		mediaRouterCallback = new MediaRouter.Callback() {
			@Override
			public void onRouteSelected(MediaRouter router, RouteInfo route) {
				MediaRouteHelper.requestCastDeviceForRoute(route);
			}

			@Override
			public void onRouteUnselected(MediaRouter router, RouteInfo route) {
				try {
					if (applicationSession != null) {
						applicationSession.setStopApplicationWhenEnding(true);
						applicationSession.endSession();
					} else {
						Log.e(TAG, "onRouteUnselected: mSession is null");
					}
				} catch (IllegalStateException e) {
					Log.e(TAG, "onRouteUnselected:");
					e.printStackTrace();
				} catch (IOException e) {
					Log.e(TAG, "onRouteUnselected:");
					e.printStackTrace();
				}
				mediaProtocolMessageStream = null;
				castDevice = null;
				mediaRouteStateChangeListener = null;
			}
		};
	}

	private void clearCasting() {
		try {
			if (applicationSession != null) {
				applicationSession.setStopApplicationWhenEnding(true);
				applicationSession.endSession();
			} else {
				Log.e(TAG, "onRouteUnselected: mSession is null");
			}
		} catch (IllegalStateException e) {
			Log.e(TAG, "onRouteUnselected:");
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(TAG, "onRouteUnselected:");
			e.printStackTrace();
		}
		mediaProtocolMessageStream = null;
		castDevice = null;
		mediaRouteStateChangeListener = null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);

		MenuItem mediaRouteItem = menu.findItem(R.id.action_mediaroute);

		if (mediaRouteSelector != null) {
			mediaRouteButton = (MediaRouteButton) mediaRouteItem
					.getActionView();
			mediaRouteButton.setRouteSelector(mediaRouteSelector);
			mediaRouteButton.setDialogFactory(new MediaRouteDialogFactory());
			mediaRouteItem.setVisible(true);
			mediaRouteItem.setEnabled(true);
		} else {
			mediaRouteItem.setVisible(false);
			mediaRouteItem.setEnabled(false);
		}

		return true;
	}

	@Override
	protected void onStart() {
		super.onStart();

		if (mediaRouter != null) {
			mediaRouter.addCallback(mediaRouteSelector, mediaRouterCallback,
					MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		if ((appId == null) || (mediaHost == null)) {
			startActivityForResult(
					new Intent(this, SettingsActivity.class).putExtra(
							SettingsActivity.EXTRA_APP_ID, appId).putExtra(
							SettingsActivity.EXTRA_HOST, mediaHost), 0);
		}

		if (applicationSession != null) {
			try {
				applicationSession.resumeSession();
			} catch (IOException e) {
				Log.e(TAG, "No session to resume", e);
			} catch (IllegalStateException e) {
				Log.e(TAG, "No session to resume", e);
			} catch (RuntimeException e) {
				Log.e(TAG, "No session to resume", e);
			}
		}
	}

	@Override
	protected void onActivityResult(int resultCode, int resultType, Intent data) {

		resultCode = resultType >> 16;

		if ((resultCode == RESULT_OK) && (data != null)) {
			String resultAppId = data
					.getStringExtra(SettingsActivity.EXTRA_APP_ID);

			if (((resultAppId != null) && (!resultAppId.equals(appId)) || (appId != null))) {
				clearCasting();
				appId = resultAppId;

				if (appId != null) {
					setupCasting();
				}

				invalidateOptionsMenu();

			}

			String resultHost = data
					.getStringExtra(SettingsActivity.EXTRA_HOST);

			if (((resultHost != null) && (!resultHost.equals(mediaHost)) || (mediaHost != null))) {
				mediaHost = resultHost;
				media = new ArrayList<Medium>();
				dirIdx.clear();
				dirIdx.add(0);
				dirPagerAdapter.notifyDataSetChanged();
				MediaLoader loader = (MediaLoader) getSupportLoaderManager()
						.initLoader(0, null, this);

				if (loader != null) {
					loader.loadHost(mediaHost);
				}
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		PreferenceManager.getDefaultSharedPreferences(this).edit()
				.putString(getString(R.string.preference_app_id), appId)
				.putString(getString(R.string.preference_host), mediaHost)
				.commit();
	}

	/**
	 * Closes a running session upon destruction of this Activity.
	 */
	@Override
	protected void onStop() {
		if (mediaRouter != null) {
			mediaRouter.removeCallback(mediaRouterCallback);
		}

		super.onStop();
	}

	@Override
	protected void onDestroy() {
		if (applicationSession != null) {
			try {
				if (!applicationSession.hasStopped()) {
					applicationSession.endSession();
				}
			} catch (IOException e) {
				Log.e(TAG, "Failed to end session.");
			}
		}

		applicationSession = null;
		super.onDestroy();
	}

	public class DirPagerAdapter extends FragmentPagerAdapter {

		public DirPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			Fragment fragment = new MediaListFragment();
			Bundle args = new Bundle();
			args.putInt(MediaListFragment.EXTRA_DIR_POSITION, position);
			fragment.setArguments(args);
			return fragment;
		}

		@Override
		public int getCount() {
			return dirIdx.size();
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();

			if (position == FRAGMENT_MEDIA_ROOT) {
				return mediaHost.toUpperCase(l);
			} else {
				Medium m = getMediumAt(position);
				if (m != null) {
					return m.getFile().substring(m.getFile().lastIndexOf(File.separator) + 1).toUpperCase(l);
				} else {
					return "";
				}
			}
		}
	}

	private void openSession() {
		applicationSession = new ApplicationSession(castContext, castDevice);

		// TODO: The below lines allow you to specify either that your
		// application uses the default
		// implementations of the Notification and Lock Screens, or that you
		// will be using your own.
		int flags = 0;

		// Comment out the below line if you are not writing your own
		// Notification Screen.
		// flags |= ApplicationSession.FLAG_DISABLE_NOTIFICATION;

		// Comment out the below line if you are not writing your own Lock
		// Screen.
		// flags |= ApplicationSession.FLAG_DISABLE_LOCK_SCREEN_REMOTE_CONTROL;
		applicationSession.setApplicationOptions(flags);

		applicationSession.setListener(new ApplicationSession.Listener() {

			@Override
			public void onSessionStarted(ApplicationMetadata appMetadata) {
				ApplicationChannel channel = applicationSession.getChannel();

				if (channel == null) {
					Log.e(TAG, "channel = null");
					return;
				}

				if (mediaProtocolMessageStream != null) {
					mediaProtocolMessageStream = null;
				}

				mediaProtocolMessageStream = new MediaProtocolMessageStream();
				channel.attachMessageStream(mediaProtocolMessageStream);
				(new CastStatusThread()).start();

				PlayerState playerState = mediaProtocolMessageStream.getPlayerState();

				if (PlayerState.PLAYING.equals(playerState)) {
					btnPlay.setText(getString(R.string.pause) + " " + mediaProtocolMessageStream.getTitle());
				} else if (PlayerState.STOPPED.equals(playerState)) {
					btnPlay.setText(getString(R.string.play) + " " + mediaProtocolMessageStream.getTitle());
				} else {
					Toast.makeText(getBaseContext(), "Select media",
							Toast.LENGTH_SHORT).show();
				}
			}

			@Override
			public void onSessionStartFailed(SessionError error) {
				Log.e(TAG, "onStartFailed " + error);
				mediaProtocolMessageStream = null;
			}

			@Override
			public void onSessionEnded(SessionError error) {
				Log.i(TAG, "onEnded " + error);
				mediaProtocolMessageStream = null;
			}
		});

		try {
			applicationSession.startSession(appId);
		} catch (IOException e) {
			Log.e(TAG, "Failed to open session", e);
		}
	}

	private Medium getMediumAt(int position) {
		if (position == 0) {
			return null;
		} else if (position < dirIdx.size()) {
			int i = 1;
			Medium m = media.get(dirIdx.get(i));

			while (i < position) {
				i++;
				m = m.getMediumAt(dirIdx.get(i));
			}

			return m;
		} else {
			return null;
		}
	}

	@Override
	public List<Medium> getMediaAt(int dirPosition) {
		if (dirPosition == 0) {
			return media;
		} else {
			Medium m = getMediumAt(dirPosition);
			if (m != null) {
				return m.getDir();
			} else {
				return new ArrayList<Medium>();
			}
		}
	}

	@Override
	public Loader<List<Medium>> onCreateLoader(int arg0, Bundle arg1) {
		if (arg0 == 0) {
			return new MediaLoader(this, mediaHost);
		} else {
			return null;
		}
	}

	@Override
	public void onLoadFinished(Loader<List<Medium>> arg0, List<Medium> arg1) {
		media = arg1;
		dirIdx.clear();
		dirIdx.add(0);
		dirPagerAdapter.notifyDataSetChanged();
		viewPager.setCurrentItem(FRAGMENT_MEDIA_ROOT);
		MediaListFragment mediaListFragment = (MediaListFragment) getSupportFragmentManager()
				.findFragmentByTag(getFragmentTag(FRAGMENT_MEDIA_ROOT));
		mediaListFragment.onMediaLoaded(media);
	}

	@Override
	public void onLoaderReset(Loader<List<Medium>> arg0) {
	}

	@Override
	public void onDeviceAvailable(CastDevice device, String arg1,
			MediaRouteStateChangeListener listener) {
		castDevice = device;
		mediaRouteStateChangeListener = listener;
		openSession();
	}

	private String getFragmentTag(int position) {
		return "android:switcher:" + R.id.pager + ":" + position;
	}

	@Override
	public void openDir(int parent, int child) {
		int currSize = dirIdx.size() - 1;

		while (currSize > parent) {
			dirIdx.remove(currSize);
			currSize--;
		}

		dirIdx.add(child);
		dirPagerAdapter.notifyDataSetChanged();
		parent++;
		viewPager.setCurrentItem(parent, true);
		MediaListFragment mediaListFragment = (MediaListFragment) getSupportFragmentManager()
				.findFragmentByTag(getFragmentTag(parent));
		mediaListFragment.onMediaLoaded(getMediaAt(parent));
	}

	@Override
	public void openMedium(int parent, int child) {
		Medium m = getMediaAt(parent).get(child);
		final ContentMetadata contentMetadata = new ContentMetadata();
		contentMetadata.setTitle(m.getFile().substring(m.getFile().lastIndexOf(File.separator) + 1));

		if (mediaProtocolMessageStream != null) {
			String urlStr = String.format("http://%s/%s", mediaHost,
					m.getFile());

			try {
				URL url = new URL(urlStr);
				URI uri = new URI(url.getProtocol(), url.getUserInfo(),
						url.getHost(), url.getPort(), url.getPath(),
						url.getQuery(), url.getRef());
				url = uri.toURL();
				MediaProtocolCommand cmd = mediaProtocolMessageStream
						.loadMedia(url.toString(), contentMetadata, true);
				cmd.setListener(new MediaProtocolCommand.Listener() {
					@Override
					public void onCompleted(MediaProtocolCommand mPCommand) {
						btnPlay.setText(getString(R.string.pause) + " "	+ contentMetadata.getTitle());
						onSetVolume(0.5);
					}

					@Override
					public void onCancelled(MediaProtocolCommand mPCommand) {
						btnPlay.setText(getString(R.string.play) + " " + contentMetadata.getTitle());
					}
				});
			} catch (IllegalStateException e) {
				Log.e(TAG,
						"Problem occurred with MediaProtocolCommand during loading",
						e);
			} catch (IOException e) {
				Log.e(TAG,
						"Problem opening MediaProtocolCommand during loading",
						e);
			} catch (URISyntaxException e) {
				Log.e(TAG, "Problem encoding URI: " + urlStr, e);
			}
		} else {
			Toast.makeText(this, "No message stream", Toast.LENGTH_SHORT)
					.show();
		}
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.btn_play) {
			if (castDevice != null) {
				if (mediaProtocolMessageStream != null) {
					String text = ((Button) v).getText().toString();
					if (text.startsWith(getString(R.string.play))) {
						Toast.makeText(this, getString(R.string.play), Toast.LENGTH_SHORT).show();

						try {
							if (seekBar.getProgress() > 0) {
								mediaProtocolMessageStream.resume();
							} else {
								mediaProtocolMessageStream.play();
							}
							btnPlay.setText(R.string.pause);
						} catch (IllegalStateException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else {
						Toast.makeText(this, getString(R.string.pause),	Toast.LENGTH_SHORT).show();

						try {
							mediaProtocolMessageStream.stop();
							btnPlay.setText(R.string.play);
						} catch (IllegalStateException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			} else {
				Toast.makeText(this, getString(R.string.select_cast_device),
						Toast.LENGTH_SHORT).show();
			}
		}
	}

	@Override
	public String getTitle(int dirPosition) {
		return dirPagerAdapter.getPageTitle(dirPosition).toString();
	}

	@Override
	public void onSetVolume(double volume) {
		try {
			mediaProtocolMessageStream.setVolume(volume);
		} catch (IllegalStateException e) {
			Log.e(TAG, "Problem sending Set Volume", e);
		} catch (IOException e) {
			Log.e(TAG, "Problem sending Set Volume", e);
		}
	}

	@Override
	public void onUpdateVolume(double volumeChange) {
		RouteInfo ri = mediaRouter.getSelectedRoute();

		if (ri != null) {
			ri.requestUpdateVolume((int) (volumeChange * MAX_VOLUME_LEVEL));
		}
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		int action = event.getAction();
		int keyCode = event.getKeyCode();
		switch (keyCode) {
		case KeyEvent.KEYCODE_VOLUME_UP:
			if (action == KeyEvent.ACTION_DOWN) {
				double currentVolume;

				if (mediaProtocolMessageStream != null) {
					currentVolume = mediaProtocolMessageStream.getVolume();

					if (currentVolume < 1.0) {
						onSetVolume(currentVolume + VOLUME_INCREMENT);
					}
				} else {
					Log.e(TAG, "dispatchKeyEvent - volume up - mMPMS==null");
				}
			}

			return true;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			if (action == KeyEvent.ACTION_DOWN) {
				double currentVolume;

				if (mediaProtocolMessageStream != null) {
					currentVolume = mediaProtocolMessageStream.getVolume();

					if (currentVolume > 0.0) {
						onSetVolume(currentVolume - VOLUME_INCREMENT);
					}
				} else {
					Log.e(TAG, "dispatchKeyEvent - volume down - mMPMS==null");
				}
			}
			return true;
		default:
			return super.dispatchKeyEvent(event);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();

		if (itemId == R.id.action_refresh) {
			if (mediaHost != null) {
				Loader<List<Medium>> loader = getSupportLoaderManager()
						.initLoader(0, null, this);
				if (loader != null) {
					loader.forceLoad();
				}
			}

			return true;
		} else if (itemId == android.R.id.home) {
			int tabIdx = viewPager.getCurrentItem();

			if (tabIdx > 0) {
				viewPager.setCurrentItem(--tabIdx);
			}

			return true;
		} else if (itemId == R.id.action_settings) {
			startActivityForResult(
					new Intent(this, SettingsActivity.class).putExtra(
							SettingsActivity.EXTRA_APP_ID, appId).putExtra(
							SettingsActivity.EXTRA_HOST, mediaHost), 0);
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
	}

	private Object isSeekingLock = new Object();
	private boolean isSeeking = false;

	@Override
	public void onStartTrackingTouch(SeekBar arg0) {
		synchronized (isSeekingLock) {
			isSeeking = true;
		}
		if (mediaProtocolMessageStream != null) {
			try {
				mediaProtocolMessageStream.stop();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		synchronized (isSeekingLock) {
			isSeeking = false;
		}
		if (mediaProtocolMessageStream != null) {
			try {
				mediaProtocolMessageStream.playFrom(seekBar.getProgress());
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	class CastStatusThread extends Thread {

		@Override
		public void run() {
			while (mediaProtocolMessageStream != null) {
				try {
					synchronized (isSeekingLock) {
						if (!isSeeking) {
							final int duration = (int) mediaProtocolMessageStream
									.getStreamDuration();
							final int position = (int) mediaProtocolMessageStream
									.getStreamPosition();
							MainActivity.this.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									if (seekBar.getMax() < duration) {
										seekBar.setMax(duration);
									}

									if (seekBar.getProgress() != position) {
										seekBar.setProgress(position);
									}
								}
							});
						}
					}
					Thread.sleep(1500);
				} catch (Exception e) {
					Log.e(TAG, "Thread interrupted: " + e);
				}
			}
		}

	}

	@Override
	public String getHost() {
		return mediaHost;
	}
}
