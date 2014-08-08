package rename;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;

import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;

public class M3uFile {

	private static final String	OUTPUT_NAME		= "allfiles.m3u";

	private static final String	HEADER			= "#EXTM3U";
	private static final String	TRACK_INFO_FMT	= "#EXTINF:%d,%s - %s";

	private static final String	MP3				= ".mp3";

	private PrintWriter			m3uWriter;

	public static void main(String[] args) throws IOException {
		if (args.length != 0) {
			File rootDirectory = new File(args[0]);
			if (!rootDirectory.exists()) {
				System.out.println(String.format("The directory %s does not exist", args[0]));
			}
			else if (!rootDirectory.isDirectory()) {
				System.out.println(String.format("The directory %s is not a directory", args[0]));
			}
			else if (!rootDirectory.canRead()) {
				System.out.println(String.format("Cannot read directory %s", args[0]));
			}
			else if (!rootDirectory.canWrite()) {
				System.out.println(String.format("Cannot write directory %s", args[0]));
			}
			else {
				System.out.println("Create all.m3u file in " + rootDirectory);
				new M3uFile().create(rootDirectory);

			}
		}
		else {
			System.out.println("You have to provide the root directory");
		}
	}

	private void create(File rootDirectory) throws IOException {
		m3uWriter = new PrintWriter(new File(rootDirectory, OUTPUT_NAME));
		try {
			m3uWriter.println(HEADER);
			searchDirectory(rootDirectory);
		}
		finally {
			m3uWriter.close();
		}

	}

	private void searchDirectory(File files) throws IOException {
		for (File file : files.listFiles()) {
			if (file.isDirectory()) {
				searchDirectory(file);
			}
			else {
				writeM3uTag(file);
			}
		}
	}

	private void writeM3uTag(File file) throws IOException {
		if (file.getName().endsWith(MP3)) {
			writeMp3Tag(file);
		}
		else if (file.getName().endsWith(".m4a")) {
			writeTag(file);
		}

	}

	private void writeMp3Tag(File file) throws IOException {
		try {
			Mp3File mp3File = new Mp3File(file.getCanonicalPath());

			m3uWriter.println(String.format(TRACK_INFO_FMT, mp3File.getLengthInSeconds(), getArtist(mp3File),
						getTitle(mp3File)));
			m3uWriter.println(file.getPath());
			m3uWriter.println("");
		}
		catch (UnsupportedTagException ite) {
			throw new IOException(String.format("File %s has an unsupported tag.", file), ite);
		}
		catch (InvalidDataException ide) {
			throw new IOException(String.format("File %s has invalid data.", file), ide);
		}
	}

	private String getArtist(Mp3File mp3File) {
		ID3v1 id3v1Tag = mp3File.getId3v1Tag();
		return id3v1Tag == null ? "" : id3v1Tag.getArtist();
	}

	private Object getTitle(Mp3File mp3File) {
		ID3v1 id3v1Tag = mp3File.getId3v1Tag();
		return id3v1Tag == null ? "" : id3v1Tag.getTitle();
	}

	private void writeTag(File file) throws IOException {
		try {
			AudioFile audioFile = AudioFileIO.read(file);

			m3uWriter.println(String.format(TRACK_INFO_FMT, getTrackLength(audioFile), getArtist(audioFile),
						getTitle(audioFile)));
			m3uWriter.println(file.getPath());
			m3uWriter.println("");
		}
		catch (CannotReadException cre) {
			throw new IOException(String.format("Cannot read %s.", file), cre);
		}
		catch (TagException e) {
			throw new IOException(String.format("Cannot read %s.", file), e);
		}
		catch (ReadOnlyFileException e) {
			throw new IOException(String.format("Cannot read %s.", file), e);
		}
		catch (InvalidAudioFrameException e) {
			throw new IOException(String.format("Cannot read %s.", file), e);
		}
	}

	private int getTrackLength(AudioFile audioFile) {
		return audioFile.getAudioHeader().getTrackLength();
	}

	private String getArtist(AudioFile audioFile) {
		String artist = audioFile.getTag().getFirst(FieldKey.ARTIST);
		if (artist == null || artist.length() == 0) {
			artist = audioFile.getTag().getFirst(FieldKey.ALBUM_ARTIST);
		}

		return artist;
	}

	private String getTitle(AudioFile audioFile) {
		return audioFile.getTag().getFirst(FieldKey.TITLE);
	}
}
