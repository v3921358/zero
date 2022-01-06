package lib.bin;

import tools.data.ByteArrayByteStream;
import tools.data.LittleEndianAccessor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReadBin {

    private FileInputStream fis;
    private ByteArrayByteStream bis;
    private LittleEndianAccessor dis;

    public ReadBin(String file) {
        try {
            this.fis = new FileInputStream("resources/bin/" + file);
        } catch (FileNotFoundException e) {
            throw new UnsupportedOperationException("Bin loading failed! The file '" + file + "' could not be found.");
        }
        try {
            byte[] b = new byte[fis.available()];
            fis.read(b);
            this.bis = new ByteArrayByteStream(b);
            this.dis = new LittleEndianAccessor(bis);
        } catch (IOException ex) {
            Logger.getLogger(ReadBin.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Read a byte from the input stream.
     *
     * @throws IOException
     */
    public byte readByte() throws IOException {
        return dis.readByte();
    }

    /**
     * Read a boolean from the input stream.
     *
     * @return
     * @throws IOException
     */
    public boolean readBool() throws IOException {
        return dis.readByte() > 0;
    }

    /**
     * Read a short from the input stream.
     *
     * @throws IOException
     */
    public short readShort() throws IOException {
        return dis.readShort();
    }

    /**
     * Read an integer from the input stream.
     *
     * @throws IOException
     */
    public int readInt() throws IOException {
        return dis.readInt();
    }

    /**
     * Read a long from the input stream.
     *
     * @throws IOException
     */
    public long readLong() throws IOException {
        return dis.readLong();
    }

    /**
     * Read a double from the input stream.
     *
     * @throws IOException
     */
    public double readDouble() throws IOException {
        return dis.readDouble();
    }

    /**
     * Read a float from the input stream.
     *
     * @throws IOException
     */
    public float readFloat() throws IOException {
        return dis.readFloat();
    }

    /**
     * Read a string from the input stream.
     *
     * @throws IOException
     */
    public String readString() throws IOException {
        return dis.readMapleAsciiString();
    }

    /**
     * Skip a set amount of bytes.
     *
     * @param n
     * @throws IOException
     */
    public void skip(int n) throws IOException {
        dis.skip(n);
    }

    /**
     * Close all the output streams.
     *
     * @throws IOException
     */
    public void close() throws IOException {
        // dis.();
        //  dis.close();
        fis.close();
    }

}
