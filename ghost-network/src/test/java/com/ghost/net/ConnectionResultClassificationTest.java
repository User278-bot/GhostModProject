package com.ghost.net;

import com.ghost.net.ConnectionResult.FailureReason;
import org.junit.jupiter.api.Test;

import javax.crypto.AEADBadTagException;
import javax.net.ssl.SSLHandshakeException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link GhostWebSocketClient} および {@link ConnectionResult} の失敗原因分類テスト。
 */
class ConnectionResultClassificationTest {

    @Test
    void classifyCloseCode_MapsServerCustomCodesCorrectly() {
        assertEquals(FailureReason.AUTH_FAILED,
                GhostWebSocketClient.classifyCloseCode(GhostWebSocketClient.CLOSE_AUTH_FAILED, true));
        assertEquals(FailureReason.AUTH_FAILED,
                GhostWebSocketClient.classifyCloseCode(GhostWebSocketClient.CLOSE_DECRYPTION_FAILED, true));
        assertEquals(FailureReason.PROTOCOL_MISMATCH,
                GhostWebSocketClient.classifyCloseCode(GhostWebSocketClient.CLOSE_PROTOCOL_MISMATCH, true));
        assertEquals(FailureReason.UNREACHABLE,
                GhostWebSocketClient.classifyCloseCode(1006, false));
        assertEquals(FailureReason.CONNECTION_ERROR,
                GhostWebSocketClient.classifyCloseCode(1002, true));
    }

    @Test
    void classifyException_UnwrapsCausesCorrectly() {
        assertEquals(FailureReason.TLS_ERROR,
                GhostWebSocketClient.classifyException(
                        new ExecutionException(new SSLHandshakeException("PKIX path building failed")), false));

        assertEquals(FailureReason.UNREACHABLE,
                GhostWebSocketClient.classifyException(new ConnectException("Connection refused"), false));
        assertEquals(FailureReason.UNREACHABLE,
                GhostWebSocketClient.classifyException(new UnknownHostException("invalid.host"), false));

        assertEquals(FailureReason.AUTH_FAILED,
                GhostWebSocketClient.classifyException(
                        new ExecutionException(new SecurityException("bad password", new AEADBadTagException())), true));
    }

    @Test
    void connectionResult_InvariantsHold() {
        ConnectionResult ok = ConnectionResult.success();
        assertTrue(ok.connected());
        assertEquals(FailureReason.NONE, ok.failureReason());

        ConnectionResult fail = ConnectionResult.failure(FailureReason.AUTH_FAILED);
        assertFalse(fail.connected());
        assertEquals(FailureReason.AUTH_FAILED, fail.failureReason());

        // connected=false で NONE が渡された場合は CONNECTION_ERROR に正規化される
        ConnectionResult normalized = new ConnectionResult(false, FailureReason.NONE);
        assertEquals(FailureReason.CONNECTION_ERROR, normalized.failureReason());
    }
}
