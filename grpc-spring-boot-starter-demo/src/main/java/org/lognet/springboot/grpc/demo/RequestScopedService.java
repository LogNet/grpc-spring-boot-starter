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

package org.lognet.springboot.grpc.demo;

import io.grpc.examples.RequestScopedGrpc;
import io.grpc.examples.RequestScopedOuterClass;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@GRpcService
public class RequestScopedService extends RequestScopedGrpc.RequestScopedImplBase {
    @Autowired
    private RandomUUID testRequestScopeBean;

    @Override
    public StreamObserver<RequestScopedOuterClass.RequestScopedMessage> requestScoped(StreamObserver<RequestScopedOuterClass.RequestScopedMessage> observer) {
        return new StreamObserver<RequestScopedOuterClass.RequestScopedMessage>() {
            @Override
            public void onNext(RequestScopedOuterClass.RequestScopedMessage value) {
                observer.onNext(value.toBuilder().setStr(RequestScopedService.this.testRequestScopeBean.getId()).build());
            }

            @Override
            public void onError(Throwable t) {
                observer.onError(t);
            }

            @Override
            public void onCompleted() {
                observer.onCompleted();
            }
        };
    }
}
