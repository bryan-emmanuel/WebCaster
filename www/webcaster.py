"""
 WebCaster - Chromecast Web Media Library
 Copyright (C) 2013 Bryan Emmanuel

 This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.

  Bryan Emmanuel piusvelte@gmail.com
"""
import os
import re

def getMedia(cwd):
	media = []
	dirs = sorted(os.listdir(cwd))
	for file in dirs:
		src = cwd[len(media_root) + 1:]
		file_path = os.path.join(cwd, file)
		if (os.path.isfile(file_path)):
			if (re.search(video_ext, file)):
				img = '%sjpg' % file[:-3]
				if (not os.path.exists(os.path.join(cwd, img))):
					img = ''
				media.append('{"img":"%s","file":"%s","dir":[%s]}' % (os.path.join(src, img), os.path.join(src, file), ''))
		else:
			img = os.path.join(file, 'cover.jpg')
			if (not os.path.exists(os.path.join(cwd, img))):
				img = ''
			media.append('{"img":"%s/cover.jpg","file":"%s","dir":[%s]}' % (os.path.join(src, file), os.path.join(src, file), getMedia(os.path.join(cwd, file))))
	return ','.join(media)

def index(req):
	return '[%s]' % getMedia(media_root)

video_ext = '(\.avi$|\.mp4$|\.mpg$|\.mkv$)'
media_root = os.path.dirname(__file__)

