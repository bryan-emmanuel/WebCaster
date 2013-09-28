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
