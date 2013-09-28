package com.piusvelte.webcaster;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.gson.Gson;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

public class MediaLoader extends AsyncTaskLoader<List<Medium>> {

	private static final String TAG = "MediaLoader";
	private static final String MEDIA_LIBRARY_URL_FORMAT = "http://%s/webcaster.py";

	List<Medium> media = null;
	URL mediaUrl = null;

	public MediaLoader(Context context, String host) {
		super(context);

		if (host != null) {

			try {
				mediaUrl = new URL(
						String.format(MEDIA_LIBRARY_URL_FORMAT, host));
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void loadHost(String host) {
		if (host != null) {
			try {
				mediaUrl = new URL(
						String.format(MEDIA_LIBRARY_URL_FORMAT, host));
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			forceLoad();
		} else {
			mediaUrl = null;
		}
	}

	@Override
	public List<Medium> loadInBackground() {
		List<Medium> media = new ArrayList<Medium>();
		if (mediaUrl != null) {
			HttpURLConnection httpURLConnection;
			try {
				String response;
				httpURLConnection = (HttpURLConnection) mediaUrl
						.openConnection();
				InputStream in = new BufferedInputStream(
						httpURLConnection.getInputStream());
				byte[] buffer = new byte[512];
				ByteArrayOutputStream content = new ByteArrayOutputStream();
				int readBytes = 0;
				while ((readBytes = in.read(buffer)) != -1) {
					content.write(buffer, 0, readBytes);
				}
				response = new String(content.toByteArray());
				JSONParser jsonParser = new JSONParser();
				JSONArray mediaJArr = (JSONArray) jsonParser.parse(response);
				Gson gson = new Gson();
				for (int i = 0, s = mediaJArr.size(); i < s; i++) {
					media.add(gson.fromJson(mediaJArr.get(i).toString(),
							Medium.class));
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return media;
	}

	@Override
	public void deliverResult(List<Medium> media) {
		this.media = media;
		if (isStarted()) {
			super.deliverResult(media);
		}
	}

	@Override
	protected void onStartLoading() {
		if (takeContentChanged() || (media == null)) {
			forceLoad();
		} else if (media != null) {
			deliverResult(media);
		}
	}

	@Override
	protected void onReset() {
		super.onReset();
		onStopLoading();
		media = null;
	}

	@Override
	protected void onStopLoading() {
		cancelLoad();
	}

}
