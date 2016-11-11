package org.lognet.springboot.grpc.register;

/**
 * Clone from https://github.com/rucky2013/grpc-spring-boot-consul.git
 * ServiceInfo
 */
public class ServiceInfo {

    public ServiceInfo(String name, int port, String version) {
        this.name = name;
        this.port = port;
        this.version = version;
    }

    private String name;
    private int port;
    private String version;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getVersion() {
        if (version.contains(".")) {
            version = version.replace(".", "_");
        }
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "ServiceInfo{" +
                " name='" + name + '\'' +
                ", port=" + port +
                ", version='" + version + '\'' +
                '}';
    }
}
