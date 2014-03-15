WebCaster allows browsing and casting videos over HTTP.

Whitelist your chromecast device, using your server as the receiver URL,
 such as http://192.168.0.1/receiver.html

Update your whitelist app id in the www/receiver.html file. This is the same file as
 provided in the cast sample project. Copy this file to the server location used in
 whitelisting.

Copy the www/webcaster.py file to your webserver in the same directory as the videos.

Note: the videos need to be in a format supported by Chromecast https://developers.google.com/cast/supported_media_types

In the app settings, provide the URL for the directory containing the videos and the
 webcaster.py file, excluding protocol, such as 192.168.0.1. Also provide the
 whitelist app id.

Movie Covers:

Covers are supported, and should be jpg's, measuring 200x300 pixels.

For an individual video, use the same name but with a jpg extension:
/Movie.mp4
/Movie.jpg

For a directory of videos, use cover.jpg:
/Season 1/cover.jpg

Bitcoin address: 14cHZwUCuXbYtciL8j22QEamHKxtt5uZnV


