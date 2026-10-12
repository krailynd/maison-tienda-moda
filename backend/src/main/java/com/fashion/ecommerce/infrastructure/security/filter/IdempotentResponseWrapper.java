package com.fashion.ecommerce.infrastructure.security.filter;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import lombok.Getter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Captures the response body so the {@link IdempotencyFilter} can cache it
 * (moved to the canonical package, design D7).
 */
@Getter
public class IdempotentResponseWrapper extends HttpServletResponseWrapper {

    private final HttpServletResponse response;
    private ByteArrayOutputStream byteArrayOutputStream;
    private PrintWriter printWriter;
    private ServletOutputStream outputStream;

    public IdempotentResponseWrapper(HttpServletResponse response) {
        super(response);
        this.response = response;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (printWriter != null) {
            throw new IllegalStateException("getWriter() was already called");
        }
        if (outputStream == null) {
            byteArrayOutputStream = new ByteArrayOutputStream();
            outputStream = new CustomServletOutputStream(byteArrayOutputStream);
        }
        return outputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (outputStream != null) {
            throw new IllegalStateException("getOutputStream() was already called");
        }
        if (printWriter == null) {
            byteArrayOutputStream = new ByteArrayOutputStream();
            printWriter = new PrintWriter(new OutputStreamWriter(byteArrayOutputStream, "UTF-8"));
        }
        return printWriter;
    }

    @Override
    public void flushBuffer() throws IOException {
        if (printWriter != null) {
            printWriter.flush();
        }
        if (outputStream != null) {
            outputStream.flush();
        }
        super.flushBuffer();
    }

    public String getResponseBody() throws IOException {
        if (printWriter != null) {
            printWriter.flush();
        }
        if (byteArrayOutputStream != null) {
            return byteArrayOutputStream.toString("UTF-8");
        }
        return null;
    }

    private static class CustomServletOutputStream extends ServletOutputStream {
        private final ByteArrayOutputStream outputStream;

        public CustomServletOutputStream(ByteArrayOutputStream outputStream) {
            this.outputStream = outputStream;
        }

        @Override
        public void write(int b) throws IOException {
            outputStream.write(b);
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            // Not needed for this use case
        }
    }
}
