/*
 * Copyright (c) 2016-2021 Michael Zhang <yidongnan@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.lognet.springboot.grpc;

import io.grpc.examples.RequestScopedGrpc;
import io.grpc.examples.RequestScopedOuterClass;
import io.grpc.stub.StreamObserver;
import junit.framework.AssertionFailedError;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.demo.DemoApp;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApp.class}, webEnvironment = NONE)
@DirtiesContext
public class GrpcRequestScopeTest extends GrpcServerTestBase {

    @Test
    @DirtiesContext
    public void testScope() throws InterruptedException {
        // Prepare
        RequestScopedGrpc.RequestScopedStub requestScopedServiceStub = RequestScopedGrpc.newStub(channel);
        ScopedStreamObserverChecker scope1 = new ScopedStreamObserverChecker();
        StreamObserver<RequestScopedOuterClass.RequestScopedMessage> request1 = requestScopedServiceStub.requestScoped(scope1);
        ScopedStreamObserverChecker scope2 = new ScopedStreamObserverChecker();
        StreamObserver<RequestScopedOuterClass.RequestScopedMessage> request2 = requestScopedServiceStub.requestScoped(scope2);

        // Run
        request1.onNext(RequestScopedOuterClass.RequestScopedMessage.getDefaultInstance());
        request1.onNext(RequestScopedOuterClass.RequestScopedMessage.getDefaultInstance());
        Thread.sleep(150);

        request2.onNext(RequestScopedOuterClass.RequestScopedMessage.getDefaultInstance());
        request2.onNext(RequestScopedOuterClass.RequestScopedMessage.getDefaultInstance());
        Thread.sleep(150);

        request1.onNext(RequestScopedOuterClass.RequestScopedMessage.getDefaultInstance());
        request2.onNext(RequestScopedOuterClass.RequestScopedMessage.getDefaultInstance());
        Thread.sleep(150);

        request2.onNext(RequestScopedOuterClass.RequestScopedMessage.getDefaultInstance());
        request1.onNext(RequestScopedOuterClass.RequestScopedMessage.getDefaultInstance());
        Thread.sleep(150);

        request1.onCompleted();
        request2.onCompleted();
        Thread.sleep(150);

        // Assert
        assertTrue(scope1.isCompleted());
        assertTrue(scope2.isCompleted());
        assertNull(scope1.getError());
        assertNull(scope2.getError());
        assertNotNull(scope1.getText());
        assertNotNull(scope2.getText());
        assertNotEquals(scope1.getText(), scope2.getText());
    }


    /**
     * Helper class used to check that the scoped responses are different per request, but the same for different
     * messages in the same request.
     */
    private static class ScopedStreamObserverChecker implements StreamObserver<RequestScopedOuterClass.RequestScopedMessage> {

        private String text;
        private boolean completed = false;
        private Throwable error;

        @Override
        public void onNext(RequestScopedOuterClass.RequestScopedMessage value) {
            if (this.text == null) {
                this.text = value.getStr();
            }
            try {
                assertEquals(this.text, value.getStr());
            } catch (AssertionFailedError e) {
                if (this.error == null) {
                    this.error = e;
                } else {
                    this.error.addSuppressed(e);
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (this.error == null) {
                this.error = t;
            } else {
                this.error.addSuppressed(t);
            }
            this.completed = true;
        }

        @Override
        public void onCompleted() {
            this.completed = true;
        }

        public String getText() {
            return this.text;
        }

        public boolean isCompleted() {
            return this.completed;
        }

        public Throwable getError() {
            return this.error;
        }

    }
}
