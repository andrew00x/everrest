package org.everrest.core.impl.provider;

import com.google.common.io.ByteStreams;
import org.everrest.core.impl.FileCollector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author andrew00x
 */
public class IOHelperTest {
    private byte[] testContent;

    @Before
    public void setUp() throws Exception {
        Random random = new Random();
        testContent = new byte[64];
        random.nextBytes(testContent);
    }

    @After
    public void tearDown() throws Exception {
        FileCollector.getInstance().clean();
    }

    @Test
    public void buffersGivenInputStreamInMemoryWhenLengthDoesNotExceedThreshold() throws Exception {
        InputStream inputStream = new ByteArrayInputStream(testContent);
        InputStream bufferedStream = IOHelper.bufferStream(inputStream, testContent.length + 1);

        assertTrue(bufferedStream instanceof ByteArrayInputStream);
        assertArrayEquals(testContent, ByteStreams.toByteArray(bufferedStream));
    }

    @Test
    public void buffersGivenInputStreamOnFileSystemWhenLengthExceedsThreshold() throws Exception {
        InputStream inputStream = new ByteArrayInputStream(testContent);
        InputStream bufferedStream = IOHelper.bufferStream(inputStream, testContent.length - 1);

        assertTrue(bufferedStream instanceof FileInputStream);
        try {
            assertArrayEquals(testContent, ByteStreams.toByteArray(bufferedStream));
        } finally {
            bufferedStream.close();
        }
    }

    @Test
    public void testsIsEmptyWhenStreamSupportsMarkReset() throws Exception {
        InputStream entity = mock(InputStream.class);
        when(entity.markSupported()).thenReturn(true);
        when(entity.read()).thenReturn(1);
        assertFalse(IOHelper.isEmpty(entity));
        verify(entity).mark(1);
    }

    @Test
    public void testsIsEmptyWhenStreamDoesNotSupportMarkReset() throws Exception {
        InputStream entity = mock(InputStream.class);
        when(entity.available()).thenReturn(1);
        assertFalse(IOHelper.isEmpty(entity));
    }

    @Test
    public void testsIsEmptyReturnsFalseWhenStreamIsNull() {
        assertTrue(IOHelper.isEmpty(null));
    }
}