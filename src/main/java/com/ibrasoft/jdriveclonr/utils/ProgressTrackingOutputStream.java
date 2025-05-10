package com.ibrasoft.jdriveclonr.utils;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.LongConsumer;

public class ProgressTrackingOutputStream extends FilterOutputStream {
    private final LongConsumer byteWrittenConsumer;

    public ProgressTrackingOutputStream(OutputStream out, LongConsumer byteWrittenConsumer) {
        super(out);
        this.byteWrittenConsumer = byteWrittenConsumer;
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
        byteWrittenConsumer.accept(1);
    }

    @Override
    public void write(byte[] b) throws IOException {
        out.write(b);
        byteWrittenConsumer.accept(b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        byteWrittenConsumer.accept(len);
    }
}
