/*
 * Copyright 2016-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lognet.springboot.grpc;

import io.grpc.Server;
import io.grpc.ServerServiceDefinition;

/**
 * Creates a gRPC server with all the gRPC services added to the registry. The server
 * returned is ready to be started by the caller.
 * @author Ray Tsang
 */
public interface GrpcServerFactory {
	Server createServer();

	String getAddress();

	int getPort();

	void addService(ServerServiceDefinition service);
}
