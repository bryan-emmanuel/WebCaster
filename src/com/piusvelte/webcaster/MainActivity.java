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

	private String mMediaHost = null;
	private String mAppId = null;
	private List<Medium> mMedia = new ArrayList<Medium>();
	private List<Integer> mDirIdx = new ArrayList<Integer>();
	private DirPagerAdapter mDirPagerAdapter;
	private ViewPager mViewPager;
	private CastContext mCastContext;
	private MediaRouter mMediaRouter;
	private MediaRouteSelector mMediaRouteSelector;
	private MediaRouteButton mMediaRouteButton;
	private MediaRouter.Callback mMediaRouterCallback;
	private CastDevice mCastDevice;
	private ApplicationSession mApplicationSession;
	private MediaProtocolMessageStream mMediaProtocolMessageStream;
	private Button mBtnPlay;
	private MediaRouteStateChangeListener mMediaRouteStateChangeListener;
	private SeekBar mSeekBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the app.
		mDirPagerAdapter = new DirPagerAdapter(getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mDirPagerAdapter);

		mCastContext = new CastContext(getApplicationContext());

		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(this);
		mMediaHost = sp.getString(getString(R.string.preference_host), null);
		mAppId = sp.getString(getString(R.string.preference_app_id), null);

		if (mAppId != null) {
			setupCasting();
		}

		mSeekBar = (SeekBar) findViewById(R.id.seek);
		mSeekBar.setOnSeekBarChangeListener(this);

		mBtnPlay = (Button) findViewById(R.id.btn_play);
		mBtnPlay.setOnClickListener(this);

		getActionBar().setDisplayHomeAsUpEnabled(true);

		if (mMediaHost != null) {
			getSupportLoaderManager().initLoader(0, null, this);
		}
	}

	private void setupCasting() {
		MediaRouteHelper.registerMinimalMediaRouteProvider(mCastContext, this);
		mMediaRouter = MediaRouter.getInstance(getApplicationContext());
		mMediaRouteSelector = MediaRouteHelper.buildMediaRouteSelector(
				MediaRouteHelper.CATEGORY_CAST, mAppId, null);

		mMediaRouterCallback = new MediaRouter.Callback() {
			@Override
			public void onRouteSelected(MediaRouter router, RouteInfo route) {
				MediaRouteHelper.requestCastDeviceForRoute(route);
			}

			@Override
			public void onRouteUnselected(MediaRouter router, RouteInfo route) {
				try {
					if (mApplicationSession != null) {
						mApplicationSession.setStopApplicationWhenEnding(true);
						mApplicationSession.endSession();
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

				mMediaProtocolMessageStream = null;
				mCastDevice = null;
				mMediaRouteStateChangeListener = null;
			}
		};
	}

	private void clearCasting() {
		try {
			if (mApplicationSession != null) {
				mApplicationSession.setStopApplicationWhenEnding(true);
				mApplicationSession.endSession();
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

		mMediaProtocolMessageStream = null;
		mCastDevice = null;
		mMediaRouteStateChangeListener = null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);

		MenuItem mediaRouteItem = menu.findItem(R.id.action_mediaroute);

		if (mMediaRouteSelector != null) {
			mMediaRouteButton = (MediaRouteButton) mediaRouteItem
					.getActionView();
			mMediaRouteButton.setRouteSelector(mMediaRouteSelector);
			mMediaRouteButton.setDialogFactory(new MediaRouteDialogFactory());
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

		if (mMediaRouter != null) {
			mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
					MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		if ((mAppId == null) || (mMediaHost == null)) {
			startActivityForResult(
					new Intent(this, SettingsActivity.class).putExtra(
							SettingsActivity.EXTRA_APP_ID, mAppId).putExtra(
							SettingsActivity.EXTRA_HOST, mMediaHost), 0);
		}

		if (mApplicationSession != null) {
			try {
				mApplicationSession.resumeSession();
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

			if (((resultAppId != null) && (!resultAppId.equals(mAppId)) || (mAppId != null))) {
				clearCasting();
				mAppId = resultAppId;

				if (mAppId != null) {
					setupCasting();
				}

				invalidateOptionsMenu();

			}

			String resultHost = data
					.getStringExtra(SettingsActivity.EXTRA_HOST);

			if (((resultHost != null) && (!resultHost.equals(mMediaHost)) || (mMediaHost != null))) {
				mMediaHost = resultHost;
				mMedia = new ArrayList<Medium>();
				mDirIdx.clear();
				mDirIdx.add(0);
				mDirPagerAdapter.notifyDataSetChanged();
				MediaLoader loader = (MediaLoader) getSupportLoaderManager()
						.initLoader(0, null, this);

				if (loader != null) {
					loader.loadHost(mMediaHost);
				}
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		PreferenceManager.getDefaultSharedPreferences(this).edit()
				.putString(getString(R.string.preference_app_id), mAppId)
				.putString(getString(R.string.preference_host), mMediaHost)
				.commit();
	}

	/**
	 * Closes a running session upon destruction of this Activity.
	 */
	@Override
	protected void onStop() {
		if (mMediaRouter != null) {
			mMediaRouter.removeCallback(mMediaRouterCallback);
		}

		super.onStop();
	}

	@Override
	protected void onDestroy() {
		if (mApplicationSession != null) {
			try {
				if (!mApplicationSession.hasStopped()) {
					mApplicationSession.endSession();
				}
			} catch (IOException e) {
				Log.e(TAG, "Failed to end session.");
			}
		}

		mApplicationSession = null;
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
			return mDirIdx.size();
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();

			if (position == FRAGMENT_MEDIA_ROOT) {
				return mMediaHost.toUpperCase(l);
			} else {
				Medium m = getMediumAt(position);
				if (m != null) {
					return m.getFile()
							.substring(
									m.getFile().lastIndexOf(File.separator) + 1)
							.toUpperCase(l);
				} else {
					return "";
				}
			}
		}
	}

	private void openSession() {
		mApplicationSession = new ApplicationSession(mCastContext, mCastDevice);

		int flags = 0;
		flags |= ApplicationSession.FLAG_DISABLE_NOTIFICATION;
		flags |= ApplicationSession.FLAG_DISABLE_LOCK_SCREEN_REMOTE_CONTROL;
		mApplicationSession.setApplicationOptions(flags);

		mApplicationSession.setListener(new ApplicationSession.Listener() {

			@Override
			public void onSessionStarted(ApplicationMetadata appMetadata) {
				ApplicationChannel channel = mApplicationSession.getChannel();

				if (channel == null) {
					Log.e(TAG, "channel = null");
					return;
				}

				if (mMediaProtocolMessageStream != null) {
					mMediaProtocolMessageStream = null;
				}

				mMediaProtocolMessageStream = new MediaProtocolMessageStream();
				channel.attachMessageStream(mMediaProtocolMessageStream);
				(new CastStatusThread()).start();

				PlayerState playerState = mMediaProtocolMessageStream
						.getPlayerState();

				if (PlayerState.PLAYING.equals(playerState)) {
					mBtnPlay.setText(getString(R.string.pause) + " "
							+ mMediaProtocolMessageStream.getTitle());
				} else if (PlayerState.STOPPED.equals(playerState)) {
					mBtnPlay.setText(getString(R.string.play) + " "
							+ mMediaProtocolMessageStream.getTitle());
				} else {
					Toast.makeText(getBaseContext(), "Select media",
							Toast.LENGTH_SHORT).show();
				}
			}

			@Override
			public void onSessionStartFailed(SessionError error) {
				Log.e(TAG, "onStartFailed " + error);
				mMediaProtocolMessageStream = null;
			}

			@Override
			public void onSessionEnded(SessionError error) {
				Log.i(TAG, "onEnded " + error);
				mMediaProtocolMessageStream = null;
			}
		});

		try {
			mApplicationSession.startSession(mAppId);
		} catch (IOException e) {
			Log.e(TAG, "Failed to open session", e);
		}
	}

	private Medium getMediumAt(int position) {
		if (position == 0) {
			return null;
		} else if (position < mDirIdx.size()) {
			int i = 1;
			Medium m = mMedia.get(mDirIdx.get(i));

			while (i < position) {
				i++;
				m = m.getMediumAt(mDirIdx.get(i));
			}

			return m;
		} else {
			return null;
		}
	}

	@Override
	public List<Medium> getMediaAt(int dirPosition) {
		if (dirPosition == 0) {
			return mMedia;
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
			return new MediaLoader(this, mMediaHost);
		} else {
			return null;
		}
	}

	@Override
	public void onLoadFinished(Loader<List<Medium>> arg0, List<Medium> arg1) {
		mMedia = arg1;
		mDirIdx.clear();
		mDirIdx.add(0);
		mDirPagerAdapter.notifyDataSetChanged();
		mViewPager.setCurrentItem(FRAGMENT_MEDIA_ROOT);
		MediaListFragment mediaListFragment = (MediaListFragment) getSupportFragmentManager()
				.findFragmentByTag(getFragmentTag(FRAGMENT_MEDIA_ROOT));
		mediaListFragment.onMediaLoaded(mMedia);
	}

	@Override
	public void onLoaderReset(Loader<List<Medium>> arg0) {
	}

	@Override
	public void onDeviceAvailable(CastDevice device, String arg1,
			MediaRouteStateChangeListener listener) {
		mCastDevice = device;
		mMediaRouteStateChangeListener = listener;
		openSession();
	}

	private String getFragmentTag(int position) {
		return "android:switcher:" + R.id.pager + ":" + position;
	}

	@Override
	public void openDir(int parent, int child) {
		int currSize = mDirIdx.size() - 1;

		while (currSize > parent) {
			mDirIdx.remove(currSize);
			currSize--;
		}

		mDirIdx.add(child);
		mDirPagerAdapter.notifyDataSetChanged();
		parent++;
		mViewPager.setCurrentItem(parent, true);
		MediaListFragment mediaListFragment = (MediaListFragment) getSupportFragmentManager()
				.findFragmentByTag(getFragmentTag(parent));
		mediaListFragment.onMediaLoaded(getMediaAt(parent));
	}

	@Override
	public void openMedium(int parent, int child) {
		Medium m = getMediaAt(parent).get(child);
		final ContentMetadata contentMetadata = new ContentMetadata();
		contentMetadata.setTitle(m.getFile().substring(
				m.getFile().lastIndexOf(File.separator) + 1));

		if (mMediaProtocolMessageStream != null) {
			String urlStr = String.format("http://%s/%s", mMediaHost,
					m.getFile());

			try {
				URL url = new URL(urlStr);
				URI uri = new URI(url.getProtocol(), url.getUserInfo(),
						url.getHost(), url.getPort(), url.getPath(),
						url.getQuery(), url.getRef());
				url = uri.toURL();
				MediaProtocolCommand cmd = mMediaProtocolMessageStream
						.loadMedia(url.toString(), contentMetadata, true);
				cmd.setListener(new MediaProtocolCommand.Listener() {
					@Override
					public void onCompleted(MediaProtocolCommand mPCommand) {
						mBtnPlay.setText(getString(R.string.pause) + " "
								+ contentMetadata.getTitle());
						onSetVolume(0.5);
					}

					@Override
					public void onCancelled(MediaProtocolCommand mPCommand) {
						mBtnPlay.setText(getString(R.string.play) + " "
								+ contentMetadata.getTitle());
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
			if (mCastDevice != null) {
				if (mMediaProtocolMessageStream != null) {
					String text = ((Button) v).getText().toString();
					if (text.startsWith(getString(R.string.play))) {
						Toast.makeText(this, getString(R.string.play),
								Toast.LENGTH_SHORT).show();

						try {
							if (mSeekBar.getProgress() > 0) {
								mMediaProtocolMessageStream.resume();
							} else {
								mMediaProtocolMessageStream.play();
							}
							mBtnPlay.setText(R.string.pause);
						} catch (IllegalStateException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else {
						Toast.makeText(this, getString(R.string.pause),
								Toast.LENGTH_SHORT).show();

						try {
							mMediaProtocolMessageStream.stop();
							mBtnPlay.setText(R.string.play);
						} catch (IllegalStateException e) {
							e.printStackTrace();
						} catch (IOException e) {
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
		return mDirPagerAdapter.getPageTitle(dirPosition).toString();
	}

	@Override
	public void onSetVolume(double volume) {
		try {
			mMediaProtocolMessageStream.setVolume(volume);
			mMediaRouteStateChangeListener.onVolumeChanged(volume);
		} catch (IllegalStateException e) {
			Log.e(TAG, "Problem sending Set Volume", e);
		} catch (IOException e) {
			Log.e(TAG, "Problem sending Set Volume", e);
		}
	}

	@Override
	public void onUpdateVolume(double volumeChange) {
		RouteInfo ri = mMediaRouter.getSelectedRoute();

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

				if (mMediaProtocolMessageStream != null) {
					currentVolume = mMediaProtocolMessageStream.getVolume();

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

				if (mMediaProtocolMessageStream != null) {
					currentVolume = mMediaProtocolMessageStream.getVolume();

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
			if (mMediaHost != null) {
				Loader<List<Medium>> loader = getSupportLoaderManager()
						.initLoader(0, null, this);
				if (loader != null) {
					loader.forceLoad();
				}
			}

			return true;
		} else if (itemId == android.R.id.home) {
			int tabIdx = mViewPager.getCurrentItem();

			if (tabIdx > 0) {
				mViewPager.setCurrentItem(--tabIdx);
			}

			return true;
		} else if (itemId == R.id.action_settings) {
			startActivityForResult(
					new Intent(this, SettingsActivity.class).putExtra(
							SettingsActivity.EXTRA_APP_ID, mAppId).putExtra(
							SettingsActivity.EXTRA_HOST, mMediaHost), 0);
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

		if (mMediaProtocolMessageStream != null) {
			try {
				mMediaProtocolMessageStream.stop();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		synchronized (isSeekingLock) {
			isSeeking = false;
		}
		if (mMediaProtocolMessageStream != null) {
			try {
				mMediaProtocolMessageStream.playFrom(seekBar.getProgress());
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	class CastStatusThread extends Thread {

		@Override
		public void run() {
			while (mMediaProtocolMessageStream != null) {
				try {
					synchronized (isSeekingLock) {
						if (!isSeeking) {
							final int duration = (int) mMediaProtocolMessageStream
									.getStreamDuration();
							final int position = (int) mMediaProtocolMessageStream
									.getStreamPosition();
							MainActivity.this.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									if (mSeekBar.getMax() < duration) {
										mSeekBar.setMax(duration);
									}

									if (mSeekBar.getProgress() != position) {
										mSeekBar.setProgress(position);
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
		return mMediaHost;
	}
}
