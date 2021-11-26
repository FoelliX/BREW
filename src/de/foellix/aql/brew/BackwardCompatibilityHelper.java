package de.foellix.aql.brew;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;

import de.foellix.aql.Log;
import de.foellix.aql.helper.FileHelper;

public class BackwardCompatibilityHelper {
	public static File updateFile(File oldSerializedFile) {
		try {
			final File output = FileHelper.makeUnique(new File(oldSerializedFile.getParentFile(),
					oldSerializedFile.getName().replace(".ser", "_updated-0.ser")));
			Files.write(output.toPath(),
					changePathInSerializedFile(oldSerializedFile, "de.foellix.aql.ggwiz", "de.foellix.aql.brew"));
			return output;
		} catch (final IOException e) {
			Log.error("Could not update file: " + oldSerializedFile.getAbsolutePath() + Log.getExceptionAppendix(e));
			return null;
		}
	}

	static public byte[] changePathInSerializedFile(File f, String fromPath, String toPath) throws IOException {
		final byte[] buffer = new byte[(int) f.length()];
		final FileInputStream in = new FileInputStream(f);
		in.read(buffer);
		in.close();
		return changePathInSerializedData(buffer, fromPath, toPath);
	}

	static public byte[] changePathInSerializedData(byte[] buffer, String fromPath, String toPath) throws IOException {
		final byte[] search = fromPath.getBytes("UTF-8");
		final byte[] replace = toPath.getBytes("UTF-8");

		final ByteArrayOutputStream f = new ByteArrayOutputStream();

		for (int i = 0; i < buffer.length; i++) {
			boolean found = false;
			final int searchMaxIndex = i + search.length + 2;
			if (searchMaxIndex <= buffer.length) {
				found = true;
				for (int j = i + 2; j < searchMaxIndex; j++) {
					if (search[j - i - 2] != buffer[j]) {
						found = false;
						break;
					}
				}
			}
			if (found) {
				final int high = ((buffer[i]) & 0xff);
				final int low = ((buffer[i + 1]) & 0xff);
				int classNameLength = (high << 8) + low;
				classNameLength += replace.length - search.length;
				f.write((classNameLength >> 8) & 0xff);
				f.write((classNameLength) & 0xff);
				f.write(replace);
				i = searchMaxIndex - 1;
			} else {
				f.write(buffer[i]);
			}
		}

		f.flush();
		f.close();

		return f.toByteArray();
	}
}
