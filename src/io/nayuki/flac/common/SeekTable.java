package io.nayuki.flac.common;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import io.nayuki.flac.encode.BitOutputStream;


public final class SeekTable {
	
	/*---- Fields ----*/
	
	public List<SeekPoint> points;
	
	
	
	/*---- Constructors ----*/
	
	public SeekTable() {
		points = new ArrayList<>();
	}
	
	
	public SeekTable(byte[] b) {
		this();
		Objects.requireNonNull(b);
		if (b.length % 18 != 0)
			throw new IllegalArgumentException("Data contains a partial seek point");
		try {
			DataInput in = new DataInputStream(new ByteArrayInputStream(b));
			for (int i = 0; i < b.length; i += 18) {
				SeekPoint p = new SeekPoint();
				p.sampleOffset = in.readLong();
				p.fileOffset = in.readLong();
				p.frameSamples = in.readUnsignedShort();
				points.add(p);
			}
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}
	
	
	
	/*---- Methods ----*/
	
	public void checkValues() {
		for (SeekPoint p : points) {
			if ((p.frameSamples & 0xFFFF) != p.frameSamples)
				throw new IllegalStateException("Frame samples outside uint16 range");
		}
		for (int i = 1; i < points.size(); i++) {
			SeekPoint p = points.get(i);
			if (p.sampleOffset != -1) {
				SeekPoint q = points.get(i - 1);
				if (p.sampleOffset <= q.sampleOffset)
					throw new IllegalStateException("Sample offsets out of order");
				if (p.sampleOffset < q.sampleOffset)
					throw new IllegalStateException("File offsets out of order");
			}
		}
	}
	
	
	public void write(boolean last, BitOutputStream out) throws IOException {
		if (points.size() > ((1 << 24) - 1) / 18)
			throw new IllegalStateException("Too many seek points");
		checkValues();
		out.writeInt(1, last ? 1 : 0);
		out.writeInt(7, 3);
		out.writeInt(24, points.size() * 18);
		for (SeekPoint p : points) {
			out.writeInt(32, (int)(p.sampleOffset >>> 32));
			out.writeInt(32, (int)(p.sampleOffset >>>  0));
			out.writeInt(32, (int)(p.fileOffset >>> 32));
			out.writeInt(32, (int)(p.fileOffset >>>  0));
			out.writeInt(16, p.frameSamples);
		}
	}
	
	
	
	/*---- Helper structure ----*/
	
	public static final class SeekPoint {
		
		public long sampleOffset;  // uint64 value, and -1 means placeholder point
		public long fileOffset;    // uint64 value
		public int frameSamples;   // uint16 value
		
	}
	
}