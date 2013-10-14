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

import java.util.List;

public class Medium {

	private String img;
	private String file;
	private List<Medium> dir;

	public String getImg() {
		return img;
	}

	public void setImg(String img) {
		this.img = img;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public List<Medium> getDir() {
		return dir;
	}

	public void setDir(List<Medium> dir) {
		this.dir = dir;
	}

	public Medium getMediumAt(int i) {
		if (i < dir.size()) {
			return dir.get(i);
		} else {
			return null;
		}
	}

}
