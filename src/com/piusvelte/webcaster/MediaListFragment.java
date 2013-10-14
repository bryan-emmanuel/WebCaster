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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.squareup.picasso.Picasso;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class MediaListFragment extends ListFragment {

	private static final String TAG = "MediaListFragment";
	public static final String EXTRA_DIR_POSITION = "com.piusvelte.webcaster.EXTRA_DIR_POSITION";

	int dirPosition = 0;
	MediaListAdapter adapter;
	private Listener callback;

	interface Listener {

		List<Medium> getMediaAt(int dirPosition);

		void openDir(int parent, int child);

		void openMedium(int parent, int child);

		String getTitle(int dirPosition);

		String getHost();

	}

	public void onMediaLoaded(List<Medium> media) {
		adapter.clear();
		adapter.addAll(media);
		adapter.notifyDataSetChanged();
	}

	class MediaListAdapter extends ArrayAdapter<Medium> {

		public MediaListAdapter(Context context, int textViewResourceId,
				List<Medium> rowMedium) {
			super(context, textViewResourceId, rowMedium);
		}

		@Override
		public View getView(int position, View v, ViewGroup parent) {
			MediaViewHolder mvh;

			if (v == null) {
				v = (View) (LayoutInflater.from(parent.getContext())).inflate(
						R.layout.media_list_item, null);
				mvh = new MediaViewHolder();
				mvh.imgCover = (ImageView) v.findViewById(R.id.cover);
				mvh.tvFile = (TextView) v.findViewById(R.id.title);
				mvh.imgMore = (ImageView) v.findViewById(R.id.more);
				v.setTag(mvh);
			} else {
				mvh = (MediaViewHolder) v.getTag();
			}

			Medium m = adapter.getItem(position);

			if ((m.getImg() != null) && (m.getImg().length() > 0)) {
				String urlStr = String.format("http://%s/%s",
						callback.getHost(), m.getImg());

				try {
					URL url = new URL(urlStr);
					URI uri = new URI(url.getProtocol(), url.getUserInfo(),
							url.getHost(), url.getPort(), url.getPath(),
							url.getQuery(), url.getRef());
					url = uri.toURL();
					Picasso.with(getContext())
							.load(url.toString())
							.resizeDimen(R.dimen.cover_width,
									R.dimen.cover_height)
							.placeholder(android.R.drawable.ic_menu_rotate)
							.error(android.R.drawable.ic_menu_close_clear_cancel)
							.into(mvh.imgCover);
				} catch (MalformedURLException e) {
					Log.e(TAG, "Problem parsing url: " + urlStr, e);
				} catch (URISyntaxException e) {
					Log.e(TAG, "Problem encoding URI: " + urlStr, e);
				}
			} else {
				Picasso.with(getContext())
						.load(android.R.drawable.ic_menu_close_clear_cancel)
						.into(mvh.imgCover);
			}

			String title = m.getFile().substring(m.getFile().lastIndexOf(File.separator) + 1);
			int l = title.length() - 4;

			if ((l > 0) && ".".equals(title.substring(l, (l + 1)))) {
				title = title.substring(0, l);
			}

			mvh.tvFile.setText(title);

			if (m.getDir().size() > 0) {
				mvh.imgMore.setVisibility(View.VISIBLE);
			} else {
				mvh.imgMore.setVisibility(View.GONE);
			}

			return v;
		}

	}

	static class MediaViewHolder {

		ImageView imgCover;
		TextView tvFile;
		ImageView imgMore;

	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			callback = (Listener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString());
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View rootView = inflater.inflate(R.layout.media_list, container, false);
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		adapter = new MediaListAdapter(getActivity(), R.layout.media_list_item,
				new ArrayList<Medium>());
		setListAdapter(adapter);
		Bundle extras = getArguments();

		if (extras != null) {
			dirPosition = extras.getInt(EXTRA_DIR_POSITION, 0);
		}
	}

	@Override
	public void onListItemClick(ListView list, View view, int position, long id) {
		super.onListItemClick(list, view, position, id);

		if (callback != null) {
			Medium m = adapter.getItem(position);

			if (m.getDir().size() > 0) {
				callback.openDir(dirPosition, position);
			} else if (m.getFile() != null) {
				callback.openMedium(dirPosition, position);
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		if (adapter.isEmpty() && (callback != null)) {
			onMediaLoaded(callback.getMediaAt(dirPosition));
		}
	}

}
