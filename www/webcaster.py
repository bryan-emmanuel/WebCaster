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

