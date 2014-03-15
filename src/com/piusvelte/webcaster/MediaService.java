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

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.media.AudioManager;
import android.os.IBinder;

public class MediaService extends Service {

	public static final String ACTION_PLAY = "com.piusvelte.webcaster.action.PLAY";
	public static final String ACTION_PAUSE = "com.piusvelte.webcaster.action.PAUSE";
	public static final String ACTION_STOP = "com.piusvelte.webcaster.action.STOP";

	private AudioManager mAudioManager;
	private ComponentName mMediaButtonReceiverComponent;

    @Override
    public void onCreate() {
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mMediaButtonReceiverComponent = new ComponentName(this, MediaButtonReceiver.class);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        return START_NOT_STICKY;
    }

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
