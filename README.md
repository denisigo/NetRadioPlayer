# NetRadioPlayer
Example Android app showing how to read, decode and play network radio stream.

It uses HTTPUrlConnection for connection to radio host, InputStream to read stream data,
native <a href="http://www.mpg123.de/">mpg123 decoder</a> to decode MPEG data to PCM, JNI lib to
access native decoder from Java, and AudioTrack to play decoded PCM data.