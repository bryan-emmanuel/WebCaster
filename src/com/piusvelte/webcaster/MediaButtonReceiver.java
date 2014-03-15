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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

public class MediaButtonReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(Intent.ACTION_MEDIA_BUTTON)) {
			KeyEvent keyEvent = (KeyEvent) intent.getExtras().get(
					Intent.EXTRA_KEY_EVENT);
			if (keyEvent.getAction() != KeyEvent.ACTION_DOWN)
				return;

			switch (keyEvent.getKeyCode()) {
			case KeyEvent.KEYCODE_MEDIA_PLAY:
				context.startService(new Intent(MediaService.ACTION_PLAY));
				break;
			case KeyEvent.KEYCODE_MEDIA_PAUSE:
				context.startService(new Intent(MediaService.ACTION_PAUSE));
				break;
			case KeyEvent.KEYCODE_MEDIA_STOP:
				context.startService(new Intent(MediaService.ACTION_STOP));
				break;
			}
		}
	}

}
